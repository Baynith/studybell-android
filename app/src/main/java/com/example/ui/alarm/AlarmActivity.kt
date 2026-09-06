package com.example.ui.alarm

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.alarm.StudyBellNotificationManager
import com.example.audio.SoundCategory
import com.example.audio.SoundChoice
import com.example.audio.StudyBellSoundManager
import com.example.data.db.AppDatabase
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDark
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.StudyBellTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen up and show when locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 1001)
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Science Class"
        val subtitle = intent.getStringExtra("EXTRA_SUBTITLE") ?: "Class starts in 15 minutes"
        val details = intent.getStringExtra("EXTRA_DETAILS") ?: "Mr. John • Lab 2"
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "CLASS"
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", 0L)
        val defaultSnoozeMins = intent.getIntExtra("EXTRA_SNOOZE_MINUTES", 10)

        // Start ringing sound & vibration from local storage/presets
        startAlarmFeedback()

        setContent {
            StudyBellTheme(darkTheme = true) {
                AlarmRingingView(
                    title = title,
                    subtitle = subtitle,
                    details = details,
                    category = category,
                    defaultSnoozeMinutes = defaultSnoozeMins,
                    onSnooze = { minutes ->
                        stopAlarmFeedback()
                        val scheduler = AlarmScheduler(this)
                        scheduler.snoozeAlarm(
                            reminderId = reminderId,
                            snoozeMinutes = minutes,
                            category = category,
                            title = title
                        )
                        StudyBellNotificationManager.cancelNotification(this, notificationId)
                        finish()
                    },
                    onDismiss = {
                        stopAlarmFeedback()
                        StudyBellNotificationManager.cancelNotification(this, notificationId)
                        finish()
                    }
                )
            }
        }
    }

    private fun startAlarmFeedback() {
        // 1. Play sound
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@AlarmActivity)
            val settings = db.appSettingsDao().getSettingsDirect()
            val soundSetting = settings?.defaultAlarmSound ?: "Classic Bell"

            val soundChoice = if (soundSetting.contains("|")) {
                val parts = soundSetting.split("|")
                val name = parts.getOrNull(0) ?: "Custom Sound"
                val uri = parts.getOrNull(1)
                SoundChoice("custom", name, SoundCategory.USER_CUSTOM, uri)
            } else if (soundSetting.startsWith("sys_")) {
                SoundChoice("sys", soundSetting, SoundCategory.DEVICE_SYSTEM)
            } else {
                SoundChoice("preset", soundSetting, SoundCategory.PRESET)
            }

            StudyBellSoundManager.playSound(this@AlarmActivity, soundChoice, loop = true)
        }

        // 2. Play vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 800, 400, 800), 0)
            }
        } catch (_: Exception) {}
    }

    private fun stopAlarmFeedback() {
        StudyBellSoundManager.stopAllSounds()
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmFeedback()
    }
}

@Composable
fun AlarmRingingView(
    title: String,
    subtitle: String,
    details: String,
    category: String = "CLASS",
    defaultSnoozeMinutes: Int = 10,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTime = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm"))
    }
    val currentDate = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    }

    // Bell vibrating 3D animation
    val infiniteTransition = rememberInfiniteTransition(label = "bell_shake")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    var showSnoozeOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070C18),
                        DarkBackground,
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)
        ) {
            // Top lock-screen clock
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentTime,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentDate,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Central 3D Card (matching Image 1 Screen 2 & Image 2 Screen 12)
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.25f),
                                PurplePrimary.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 3D Hero Visual Asset
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        PurplePrimary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val isScience = title.contains("Science", ignoreCase = true) || category == "CLASS"
                        val isHomework = category == "HOMEWORK"

                        val imageRes = when {
                            isScience -> R.drawable.science_flask_3d_1788614104425
                            isHomework -> R.drawable.homework_stack_3d_1788614156639
                            else -> R.drawable.alarm_ringing_3d_1788614121508
                        }

                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "3D Alarm Asset",
                            modifier = Modifier
                                .size(110.dp)
                                .rotate(if (imageRes == R.drawable.alarm_ringing_3d_1788614121508) rotation else 0f)
                                .shadow(12.dp, RoundedCornerShape(20.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = GoldAccent.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.padding(top = 14.dp)
                    ) {
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Bottom Actions: Snooze and Dismiss (Image 1 Screen 2)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Snooze Button
                    Button(
                        onClick = { onSnooze(defaultSnoozeMinutes) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyDark,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .testTag("alarm_snooze_button")
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Snooze $defaultSnoozeMinutes min",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Dismiss Button
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("alarm_dismiss_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dismiss", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // Snooze duration quick options toggle
                TextButton(
                    onClick = { showSnoozeOptions = !showSnoozeOptions },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (showSnoozeOptions) "Hide Snooze Durations" else "More Snooze Durations",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }

                if (showSnoozeOptions) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val snoozeChoices = listOf(5, 10, 15, 20, 30)
                        items(snoozeChoices) { mins ->
                            FilledTonalButton(
                                onClick = { onSnooze(mins) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("$mins m", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
