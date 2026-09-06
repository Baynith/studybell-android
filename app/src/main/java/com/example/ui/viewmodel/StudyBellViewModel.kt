package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.alarm.NextOccurrenceEngine
import com.example.alarm.StudyBellNotificationManager
import com.example.data.model.*
import com.example.data.repository.StudyBellRepository
import com.example.timer.PomodoroMode
import com.example.timer.StudyTimerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class NavTab {
    HOME,
    TIMETABLE,
    TASKS,
    TIMER,
    CALENDAR,
    SETTINGS
}

data class TodayScheduleItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val time: String, // "08:00 AM"
    val rawTime: String, // "08:00"
    val category: String, // "CLASS", "CLASS_REMINDER", "CLASS_END", "HOMEWORK", "EXAM", "WAKE_UP", "REMINDER"
    val colorHex: String = "#6366F1",
    val isCompleted: Boolean = false,
    val originalId: Long = 0L
)

data class NextUpItem(
    val title: String,
    val teacher: String,
    val room: String,
    val time: String,
    val startsInText: String,
    val colorHex: String
)

data class StudyBellUiState(
    val classes: List<ClassWithDays> = emptyList(),
    val homework: List<Homework> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val reminders: List<ReminderItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val profile: StudentProfile = StudentProfile(),
    val todaySchedule: List<TodayScheduleItem> = emptyList(),
    val nextUp: NextUpItem? = null,
    val currentTab: NavTab = NavTab.HOME,
    val searchQuery: String = "",
    val isDarkTheme: Boolean = false,
    val message: String? = null
)

class StudyBellViewModel(
    application: Application,
    private val repository: StudyBellRepository
) : AndroidViewModel(application) {

    private val alarmScheduler = AlarmScheduler(application)
    val studyTimerManager = StudyTimerManager(application)
    val timerState = studyTimerManager.uiState
    val authManager = com.example.auth.FirebaseAuthManager.getInstance(application)
    val authUserState = authManager.userState

    private val _currentTab = MutableStateFlow(NavTab.HOME)
    private val _searchQuery = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StudyBellUiState> = combine(
        repository.allClasses,
        repository.allHomework,
        repository.allExams,
        repository.allReminders,
        repository.appSettings,
        repository.studentProfile,
        _currentTab,
        _searchQuery,
        _userMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val classes = args[0] as? List<ClassWithDays> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val homework = args[1] as? List<Homework> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val exams = args[2] as? List<Exam> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val reminders = args[3] as? List<ReminderItem> ?: emptyList()
        val settings = args[4] as? AppSettings ?: AppSettings()
        val profile = args[5] as? StudentProfile ?: StudentProfile()
        val tab = args[6] as? NavTab ?: NavTab.HOME
        val query = args[7] as? String ?: ""
        val msg = args[8] as? String

        val todaySchedule = computeTodaySchedule(classes, homework, exams, reminders)
        val nextUp = computeNextUp(todaySchedule, classes)

        val isDark = when (settings.themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> false // default comfortable clean styling
        }

        StudyBellUiState(
            classes = classes,
            homework = homework,
            exams = exams,
            reminders = reminders,
            settings = settings,
            profile = profile,
            todaySchedule = todaySchedule,
            nextUp = nextUp,
            currentTab = tab,
            searchQuery = query,
            isDarkTheme = isDark,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudyBellUiState()
    )

    init {
        // Initialize notification channels
        StudyBellNotificationManager.createNotificationChannels(application)

        // Reschedule all alarms on launch
        viewModelScope.launch {
            if (repository.appSettings.firstOrNull() == null) {
                com.example.data.db.AppDatabase.populateInitialData(com.example.data.db.AppDatabase.getDatabase(application))
            }
            alarmScheduler.rescheduleAllAlarms(repository)
            repository.appSettings.firstOrNull()?.let { settings ->
                studyTimerManager.updateSettings(
                    workMinutes = settings.pomodoroWorkMinutes,
                    breakMinutes = settings.pomodoroBreakMinutes,
                    longBreakMinutes = settings.pomodoroLongBreakMinutes,
                    soundEnabled = settings.pomodoroSoundEnabled,
                    vibrateEnabled = settings.pomodoroVibrateEnabled
                )
            }
        }
    }

    fun setTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    // --- Study Session Timer (Pomodoro) Controls ---
    fun toggleTimerStartPause() = studyTimerManager.toggleStartPause()
    fun resetTimer() = studyTimerManager.resetTimer()
    fun skipTimerSession() = studyTimerManager.skipSession()
    fun selectTimerMode(mode: PomodoroMode) = studyTimerManager.selectMode(mode)
    fun selectTimerSubject(subject: String) = studyTimerManager.setSubject(subject)

    fun updateTimerSettings(
        workMins: Int,
        breakMins: Int,
        longBreakMins: Int,
        sound: Boolean,
        vibrate: Boolean
    ) {
        studyTimerManager.updateSettings(workMins, breakMins, longBreakMins, sound, vibrate)
        val current = uiState.value.settings
        updateSettings(
            current.copy(
                pomodoroWorkMinutes = workMins,
                pomodoroBreakMinutes = breakMins,
                pomodoroLongBreakMinutes = longBreakMins,
                pomodoroSoundEnabled = sound,
                pomodoroVibrateEnabled = vibrate
            )
        )
        _userMessage.value = "Study session intervals updated"
    }

    // --- Custom Snooze & Settings Controls ---
    fun updateCustomSnoozeDuration(minutes: Int) {
        val current = uiState.value.settings
        updateSettings(
            current.copy(
                customSnoozeMinutes = minutes,
                defaultSnoozeMinutes = minutes
            )
        )
        _userMessage.value = "Default snooze updated to $minutes minutes"
    }

    fun updateSnoozePresets(presets: String) {
        val current = uiState.value.settings
        updateSettings(current.copy(snoozePresetOptions = presets))
        _userMessage.value = "Snooze presets updated"
    }

    // --- Persistent Notification Controls ---
    fun togglePersistentClassReminders(enabled: Boolean) {
        val current = uiState.value.settings
        updateSettings(current.copy(persistentClassRemindersEnabled = enabled))
        if (!enabled) {
            StudyBellNotificationManager.clearPersistentClassReminder(getApplication())
            _userMessage.value = "Persistent class notifications disabled"
        } else {
            refreshPersistentClassReminder()
            _userMessage.value = "Persistent class notifications enabled"
        }
    }

    fun togglePersistentExamReminders(enabled: Boolean) {
        val current = uiState.value.settings
        updateSettings(current.copy(persistentExamRemindersEnabled = enabled))
        if (!enabled) {
            StudyBellNotificationManager.clearPersistentExamReminder(getApplication())
            _userMessage.value = "Persistent exam countdowns disabled"
        } else {
            refreshPersistentExamReminder()
            _userMessage.value = "Persistent exam countdowns enabled"
        }
    }

    fun refreshPersistentClassReminder() {
        val state = uiState.value
        if (!state.settings.persistentClassRemindersEnabled) return
        val next = state.nextUp ?: return
        StudyBellNotificationManager.showPersistentClassReminder(
            context = getApplication(),
            subject = next.title,
            room = next.room,
            teacher = next.teacher,
            timeRange = next.time,
            countdownText = next.startsInText
        )
    }

    fun refreshPersistentExamReminder() {
        val state = uiState.value
        if (!state.settings.persistentExamRemindersEnabled) return
        val today = LocalDate.now()
        val upcomingExam = state.exams
            .filter { it.isEnabled }
            .mapNotNull { exam ->
                try {
                    val examDate = LocalDate.parse(exam.date)
                    val daysBetween = ChronoUnit.DAYS.between(today, examDate)
                    if (daysBetween >= 0) exam to daysBetween.toInt() else null
                } catch (_: Exception) {
                    null
                }
            }
            .minByOrNull { it.second }

        if (upcomingExam != null) {
            val (exam, days) = upcomingExam
            StudyBellNotificationManager.showPersistentExamCountdown(
                context = getApplication(),
                examName = exam.examName,
                subject = exam.subject,
                examDate = exam.date,
                daysLeft = days,
                room = exam.room
            )
        }
    }

    // --- Class CRUD ---
    fun saveClass(classSchedule: ClassSchedule, days: List<ScheduleDay>) {
        viewModelScope.launch {
            val id = repository.saveClass(classSchedule, days)
            val updated = classSchedule.copy(id = id)
            alarmScheduler.scheduleClassAlarms(ClassWithDays(updated, days))
            _userMessage.value = "Class \"${classSchedule.subject}\" saved & scheduled"
            refreshPersistentClassReminder()
        }
    }

    fun updateClass(classSchedule: ClassSchedule, days: List<ScheduleDay>) {
        viewModelScope.launch {
            alarmScheduler.cancelClassAlarms(classSchedule.id)
            repository.updateClass(classSchedule, days)
            alarmScheduler.scheduleClassAlarms(ClassWithDays(classSchedule, days))
            _userMessage.value = "Class \"${classSchedule.subject}\" updated"
            refreshPersistentClassReminder()
        }
    }

    fun toggleClassEnabled(classWithDays: ClassWithDays) {
        viewModelScope.launch {
            val newEnabled = !classWithDays.classSchedule.isEnabled
            repository.setClassEnabled(classWithDays.classSchedule.id, newEnabled)
            if (newEnabled) {
                alarmScheduler.scheduleClassAlarms(classWithDays.copy(classSchedule = classWithDays.classSchedule.copy(isEnabled = true)))
                _userMessage.value = "Class alarms enabled"
            } else {
                alarmScheduler.cancelClassAlarms(classWithDays.classSchedule.id)
                _userMessage.value = "Class alarms disabled"
            }
            refreshPersistentClassReminder()
        }
    }

    fun deleteClass(id: Long) {
        viewModelScope.launch {
            alarmScheduler.cancelClassAlarms(id)
            repository.deleteClass(id)
            _userMessage.value = "Class deleted"
            refreshPersistentClassReminder()
        }
    }

    // --- Homework CRUD ---
    fun saveHomework(hw: Homework) {
        viewModelScope.launch {
            val id = repository.saveHomework(hw)
            val updated = hw.copy(id = id)
            alarmScheduler.scheduleHomeworkAlarm(updated)
            _userMessage.value = "Homework \"${hw.title}\" saved"
            com.example.widget.HomeworkAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun updateHomework(hw: Homework) {
        viewModelScope.launch {
            alarmScheduler.cancelHomeworkAlarm(hw.id)
            repository.updateHomework(hw)
            alarmScheduler.scheduleHomeworkAlarm(hw)
            _userMessage.value = "Homework \"${hw.title}\" updated"
            com.example.widget.HomeworkAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleHomeworkStatus(hw: Homework) {
        viewModelScope.launch {
            val nextStatus = when (hw.status) {
                TaskStatus.NOT_STARTED -> TaskStatus.IN_PROGRESS
                TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                TaskStatus.COMPLETED -> TaskStatus.NOT_STARTED
            }
            repository.updateHomework(hw.copy(status = nextStatus))
            if (nextStatus == TaskStatus.COMPLETED) {
                alarmScheduler.cancelHomeworkAlarm(hw.id)
                _userMessage.value = "Homework completed! 🎉"
            } else {
                alarmScheduler.scheduleHomeworkAlarm(hw.copy(status = nextStatus))
            }
            com.example.widget.HomeworkAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteHomework(id: Long) {
        viewModelScope.launch {
            alarmScheduler.cancelHomeworkAlarm(id)
            repository.deleteHomework(id)
            _userMessage.value = "Homework deleted"
            com.example.widget.HomeworkAppWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    // --- Exam CRUD ---
    fun saveExam(exam: Exam) {
        viewModelScope.launch {
            val id = repository.saveExam(exam)
            val updated = exam.copy(id = id)
            alarmScheduler.scheduleExamAlarms(updated)
            _userMessage.value = "Exam \"${exam.examName}\" scheduled"
            refreshPersistentExamReminder()
        }
    }

    fun updateExam(exam: Exam) {
        viewModelScope.launch {
            alarmScheduler.cancelExamAlarm(exam.id)
            repository.updateExam(exam)
            alarmScheduler.scheduleExamAlarms(exam)
            _userMessage.value = "Exam \"${exam.examName}\" updated"
            refreshPersistentExamReminder()
        }
    }

    fun toggleExamEnabled(exam: Exam) {
        viewModelScope.launch {
            val newEnabled = !exam.isEnabled
            val updated = exam.copy(isEnabled = newEnabled)
            repository.setExamEnabled(exam.id, newEnabled)
            if (newEnabled) {
                alarmScheduler.scheduleExamAlarms(updated)
                _userMessage.value = "Exam reminders enabled"
            } else {
                alarmScheduler.cancelExamAlarm(exam.id)
                _userMessage.value = "Exam reminders disabled"
            }
            refreshPersistentExamReminder()
        }
    }

    fun deleteExam(id: Long) {
        viewModelScope.launch {
            alarmScheduler.cancelExamAlarm(id)
            repository.deleteExam(id)
            _userMessage.value = "Exam deleted"
            refreshPersistentExamReminder()
        }
    }

    // --- Reminder CRUD ---
    fun saveReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            val id = repository.saveReminder(reminder)
            val updated = reminder.copy(id = id)
            alarmScheduler.scheduleReminderAlarm(updated)
            _userMessage.value = "Reminder scheduled"
        }
    }

    fun updateReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            alarmScheduler.cancelReminderAlarm(reminder.id)
            repository.updateReminder(reminder)
            alarmScheduler.scheduleReminderAlarm(reminder)
            _userMessage.value = "Reminder updated"
        }
    }

    fun toggleReminderEnabled(reminder: ReminderItem) {
        viewModelScope.launch {
            val newEnabled = !reminder.isEnabled
            val updated = reminder.copy(isEnabled = newEnabled)
            repository.setReminderEnabled(reminder.id, newEnabled)
            if (newEnabled) {
                alarmScheduler.scheduleReminderAlarm(updated)
                _userMessage.value = "Reminder enabled"
            } else {
                alarmScheduler.cancelReminderAlarm(reminder.id)
                _userMessage.value = "Reminder disabled"
            }
        }
    }

    fun toggleReminderCompleted(reminder: ReminderItem) {
        viewModelScope.launch {
            val newCompleted = !reminder.isCompleted
            repository.setReminderCompleted(reminder.id, newCompleted)
            if (newCompleted) {
                alarmScheduler.cancelReminderAlarm(reminder.id)
                _userMessage.value = "Reminder completed"
            } else {
                alarmScheduler.scheduleReminderAlarm(reminder.copy(isCompleted = false))
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            alarmScheduler.cancelReminderAlarm(id)
            repository.deleteReminder(id)
            _userMessage.value = "Reminder deleted"
        }
    }

    // --- Settings & Profile ---
    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            _userMessage.value = "Settings updated"
        }
    }

    fun updateProfile(profile: StudentProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
            _userMessage.value = "Profile updated"
        }
    }

    fun testAlarm(seconds: Int = 5) {
        alarmScheduler.scheduleTestAlarm(seconds)
        _userMessage.value = "⏰ Test alarm set! Triggering in $seconds seconds..."
    }

    fun rescheduleAllAlarms() {
        viewModelScope.launch {
            alarmScheduler.rescheduleAllAlarms(repository)
            _userMessage.value = "All class bells and study alarms synchronized with exact timing"
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.resetToSampleData()
            alarmScheduler.rescheduleAllAlarms(repository)
            _userMessage.value = "Sample data restored"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _userMessage.value = "All data cleared"
        }
    }

    fun importDataJson(jsonString: String) {
        viewModelScope.launch {
            try {
                repository.importDataJson(jsonString)
                alarmScheduler.rescheduleAllAlarms(repository)
                _userMessage.value = "Local backup restored successfully from phone storage"
            } catch (e: Exception) {
                _userMessage.value = "Failed to import backup: ${e.localizedMessage}"
            }
        }
    }

    // --- Calculations for Today & Next Up ---
    private fun computeTodaySchedule(
        classes: List<ClassWithDays>,
        homework: List<Homework>,
        exams: List<Exam>,
        reminders: List<ReminderItem>
    ): List<TodayScheduleItem> {
        val today = LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value // 1..7
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val items = mutableListOf<TodayScheduleItem>()

        // 1. Classes for today
        for (c in classes) {
            if (!c.classSchedule.isEnabled) continue
            for (d in c.days) {
                if (d.dayOfWeek == currentDayOfWeek) {
                    val formattedStartTime = NextOccurrenceEngine.formatDisplayTime(d.startTime)
                    val formattedEndTime = NextOccurrenceEngine.formatDisplayTime(d.endTime)

                    // Class Start item
                    items.add(
                        TodayScheduleItem(
                            id = c.classSchedule.id * 1000 + d.dayOfWeek * 10,
                            title = c.classSchedule.subject,
                            subtitle = "${c.classSchedule.teacher} • ${c.classSchedule.room}",
                            time = formattedStartTime,
                            rawTime = d.startTime,
                            category = "CLASS",
                            colorHex = c.classSchedule.colorHex,
                            originalId = c.classSchedule.id
                        )
                    )

                    // Optional Class End
                    if (c.classSchedule.notifyClassEnd && d.endTime.isNotBlank()) {
                        items.add(
                            TodayScheduleItem(
                                id = c.classSchedule.id * 1000 + d.dayOfWeek * 10 + 1,
                                title = "${c.classSchedule.subject} Ends",
                                subtitle = "Class concludes at $formattedEndTime",
                                time = formattedEndTime,
                                rawTime = d.endTime,
                                category = "CLASS_END",
                                colorHex = c.classSchedule.colorHex,
                                originalId = c.classSchedule.id
                            )
                        )
                    }
                }
            }
        }

        // 2. Homework due today
        for (h in homework) {
            if (h.dueDate == todayStr) {
                items.add(
                    TodayScheduleItem(
                        id = 300000L + h.id,
                        title = "${h.subject} Homework",
                        subtitle = h.title,
                        time = NextOccurrenceEngine.formatDisplayTime(h.dueTime),
                        rawTime = h.dueTime,
                        category = "HOMEWORK",
                        colorHex = "#F59E0B",
                        isCompleted = h.status == TaskStatus.COMPLETED,
                        originalId = h.id
                    )
                )
            }
        }

        // 3. Exams today
        for (e in exams) {
            if (e.date == todayStr) {
                items.add(
                    TodayScheduleItem(
                        id = 400000L + e.id,
                        title = "${e.subject} Exam",
                        subtitle = "${e.examName} • ${e.room}",
                        time = NextOccurrenceEngine.formatDisplayTime(e.time),
                        rawTime = e.time,
                        category = "EXAM",
                        colorHex = "#EF4444",
                        originalId = e.id
                    )
                )
            }
        }

        // 4. Reminders today (wake up, personal, custom)
        for (r in reminders) {
            if (!r.isEnabled) continue
            val appliesToday = when (r.repeatType) {
                RepeatType.ONCE, RepeatType.NO_REPEAT -> r.date == todayStr || r.date.isBlank()
                RepeatType.DAILY -> true
                RepeatType.WEEKDAYS -> currentDayOfWeek in 1..5
                RepeatType.WEEKENDS -> currentDayOfWeek in 6..7
                RepeatType.WEEKLY -> true
                RepeatType.CUSTOM_DAYS -> {
                    r.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(currentDayOfWeek)
                }
            }

            if (appliesToday) {
                items.add(
                    TodayScheduleItem(
                        id = 500000L + r.id,
                        title = r.title,
                        subtitle = r.description.ifBlank { "${r.category.name.lowercase().capitalize()} Reminder" },
                        time = NextOccurrenceEngine.formatDisplayTime(r.time),
                        rawTime = r.time,
                        category = r.category.name,
                        colorHex = when (r.category) {
                            ReminderCategory.WAKE_UP -> "#F59E0B"
                            ReminderCategory.STUDY -> "#6366F1"
                            ReminderCategory.PERSONAL -> "#10B981"
                            else -> "#8B5CF6"
                        },
                        isCompleted = r.isCompleted,
                        originalId = r.id
                    )
                )
            }
        }

        // Sort chronologically by rawTime (HH:mm)
        return items.sortedBy { it.rawTime }
    }

    private fun computeNextUp(
        todaySchedule: List<TodayScheduleItem>,
        classes: List<ClassWithDays>
    ): NextUpItem? {
        val now = LocalTime.now()
        val upcomingToday = todaySchedule.firstOrNull {
            val t = NextOccurrenceEngine.parseTime(it.rawTime)
            t != null && t.isAfter(now)
        }

        if (upcomingToday != null) {
            val targetTime = NextOccurrenceEngine.parseTime(upcomingToday.rawTime) ?: now
            val minutesUntil = java.time.Duration.between(now, targetTime).toMinutes().coerceAtLeast(1)
            val teacherRoom = upcomingToday.subtitle.split("•")
            val teacher = teacherRoom.getOrNull(0)?.trim() ?: ""
            val room = teacherRoom.getOrNull(1)?.trim() ?: ""

            return NextUpItem(
                title = upcomingToday.title,
                teacher = teacher,
                room = room,
                time = upcomingToday.time,
                startsInText = "Starts in $minutesUntil minutes",
                colorHex = upcomingToday.colorHex
            )
        }

        // If nothing today, show first class from tomorrow or general next
        val firstClass = classes.firstOrNull { it.classSchedule.isEnabled && it.days.isNotEmpty() }
        if (firstClass != null) {
            val firstDay = firstClass.days.first()
            return NextUpItem(
                title = firstClass.classSchedule.subject,
                teacher = firstClass.classSchedule.teacher,
                room = firstClass.classSchedule.room,
                time = NextOccurrenceEngine.formatDisplayTime(firstDay.startTime),
                startsInText = "Upcoming on ${dayName(firstDay.dayOfWeek)}",
                colorHex = firstClass.classSchedule.colorHex
            )
        }

        return null
    }

    private fun dayName(day: Int): String {
        return when (day) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Day $day"
        }
    }
}
