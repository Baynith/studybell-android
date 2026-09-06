package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timer.PomodoroMode
import com.example.timer.PomodoroUiState
import com.example.ui.components.Card3D
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTimerScreen(
    timerState: PomodoroUiState,
    availableSubjects: List<String>,
    onToggleStartPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onSelectMode: (PomodoroMode) -> Unit,
    onSelectSubject: (String) -> Unit,
    onUpdateSettings: (workMins: Int, breakMins: Int, longBreakMins: Int, sound: Boolean, vibrate: Boolean) -> Unit
) {
    var showConfigDialog by remember { mutableStateOf(false) }

    val minutes = timerState.secondsRemaining / 60
    val seconds = timerState.secondsRemaining % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    val progressFraction = if (timerState.totalSecondsForCurrentMode > 0) {
        (timerState.secondsRemaining.toFloat() / timerState.totalSecondsForCurrentMode.toFloat())
    } else 1f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "pomodoro_gauge")

    val accentColor = when (timerState.mode) {
        PomodoroMode.WORK -> PurplePrimary
        PomodoroMode.SHORT_BREAK -> EmeraldGreen
        PomodoroMode.LONG_BREAK -> GoldDark
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Study Session Timer",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Pomodoro focus & rest intervals",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showConfigDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("study_timer_settings_button")
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Timer Settings", tint = PurplePrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subject Picker
        val subjectsList = remember(availableSubjects) {
            val list = availableSubjects.toMutableList()
            if (!list.contains("Mathematics")) list.add(0, "Mathematics")
            if (!list.contains("General Study")) list.add("General Study")
            list.distinct()
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(subjectsList) { subj ->
                val isSelected = timerState.selectedSubject.equals(subj, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectSubject(subj) },
                    label = { Text(subj, fontSize = 12.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PomodoroMode.values().forEach { mode ->
                val isSelected = timerState.mode == mode
                val label = when (mode) {
                    PomodoroMode.WORK -> "Focus (${timerState.workDurationMinutes}m)"
                    PomodoroMode.SHORT_BREAK -> "Short Break (${timerState.breakDurationMinutes}m)"
                    PomodoroMode.LONG_BREAK -> "Long Break (${timerState.longBreakDurationMinutes}m)"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Circular Countdown Gauge
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                // Track
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = strokeWidth)
                )
                // Progress Arc
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (timerState.isRunning) timerState.mode.displayName.uppercase() else "PAUSED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = timerState.selectedSubject,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session Progress Dots (e.g. 4 dots for sessions until long break)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentCycleSession = timerState.completedWorkSessions % timerState.longBreakInterval
            for (i in 1..timerState.longBreakInterval) {
                val isCompleted = i <= currentCycleSession
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) GoldAccent else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (isCompleted) GoldDark else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Session ${currentCycleSession + 1} of ${timerState.longBreakInterval}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            OutlinedIconButton(
                onClick = onReset,
                shape = CircleShape,
                modifier = Modifier.size(52.dp).testTag("timer_reset_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Timer")
            }

            // Big 3D Play/Pause Button
            Button(
                onClick = onToggleStartPause,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier
                    .height(64.dp)
                    .width(140.dp)
                    .testTag("timer_play_pause_button")
            ) {
                Icon(
                    imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (timerState.isRunning) "Pause" else "Start",
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (timerState.isRunning) "Pause" else "Start",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Skip Session Button
            OutlinedIconButton(
                onClick = onSkip,
                shape = CircleShape,
                modifier = Modifier.size(52.dp).testTag("timer_skip_button")
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Skip Session")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Metric Card
        Card3D(elevation = 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Today's Focus Time", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${timerState.completedWorkSessions} completed sessions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PurplePrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${timerState.totalMinutesFocusedToday} mins",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Timer Settings Dialog (Customizable durations & alert settings)
    if (showConfigDialog) {
        var workMins by remember { mutableIntStateOf(timerState.workDurationMinutes) }
        var breakMins by remember { mutableIntStateOf(timerState.breakDurationMinutes) }
        var longBreakMins by remember { mutableIntStateOf(timerState.longBreakDurationMinutes) }
        var sound by remember { mutableStateOf(timerState.soundAlerts) }
        var vibrate by remember { mutableStateOf(timerState.vibrationAlerts) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Customize Study Timer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Work Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Work Interval", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("$workMins minutes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (workMins > 5) workMins -= 5 }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text("$workMins", fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                            IconButton(onClick = { if (workMins < 120) workMins += 5 }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }

                    // Break Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Short Break", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("$breakMins minutes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (breakMins > 1) breakMins -= 1 }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text("$breakMins", fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                            IconButton(onClick = { if (breakMins < 30) breakMins += 1 }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }

                    // Long Break Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Long Break", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("$longBreakMins minutes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (longBreakMins > 5) longBreakMins -= 5 }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text("$longBreakMins", fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                            IconButton(onClick = { if (longBreakMins < 60) longBreakMins += 5 }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                    // Sound alert toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound Alerts on Transitions", fontSize = 13.sp)
                        Switch(checked = sound, onCheckedChange = { sound = it })
                    }

                    // Vibration alert toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration on Transitions", fontSize = 13.sp)
                        Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showConfigDialog = false
                    onUpdateSettings(workMins, breakMins, longBreakMins, sound, vibrate)
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
