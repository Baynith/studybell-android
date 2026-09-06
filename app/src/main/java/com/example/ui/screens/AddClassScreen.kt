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
import com.example.data.model.ClassSchedule
import com.example.data.model.ClassWithDays
import com.example.data.model.ScheduleDay
import com.example.ui.components.Card3D
import com.example.ui.theme.*

data class EditableDayConfig(
    val dayOfWeek: Int,
    var startTime: String = "08:00",
    var endTime: String = "09:00",
    var reminderMinutes: String = "15"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassScreen(
    initialClassId: Long? = null,
    existingClasses: List<ClassWithDays>,
    onSave: (ClassSchedule, List<ScheduleDay>) -> Unit,
    onBack: () -> Unit
) {
    val existing = remember(initialClassId, existingClasses) {
        if (initialClassId != null && initialClassId > 0) {
            existingClasses.firstOrNull { it.classSchedule.id == initialClassId }
        } else null
    }

    var subject by remember { mutableStateOf(existing?.classSchedule?.subject ?: "") }
    var teacher by remember { mutableStateOf(existing?.classSchedule?.teacher ?: "") }
    var room by remember { mutableStateOf(existing?.classSchedule?.room ?: "") }
    var notes by remember { mutableStateOf(existing?.classSchedule?.notes ?: "") }
    var notifyClassEnd by remember { mutableStateOf(existing?.classSchedule?.notifyClassEnd ?: true) }
    var selectedColorHex by remember { mutableStateOf(existing?.classSchedule?.colorHex ?: "#6366F1") }

    // Multi-day selection with independent start/end times per day!
    val dayConfigs = remember {
        val initialMap = mutableStateMapOf<Int, EditableDayConfig>()
        if (existing != null && existing.days.isNotEmpty()) {
            existing.days.forEach { d ->
                initialMap[d.dayOfWeek] = EditableDayConfig(
                    dayOfWeek = d.dayOfWeek,
                    startTime = d.startTime,
                    endTime = d.endTime,
                    reminderMinutes = d.reminderMinutesBefore
                )
            }
        } else {
            // Default Monday, Wednesday, Friday
            initialMap[1] = EditableDayConfig(1, "08:00", "09:00", "15")
            initialMap[3] = EditableDayConfig(3, "10:30", "11:30", "15")
            initialMap[5] = EditableDayConfig(5, "09:00", "10:00", "15")
        }
        initialMap
    }

    val daysList = listOf(
        Pair(1, "Monday"),
        Pair(2, "Tuesday"),
        Pair(3, "Wednesday"),
        Pair(4, "Thursday"),
        Pair(5, "Friday"),
        Pair(6, "Saturday"),
        Pair(7, "Sunday")
    )

    val colorOptions = listOf(
        "#6366F1", "#F59E0B", "#10B981", "#EF4444", "#0EA5E9", "#8B5CF6", "#EC4899"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add Class" else "Edit Class", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && dayConfigs.isNotEmpty()) {
                                val schedule = ClassSchedule(
                                    id = existing?.classSchedule?.id ?: 0,
                                    subject = subject.trim(),
                                    teacher = teacher.trim(),
                                    room = room.trim(),
                                    notes = notes.trim(),
                                    isEnabled = true,
                                    notifyClassEnd = notifyClassEnd,
                                    colorHex = selectedColorHex
                                )
                                val days = dayConfigs.values.map {
                                    ScheduleDay(
                                        classScheduleId = schedule.id,
                                        dayOfWeek = it.dayOfWeek,
                                        startTime = it.startTime,
                                        endTime = it.endTime,
                                        reminderMinutesBefore = it.reminderMinutes
                                    )
                                }
                                onSave(schedule, days)
                                onBack()
                            }
                        },
                        enabled = subject.isNotBlank() && dayConfigs.isNotEmpty(),
                        modifier = Modifier.testTag("save_class_button")
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Details Card
            item {
                Card3D(elevation = 2) {
                    Text("CLASS INFORMATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject Name *") },
                        placeholder = { Text("e.g. Science, Mathematics") },
                        modifier = Modifier.fillMaxWidth().testTag("class_subject_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher") },
                        placeholder = { Text("e.g. Mr. John, Ms. Lily") },
                        modifier = Modifier.fillMaxWidth().testTag("class_teacher_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room / Location") },
                        placeholder = { Text("e.g. Lab 2, Room 204") },
                        modifier = Modifier.fillMaxWidth().testTag("class_room_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Materials") },
                        placeholder = { Text("e.g. Bring textbook & lab coat") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }

            // Theme Color Picker
            item {
                Card3D(elevation = 2) {
                    Text("ACCENT COLOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(colorOptions) { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 3.dp else 0.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Day Selection Header
            item {
                Column {
                    Text("SCHEDULE DAYS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Select days when this class takes place. Each day can have its own independent start and end time.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Day Selector Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(daysList) { (dayNum, dayName) ->
                        val isSelected = dayConfigs.containsKey(dayNum)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    if (dayConfigs.size > 1) {
                                        dayConfigs.remove(dayNum)
                                    }
                                } else {
                                    dayConfigs[dayNum] = EditableDayConfig(dayNum)
                                }
                            },
                            label = { Text(dayName.take(3)) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Custom Time Configs for EACH Selected Day
            items(daysList.filter { dayConfigs.containsKey(it.first) }) { (dayNum, dayName) ->
                val config = dayConfigs[dayNum] ?: return@items
                var startTime by remember(config) { mutableStateOf(config.startTime) }
                var endTime by remember(config) { mutableStateOf(config.endTime) }
                var reminderMins by remember(config) { mutableStateOf(config.reminderMinutes) }

                Card3D(elevation = 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🗓️ $dayName Schedule",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = {
                                startTime = it
                                config.startTime = it
                            },
                            label = { Text("Start Time") },
                            placeholder = { Text("08:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = endTime,
                            onValueChange = {
                                endTime = it
                                config.endTime = it
                            },
                            label = { Text("End Time") },
                            placeholder = { Text("09:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reminder Timing multi-choice
                    Text("Reminder Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("0" to "At Time", "5" to "5m", "10" to "10m", "15" to "15m", "30" to "30m").forEach { (valStr, label) ->
                            val isSel = reminderMins.split(",").contains(valStr)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    val currentList = reminderMins.split(",").filter { it.isNotBlank() }.toMutableList()
                                    if (isSel) {
                                        if (currentList.size > 1) currentList.remove(valStr)
                                    } else {
                                        currentList.add(valStr)
                                    }
                                    reminderMins = currentList.joinToString(",")
                                    config.reminderMinutes = reminderMins
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Class End Alert Toggle
            item {
                Card3D(elevation = 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Class-End Notification", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Notify when class concludes (e.g. \"Science has ended\")", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = notifyClassEnd,
                            onCheckedChange = { notifyClassEnd = it }
                        )
                    }
                }
            }
        }
    }
}
