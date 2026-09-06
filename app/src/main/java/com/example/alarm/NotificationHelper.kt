package com.example.alarm

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

object NotificationHelper {

    const val CHANNEL_CLASS = "studybell_class"
    const val CHANNEL_CLASS_END = "studybell_class_end"
    const val CHANNEL_HOMEWORK = "studybell_homework"
    const val CHANNEL_EXAM = "studybell_exam"
    const val CHANNEL_ALARM = "studybell_alarm"
    const val CHANNEL_CUSTOM = "studybell_custom"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val classChannel = NotificationChannel(
                CHANNEL_CLASS,
                "Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before classes start"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val classEndChannel = NotificationChannel(
                CHANNEL_CLASS_END,
                "Class End Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when classes conclude"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val homeworkChannel = NotificationChannel(
                CHANNEL_HOMEWORK,
                "Homework & Assignments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Homework deadlines and task reminders"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val examChannel = NotificationChannel(
                CHANNEL_EXAM,
                "Exam Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upcoming test and exam countdown alerts"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

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

            val customChannel = NotificationChannel(
                CHANNEL_CUSTOM,
                "Personal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Personal study, health, and custom student reminders"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(
                listOf(classChannel, classEndChannel, homeworkChannel, examChannel, alarmChannel, customChannel)
            )
        }
    }

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
        reminderId: Long = 0L
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Content Intent (tapping notification opens MainActivity)
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

        // Full Screen Intent for Alarms / Wake Up
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_SUBTITLE", subtitle)
            putExtra("EXTRA_DETAILS", details)
            putExtra("EXTRA_CATEGORY", categoryName)
            putExtra("EXTRA_REMINDER_ID", reminderId)
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
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        // Add Full Screen Intent for urgent alarms or class start reminders
        if (channelId == CHANNEL_ALARM || channelId == CHANNEL_CLASS) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        // Action Buttons
        if (canSnooze) {
            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_SNOOZE
                putExtra("EXTRA_NOTIFICATION_ID", notificationId)
                putExtra("EXTRA_REMINDER_ID", reminderId)
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_CATEGORY", categoryName)
                putExtra("EXTRA_SNOOZE_MINUTES", 5)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 20000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Snooze 5m", snoozePendingIntent)
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

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
