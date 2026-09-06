package com.example.alarm

import com.example.data.model.RepeatType
import com.example.data.model.ScheduleDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NextOccurrenceEngine {

    /**
     * Finds the next future epoch millisecond for a class schedule day.
     * Takes into account:
     * - dayOfWeek (1 = Monday ... 7 = Sunday)
     * - time ("HH:mm")
     * - reminderMinutesBefore (e.g. 15 -> fires 15 mins before class time)
     * - optional startDate and endDate
     */
    fun getNextClassOccurrence(
        dayOfWeek: Int,
        timeStr: String,
        reminderMinutesBefore: Int = 0,
        startDateStr: String = "",
        endDateStr: String = ""
    ): Long? {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val time = parseTime(timeStr) ?: return null

        // Calculate fire time by subtracting reminderMinutesBefore
        val reminderTime = time.minusMinutes(reminderMinutesBefore.toLong())

        // Map 1..7 to DayOfWeek
        val targetDay = try {
            DayOfWeek.of(dayOfWeek)
        } catch (e: Exception) {
            return null
        }

        // Check if endDate has passed
        if (endDateStr.isNotBlank()) {
            val endDate = parseDate(endDateStr)
            if (endDate != null && now.toLocalDate().isAfter(endDate)) {
                return null
            }
        }

        // Find candidate date starting today
        var candidateDate = now.toLocalDate()
        val startDate = if (startDateStr.isNotBlank()) parseDate(startDateStr) else null
        if (startDate != null && candidateDate.isBefore(startDate)) {
            candidateDate = startDate
        }

        // Advance to matching day of week
        while (candidateDate.dayOfWeek != targetDay) {
            candidateDate = candidateDate.plusDays(1)
        }

        var candidateDateTime = LocalDateTime.of(candidateDate, reminderTime)
        if (candidateDateTime.isBefore(now) || candidateDateTime.isEqual(now)) {
            // Next week's occurrence
            candidateDateTime = candidateDateTime.plusWeeks(1)
        }

        // Check if candidate exceeds end date
        if (endDateStr.isNotBlank()) {
            val endDate = parseDate(endDateStr)
            if (endDate != null && candidateDateTime.toLocalDate().isAfter(endDate)) {
                return null
            }
        }

        return candidateDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * For a single date & time event (like Homework or Exam or One-time reminder).
     */
    fun getSingleOccurrence(
        dateStr: String,
        timeStr: String,
        reminderMinutesBefore: Int = 0
    ): Long? {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val date = parseDate(dateStr) ?: return null
        val time = parseTime(timeStr) ?: return null

        val eventTime = LocalDateTime.of(date, time).minusMinutes(reminderMinutesBefore.toLong())
        if (eventTime.isBefore(now)) {
            return null // Already in the past
        }

        return eventTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * For recurring reminders (wake up, personal alarms, daily study reminders).
     */
    fun getNextReminderOccurrence(
        timeStr: String,
        repeatType: RepeatType,
        repeatDaysStr: String = "", // e.g. "1,2,3,4,5"
        reminderMinutesBefore: Int = 0
    ): Long? {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val time = parseTime(timeStr) ?: return null
        val fireTime = time.minusMinutes(reminderMinutesBefore.toLong())

        val targetDays = when (repeatType) {
            RepeatType.ONCE, RepeatType.NO_REPEAT -> emptySet()
            RepeatType.DAILY -> (1..7).toSet()
            RepeatType.WEEKDAYS -> (1..5).toSet()
            RepeatType.WEEKENDS -> setOf(6, 7)
            RepeatType.WEEKLY -> {
                setOf(now.dayOfWeek.value)
            }
            RepeatType.CUSTOM_DAYS -> {
                repeatDaysStr.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()
            }
        }

        if (targetDays.isEmpty()) {
            // Fires today if time is future, else tomorrow
            var candidate = LocalDateTime.of(now.toLocalDate(), fireTime)
            if (candidate.isBefore(now) || candidate.isEqual(now)) {
                candidate = candidate.plusDays(1)
            }
            return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        // Loop forward up to 8 days to find the closest matching day
        var candidateDate = now.toLocalDate()
        for (i in 0..7) {
            val dayVal = candidateDate.dayOfWeek.value
            if (dayVal in targetDays) {
                val candidateDateTime = LocalDateTime.of(candidateDate, fireTime)
                if (candidateDateTime.isAfter(now)) {
                    return candidateDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
            candidateDate = candidateDate.plusDays(1)
        }

        return null
    }

    fun parseTime(timeStr: String): LocalTime? {
        return try {
            LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            try {
                // Support H:mm
                LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("H:mm"))
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun parseDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            null
        }
    }

    fun formatDisplayTime(timeStr: String): String {
        val time = parseTime(timeStr) ?: return timeStr
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    fun formatDisplayDate(dateStr: String): String {
        val date = parseDate(dateStr) ?: return dateStr
        return date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }
}
