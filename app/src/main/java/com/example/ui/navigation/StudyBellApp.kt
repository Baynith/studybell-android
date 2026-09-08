package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ads.AdMobManager
import com.example.data.model.ReminderCategory
import com.example.ui.components.AddChoiceBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PurplePrimary
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.StudyBellViewModel

sealed class AppDestination {
    object Main : AppDestination()
    data class AddEditClass(val classId: Long? = null) : AppDestination()
    data class AddEditHomework(val homeworkId: Long? = null) : AppDestination()
    data class AddEditExam(val examId: Long? = null) : AppDestination()
    data class AddEditReminder(val category: ReminderCategory = ReminderCategory.CUSTOM, val reminderId: Long? = null) : AppDestination()
    object Search : AppDestination()
    object Stats : AppDestination()
    object AlarmSounds : AppDestination()
}

@Composable
fun StudyBellApp(
    viewModel: StudyBellViewModel,
    initialDestination: AppDestination? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Check & request notification permission for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Display messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    var currentDestination by remember { mutableStateOf<AppDestination>(initialDestination ?: AppDestination.Main) }
    var showAddChoiceSheet by remember { mutableStateOf(false) }

    // If onboarding not completed
    if (!uiState.settings.isOnboarded) {
        OnboardingScreen(
            currentProfile = uiState.profile,
            onComplete = { name, school, grade ->
                viewModel.updateProfile(
                    uiState.profile.copy(
                        name = name,
                        schoolName = school,
                        gradeClass = grade
                    )
                )
                viewModel.updateSettings(
                    uiState.settings.copy(
                        studentName = name,
                        schoolName = school,
                        gradeClass = grade,
                        isOnboarded = true
                    )
                )
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentDestination == AppDestination.Main) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("studybell_bottom_nav")
                ) {
                    val tabs = listOf(
                        NavTab.HOME to Pair(Icons.Filled.Home, Icons.Outlined.Home),
                        NavTab.TIMETABLE to Pair(Icons.Filled.ViewTimeline, Icons.Outlined.ViewTimeline),
                        NavTab.TASKS to Pair(Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
                        NavTab.TIMER to Pair(Icons.Filled.HourglassTop, Icons.Outlined.HourglassTop),
                        NavTab.CALENDAR to Pair(Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
                        NavTab.SETTINGS to Pair(Icons.Filled.Settings, Icons.Outlined.Settings)
                    )

                    tabs.forEach { (tab, icons) ->
                        val isSelected = uiState.currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) icons.first else icons.second,
                                    contentDescription = tab.name,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = when (tab) {
                                        NavTab.HOME -> "Home"
                                        NavTab.TIMETABLE -> "Classes"
                                        NavTab.TASKS -> "Tasks"
                                        NavTab.TIMER -> "Timer"
                                        NavTab.CALENDAR -> "Calendar"
                                        NavTab.SETTINGS -> "Settings"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurplePrimary,
                                selectedTextColor = PurplePrimary,
                                indicatorColor = PurplePrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentDestination == AppDestination.Main && uiState.currentTab != NavTab.SETTINGS && uiState.currentTab != NavTab.TIMER) {
                FloatingActionButton(
                    onClick = { showAddChoiceSheet = true },
                    containerColor = PurplePrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("global_fab_add")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { dest ->
                when (dest) {
                    is AppDestination.Main -> {
                        when (uiState.currentTab) {
                            NavTab.HOME -> HomeScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateToAdd = { type ->
                                    when (type) {
                                        "Class" -> currentDestination = AppDestination.AddEditClass()
                                        "Homework" -> currentDestination = AppDestination.AddEditHomework()
                                        "Exam" -> currentDestination = AppDestination.AddEditExam()
                                        "Alarm" -> currentDestination = AppDestination.AddEditReminder(ReminderCategory.WAKE_UP)
                                        else -> currentDestination = AppDestination.AddEditReminder(ReminderCategory.CUSTOM)
                                    }
                                },
                                onNavigateToSearch = { currentDestination = AppDestination.Search },
                                onNavigateToItem = { category, id ->
                                    when (category) {
                                        "CLASS", "CLASS_END" -> currentDestination = AppDestination.AddEditClass(id)
                                        "HOMEWORK" -> currentDestination = AppDestination.AddEditHomework(id)
                                        "EXAM" -> currentDestination = AppDestination.AddEditExam(id)
                                        else -> currentDestination = AppDestination.AddEditReminder(reminderId = id)
                                    }
                                }
                            )
                            NavTab.TIMETABLE -> TimetableScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onAddClass = { currentDestination = AppDestination.AddEditClass() },
                                onEditClass = { id -> currentDestination = AppDestination.AddEditClass(id) }
                            )
                            NavTab.TASKS -> TasksScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onAddHomework = { currentDestination = AppDestination.AddEditHomework() },
                                onEditHomework = { id -> currentDestination = AppDestination.AddEditHomework(id) }
                            )
                            NavTab.TIMER -> {
                                val availableSubjects = remember(uiState.classes) {
                                    uiState.classes.map { it.classSchedule.subject }.distinct()
                                }
                                StudyTimerScreen(
                                    timerState = timerState,
                                    availableSubjects = availableSubjects,
                                    onToggleStartPause = { viewModel.toggleTimerStartPause() },
                                    onReset = { viewModel.resetTimer() },
                                    onSkip = { viewModel.skipTimerSession() },
                                    onSelectMode = { viewModel.selectTimerMode(it) },
                                    onSelectSubject = { viewModel.selectTimerSubject(it) },
                                    onUpdateSettings = { work, brk, lbrk, sound, vib ->
                                        viewModel.updateTimerSettings(work, brk, lbrk, sound, vib)
                                    }
                                )
                            }
                            NavTab.CALENDAR -> CalendarScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onAddEvent = {
                                    currentDestination = AppDestination.AddEditClass()
                                }
                            )
                            NavTab.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateToStats = { currentDestination = AppDestination.Stats },
                                onNavigateToAlarmSounds = { currentDestination = AppDestination.AlarmSounds }
                            )
                        }
                    }

                    is AppDestination.AddEditClass -> {
                        AddClassScreen(
                            initialClassId = dest.classId,
                            existingClasses = uiState.classes,
                            onSave = { schedule, days ->
                                if (dest.classId != null && dest.classId > 0) {
                                    viewModel.updateClass(schedule, days)
                                } else {
                                    viewModel.saveClass(schedule, days)
                                }
                                (context as? android.app.Activity)?.let { act ->
                                    AdMobManager.showInterstitialAd(act)
                                }
                            },
                            onBack = { currentDestination = AppDestination.Main }
                        )
                    }

                    is AppDestination.AddEditHomework -> {
                        AddHomeworkScreen(
                            initialHomeworkId = dest.homeworkId,
                            existingHomeworkList = uiState.homework,
                            onSave = { hw ->
                                if (dest.homeworkId != null && dest.homeworkId > 0) {
                                    viewModel.updateHomework(hw)
                                } else {
                                    viewModel.saveHomework(hw)
                                }
                                (context as? android.app.Activity)?.let { act ->
                                    AdMobManager.showInterstitialAd(act)
                                }
                            },
                            onBack = { currentDestination = AppDestination.Main }
                        )
                    }

                    is AppDestination.AddEditExam -> {
                        AddExamScreen(
                            initialExamId = dest.examId,
                            existingExams = uiState.exams,
                            onSave = { exam ->
                                if (dest.examId != null && dest.examId > 0) {
                                    viewModel.updateExam(exam)
                                } else {
                                    viewModel.saveExam(exam)
                                }
                                (context as? android.app.Activity)?.let { act ->
                                    AdMobManager.showInterstitialAd(act)
                                }
                            },
                            onBack = { currentDestination = AppDestination.Main }
                        )
                    }

                    is AppDestination.AddEditReminder -> {
                        AddCustomReminderScreen(
                            initialCategory = dest.category,
                            initialReminderId = dest.reminderId,
                            existingReminders = uiState.reminders,
                            onSave = { reminder ->
                                if (dest.reminderId != null && dest.reminderId > 0) {
                                    viewModel.updateReminder(reminder)
                                } else {
                                    viewModel.saveReminder(reminder)
                                }
                                (context as? android.app.Activity)?.let { act ->
                                    AdMobManager.showInterstitialAd(act)
                                }
                            },
                            onBack = { currentDestination = AppDestination.Main }
                        )
                    }

                    is AppDestination.Search -> {
                        SearchScreen(
                            uiState = uiState,
                            onBack = { currentDestination = AppDestination.Main },
                            onSelectClass = { id -> currentDestination = AppDestination.AddEditClass(id) },
                            onSelectHomework = { id -> currentDestination = AppDestination.AddEditHomework(id) },
                            onSelectExam = { id -> currentDestination = AppDestination.AddEditExam(id) }
                        )
                    }

                    is AppDestination.Stats -> {
                        StatisticsScreen(
                            uiState = uiState,
                            onBack = { currentDestination = AppDestination.Main }
                        )
                    }

                    is AppDestination.AlarmSounds -> {
                        AlarmSoundPickerScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = AppDestination.Main }
                        )
                    }
                }
            }
        }
    }

    if (showAddChoiceSheet) {
        AddChoiceBottomSheet(
            onDismiss = { showAddChoiceSheet = false },
            onSelectOption = { option ->
                when (option) {
                    "Class" -> currentDestination = AppDestination.AddEditClass()
                    "Homework" -> currentDestination = AppDestination.AddEditHomework()
                    "Exam" -> currentDestination = AppDestination.AddEditExam()
                    "Personal Alarm" -> currentDestination = AppDestination.AddEditReminder(ReminderCategory.WAKE_UP)
                    "Study Reminder" -> currentDestination = AppDestination.AddEditReminder(ReminderCategory.STUDY)
                    "Custom Reminder" -> currentDestination = AppDestination.AddEditReminder(ReminderCategory.CUSTOM)
                }
            }
        )
    }
}
