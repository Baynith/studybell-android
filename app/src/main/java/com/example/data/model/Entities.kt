package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.ForeignKey
import androidx.room.Index

enum class RepeatType {
    ONCE,
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    WEEKLY,
    CUSTOM_DAYS,
    NO_REPEAT
}

enum class TaskStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

enum class ReminderCategory {
    CLASS,
    HOMEWORK,
    EXAM,
    WAKE_UP,
    STUDY,
    PERSONAL,
    CUSTOM
}

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Alex",
    val schoolName: String = "Lincoln High School",
    val gradeClass: String = "Grade 11 - Class A",
    val schoolStartTime: String = "08:00",
    val schoolEndTime: String = "15:00"
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val studentName: String = "Alex",
    val schoolName: String = "Lincoln High School",
    val gradeClass: String = "Grade 11 - Class A",
    val defaultAlarmSound: String = "Classic Bell",
    val defaultSnoozeMinutes: Int = 10,
    val customSnoozeMinutes: Int = 10,
    val snoozePresetOptions: String = "5,7,10,15,25,30",
    val pomodoroWorkMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val pomodoroLongBreakInterval: Int = 4,
    val pomodoroSoundEnabled: Boolean = true,
    val pomodoroVibrateEnabled: Boolean = true,
    val persistentClassRemindersEnabled: Boolean = true,
    val persistentExamRemindersEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val animationsEnabled: Boolean = true,
    val effects3dEnabled: Boolean = true,
    val isOnboarded: Boolean = true
)

@Entity(
    tableName = "class_schedules",
    indices = [Index(value = ["subject"]), Index(value = ["isEnabled"])]
)
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val teacher: String = "",
    val room: String = "",
    val notes: String = "",
    val startDate: String = "", // e.g. "2026-09-01"
    val endDate: String = "",   // e.g. "2026-12-15"
    val isEnabled: Boolean = true,
    val notifyClassEnd: Boolean = true,
    val colorHex: String = "#6366F1"
)

/**
 * Individual schedule day for a class with foreign key cascading deletion and indices.
 */
@Entity(
    tableName = "schedule_days",
    foreignKeys = [
        ForeignKey(
            entity = ClassSchedule::class,
            parentColumns = ["id"],
            childColumns = ["classScheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["classScheduleId"]),
        Index(value = ["dayOfWeek"])
    ]
)
data class ScheduleDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classScheduleId: Long,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val startTime: String, // e.g. "08:00"
    val endTime: String,   // e.g. "09:00"
    val reminderMinutesBefore: String = "15" // Comma separated, e.g. "0,15,30"
)

data class ClassWithDays(
    @Embedded val classSchedule: ClassSchedule,
    @Relation(
        parentColumn = "id",
        entityColumn = "classScheduleId"
    )
    val days: List<ScheduleDay> = emptyList()
)

@Entity(
    tableName = "homework",
    indices = [
        Index(value = ["dueDate", "dueTime"]),
        Index(value = ["status"]),
        Index(value = ["subject"])
    ]
)
data class Homework(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val description: String = "",
    val dueDate: String, // "YYYY-MM-DD"
    val dueTime: String = "08:00", // "HH:mm"
    val reminderMinutesBefore: Int = 720, // default 12 hours / previous evening
    val teacher: String = "",
    val attachmentName: String = "",
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "exams",
    indices = [
        Index(value = ["date", "time"]),
        Index(value = ["subject"])
    ]
)
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examName: String,
    val subject: String,
    val date: String, // "YYYY-MM-DD"
    val time: String = "09:00", // "HH:mm"
    val room: String = "",
    val teacher: String = "",
    val notes: String = "",
    val reminderOptions: String = "7d,3d,1d,1h", // comma separated
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isCompleted"])
    ]
)
data class ReminderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: ReminderCategory = ReminderCategory.CUSTOM,
    val date: String = "", // "YYYY-MM-DD"
    val time: String = "08:00", // "HH:mm"
    val repeatType: RepeatType = RepeatType.ONCE,
    val repeatDays: String = "", // "1,2,3,4,5"
    val reminderMinutesBefore: Int = 0,
    val sound: String = "Classic Bell",
    val vibrate: Boolean = true,
    val priority: String = "HIGH",
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "alarm_occurrences",
    indices = [
        Index(value = ["reminderId"]),
        Index(value = ["scheduledTimeMillis"])
    ]
)
data class AlarmOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val reminderCategory: ReminderCategory,
    val scheduledTimeMillis: Long,
    val status: String = "SCHEDULED" // SCHEDULED, TRIGGERED, SNOOZED, DISMISSED
)
