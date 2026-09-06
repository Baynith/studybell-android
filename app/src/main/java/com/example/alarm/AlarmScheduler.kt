package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.ClassWithDays
import com.example.data.model.Exam
import com.example.data.model.Homework
import com.example.data.model.ReminderItem
import com.example.data.model.TaskStatus
import com.example.data.repository.StudyBellRepository
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.flow.firstOrNull

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        const val BASE_CLASS_CODE = 100000
        const val BASE_CLASS_END_CODE = 200000
        const val BASE_HOMEWORK_CODE = 300000
        const val BASE_EXAM_CODE = 400000
        const val BASE_REMINDER_CODE = 500000
        const val BASE_SNOOZE_CODE = 600000
        const val BASE_TEST_CODE = 700000
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact / window if exact permission is disabled
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled alarm for millis: $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm: ${e.message}")
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback failed: ${fallbackEx.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    fun scheduleClassAlarms(classWithDays: ClassWithDays) {
        val classItem = classWithDays.classSchedule
        if (!classItem.isEnabled) return

        classWithDays.days.forEachIndexed { index, day ->
            // Parse reminder minutes before, e.g. "0,15,30"
            val reminderMinutesList = day.reminderMinutesBefore.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .ifEmpty { listOf(15) }

            for (minsBefore in reminderMinutesList) {
                val nextTrigger = NextOccurrenceEngine.getNextClassOccurrence(
                    dayOfWeek = day.dayOfWeek,
                    timeStr = day.startTime,
                    reminderMinutesBefore = minsBefore,
                    startDateStr = classItem.startDate,
                    endDateStr = classItem.endDate
                )

                if (nextTrigger != null) {
                    val requestCode = BASE_CLASS_CODE + (classItem.id * 100 + day.dayOfWeek * 10 + minsBefore).toInt()
                    val subtitle = if (minsBefore == 0) "Class is starting now!" else "Starts in $minsBefore minutes"
                    val details = "${classItem.teacher} • ${classItem.room} • ${day.startTime}"

                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_ALARM_TRIGGER
                        putExtra("EXTRA_NOTIFICATION_ID", requestCode)
                        putExtra("EXTRA_REMINDER_ID", classItem.id)
                        putExtra("EXTRA_TITLE", "🧪 ${classItem.subject}")
                        putExtra("EXTRA_SUBTITLE", subtitle)
                        putExtra("EXTRA_DETAILS", details)
                        putExtra("EXTRA_CHANNEL_ID", NotificationHelper.CHANNEL_CLASS)
                        putExtra("EXTRA_CATEGORY", "CLASS")
                        putExtra("EXTRA_CAN_SNOOZE", true)
                        putExtra("EXTRA_TIME", day.startTime)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(nextTrigger, pendingIntent)
                }
            }

            // Schedule class end alarm if enabled
            if (classItem.notifyClassEnd && day.endTime.isNotBlank()) {
                val endTrigger = NextOccurrenceEngine.getNextClassOccurrence(
                    dayOfWeek = day.dayOfWeek,
                    timeStr = day.endTime,
                    reminderMinutesBefore = 0,
                    startDateStr = classItem.startDate,
                    endDateStr = classItem.endDate
                )
                if (endTrigger != null) {
                    val endRequestCode = BASE_CLASS_END_CODE + (classItem.id * 100 + day.dayOfWeek).toInt()
                    val endIntent = Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_ALARM_TRIGGER
                        putExtra("EXTRA_NOTIFICATION_ID", endRequestCode)
                        putExtra("EXTRA_REMINDER_ID", classItem.id)
                        putExtra("EXTRA_TITLE", "🔔 ${classItem.subject} has ended")
                        putExtra("EXTRA_SUBTITLE", "Class completed at ${day.endTime}")
                        putExtra("EXTRA_DETAILS", "${classItem.room}")
                        putExtra("EXTRA_CHANNEL_ID", NotificationHelper.CHANNEL_CLASS_END)
                        putExtra("EXTRA_CATEGORY", "CLASS_END")
                        putExtra("EXTRA_CAN_SNOOZE", false)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        endRequestCode,
                        endIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(endTrigger, pendingIntent)
                }
            }
        }
    }

    fun cancelClassAlarms(classId: Long) {
        // Cancel possible day combinations 1..7 and reminder offsets
        for (day in 1..7) {
            for (mins in listOf(0, 5, 10, 15, 30, 60)) {
                val reqCode = BASE_CLASS_CODE + (classId * 100 + day * 10 + mins).toInt()
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
            val endReqCode = BASE_CLASS_END_CODE + (classId * 100 + day).toInt()
            val endIntent = Intent(context, AlarmReceiver::class.java)
            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                endReqCode,
                endIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (endPendingIntent != null) {
                alarmManager.cancel(endPendingIntent)
                endPendingIntent.cancel()
            }
        }
    }

    fun scheduleHomeworkAlarm(homework: Homework) {
        if (!homework.isEnabled || homework.status == TaskStatus.COMPLETED) return

        val nextTrigger = NextOccurrenceEngine.getSingleOccurrence(
            dateStr = homework.dueDate,
            timeStr = homework.dueTime,
            reminderMinutesBefore = homework.reminderMinutesBefore
        ) ?: return

        val requestCode = (BASE_HOMEWORK_CODE + homework.id).toInt()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra("EXTRA_NOTIFICATION_ID", requestCode)
            putExtra("EXTRA_REMINDER_ID", homework.id)
            putExtra("EXTRA_TITLE", "📚 ${homework.title}")
            putExtra("EXTRA_SUBTITLE", "Due on ${homework.dueDate} at ${homework.dueTime}")
            putExtra("EXTRA_DETAILS", homework.description)
            putExtra("EXTRA_CHANNEL_ID", NotificationHelper.CHANNEL_HOMEWORK)
            putExtra("EXTRA_CATEGORY", "HOMEWORK")
            putExtra("EXTRA_CAN_COMPLETE", true)
            putExtra("EXTRA_CAN_SNOOZE", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(nextTrigger, pendingIntent)
    }

    fun cancelHomeworkAlarm(homeworkId: Long) {
        val requestCode = (BASE_HOMEWORK_CODE + homeworkId).toInt()
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleExamAlarms(exam: Exam) {
        if (!exam.isEnabled) return

        val reminderOpts = exam.reminderOptions.split(",").map { it.trim() }
        for (opt in reminderOpts) {
            val minsBefore = when (opt) {
                "7d" -> 7 * 24 * 60
                "3d" -> 3 * 24 * 60
                "1d" -> 24 * 60
                "1h" -> 60
                else -> 60
            }

            val trigger = NextOccurrenceEngine.getSingleOccurrence(
                dateStr = exam.date,
                timeStr = exam.time,
                reminderMinutesBefore = minsBefore
            ) ?: continue

            val reqCode = (BASE_EXAM_CODE + exam.id * 10 + (minsBefore % 10)).toInt()
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra("EXTRA_NOTIFICATION_ID", reqCode)
                putExtra("EXTRA_REMINDER_ID", exam.id)
                putExtra("EXTRA_TITLE", "📝 ${exam.examName}")
                putExtra("EXTRA_SUBTITLE", "Exam on ${exam.date} at ${exam.time}")
                putExtra("EXTRA_DETAILS", "${exam.subject} • ${exam.room}")
                putExtra("EXTRA_CHANNEL_ID", NotificationHelper.CHANNEL_EXAM)
                putExtra("EXTRA_CATEGORY", "EXAM")
                putExtra("EXTRA_CAN_SNOOZE", true)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(trigger, pendingIntent)
        }
    }

    fun cancelExamAlarm(examId: Long) {
        for (sub in 0..9) {
            val reqCode = (BASE_EXAM_CODE + examId * 10 + sub).toInt()
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun scheduleReminderAlarm(reminder: ReminderItem) {
        if (!reminder.isEnabled || reminder.isCompleted) return

        val trigger = NextOccurrenceEngine.getNextReminderOccurrence(
            timeStr = reminder.time,
            repeatType = reminder.repeatType,
            repeatDaysStr = reminder.repeatDays,
            reminderMinutesBefore = reminder.reminderMinutesBefore
        ) ?: return

        val channelId = when (reminder.category) {
            com.example.data.model.ReminderCategory.WAKE_UP -> NotificationHelper.CHANNEL_ALARM
            com.example.data.model.ReminderCategory.PERSONAL -> NotificationHelper.CHANNEL_ALARM
            else -> NotificationHelper.CHANNEL_CUSTOM
        }

        val iconEmoji = when (reminder.category) {
            com.example.data.model.ReminderCategory.WAKE_UP -> "⏰"
            com.example.data.model.ReminderCategory.STUDY -> "📖"
            else -> "🔔"
        }

        val requestCode = (BASE_REMINDER_CODE + reminder.id).toInt()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra("EXTRA_NOTIFICATION_ID", requestCode)
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_TITLE", "$iconEmoji ${reminder.title}")
            putExtra("EXTRA_SUBTITLE", reminder.description.ifBlank { "Scheduled for ${reminder.time}" })
            putExtra("EXTRA_DETAILS", "Time: ${reminder.time}")
            putExtra("EXTRA_CHANNEL_ID", channelId)
            putExtra("EXTRA_CATEGORY", reminder.category.name)
            putExtra("EXTRA_CAN_SNOOZE", true)
            putExtra("EXTRA_CAN_COMPLETE", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(trigger, pendingIntent)
    }

    fun cancelReminderAlarm(reminderId: Long) {
        val requestCode = (BASE_REMINDER_CODE + reminderId).toInt()
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun snoozeAlarm(
        reminderId: Long,
        snoozeMinutes: Int,
        category: String,
        title: String
    ) {
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val requestCode = (BASE_SNOOZE_CODE + (reminderId % 100000)).toInt()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra("EXTRA_NOTIFICATION_ID", requestCode)
            putExtra("EXTRA_REMINDER_ID", reminderId)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_SUBTITLE", "Snoozed alarm ($snoozeMinutes min)")
            putExtra("EXTRA_DETAILS", "Alarm snoozed")
            putExtra("EXTRA_CHANNEL_ID", NotificationHelper.CHANNEL_ALARM)
            putExtra("EXTRA_CATEGORY", category)
            putExtra("EXTRA_CAN_SNOOZE", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(triggerAtMillis, pendingIntent)
    }

    fun scheduleTestAlarm(secondsFromNow: Int = 10) {
        val triggerAtMillis = System.currentTimeMillis() + (secondsFromNow * 1000L)
        val requestCode = BASE_TEST_CODE

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra("EXTRA_NOTIFICATION_ID", requestCode)
            putExtra("EXTRA_REMINDER_ID", 99999L)
            putExtra("EXTRA_TITLE", "⏰ Test Alarm Triggered!")
            putExtra("EXTRA_SUBTITLE", "StudyBell scheduled alarm fired successfully.")
            putExtra("EXTRA_DETAILS", "AlarmManager exact timing verified offline.")
            putExtra("EXTRA_CHANNEL_ID", NotificationHelper.CHANNEL_ALARM)
            putExtra("EXTRA_CATEGORY", "WAKE_UP")
            putExtra("EXTRA_CAN_SNOOZE", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(triggerAtMillis, pendingIntent)
    }

    suspend fun rescheduleAllAlarms(repository: StudyBellRepository) {
        val classes = repository.allClasses.firstOrNull() ?: emptyList()
        for (c in classes) {
            scheduleClassAlarms(c)
        }

        val homework = repository.allHomework.firstOrNull() ?: emptyList()
        for (h in homework) {
            scheduleHomeworkAlarm(h)
        }

        val exams = repository.allExams.firstOrNull() ?: emptyList()
        for (e in exams) {
            scheduleExamAlarms(e)
        }

        val reminders = repository.allReminders.firstOrNull() ?: emptyList()
        for (r in reminders) {
            scheduleReminderAlarm(r)
        }
    }
}
