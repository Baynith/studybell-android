package com.example.timer

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.alarm.StudyBellNotificationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PomodoroMode(val displayName: String) {
    WORK("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class PomodoroUiState(
    val mode: PomodoroMode = PomodoroMode.WORK,
    val isRunning: Boolean = false,
    val secondsRemaining: Int = 25 * 60,
    val totalSecondsForCurrentMode: Int = 25 * 60,
    val completedWorkSessions: Int = 0,
    val totalMinutesFocusedToday: Int = 0,
    val selectedSubject: String = "Mathematics",
    val workDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val longBreakInterval: Int = 4,
    val soundAlerts: Boolean = true,
    val vibrationAlerts: Boolean = true
)

class StudyTimerManager(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    fun updateSettings(
        workMinutes: Int,
        breakMinutes: Int,
        longBreakMinutes: Int,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean
    ) {
        val current = _uiState.value
        val safeWork = workMinutes.coerceIn(1, 180)
        val safeBreak = breakMinutes.coerceIn(1, 60)
        val safeLongBreak = longBreakMinutes.coerceIn(1, 90)

        val newTotalSeconds = when (current.mode) {
            PomodoroMode.WORK -> safeWork * 60
            PomodoroMode.SHORT_BREAK -> safeBreak * 60
            PomodoroMode.LONG_BREAK -> safeLongBreak * 60
        }

        val newRemaining = if (!current.isRunning) newTotalSeconds else current.secondsRemaining.coerceAtMost(newTotalSeconds)

        _uiState.value = current.copy(
            workDurationMinutes = safeWork,
            breakDurationMinutes = safeBreak,
            longBreakDurationMinutes = safeLongBreak,
            totalSecondsForCurrentMode = newTotalSeconds,
            secondsRemaining = newRemaining,
            soundAlerts = soundEnabled,
            vibrationAlerts = vibrateEnabled
        )
    }

    fun setSubject(subject: String) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
    }

    fun selectMode(mode: PomodoroMode) {
        stopTimer()
        val current = _uiState.value
        val durationMinutes = when (mode) {
            PomodoroMode.WORK -> current.workDurationMinutes
            PomodoroMode.SHORT_BREAK -> current.breakDurationMinutes
            PomodoroMode.LONG_BREAK -> current.longBreakDurationMinutes
        }
        val totalSecs = durationMinutes * 60
        _uiState.value = current.copy(
            mode = mode,
            secondsRemaining = totalSecs,
            totalSecondsForCurrentMode = totalSecs,
            isRunning = false
        )
        StudyBellNotificationManager.clearStudyTimerNotification(context)
    }

    fun toggleStartPause() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        _uiState.value = _uiState.value.copy(isRunning = true)
        updateOngoingNotification()

        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (isActive && _uiState.value.secondsRemaining > 0) {
                delay(1000L)
                val remaining = _uiState.value.secondsRemaining - 1
                _uiState.value = _uiState.value.copy(secondsRemaining = remaining)
                updateOngoingNotification()
            }

            if (_uiState.value.secondsRemaining <= 0) {
                handleSessionFinished()
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(isRunning = false)
        updateOngoingNotification()
    }

    fun resetTimer() {
        stopTimer()
        val current = _uiState.value
        val totalSecs = when (current.mode) {
            PomodoroMode.WORK -> current.workDurationMinutes * 60
            PomodoroMode.SHORT_BREAK -> current.breakDurationMinutes * 60
            PomodoroMode.LONG_BREAK -> current.longBreakDurationMinutes * 60
        }
        _uiState.value = current.copy(
            secondsRemaining = totalSecs,
            totalSecondsForCurrentMode = totalSecs,
            isRunning = false
        )
        StudyBellNotificationManager.clearStudyTimerNotification(context)
    }

    fun skipSession() {
        stopTimer()
        transitionToNextMode(userSkipped = true)
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    private fun handleSessionFinished() {
        stopTimer()
        val current = _uiState.value

        // Trigger sound & vibration alerts
        if (current.soundAlerts) {
            playTransitionSound()
        }
        if (current.vibrationAlerts) {
            playTransitionVibration(isWorkFinished = current.mode == PomodoroMode.WORK)
        }

        transitionToNextMode(userSkipped = false)
    }

    private fun transitionToNextMode(userSkipped: Boolean) {
        val current = _uiState.value

        if (current.mode == PomodoroMode.WORK) {
            val newCompleted = current.completedWorkSessions + (if (!userSkipped) 1 else 0)
            val newTotalMinutes = current.totalMinutesFocusedToday + (if (!userSkipped) current.workDurationMinutes else 0)
            val isLongBreakTime = newCompleted > 0 && (newCompleted % current.longBreakInterval == 0)

            val nextMode = if (isLongBreakTime) PomodoroMode.LONG_BREAK else PomodoroMode.SHORT_BREAK
            val nextTotalSecs = if (isLongBreakTime) current.longBreakDurationMinutes * 60 else current.breakDurationMinutes * 60

            _uiState.value = current.copy(
                mode = nextMode,
                secondsRemaining = nextTotalSecs,
                totalSecondsForCurrentMode = nextTotalSecs,
                completedWorkSessions = newCompleted,
                totalMinutesFocusedToday = newTotalMinutes,
                isRunning = false
            )

            // Alert user of transition
            StudyBellNotificationManager.showAlarmNotification(
                context = context,
                notificationId = 9101,
                channelId = StudyBellNotificationManager.CHANNEL_STUDY_TIMER,
                title = "🎉 Focus Session Finished!",
                subtitle = "Great work! Time for a ${if (isLongBreakTime) current.longBreakDurationMinutes else current.breakDurationMinutes}-minute break.",
                details = "Studied: ${current.selectedSubject}. Sessions completed: $newCompleted.",
                categoryName = "STUDY",
                canComplete = false,
                canSnooze = false
            )
        } else {
            // Break finished -> Transition to WORK
            val nextTotalSecs = current.workDurationMinutes * 60
            _uiState.value = current.copy(
                mode = PomodoroMode.WORK,
                secondsRemaining = nextTotalSecs,
                totalSecondsForCurrentMode = nextTotalSecs,
                isRunning = false
            )

            StudyBellNotificationManager.showAlarmNotification(
                context = context,
                notificationId = 9102,
                channelId = StudyBellNotificationManager.CHANNEL_STUDY_TIMER,
                title = "⏰ Break Finished!",
                subtitle = "Ready to start your next focus session on ${current.selectedSubject}?",
                details = "Session duration: ${current.workDurationMinutes} minutes.",
                categoryName = "STUDY",
                canComplete = false,
                canSnooze = false
            )
        }

        StudyBellNotificationManager.clearStudyTimerNotification(context)
    }

    private fun updateOngoingNotification() {
        val current = _uiState.value
        val progress = if (current.totalSecondsForCurrentMode > 0) {
            (((current.totalSecondsForCurrentMode - current.secondsRemaining).toFloat() / current.totalSecondsForCurrentMode.toFloat()) * 100).toInt()
        } else 0

        val minutes = current.secondsRemaining / 60
        val seconds = current.secondsRemaining % 60
        val formatted = "%02d:%02d".format(minutes, seconds)

        StudyBellNotificationManager.showStudyTimerOngoing(
            context = context,
            modeTitle = current.mode.displayName,
            subject = current.selectedSubject,
            remainingFormatted = formatted,
            isRunning = current.isRunning,
            progressPercent = progress
        )
    }

    private fun playTransitionSound() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            ringtone?.play()
        } catch (_: Exception) {
        }
    }

    private fun playTransitionVibration(isWorkFinished: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = if (isWorkFinished) {
                    longArrayOf(0, 400, 200, 400, 200, 600)
                } else {
                    longArrayOf(0, 300, 200, 300)
                }
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(500L)
            }
        } catch (_: Exception) {
        }
    }
}
