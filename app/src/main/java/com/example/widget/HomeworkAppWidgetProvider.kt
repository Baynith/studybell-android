package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.Homework
import com.example.data.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * AppWidgetProvider for Home Screen displaying upcoming homework deadlines
 * and quick-toggle switches for overdue and pending tasks.
 */
class HomeworkAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_TOGGLE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getDatabase(context)
                        val hw = db.homeworkDao().getHomeworkById(taskId)
                        if (hw != null) {
                            val nextStatus = when (hw.status) {
                                TaskStatus.COMPLETED -> TaskStatus.IN_PROGRESS
                                else -> TaskStatus.COMPLETED
                            }
                            db.homeworkDao().updateHomeworkStatus(taskId, nextStatus)
                            withContext(Dispatchers.Main) {
                                val msg = if (nextStatus == TaskStatus.COMPLETED) {
                                    "✓ \"${hw.title}\" marked completed!"
                                } else {
                                    "\"${hw.title}\" marked in progress"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            updateAllWidgets(context)
                        }
                    }
                }
            }

            ACTION_TOGGLE_ALL_OVERDUE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val allHw = db.homeworkDao().getAllHomeworkDirect()
                    var updatedCount = 0
                    for (hw in allHw) {
                        if (isOverdue(hw) && hw.status != TaskStatus.COMPLETED) {
                            db.homeworkDao().updateHomeworkStatus(hw.id, TaskStatus.COMPLETED)
                            updatedCount++
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (updatedCount > 0) {
                            Toast.makeText(context, "✓ $updatedCount overdue task${if (updatedCount > 1) "s" else ""} marked done! 🎉", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No overdue tasks to toggle", Toast.LENGTH_SHORT).show()
                        }
                    }
                    updateAllWidgets(context)
                }
            }

            ACTION_REFRESH -> {
                updateAllWidgets(context)
                Toast.makeText(context, "Widget refreshed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TASK = "com.example.studybell.ACTION_WIDGET_TOGGLE_TASK"
        const val ACTION_TOGGLE_ALL_OVERDUE = "com.example.studybell.ACTION_WIDGET_TOGGLE_OVERDUE"
        const val ACTION_REFRESH = "com.example.studybell.ACTION_WIDGET_REFRESH"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, HomeworkAppWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, HomeworkAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }

        private fun isOverdue(hw: Homework): Boolean {
            if (hw.status == TaskStatus.COMPLETED) return false
            return try {
                val today = LocalDate.now().toString()
                if (hw.dueDate < today) {
                    true
                } else if (hw.dueDate == today) {
                    val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    hw.dueTime < nowTime
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        private fun formatDueText(hw: Homework): String {
            return try {
                val today = LocalDate.now()
                val dueDate = LocalDate.parse(hw.dueDate)
                val diffDays = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)
                val timeStr = hw.dueTime

                when {
                    diffDays < -1 -> "Overdue by ${-diffDays} days (${hw.dueDate})"
                    diffDays == -1L -> "Overdue yesterday at $timeStr"
                    diffDays == 0L -> "Today at $timeStr"
                    diffDays == 1L -> "Tomorrow at $timeStr"
                    diffDays in 2..6 -> "In $diffDays days ($timeStr)"
                    else -> "${hw.dueDate} • $timeStr"
                }
            } catch (e: Exception) {
                "${hw.dueDate} ${hw.dueTime}"
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_homework)

            // Setup Header click to open main Tasks screen
            val openTasksIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "TASKS")
            }
            val openTasksPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openTasksIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openTasksPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_footer_text, openTasksPendingIntent)

            // Add Homework button
            val addHomeworkIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "ADD_HOMEWORK")
            }
            val addHomeworkPendingIntent = PendingIntent.getActivity(
                context,
                1,
                addHomeworkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addHomeworkPendingIntent)

            // Refresh button
            val refreshIntent = Intent(context, HomeworkAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // Fetch Homework asynchronously from database
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val allHomework = db.homeworkDao().getAllHomeworkDirect()

                val overdueList = allHomework.filter { isOverdue(it) && it.status != TaskStatus.COMPLETED }
                val pendingUpcoming = allHomework.filter { !isOverdue(it) && it.status != TaskStatus.COMPLETED }
                val completedList = allHomework.filter { it.status == TaskStatus.COMPLETED }

                // Prioritize: Overdue tasks at the very top, then upcoming pending, then recently completed
                val displayList = overdueList + pendingUpcoming + completedList

                withContext(Dispatchers.Main) {
                    // Update Overdue Banner
                    if (overdueList.isNotEmpty()) {
                        views.setViewVisibility(R.id.widget_overdue_banner, View.VISIBLE)
                        val overdueCount = overdueList.size
                        views.setTextViewText(
                            R.id.widget_overdue_text,
                            "⚠️ $overdueCount overdue task${if (overdueCount > 1) "s" else ""}!"
                        )

                        val toggleOverdueIntent = Intent(context, HomeworkAppWidgetProvider::class.java).apply {
                            action = ACTION_TOGGLE_ALL_OVERDUE
                        }
                        val toggleOverduePendingIntent = PendingIntent.getBroadcast(
                            context,
                            100,
                            toggleOverdueIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_btn_toggle_overdue, toggleOverduePendingIntent)
                    } else {
                        views.setViewVisibility(R.id.widget_overdue_banner, View.GONE)
                    }

                    // Status Summary
                    val activeCount = overdueList.size + pendingUpcoming.size
                    views.setTextViewText(
                        R.id.widget_status_summary,
                        if (overdueList.isNotEmpty()) "${overdueList.size} Overdue • ${activeCount} Total" else "$activeCount Pending"
                    )

                    // Empty state vs items container
                    if (displayList.isEmpty() || (activeCount == 0 && completedList.isEmpty())) {
                        views.setViewVisibility(R.id.widget_empty_state, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_task_list_container, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_empty_state, View.GONE)
                        views.setViewVisibility(R.id.widget_task_list_container, View.VISIBLE)

                        val slotIds = listOf(
                            R.id.widget_task_slot_1 to Triple(R.id.widget_task_title_1, R.id.widget_task_due_1, Pair(R.id.widget_task_toggle_1, R.id.widget_task_badge_1)),
                            R.id.widget_task_slot_2 to Triple(R.id.widget_task_title_2, R.id.widget_task_due_2, Pair(R.id.widget_task_toggle_2, R.id.widget_task_badge_2)),
                            R.id.widget_task_slot_3 to Triple(R.id.widget_task_title_3, R.id.widget_task_due_3, Pair(R.id.widget_task_toggle_3, R.id.widget_task_badge_3)),
                            R.id.widget_task_slot_4 to Triple(R.id.widget_task_title_4, R.id.widget_task_due_4, Pair(R.id.widget_task_toggle_4, R.id.widget_task_badge_4))
                        )

                        for (i in slotIds.indices) {
                            val (slotLayoutId, viewIds) = slotIds[i]
                            val (titleId, dueId, toggleAndBadge) = viewIds
                            val (toggleBtnId, badgeId) = toggleAndBadge

                            if (i < displayList.size) {
                                val item = displayList[i]
                                val isItemOverdue = isOverdue(item)
                                val isItemCompleted = item.status == TaskStatus.COMPLETED

                                views.setViewVisibility(slotLayoutId, View.VISIBLE)
                                views.setTextViewText(titleId, item.title)

                                val dueStr = formatDueText(item)
                                views.setTextViewText(dueId, "${item.subject} • $dueStr")

                                // Overdue badge
                                if (isItemOverdue) {
                                    views.setViewVisibility(badgeId, View.VISIBLE)
                                    views.setTextViewText(badgeId, "OVERDUE")
                                    views.setTextColor(badgeId, android.graphics.Color.parseColor("#DC2626"))
                                } else if (isItemCompleted) {
                                    views.setViewVisibility(badgeId, View.VISIBLE)
                                    views.setTextViewText(badgeId, "DONE ✓")
                                    views.setTextColor(badgeId, android.graphics.Color.parseColor("#059669"))
                                } else {
                                    views.setViewVisibility(badgeId, View.GONE)
                                }

                                // Toggle Button Icon
                                if (isItemCompleted) {
                                    views.setImageViewResource(toggleBtnId, R.drawable.ic_widget_check_circle)
                                } else {
                                    views.setImageViewResource(toggleBtnId, R.drawable.ic_widget_radio_unchecked)
                                }

                                // Toggle Action Intent
                                val toggleIntent = Intent(context, HomeworkAppWidgetProvider::class.java).apply {
                                    action = ACTION_TOGGLE_TASK
                                    putExtra(EXTRA_TASK_ID, item.id)
                                }
                                val togglePendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    (item.id * 1000 + i).toInt(),
                                    toggleIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(toggleBtnId, togglePendingIntent)

                                // Slot click opens homework in app
                                val openItemIntent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("destination", "EDIT_HOMEWORK")
                                    putExtra("homework_id", item.id)
                                }
                                val openItemPendingIntent = PendingIntent.getActivity(
                                    context,
                                    (item.id * 2000 + i).toInt(),
                                    openItemIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(slotLayoutId, openItemPendingIntent)
                            } else {
                                views.setViewVisibility(slotLayoutId, View.GONE)
                            }
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
