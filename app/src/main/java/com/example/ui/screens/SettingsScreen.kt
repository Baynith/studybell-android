package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.alarm.AlarmPermissionHelper
import com.example.ui.components.Card3D
import com.example.ui.auth.AuthDialog
import com.example.ui.theme.CoralPink
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldDark
import com.example.ui.theme.PurplePrimary
import com.example.ui.viewmodel.StudyBellUiState
import com.example.ui.viewmodel.StudyBellViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    uiState: StudyBellUiState,
    viewModel: StudyBellViewModel,
    onNavigateToStats: () -> Unit,
    onNavigateToAlarmSounds: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showJsonExportDialog by remember { mutableStateOf(false) }
    var showPomodoroSettingsDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    val authUser by viewModel.authUserState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var canScheduleExact by remember { mutableStateOf(AlarmPermissionHelper.canScheduleExactAlarms(context)) }
    var hasNotificationPermission by remember { mutableStateOf(AlarmPermissionHelper.isNotificationPermissionGranted(context)) }
    var isBatteryOptimizedIgnored by remember { mutableStateOf(AlarmPermissionHelper.isBatteryOptimizationIgnored(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExact = AlarmPermissionHelper.canScheduleExactAlarms(context)
                hasNotificationPermission = AlarmPermissionHelper.isNotificationPermissionGranted(context)
                isBatteryOptimizedIgnored = AlarmPermissionHelper.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Activity launcher to import backup JSON from local phone storage
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    viewModel.importDataJson(text)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Alarms, snooze, study timer and notifications",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Student Profile Card
        item {
            Card3D(
                elevation = 3,
                onClick = { showProfileDialog = true },
                modifier = Modifier.testTag("settings_profile_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PurplePrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.student_avatar_3d_1788614139283),
                            contentDescription = "Student 3D Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.profile.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.profile.schoolName} • ${uiState.profile.gradeClass}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PurplePrimary)
                }
            }
        }

        // 1b. Firebase Cloud Account Card
        item {
            Card3D(
                elevation = 2,
                modifier = Modifier.testTag("settings_firebase_account_card")
            ) {
                if (authUser.isLoggedIn) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldGreen)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = authUser.displayName.ifBlank { "Student" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "FIREBASE",
                                                color = EmeraldGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = authUser.email,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.authManager.signOut()
                                    Toast.makeText(context, "Signed out of Firebase", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPink.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_firebase_sign_out")
                            ) {
                                Text("Sign Out", color = CoralPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UID: ${authUser.uid.take(12)}...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { showAuthDialog = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Switch Account", fontSize = 12.sp, color = PurplePrimary)
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PurplePrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = PurplePrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Firebase Cloud Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real Firebase authentication via google-services.json",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showAuthDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("btn_open_firebase_auth")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In or Sign Up with Firebase", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Exact Alarms & System Permissions Section (Offline-First Timing)
        item {
            Text(
                text = "SYSTEM PERMISSIONS & TIMING ACCURACY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        item {
            Card3D(elevation = 2) {
                // Exact Alarm Scheduling
                SettingRow(
                    icon = Icons.Default.Schedule,
                    title = "Exact Alarms (Android 12+)",
                    value = if (canScheduleExact) "Granted (Exact second precision)" else "Tap to Grant (Required for bells)",
                    onClick = { AlarmPermissionHelper.openExactAlarmSettings(context) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // Notifications
                SettingRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications & Lock Screen",
                    value = if (hasNotificationPermission) "Allowed (Banners & full-screen alerts)" else "Tap to Allow (Disabled)",
                    onClick = { AlarmPermissionHelper.openNotificationSettings(context) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // Battery Saver Whitelist
                SettingRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Optimization Whitelist",
                    value = if (isBatteryOptimizedIgnored) "Unrestricted (Safe from Doze mode)" else "Tap to Allow Background Bells",
                    onClick = { AlarmPermissionHelper.requestIgnoreBatteryOptimizations(context) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // Re-sync all alarms
                SettingRow(
                    icon = Icons.Default.Sync,
                    title = "Synchronize & Reschedule Alarms",
                    value = "Re-arm all class, homework & exam bells",
                    onClick = { viewModel.rescheduleAllAlarms() }
                )
            }
        }

        // 2. Alarm & Sound Section
        item {
            Text(
                text = "ALARM & SNOOZE SETTINGS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        item {
            Card3D(elevation = 2) {
                // Sound (3D & Local Device Audio Selection)
                SettingRow(
                    icon = Icons.Default.MusicNote,
                    title = "Alarm Sound",
                    value = uiState.settings.defaultAlarmSound.substringBefore("|"),
                    onClick = onNavigateToAlarmSounds
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // Custom Snooze Duration Setting
                SettingRow(
                    icon = Icons.Default.Snooze,
                    title = "Custom Snooze Duration",
                    value = "${uiState.settings.customSnoozeMinutes} min (Customizable)",
                    onClick = { showSnoozeDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Vibration", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = uiState.settings.vibrationEnabled,
                        onCheckedChange = { viewModel.updateSettings(uiState.settings.copy(vibrationEnabled = it)) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // Instant Alarm Test
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Instant Alarm Test", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Test sound, vibration, and full-screen alert", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.testAlarm(5) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Test (5s)", fontSize = 12.sp)
                    }
                }
            }
        }

        // 3. Persistent Notifications Section
        item {
            Text(
                text = "PERSISTENT NOTIFICATIONS & TRACKERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        item {
            Card3D(elevation = 2) {
                // Persistent Class Reminder Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Next Class Lock-Screen Banner", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Show upcoming class pinned on status bar & lock screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 28.dp, top = 2.dp))
                    }
                    Switch(
                        checked = uiState.settings.persistentClassRemindersEnabled,
                        onCheckedChange = { viewModel.togglePersistentClassReminders(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // Persistent Exam Countdown Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = CoralPink, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upcoming Exam Countdown Banner", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Show countdown days for nearest exam on lock screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 28.dp, top = 2.dp))
                    }
                    Switch(
                        checked = uiState.settings.persistentExamRemindersEnabled,
                        onCheckedChange = { viewModel.togglePersistentExamReminders(it) }
                    )
                }
            }
        }

        // 4. Study Session (Pomodoro) Settings
        item {
            Text(
                text = "POMODORO STUDY TIMER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        item {
            Card3D(elevation = 2) {
                SettingRow(
                    icon = Icons.Default.HourglassTop,
                    title = "Work & Break Intervals",
                    value = "${uiState.settings.pomodoroWorkMinutes}m work / ${uiState.settings.pomodoroBreakMinutes}m break",
                    onClick = { showPomodoroSettingsDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = GoldDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sound Alerts on Transitions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = uiState.settings.pomodoroSoundEnabled,
                        onCheckedChange = {
                            viewModel.updateSettings(uiState.settings.copy(pomodoroSoundEnabled = it))
                            viewModel.studyTimerManager.updateSettings(
                                uiState.settings.pomodoroWorkMinutes,
                                uiState.settings.pomodoroBreakMinutes,
                                uiState.settings.pomodoroLongBreakMinutes,
                                it,
                                uiState.settings.pomodoroVibrateEnabled
                            )
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Vibration on Transitions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = uiState.settings.pomodoroVibrateEnabled,
                        onCheckedChange = {
                            viewModel.updateSettings(uiState.settings.copy(pomodoroVibrateEnabled = it))
                            viewModel.studyTimerManager.updateSettings(
                                uiState.settings.pomodoroWorkMinutes,
                                uiState.settings.pomodoroBreakMinutes,
                                uiState.settings.pomodoroLongBreakMinutes,
                                uiState.settings.pomodoroSoundEnabled,
                                it
                            )
                        }
                    )
                }
            }
        }

        // 5. Appearance & Preferences
        item {
            Text(
                text = "APPEARANCE & UI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        item {
            Card3D(elevation = 2) {
                // Theme Mode
                SettingRow(
                    icon = Icons.Default.Palette,
                    title = "App Theme",
                    value = uiState.settings.themeMode,
                    onClick = { showThemeDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // 3D Effects Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = GoldDark, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("3D Visual Style", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = uiState.settings.effects3dEnabled,
                        onCheckedChange = { viewModel.updateSettings(uiState.settings.copy(effects3dEnabled = it)) }
                    )
                }
            }
        }

        // 6. Statistics Entry
        item {
            Card3D(
                elevation = 2,
                onClick = onNavigateToStats
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Student Statistics", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("View weekly classes and homework completion rate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 7. Data & Backup
        item {
            Text(
                text = "DATA & BACKUP (OFFLINE)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        item {
            Card3D(elevation = 2) {
                SettingRow(
                    icon = Icons.Default.FileDownload,
                    title = "Export Local Backup (JSON)",
                    value = "Export data",
                    onClick = {
                        coroutineScope.launch {
                            val db = com.example.data.db.AppDatabase.getDatabase(context)
                            val repo = com.example.data.repository.StudyBellRepository(db)
                            exportedJsonText = repo.exportDataJson()
                            showJsonExportDialog = true
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                SettingRow(
                    icon = Icons.Default.FileUpload,
                    title = "Import Local Backup from Storage",
                    value = "Restore from JSON file",
                    onClick = {
                        importJsonLauncher.launch("application/json")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                SettingRow(
                    icon = Icons.Default.Restore,
                    title = "Restore Sample Timetable",
                    value = "Reset classes & exams",
                    onClick = { viewModel.resetData() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                SettingRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear All Timetable Data",
                    value = "Delete all",
                    textColor = CoralPink,
                    onClick = { showClearDialog = true }
                )
            }
        }

        // 8. AdMob Ads & Support
        item {
            Text(
                text = "SUPPORT & ADMOB",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        item {
            Card3D(elevation = 2) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Google Mobile Ads", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("App & Ad Units Configured", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                (context as? android.app.Activity)?.let { activity ->
                                    com.example.ads.AdMobManager.showInterstitialAd(activity) {
                                        Toast.makeText(context, "Interstitial Ad closed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_show_interstitial")
                        ) {
                            Text("Interstitial Ad", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                (context as? android.app.Activity)?.let { activity ->
                                    com.example.ads.AdMobManager.showRewardedInterstitialAd(
                                        activity,
                                        onUserEarnedReward = {
                                            Toast.makeText(context, "Reward Earned! Thank you for supporting StudyBell!", Toast.LENGTH_LONG).show()
                                        },
                                        onAdDismissed = {
                                            Toast.makeText(context, "Rewarded Ad completed", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                            modifier = Modifier.weight(1f).testTag("btn_show_rewarded")
                        ) {
                            Text("Rewarded Ad", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    com.example.ads.AdMobBanner()
                }
            }
        }

        // 9. About App
        item {
            Card3D(elevation = 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("StudyBell v1.0.0", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Your school day, on time. Fully offline.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Dialog: Edit Profile
    if (showProfileDialog) {
        var name by remember { mutableStateOf(uiState.profile.name) }
        var school by remember { mutableStateOf(uiState.profile.schoolName) }
        var grade by remember { mutableStateOf(uiState.profile.gradeClass) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Edit Student Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Student Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = school,
                        onValueChange = { school = it },
                        label = { Text("School Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("Grade / Class") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showProfileDialog = false
                    viewModel.updateProfile(
                        uiState.profile.copy(
                            name = name.ifBlank { "Alex" },
                            schoolName = school.ifBlank { "School" },
                            gradeClass = grade.ifBlank { "Class" }
                        )
                    )
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Custom Snooze Duration Dialog (Arbitrary Input + Customizable Presets)
    if (showSnoozeDialog) {
        var customDurationText by remember { mutableStateOf("${uiState.settings.customSnoozeMinutes}") }
        val presets = remember(uiState.settings.snoozePresetOptions) {
            uiState.settings.snoozePresetOptions.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .ifEmpty { listOf(5, 7, 10, 15, 25, 30) }
        }

        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Define Snooze Duration", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Choose a preset or input any custom duration (e.g. 7 min, 25 min):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick presets chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { mins ->
                            val isSelected = customDurationText == mins.toString()
                            FilterChip(
                                selected = isSelected,
                                onClick = { customDurationText = mins.toString() },
                                label = { Text("${mins}m") }
                            )
                        }
                    }

                    // Arbitrary number input field
                    OutlinedTextField(
                        value = customDurationText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                customDurationText = input
                            }
                        },
                        label = { Text("Snooze Duration (minutes)") },
                        placeholder = { Text("Enter any number (e.g. 7, 25)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val currentVal = customDurationText.toIntOrNull() ?: 10
                                    if (currentVal > 1) customDurationText = (currentVal - 1).toString()
                                }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus 1 min")
                                }
                                IconButton(onClick = {
                                    val currentVal = customDurationText.toIntOrNull() ?: 10
                                    if (currentVal < 180) customDurationText = (currentVal + 1).toString()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus 1 min")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("custom_snooze_setting_input")
                    )

                    Text(
                        text = "This duration will be used as the default snooze time across all alarms.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalMinutes = customDurationText.toIntOrNull()?.coerceIn(1, 180) ?: 10
                    viewModel.updateCustomSnoozeDuration(finalMinutes)
                    showSnoozeDialog = false
                }) {
                    Text("Save Duration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnoozeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Pomodoro Intervals Customization
    if (showPomodoroSettingsDialog) {
        var workMins by remember { mutableIntStateOf(uiState.settings.pomodoroWorkMinutes) }
        var breakMins by remember { mutableIntStateOf(uiState.settings.pomodoroBreakMinutes) }
        var longBreakMins by remember { mutableIntStateOf(uiState.settings.pomodoroLongBreakMinutes) }

        AlertDialog(
            onDismissRequest = { showPomodoroSettingsDialog = false },
            title = { Text("Pomodoro Study Intervals", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Work
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Work Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (workMins > 5) workMins -= 5 }) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text("$workMins min", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (workMins < 120) workMins += 5 }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }

                    // Short Break
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Short Break", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (breakMins > 1) breakMins -= 1 }) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text("$breakMins min", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (breakMins < 30) breakMins += 1 }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }

                    // Long Break
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Long Break", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (longBreakMins > 5) longBreakMins -= 5 }) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text("$longBreakMins min", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (longBreakMins < 60) longBreakMins += 5 }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPomodoroSettingsDialog = false
                    viewModel.updateTimerSettings(
                        workMins = workMins,
                        breakMins = breakMins,
                        longBreakMins = longBreakMins,
                        sound = uiState.settings.pomodoroSoundEnabled,
                        vibrate = uiState.settings.pomodoroVibrateEnabled
                    )
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPomodoroSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Sound Selector
    if (showSoundDialog) {
        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            title = { Text("Default Alarm Sound", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Classic Bell", "School Bell", "Digital Alarm", "Soft Chime").forEach { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSoundDialog = false
                                    viewModel.updateSettings(uiState.settings.copy(defaultAlarmSound = sound))
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.settings.defaultAlarmSound == sound,
                                onClick = {
                                    showSoundDialog = false
                                    viewModel.updateSettings(uiState.settings.copy(defaultAlarmSound = sound))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sound, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundDialog = false }) { Text("Close") }
            }
        )
    }

    // Dialog: Theme Selector
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("App Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("SYSTEM", "LIGHT", "DARK").forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showThemeDialog = false
                                    viewModel.updateSettings(uiState.settings.copy(themeMode = theme))
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.settings.themeMode == theme,
                                onClick = {
                                    showThemeDialog = false
                                    viewModel.updateSettings(uiState.settings.copy(themeMode = theme))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (theme) {
                                    "SYSTEM" -> "System Default"
                                    "LIGHT" -> "Crisp Light"
                                    "DARK" -> "Deep Navy Dark"
                                    else -> theme
                                },
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    // Dialog: Export JSON
    if (showJsonExportDialog) {
        AlertDialog(
            onDismissRequest = { showJsonExportDialog = false },
            title = { Text("Backup JSON", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Copy this backup text to keep your timetable offline:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("StudyBell Backup", exportedJsonText))
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    showJsonExportDialog = false
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonExportDialog = false }) { Text("Close") }
            }
        )
    }

    // Dialog: Clear All Confirmation
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all classes, homework, exams, and alarms. You can restore sample data anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Firebase Login / Register
    if (showAuthDialog) {
        AuthDialog(
            authManager = viewModel.authManager,
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = { displayName, email ->
                if (displayName.isNotBlank() && uiState.profile.name == "Alex") {
                    viewModel.updateProfile(uiState.profile.copy(name = displayName))
                }
                Toast.makeText(context, "Welcome, $displayName! Firebase synced.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        }
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
