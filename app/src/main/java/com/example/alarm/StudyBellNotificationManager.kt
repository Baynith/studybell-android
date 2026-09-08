package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.AlarmReceiver
import com.example.ui.alarm.AlarmActivity

/**
 * Unified NotificationManager and Alarm Scheduling utility for StudyBell.
 * Handles:
 * 1. Exact alarm scheduling with AlarmManager
 * 2. High-priority full-screen and heads-up alarm notifications
 * 3. Persistent sticky reminders for upcoming classes (lock-screen & status bar)
 * 4. Persistent countdown reminders for upcoming exams
 * 5. Ongoing study session Pomodoro timer notifications
 */
object StudyBellNotificationManager {

    // Notification Channel IDs
    const val CHANNEL_CLASS = "studybell_class"
    const val CHANNEL_CLASS_END = "studybell_class_end"
    const val CHANNEL_HOMEWORK = "studybell_homework"
    const val CHANNEL_EXAM = "studybell_exam"
    const val CHANNEL_ALARM = "studybell_alarm"
    const val CHANNEL_CUSTOM = "studybell_custom"
    const val CHANNEL_PERSISTENT = "studybell_persistent_tracker"
    const val CHANNEL_STUDY_TIMER = "studybell_study_timer"

    // Persistent Notification Fixed IDs
    const val NOTIF_ID_PERSISTENT_CLASS = 8001
    const val NOTIF_ID_PERSISTENT_EXAM = 8002
    const val NOTIF_ID_STUDY_TIMER = 8003

    // Broadcast actions for Study Timer
    const val ACTION_TIMER_PAUSE = "com.example.action.TIMER_PAUSE"
    const val ACTION_TIMER_RESUME = "com.example.action.TIMER_RESUME"
    const val ACTION_TIMER_SKIP = "com.example.action.TIMER_SKIP"

    /**
     * Initializes all system notification channels required by StudyBell.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // 1. Class Start Reminders Channel
            val classChannel = NotificationChannel(
                CHANNEL_CLASS,
                "Class Start Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alarms and notifications before classes start"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Class End Channel
            val classEndChannel = NotificationChannel(
                CHANNEL_CLASS_END,
                "Class End Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when classes conclude"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Homework Deadlines Channel
            val homeworkChannel = NotificationChannel(
                CHANNEL_HOMEWORK,
                "Homework & Assignments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Homework deadlines and task reminders"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 4. Exam Reminders Channel
            val examChannel = NotificationChannel(
                CHANNEL_EXAM,
                "Exam Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upcoming test and exam countdown alerts"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 5. Wake Up Alarms Channel
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Wake Up & Urgent Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority wake-up and custom alarms"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 6. Custom Reminders Channel
            val customChannel = NotificationChannel(
                CHANNEL_CUSTOM,
                "Personal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Personal study, health, and custom student reminders"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 7. Persistent Tracker Channel (ongoing status bar/lock screen updates)
            val persistentChannel = NotificationChannel(
                CHANNEL_PERSISTENT,
                "Persistent School & Exam Trackers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing reminders for current classes and upcoming exams"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 8. Study Session Pomodoro Timer Channel
            val timerChannel = NotificationChannel(
                CHANNEL_STUDY_TIMER,
                "Study Session Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active Pomodoro work and break session progress"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(
                listOf(
                    classChannel,
                    classEndChannel,
                    homeworkChannel,
                    examChannel,
                    alarmChannel,
                    customChannel,
                    persistentChannel,
                    timerChannel
                )
            )
        }
    }

    // =========================================================================
    // ALARM SCHEDULING (AlarmManager)
    // =========================================================================

    /**
     * Schedules an exact alarm with the system AlarmManager.
     */
    fun scheduleExactAlarm(
        context: Context,
        occurrenceId: Long,
        triggerAtMillis: Long,
        title: String,
        subtitle: String,
        details: String,
        category: String,
        reminderId: Long = 0L,
        canComplete: Boolean = false,
        canSnooze: Boolean = true
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra("EXTRA_NOTIFICATION_ID", occurrenceId.toInt())
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_SUBTITLE", subtitle)
            putExtra("EXTRA_DETAILS", details)
            putExtra("EXTRA_CATEGORY", category)
            putExtra("EXTRA_REMINDER_ID", reminderId)
            putExtra("EXTRA_CAN_COMPLETE", canComplete)
            putExtra("EXTRA_CAN_SNOOZE", canSnooze)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            occurrenceId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels an existing scheduled alarm.
     */
    fun cancelScheduledAlarm(context: Context, occurrenceId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            occurrenceId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // =========================================================================
    // ALARM & TRIGGER NOTIFICATIONS
    // =========================================================================

    /**
     * Triggers an alarm notification with full-screen capability, custom snooze,
     * and completion actions.
     */
    fun showAlarmNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        subtitle: String,
        details: String,
        categoryName: String,
        canComplete: Boolean = false,
        canSnooze: Boolean = true,
        reminderId: Long = 0L,
        customSnoozeMinutes: Int = 10
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Content intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TARGET", categoryName)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full Screen Intent
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_SUBTITLE", subtitle)
            putExtra("EXTRA_DETAILS", details)
            putExtra("EXTRA_CATEGORY", categoryName)
            putExtra("EXTRA_REMINDER_ID", reminderId)
            putExtra("EXTRA_SNOOZE_MINUTES", customSnoozeMinutes)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 500000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_DISMISS
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            putExtra("EXTRA_REMINDER_ID", reminderId)
            putExtra("EXTRA_CATEGORY", categoryName)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Public version for secure lock screen display (Mockup 1)
        val publicNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.studybell_icon_1788610146906)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText("StudyBell")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.studybell_icon_1788610146906)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText("StudyBell")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("$subtitle\n$details")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publicNotification)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        // Custom Snooze Action
        if (canSnooze) {
            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_SNOOZE
                putExtra("EXTRA_NOTIFICATION_ID", notificationId)
                putExtra("EXTRA_REMINDER_ID", reminderId)
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_CATEGORY", categoryName)
                putExtra("EXTRA_SNOOZE_MINUTES", customSnoozeMinutes)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 20000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Snooze ${customSnoozeMinutes}m", snoozePendingIntent)
        }

        if (canComplete) {
            val completeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_TASK_COMPLETE
                putExtra("EXTRA_NOTIFICATION_ID", notificationId)
                putExtra("EXTRA_REMINDER_ID", reminderId)
                putExtra("EXTRA_CATEGORY", categoryName)
            }
            val completePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 30000,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "✓ Complete", completePendingIntent)
        }

        builder.addAction(0, "Dismiss", dismissPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    // =========================================================================
    // PERSISTENT UPCOMING CLASS & EXAM REMINDERS (Status Bar & Lock Screen)
    // =========================================================================

    /**
     * Displays a sticky, persistent notification showing the upcoming/active class
     * so the student always has quick visibility from the lock screen and notification drawer.
     */
    fun showPersistentClassReminder(
        context: Context,
        subject: String,
        room: String,
        teacher: String,
        timeRange: String,
        countdownText: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "TIMETABLE")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_PERSISTENT_CLASS,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.studybell_icon_1788610146906)
            .setContentTitle("🔔 Next Class: $subject")
            .setContentText("$timeRange • $countdownText")
            .setSubText("StudyBell Class Tracker")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Next Class: $subject")
                    .bigText("Time: $timeRange\nLocation: $room • Teacher: $teacher\n$countdownText")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true) // Pinned persistent notification
            .setOnlyAlertOnce(true)

        notificationManager.notify(NOTIF_ID_PERSISTENT_CLASS, builder.build())
    }

    /**
     * Clears the persistent class reminder.
     */
    fun clearPersistentClassReminder(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIF_ID_PERSISTENT_CLASS)
    }

    /**
     * Displays a sticky, persistent countdown reminder for an upcoming exam.
     */
    fun showPersistentExamCountdown(
        context: Context,
        examName: String,
        subject: String,
        examDate: String,
        daysLeft: Int,
        room: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "CALENDAR")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_PERSISTENT_EXAM,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val countdownTitle = if (daysLeft == 0) "⚠️ Exam Today: $examName"
        else if (daysLeft == 1) "⚠️ Exam Tomorrow: $examName"
        else "📝 Exam in $daysLeft days: $examName"

        val builder = NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.studybell_icon_1788610146906)
            .setContentTitle(countdownTitle)
            .setContentText("$subject • $examDate • $room")
            .setSubText("Exam Preparation")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(countdownTitle)
                    .bigText("Subject: $subject\nDate: $examDate • Location: $room\nKeep reviewing your notes!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true) // Persistent reminder
            .setOnlyAlertOnce(true)

        notificationManager.notify(NOTIF_ID_PERSISTENT_EXAM, builder.build())
    }

    /**
     * Clears the persistent exam reminder.
     */
    fun clearPersistentExamReminder(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIF_ID_PERSISTENT_EXAM)
    }

    // =========================================================================
    // STUDY SESSION TIMER (POMODORO) ONGOING NOTIFICATION
    // =========================================================================

    /**
     * Displays an ongoing notification during an active Pomodoro study session.
     */
    fun showStudyTimerOngoing(
        context: Context,
        modeTitle: String,
        subject: String,
        remainingFormatted: String,
        isRunning: Boolean,
        progressPercent: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "TIMER")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIF_ID_STUDY_TIMER,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_STUDY_TIMER)
            .setSmallIcon(R.drawable.studybell_icon_1788610146906)
            .setContentTitle("$modeTitle: $remainingFormatted")
            .setContentText("Studying: $subject")
            .setSubText("Pomodoro Timer")
            .setProgress(100, progressPercent, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)

        notificationManager.notify(NOTIF_ID_STUDY_TIMER, builder.build())
    }

    /**
     * Clears the ongoing study timer notification.
     */
    fun clearStudyTimerNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIF_ID_STUDY_TIMER)
    }

    /**
     * Cancels any notification by ID.
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
