package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.alarm.StudyBellNotificationManager
import com.example.data.db.AppDatabase
import com.example.data.model.TaskStatus
import com.example.data.repository.StudyBellRepository
import com.example.ui.alarm.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
        const val ACTION_ALARM_TRIGGER = "com.example.studybell.ACTION_ALARM_TRIGGER"
        const val ACTION_ALARM_SNOOZE = "com.example.studybell.ACTION_ALARM_SNOOZE"
        const val ACTION_ALARM_DISMISS = "com.example.studybell.ACTION_ALARM_DISMISS"
        const val ACTION_TASK_COMPLETE = "com.example.studybell.ACTION_TASK_COMPLETE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 1001)
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", 0L)
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "CUSTOM"
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "StudyBell Reminder"
        val subtitle = intent.getStringExtra("EXTRA_SUBTITLE") ?: ""
        val details = intent.getStringExtra("EXTRA_DETAILS") ?: ""
        val channelId = intent.getStringExtra("EXTRA_CHANNEL_ID") ?: StudyBellNotificationManager.CHANNEL_ALARM
        val canComplete = intent.getBooleanExtra("EXTRA_CAN_COMPLETE", false)
        val canSnooze = intent.getBooleanExtra("EXTRA_CAN_SNOOZE", true)

        Log.d(TAG, "AlarmReceiver received action: $action, notificationId: $notificationId, category: $category")

        when (action) {
            ACTION_ALARM_TRIGGER -> {
                // Wake up screen when locked/closed
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    android.os.PowerManager.ON_AFTER_RELEASE,
                    "StudyBell:AlarmWakeLock"
                )
                try {
                    wakeLock?.acquire(15000L)
                } catch (e: Exception) {
                    Log.e(TAG, "WakeLock acquire error: ${e.message}")
                }

                // Immediately launch AlarmActivity over lock screen
                val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    putExtra("EXTRA_NOTIFICATION_ID", notificationId)
                    putExtra("EXTRA_TITLE", title)
                    putExtra("EXTRA_SUBTITLE", subtitle)
                    putExtra("EXTRA_DETAILS", details)
                    putExtra("EXTRA_CATEGORY", category)
                    putExtra("EXTRA_REMINDER_ID", reminderId)
                }
                try {
                    context.startActivity(fullScreenIntent)
                } catch (e: Exception) {
                    Log.d(TAG, "Direct activity start deferred to notification fullScreenIntent: ${e.message}")
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val settings = db.appSettingsDao().getSettingsDirect()
                    val customSnooze = settings?.customSnoozeMinutes ?: settings?.defaultSnoozeMinutes ?: 10

                    // Show rich Android notification with custom snooze minutes
                    StudyBellNotificationManager.showAlarmNotification(
                        context = context,
                        notificationId = notificationId,
                        channelId = channelId,
                        title = title,
                        subtitle = subtitle,
                        details = details,
                        categoryName = category,
                        canComplete = canComplete,
                        canSnooze = canSnooze,
                        reminderId = reminderId,
                        customSnoozeMinutes = customSnooze
                    )

                    // Re-schedule recurring classes or daily reminders
                    val scheduler = AlarmScheduler(context)
                    if (category == "CLASS" && reminderId > 0) {
                        val classItem = db.classScheduleDao().getClassById(reminderId)
                        if (classItem != null && classItem.classSchedule.isEnabled) {
                            scheduler.scheduleClassAlarms(classItem)
                        }
                    } else if (category != "HOMEWORK" && category != "EXAM" && reminderId > 0) {
                        val reminderItem = db.reminderDao().getReminderById(reminderId)
                        if (reminderItem != null && reminderItem.isEnabled && !reminderItem.isCompleted) {
                            scheduler.scheduleReminderAlarm(reminderItem)
                        }
                    }
                }

                // Trigger vibration
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        manager.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 400, 200, 400),
                                -1
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 400, 200, 400), -1)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Vibration failed: ${e.message}")
                }

                // Play notification sound
                try {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                    ringtone.play()
                } catch (e: Exception) {
                    Log.e(TAG, "Ringtone playback failed: ${e.message}")
                }
            }

            ACTION_ALARM_SNOOZE -> {
                StudyBellNotificationManager.cancelNotification(context, notificationId)
                val snoozeMinutes = intent.getIntExtra("EXTRA_SNOOZE_MINUTES", 10)
                val scheduler = AlarmScheduler(context)
                scheduler.snoozeAlarm(
                    reminderId = reminderId,
                    snoozeMinutes = snoozeMinutes,
                    category = category,
                    title = title
                )
                Log.d(TAG, "Alarm snoozed for $snoozeMinutes minutes")
            }

            ACTION_ALARM_DISMISS -> {
                StudyBellNotificationManager.cancelNotification(context, notificationId)
                Log.d(TAG, "Alarm dismissed")
            }

            ACTION_TASK_COMPLETE -> {
                StudyBellNotificationManager.cancelNotification(context, notificationId)
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    if (category == "HOMEWORK" && reminderId > 0) {
                        val hw = db.homeworkDao().getHomeworkById(reminderId)
                        if (hw != null) {
                            db.homeworkDao().updateHomework(hw.copy(status = TaskStatus.COMPLETED))
                        }
                    } else if (reminderId > 0) {
                        db.reminderDao().setReminderCompleted(reminderId, true)
                    }
                }
            }
        }
    }
}
