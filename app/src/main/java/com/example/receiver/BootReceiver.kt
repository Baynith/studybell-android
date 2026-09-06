package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.db.AppDatabase
import com.example.data.repository.StudyBellRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver triggered with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            NotificationHelper.createNotificationChannels(context)

            // Reschedule all active alarms from Room database
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repository = StudyBellRepository(db)
                    val scheduler = AlarmScheduler(context)

                    scheduler.rescheduleAllAlarms(repository)
                    Log.d(TAG, "Successfully restored all scheduled alarms after device boot/time update")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore alarms after boot: ${e.message}")
                }
            }
        }
    }
}
