package com.example.audio

import android.content.Context
import android.database.Cursor
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

data class SoundChoice(
    val id: String,
    val name: String,
    val category: SoundCategory,
    val uriString: String? = null
)

enum class SoundCategory {
    PRESET,
    DEVICE_SYSTEM,
    USER_CUSTOM
}

object StudyBellSoundManager {
    private const val TAG = "StudyBellSound"
    private var previewPlayer: MediaPlayer? = null
    private var alarmPlayer: MediaPlayer? = null
    private var synthJob: Job? = null

    val PRESET_SOUNDS = listOf(
        SoundChoice("preset_classic_bell", "Classic Bell", SoundCategory.PRESET),
        SoundChoice("preset_school_bell", "School Bell", SoundCategory.PRESET),
        SoundChoice("preset_digital_alarm", "Digital Alarm", SoundCategory.PRESET),
        SoundChoice("preset_soft_alarm", "Soft Alarm", SoundCategory.PRESET),
        SoundChoice("preset_morning_tone", "Morning Tone", SoundCategory.PRESET)
    )

    fun getDeviceSystemSounds(context: Context): List<SoundChoice> {
        val list = mutableListOf<SoundChoice>()
        try {
            val ringtoneMgr = RingtoneManager(context)
            ringtoneMgr.setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE)
            val cursor: Cursor? = ringtoneMgr.cursor
            if (cursor != null) {
                var count = 0
                while (cursor.moveToNext() && count < 25) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX)
                    val uri = ringtoneMgr.getRingtoneUri(cursor.position)
                    if (!title.isNullOrBlank() && uri != null) {
                        list.add(
                            SoundChoice(
                                id = "sys_$id",
                                name = title,
                                category = SoundCategory.DEVICE_SYSTEM,
                                uriString = uri.toString()
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device system sounds", e)
        }
        return list
    }

    // Save picked local audio (voice recording, music file) to app's local internal storage
    fun saveCustomAudio(context: Context, uri: Uri): SoundChoice? {
        return try {
            val soundsDir = File(context.filesDir, "sounds")
            if (!soundsDir.exists()) soundsDir.mkdirs()
            val fileName = "custom_sound_${System.currentTimeMillis()}.mp3"
            val destFile = File(soundsDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val displayName = resolveAudioDisplayName(context, uri) ?: "Local Audio / Voice Recording"
            SoundChoice(
                id = "custom_${System.currentTimeMillis()}",
                name = displayName,
                category = SoundCategory.USER_CUSTOM,
                uriString = destFile.absolutePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy custom audio", e)
            null
        }
    }

    private fun resolveAudioDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) cursor.getString(nameIdx) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun playSound(context: Context, soundChoice: SoundChoice, loop: Boolean = false, onCompletion: () -> Unit = {}) {
        stopAllSounds()
        try {
            if (soundChoice.category == SoundCategory.USER_CUSTOM && soundChoice.uriString != null) {
                val file = File(soundChoice.uriString)
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    isLooping = loop
                    setOnCompletionListener { onCompletion() }
                    prepare()
                    start()
                }
                if (loop) alarmPlayer = player else previewPlayer = player
                return
            }

            if (soundChoice.category == SoundCategory.DEVICE_SYSTEM && soundChoice.uriString != null) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    setDataSource(context, Uri.parse(soundChoice.uriString))
                    isLooping = loop
                    setOnCompletionListener { onCompletion() }
                    prepare()
                    start()
                }
                if (loop) alarmPlayer = player else previewPlayer = player
                return
            }

            // Presets: Generate procedural tone harmonics via AudioTrack
            playPresetTone(soundChoice.name, loop, onCompletion)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound: ${soundChoice.name}", e)
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val player = MediaPlayer.create(context, defaultUri).apply {
                    isLooping = loop
                    setOnCompletionListener { onCompletion() }
                    start()
                }
                if (loop) alarmPlayer = player else previewPlayer = player
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback also failed", ex)
            }
        }
    }

    private fun playPresetTone(presetName: String, loop: Boolean, onCompletion: () -> Unit) {
        synthJob?.cancel()
        synthJob = CoroutineScope(Dispatchers.Default).launch {
            val sampleRate = 44100
            val frequencies = when (presetName) {
                "School Bell" -> doubleArrayOf(880.0, 1108.7, 1318.5, 1760.0)
                "Digital Alarm" -> doubleArrayOf(950.0, 1200.0)
                "Soft Alarm" -> doubleArrayOf(523.25, 659.25, 783.99)
                "Morning Tone" -> doubleArrayOf(440.0, 554.37, 659.25, 880.0)
                else -> doubleArrayOf(784.0, 987.77, 1174.66)
            }

            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate * 2)

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            try {
                audioTrack.play()
                val iterations = if (loop) Int.MAX_VALUE else 3
                for (iter in 0 until iterations) {
                    if (!isActive) break
                    val durationSeconds = 1.0
                    val numSamples = (durationSeconds * sampleRate).toInt()
                    val samples = ShortArray(numSamples)

                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        var sample = 0.0
                        for (freq in frequencies) {
                            sample += sin(2.0 * Math.PI * freq * t)
                        }
                        sample /= frequencies.size
                        val envelope = Math.exp(-3.0 * t)
                        samples[i] = (sample * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
                    }
                    audioTrack.write(samples, 0, samples.size)
                    delay(300)
                }
            } finally {
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
                withContext(Dispatchers.Main) {
                    onCompletion()
                }
            }
        }
    }

    fun stopAllSounds() {
        synthJob?.cancel()
        synthJob = null
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {}
        previewPlayer = null

        try {
            alarmPlayer?.stop()
            alarmPlayer?.release()
        } catch (_: Exception) {}
        alarmPlayer = null
    }
}
