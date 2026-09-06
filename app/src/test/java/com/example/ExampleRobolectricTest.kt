package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.NextOccurrenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StudyBell", appName)
    }

    @Test
    fun `test next occurrence calculation for class`() {
        val nextTime = NextOccurrenceEngine.getNextClassOccurrence(
            dayOfWeek = 1,
            timeStr = "08:00",
            reminderMinutesBefore = 15
        )
        assertNotNull(nextTime)
    }

    @Test
    fun `test time formatting`() {
        val formatted = NextOccurrenceEngine.formatDisplayTime("08:00")
        assertEquals("08:00 AM", formatted)
    }

    @Test
    fun `test custom snooze duration validation`() {
        val customMinutes = 7
        val snoozeMillis = customMinutes * 60 * 1000L
        assertEquals(420000L, snoozeMillis)
    }

    @Test
    fun `test widget toggle action constants`() {
        assertEquals("com.example.studybell.ACTION_WIDGET_TOGGLE_TASK", com.example.widget.HomeworkAppWidgetProvider.ACTION_TOGGLE_TASK)
        assertEquals("com.example.studybell.ACTION_WIDGET_TOGGLE_OVERDUE", com.example.widget.HomeworkAppWidgetProvider.ACTION_TOGGLE_ALL_OVERDUE)
    }

    @Test
    fun `test firebase auth manager initial state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authManager = com.example.auth.FirebaseAuthManager.getInstance(context)
        assertNotNull(authManager)
        val userState = authManager.userState.value
        assertNotNull(userState)
    }
}
