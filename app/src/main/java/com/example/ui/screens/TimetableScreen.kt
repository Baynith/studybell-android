package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.NextOccurrenceEngine
import com.example.ads.AdMobBanner
import com.example.data.model.ClassWithDays
import com.example.data.model.ScheduleDay
import com.example.ui.components.Card3D
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PurplePrimary
import com.example.ui.viewmodel.StudyBellUiState
import com.example.ui.viewmodel.StudyBellViewModel
import java.time.LocalDate

@Composable
fun TimetableScreen(
    uiState: StudyBellUiState,
    viewModel: StudyBellViewModel,
    onAddClass: () -> Unit,
    onEditClass: (Long) -> Unit
) {
    val currentDayOfWeek = remember { LocalDate.now().dayOfWeek.value }
    var selectedDay by remember { mutableIntStateOf(currentDayOfWeek) }
    var isWeekView by remember { mutableStateOf(false) }

    val daysList = listOf(
        Pair(1, "Mon"),
        Pair(2, "Tue"),
        Pair(3, "Wed"),
        Pair(4, "Thu"),
        Pair(5, "Fri"),
        Pair(6, "Sat"),
        Pair(7, "Sun")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title and View Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Timetable",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${uiState.classes.size} Total Registered Classes",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // View toggle chip
            FilterChip(
                selected = isWeekView,
                onClick = { isWeekView = !isWeekView },
                label = { Text(if (isWeekView) "Week View" else "Day View") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isWeekView) Icons.Default.ViewWeek else Icons.Default.CalendarViewDay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Day of week selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(daysList) { (dayNum, dayLabel) ->
                val isSelected = selectedDay == dayNum && !isWeekView
                val isToday = dayNum == currentDayOfWeek

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) PurplePrimary else MaterialTheme.colorScheme.outlineVariant.copy(0.5f)
                    ),
                    modifier = Modifier
                        .clickable {
                            isWeekView = false
                            selectedDay = dayNum
                        }
                        .testTag("day_tab_$dayNum")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = dayLabel,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else PurplePrimary)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Classes content
        if (isWeekView) {
            // Full 7-day overview
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(daysList) { (dayNum, dayLabel) ->
                    val dayClasses = getClassesForDay(uiState.classes, dayNum)
                    if (dayClasses.isNotEmpty()) {
                        Column {
                            Text(
                                text = "$dayLabel (${fullDayName(dayNum)})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            dayClasses.forEachIndexed { idx, (cls, daySchedule) ->
                                ClassTimetableCard(
                                    classWithDays = cls,
                                    daySchedule = daySchedule,
                                    onEdit = { onEditClass(cls.classSchedule.id) },
                                    onToggle = { viewModel.toggleClassEnabled(cls) },
                                    onDelete = { viewModel.deleteClass(cls.classSchedule.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Ad banner between every 2 schedule items
                                if (idx > 0 && idx % 2 == 1) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
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
        } else {
            // Single Day View
            val dayClasses = getClassesForDay(uiState.classes, selectedDay)
            if (dayClasses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                        emoji = "☕",
                        title = "No classes on ${fullDayName(selectedDay)}",
                        subtitle = "Enjoy your free time or schedule a study session.",
                        buttonText = "Add Class for ${fullDayName(selectedDay)}",
                        onButtonClick = onAddClass
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    dayClasses.forEachIndexed { index, (cls, daySchedule) ->
                        item(key = cls.classSchedule.id.toString() + "_" + daySchedule.id) {
                            ClassTimetableCard(
                                classWithDays = cls,
                                daySchedule = daySchedule,
                                onEdit = { onEditClass(cls.classSchedule.id) },
                                onToggle = { viewModel.toggleClassEnabled(cls) },
                                onDelete = { viewModel.deleteClass(cls.classSchedule.id) }
                            )
                        }

                        // Insert ad banner between classes
                        if (index > 0 && index % 2 == 1) {
                            item(key = "ad_between_class_$index") {
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

                    // Bottom banner
                    item(key = "timetable_bottom_ad") {
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
    }
}

@Composable
fun ClassTimetableCard(
    classWithDays: ClassWithDays,
    daySchedule: ScheduleDay,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card3D(
        elevation = 3,
        modifier = Modifier.testTag("class_card_${classWithDays.classSchedule.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                category = "CLASS",
                size = 46,
                iconSize = 24
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classWithDays.classSchedule.subject,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val info = listOfNotNull(
                    classWithDays.classSchedule.teacher.takeIf { it.isNotBlank() },
                    classWithDays.classSchedule.room.takeIf { it.isNotBlank() }
                ).joinToString(" • ")

                if (info.isNotBlank()) {
                    Text(
                        text = info,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start-End Time Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${NextOccurrenceEngine.formatDisplayTime(daySchedule.startTime)} - ${NextOccurrenceEngine.formatDisplayTime(daySchedule.endTime)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Reminder offset pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PurplePrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "🔔 ${daySchedule.reminderMinutesBefore}m before",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurplePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = classWithDays.classSchedule.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("class_toggle_${classWithDays.classSchedule.id}")
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Class") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getClassesForDay(
    classes: List<ClassWithDays>,
    dayOfWeek: Int
): List<Pair<ClassWithDays, ScheduleDay>> {
    val results = mutableListOf<Pair<ClassWithDays, ScheduleDay>>()
    for (cls in classes) {
        for (day in cls.days) {
            if (day.dayOfWeek == dayOfWeek) {
                results.add(Pair(cls, day))
            }
        }
    }
    return results.sortedBy { it.second.startTime }
}

private fun fullDayName(day: Int): String = when (day) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    else -> "Day $day"
}
