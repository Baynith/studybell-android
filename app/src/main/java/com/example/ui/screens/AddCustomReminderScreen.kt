package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderItem
import com.example.data.model.RepeatType
import com.example.ui.components.Card3D
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomReminderScreen(
    initialCategory: ReminderCategory = ReminderCategory.CUSTOM,
    initialReminderId: Long? = null,
    existingReminders: List<ReminderItem>,
    onSave: (ReminderItem) -> Unit,
    onBack: () -> Unit
) {
    val existing = remember(initialReminderId, existingReminders) {
        if (initialReminderId != null && initialReminderId > 0) {
            existingReminders.firstOrNull { it.id == initialReminderId }
        } else null
    }

    var title by remember {
        mutableStateOf(
            existing?.title ?: when (initialCategory) {
                ReminderCategory.WAKE_UP -> "Wake Up for School"
                ReminderCategory.STUDY -> "Evening Study Session"
                ReminderCategory.PERSONAL -> "Personal Reminder"
                else -> ""
            }
        )
    }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: initialCategory) }
    var time by remember {
        mutableStateOf(
            existing?.time ?: when (initialCategory) {
                ReminderCategory.WAKE_UP -> "05:30"
                ReminderCategory.STUDY -> "19:00"
                else -> LocalTime.now().plusMinutes(10).format(DateTimeFormatter.ofPattern("HH:mm"))
            }
        )
    }
    var repeatType by remember {
        mutableStateOf(
            existing?.repeatType ?: when (initialCategory) {
                ReminderCategory.WAKE_UP -> RepeatType.WEEKDAYS
                ReminderCategory.STUDY -> RepeatType.DAILY
                else -> RepeatType.ONCE
            }
        )
    }
    var repeatDays by remember { mutableStateOf(existing?.repeatDays ?: "1,2,3,4,5") }
    var vibrate by remember { mutableStateOf(existing?.vibrate ?: true) }

    val daysList = listOf(
        Pair(1, "Mon"),
        Pair(2, "Tue"),
        Pair(3, "Wed"),
        Pair(4, "Thu"),
        Pair(5, "Fri"),
        Pair(6, "Sat"),
        Pair(7, "Sun")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Reminder" else "Edit Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    ReminderItem(
                                        id = existing?.id ?: 0,
                                        title = title.trim(),
                                        description = description.trim(),
                                        category = category,
                                        time = time.trim(),
                                        repeatType = repeatType,
                                        repeatDays = repeatDays,
                                        vibrate = vibrate,
                                        isEnabled = true,
                                        isCompleted = false
                                    )
                                )
                                onBack()
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_reminder_button")
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
            // Category Chips
            item {
                Card3D(elevation = 2) {
                    Text("CATEGORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReminderCategory.values()) { cat ->
                            if (cat != ReminderCategory.CLASS && cat != ReminderCategory.HOMEWORK && cat != ReminderCategory.EXAM) {
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat.name.replace("_", " ")) }
                                )
                            }
                        }
                    }
                }
            }

            // Title & Description
            item {
                Card3D(elevation = 2) {
                    Text("REMINDER DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title *") },
                        placeholder = { Text("e.g. Wake up, Study Chemistry, Pack bag") },
                        modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes") },
                        placeholder = { Text("Optional notes or checklist") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }

            // Time & Repeat
            item {
                Card3D(elevation = 2) {
                    Text("ALARM TIME & REPEAT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth().testTag("reminder_time_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Repeat Schedule", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val repeats = listOf(
                            RepeatType.ONCE to "Once",
                            RepeatType.DAILY to "Daily",
                            RepeatType.WEEKDAYS to "Weekdays",
                            RepeatType.WEEKENDS to "Weekends",
                            RepeatType.CUSTOM_DAYS to "Custom Days"
                        )
                        items(repeats) { (type, label) ->
                            FilterChip(
                                selected = repeatType == type,
                                onClick = { repeatType = type },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (repeatType == RepeatType.CUSTOM_DAYS) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Days:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            daysList.forEach { (dayNum, label) ->
                                val currentDays = repeatDays.split(",").filter { it.isNotBlank() }
                                val isSelected = currentDays.contains(dayNum.toString())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val updated = currentDays.toMutableList()
                                        if (isSelected) {
                                            if (updated.size > 1) updated.remove(dayNum.toString())
                                        } else {
                                            updated.add(dayNum.toString())
                                        }
                                        repeatDays = updated.joinToString(",")
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Vibration switch
            item {
                Card3D(elevation = 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vibrate", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { vibrate = it }
                        )
                    }
                }
            }
        }
    }
}
