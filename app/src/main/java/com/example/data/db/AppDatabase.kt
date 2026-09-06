package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AlarmOccurrence
import com.example.data.model.AppSettings
import com.example.data.model.ClassSchedule
import com.example.data.model.Exam
import com.example.data.model.Homework
import com.example.data.model.ReminderItem
import com.example.data.model.ScheduleDay
import com.example.data.model.StudentProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ClassSchedule::class,
        ScheduleDay::class,
        Homework::class,
        Exam::class,
        ReminderItem::class,
        AppSettings::class,
        StudentProfile::class,
        AlarmOccurrence::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun classScheduleDao(): ClassScheduleDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun examDao(): ExamDao
    abstract fun reminderDao(): ReminderDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop tables to resolve schema differences between v1 and v2
                db.execSQL("DROP TABLE IF EXISTS `class_schedules`")
                db.execSQL("DROP TABLE IF EXISTS `schedule_days`")
                db.execSQL("DROP TABLE IF EXISTS `homework`")
                db.execSQL("DROP TABLE IF EXISTS `exams`")
                db.execSQL("DROP TABLE IF EXISTS `reminders`")
                db.execSQL("DROP TABLE IF EXISTS `app_settings`")
                db.execSQL("DROP TABLE IF EXISTS `student_profile`")
                db.execSQL("DROP TABLE IF EXISTS `alarm_occurrences`")

                // Recreate v2 tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `class_schedules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL, `teacher` TEXT NOT NULL, `room` TEXT NOT NULL, `notes` TEXT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `notifyClassEnd` INTEGER NOT NULL, `colorHex` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `schedule_days` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `classScheduleId` INTEGER NOT NULL, `dayOfWeek` INTEGER NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `reminderMinutesBefore` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `homework` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `subject` TEXT NOT NULL, `description` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `dueTime` TEXT NOT NULL, `reminderMinutesBefore` INTEGER NOT NULL, `teacher` TEXT NOT NULL, `attachmentName` TEXT NOT NULL, `status` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `examName` TEXT NOT NULL, `subject` TEXT NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `room` TEXT NOT NULL, `teacher` TEXT NOT NULL, `notes` TEXT NOT NULL, `reminderOptions` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `repeatType` TEXT NOT NULL, `repeatDays` TEXT NOT NULL, `reminderMinutesBefore` INTEGER NOT NULL, `sound` TEXT NOT NULL, `vibrate` INTEGER NOT NULL, `priority` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, `studentName` TEXT NOT NULL, `schoolName` TEXT NOT NULL, `gradeClass` TEXT NOT NULL, `defaultAlarmSound` TEXT NOT NULL, `defaultSnoozeMinutes` INTEGER NOT NULL, `customSnoozeMinutes` INTEGER NOT NULL, `snoozePresetOptions` TEXT NOT NULL, `pomodoroWorkMinutes` INTEGER NOT NULL, `pomodoroBreakMinutes` INTEGER NOT NULL, `pomodoroLongBreakMinutes` INTEGER NOT NULL, `pomodoroLongBreakInterval` INTEGER NOT NULL, `pomodoroSoundEnabled` INTEGER NOT NULL, `pomodoroVibrateEnabled` INTEGER NOT NULL, `persistentClassRemindersEnabled` INTEGER NOT NULL, `persistentExamRemindersEnabled` INTEGER NOT NULL, `vibrationEnabled` INTEGER NOT NULL, `themeMode` TEXT NOT NULL, `animationsEnabled` INTEGER NOT NULL, `effects3dEnabled` INTEGER NOT NULL, `isOnboarded` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `student_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `schoolName` TEXT NOT NULL, `gradeClass` TEXT NOT NULL, `schoolStartTime` TEXT NOT NULL, `schoolEndTime` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `alarm_occurrences` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reminderId` INTEGER NOT NULL, `reminderCategory` TEXT NOT NULL, `scheduledTimeMillis` INTEGER NOT NULL, `status` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add SQLite performance indices for offline-first querying
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_schedules_subject` ON `class_schedules` (`subject`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_schedules_isEnabled` ON `class_schedules` (`isEnabled`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_days_classScheduleId` ON `schedule_days` (`classScheduleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_days_dayOfWeek` ON `schedule_days` (`dayOfWeek`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_dueDate_dueTime` ON `homework` (`dueDate`, `dueTime`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_status` ON `homework` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_homework_subject` ON `homework` (`subject`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exams_date_time` ON `exams` (`date`, `time`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exams_subject` ON `exams` (`subject`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_category` ON `reminders` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_isCompleted` ON `reminders` (`isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_alarm_occurrences_reminderId` ON `alarm_occurrences` (`reminderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_alarm_occurrences_scheduledTimeMillis` ON `alarm_occurrences` (`scheduledTimeMillis`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studybell_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial default settings and sample data
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val settingsDao = database.appSettingsDao()
            val classDao = database.classScheduleDao()
            val homeworkDao = database.homeworkDao()
            val examDao = database.examDao()
            val reminderDao = database.reminderDao()

            settingsDao.insertOrUpdateSettings(
                AppSettings(
                    id = 1,
                    studentName = "Alex",
                    schoolName = "Oakridge High School",
                    gradeClass = "Grade 11 - Science",
                    defaultAlarmSound = "Classic Bell",
                    defaultSnoozeMinutes = 10,
                    vibrationEnabled = true,
                    themeMode = "SYSTEM",
                    animationsEnabled = true,
                    effects3dEnabled = true,
                    isOnboarded = true
                )
            )

            settingsDao.insertOrUpdateProfile(
                StudentProfile(
                    id = 1,
                    name = "Alex",
                    schoolName = "Oakridge High School",
                    gradeClass = "Grade 11 - Science",
                    schoolStartTime = "08:00",
                    schoolEndTime = "15:00"
                )
            )

            // Seed sample classes with custom days and individual times as requested in prompt!
            // Science: Monday 08:00, Wednesday 10:30, Friday 09:00
            val scienceId = classDao.insertClass(
                ClassSchedule(
                    subject = "Science",
                    teacher = "Mr. John",
                    room = "Lab 2",
                    notes = "Bring lab coat and chemistry notebook",
                    startDate = "2026-09-01",
                    endDate = "2026-12-18",
                    isEnabled = true,
                    notifyClassEnd = true,
                    colorHex = "#6366F1"
                )
            )
            classDao.insertScheduleDays(
                listOf(
                    ScheduleDay(classScheduleId = scienceId, dayOfWeek = 1, startTime = "08:00", endTime = "09:00", reminderMinutesBefore = "15"),
                    ScheduleDay(classScheduleId = scienceId, dayOfWeek = 3, startTime = "10:30", endTime = "11:30", reminderMinutesBefore = "15"),
                    ScheduleDay(classScheduleId = scienceId, dayOfWeek = 5, startTime = "09:00", endTime = "10:00", reminderMinutesBefore = "15")
                )
            )

            // Mathematics: Tuesday 10:00, Thursday 10:00
            val mathId = classDao.insertClass(
                ClassSchedule(
                    subject = "Mathematics",
                    teacher = "Mr. Smith",
                    room = "Room 204",
                    notes = "Calculus problem sets chapter 4",
                    startDate = "2026-09-01",
                    endDate = "2026-12-18",
                    isEnabled = true,
                    notifyClassEnd = true,
                    colorHex = "#F59E0B"
                )
            )
            classDao.insertScheduleDays(
                listOf(
                    ScheduleDay(classScheduleId = mathId, dayOfWeek = 2, startTime = "10:00", endTime = "11:00", reminderMinutesBefore = "10"),
                    ScheduleDay(classScheduleId = mathId, dayOfWeek = 4, startTime = "10:00", endTime = "11:00", reminderMinutesBefore = "10")
                )
            )

            // English Literature: Monday 12:30, Thursday 12:30
            val englishId = classDao.insertClass(
                ClassSchedule(
                    subject = "English Literature",
                    teacher = "Ms. Lily",
                    room = "Room 101",
                    notes = "Shakespeare Hamlet act III analysis",
                    startDate = "2026-09-01",
                    endDate = "2026-12-18",
                    isEnabled = true,
                    notifyClassEnd = true,
                    colorHex = "#10B981"
                )
            )
            classDao.insertScheduleDays(
                listOf(
                    ScheduleDay(classScheduleId = englishId, dayOfWeek = 1, startTime = "12:30", endTime = "13:30", reminderMinutesBefore = "15"),
                    ScheduleDay(classScheduleId = englishId, dayOfWeek = 4, startTime = "12:30", endTime = "13:30", reminderMinutesBefore = "15")
                )
            )

            // Homework Sample
            homeworkDao.insertHomework(
                Homework(
                    title = "Mathematics Problem Set",
                    subject = "Mathematics",
                    description = "Complete questions 1 to 20 in chapter 5. Show full working.",
                    dueDate = "2026-09-10",
                    dueTime = "08:00",
                    reminderMinutesBefore = 720,
                    teacher = "Mr. Smith",
                    status = com.example.data.model.TaskStatus.NOT_STARTED,
                    isEnabled = true
                )
            )

            homeworkDao.insertHomework(
                Homework(
                    title = "Science Lab Report",
                    subject = "Science",
                    description = "Titration experiment data calculations and conclusion",
                    dueDate = "2026-09-12",
                    dueTime = "16:00",
                    reminderMinutesBefore = 360,
                    teacher = "Mr. John",
                    status = com.example.data.model.TaskStatus.IN_PROGRESS,
                    isEnabled = true
                )
            )

            // Exam Sample
            examDao.insertExam(
                Exam(
                    examName = "Midterm Physics Exam",
                    subject = "Physics",
                    date = "2026-09-20",
                    time = "09:00",
                    room = "Hall B",
                    teacher = "Mr. John",
                    notes = "Kinematics, Thermodynamics & Electromagnetism",
                    reminderOptions = "7d,3d,1d,1h",
                    isEnabled = true
                )
            )

            // Wake-Up & Custom Reminder Sample
            reminderDao.insertReminder(
                ReminderItem(
                    title = "Wake Up for School",
                    description = "Rise and shine! Pack your school bag.",
                    category = com.example.data.model.ReminderCategory.WAKE_UP,
                    time = "05:30",
                    repeatType = com.example.data.model.RepeatType.WEEKDAYS,
                    repeatDays = "1,2,3,4,5",
                    sound = "Classic Bell",
                    vibrate = true,
                    isEnabled = true
                )
            )

            reminderDao.insertReminder(
                ReminderItem(
                    title = "Study Mathematics",
                    description = "Review practice questions for 45 minutes",
                    category = com.example.data.model.ReminderCategory.STUDY,
                    time = "19:00",
                    repeatType = com.example.data.model.RepeatType.DAILY,
                    repeatDays = "1,2,3,4,5,6,7",
                    sound = "Soft Alarm",
                    vibrate = true,
                    isEnabled = true
                )
            )
        }
    }
}
