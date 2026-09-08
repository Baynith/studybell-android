package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.NextOccurrenceEngine
import com.example.ads.AdMobBanner
import com.example.ui.components.Card3D
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.CoralPink
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldDark
import com.example.ui.theme.PurplePrimary
import com.example.ui.viewmodel.StudyBellUiState
import com.example.ui.viewmodel.StudyBellViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    uiState: StudyBellUiState,
    viewModel: StudyBellViewModel,
    onAddEvent: (String) -> Unit
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Generate days in month
    val firstDayOfMonth = currentYearMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1 // 0 for Mon ... 6 for Sun
    val daysInMonth = currentYearMonth.lengthOfMonth()

    val calendarCells = buildList {
        repeat(dayOfWeekOffset) { add(null) }
        for (day in 1..daysInMonth) {
            add(currentYearMonth.atDay(day))
        }
    }

    val selectedDateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val selectedDayOfWeek = selectedDate.dayOfWeek.value

    // Gather events for selected date
    val dayEvents = remember(selectedDate, uiState) {
        val list = mutableListOf<CalendarEventItem>()

        // 1. Classes on this day of week
        for (c in uiState.classes) {
            if (!c.classSchedule.isEnabled) continue
            for (d in c.days) {
                if (d.dayOfWeek == selectedDayOfWeek) {
                    list.add(
                        CalendarEventItem(
                            title = c.classSchedule.subject,
                            subtitle = "${c.classSchedule.teacher} • ${c.classSchedule.room}",
                            time = "${NextOccurrenceEngine.formatDisplayTime(d.startTime)} - ${NextOccurrenceEngine.formatDisplayTime(d.endTime)}",
                            category = "CLASS",
                            color = PurplePrimary
                        )
                    )
                }
            }
        }

        // 2. Homework due on this date
        for (h in uiState.homework) {
            if (h.dueDate == selectedDateStr) {
                list.add(
                    CalendarEventItem(
                        title = "${h.subject} Homework",
                        subtitle = h.title,
                        time = "Due at ${NextOccurrenceEngine.formatDisplayTime(h.dueTime)}",
                        category = "HOMEWORK",
                        color = GoldDark
                    )
                )
            }
        }

        // 3. Exams on this date
        for (e in uiState.exams) {
            if (e.date == selectedDateStr) {
                list.add(
                    CalendarEventItem(
                        title = "${e.subject} Exam",
                        subtitle = "${e.examName} • ${e.room}",
                        time = "Exam at ${NextOccurrenceEngine.formatDisplayTime(e.time)}",
                        category = "EXAM",
                        color = CoralPink
                    )
                )
            }
        }

        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header with Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "School Calendar & Exam Planner",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }
        }

        // Calendar Grid Card
        item {
            Card3D(elevation = 3) {
                // Day-of-week header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dates Grid (chunks of 7)
                calendarCells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { date ->
                            if (date == null) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()
                                val hasEvents = dateHasEvents(date, uiState)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) PurplePrimary
                                            else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                                            else Color.Transparent
                                        )
                                        .clickable { selectedDate = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (hasEvents) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else GoldDark)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Agenda
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { onAddEvent("Class") }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Event", fontSize = 13.sp)
                }
            }
        }

        if (dayEvents.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "☀️",
                    title = "Nothing scheduled for this day",
                    subtitle = "No classes, homework, or exams on this date."
                )
            }
        } else {
            dayEvents.forEachIndexed { index, event ->
                item(key = "event_${event.title}_${event.time}_$index") {
                    Card3D(elevation = 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                category = event.category,
                                size = 42,
                                iconSize = 22
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = event.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = event.time,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Insert ad banner between events
                if (index > 0 && index % 2 == 1) {
                    item(key = "cal_ad_between_$index") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SPONSORED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                AdMobBanner()
                            }
                        }
                    }
                }
            }

            item(key = "cal_bottom_ad") {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SPONSORED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        AdMobBanner()
                    }
                }
            }
        }
    }
}

private data class CalendarEventItem(
    val title: String,
    val subtitle: String,
    val time: String,
    val category: String,
    val color: Color
)

private fun dateHasEvents(date: LocalDate, uiState: StudyBellUiState): Boolean {
    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val dayOfWeek = date.dayOfWeek.value

    val hasClass = uiState.classes.any { it.classSchedule.isEnabled && it.days.any { d -> d.dayOfWeek == dayOfWeek } }
    val hasHw = uiState.homework.any { it.dueDate == dateStr }
    val hasExam = uiState.exams.any { it.date == dateStr }

    return hasClass || hasHw || hasExam
}
