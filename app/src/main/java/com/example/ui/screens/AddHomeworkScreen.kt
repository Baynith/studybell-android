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
import com.example.data.model.Homework
import com.example.data.model.TaskStatus
import com.example.ui.components.Card3D
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHomeworkScreen(
    initialHomeworkId: Long? = null,
    existingHomeworkList: List<Homework>,
    onSave: (Homework) -> Unit,
    onBack: () -> Unit
) {
    val existing = remember(initialHomeworkId, existingHomeworkList) {
        if (initialHomeworkId != null && initialHomeworkId > 0) {
            existingHomeworkList.firstOrNull { it.id == initialHomeworkId }
        } else null
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var subject by remember { mutableStateOf(existing?.subject ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var dueDate by remember {
        mutableStateOf(
            existing?.dueDate ?: LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }
    var dueTime by remember { mutableStateOf(existing?.dueTime ?: "08:00") }
    var teacher by remember { mutableStateOf(existing?.teacher ?: "") }
    var reminderOffsetMinutes by remember { mutableIntStateOf(existing?.reminderMinutesBefore ?: 720) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add Homework" else "Edit Homework", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && subject.isNotBlank()) {
                                onSave(
                                    Homework(
                                        id = existing?.id ?: 0,
                                        title = title.trim(),
                                        subject = subject.trim(),
                                        description = description.trim(),
                                        dueDate = dueDate.trim(),
                                        dueTime = dueTime.trim(),
                                        reminderMinutesBefore = reminderOffsetMinutes,
                                        teacher = teacher.trim(),
                                        status = existing?.status ?: TaskStatus.NOT_STARTED,
                                        isEnabled = true
                                    )
                                )
                                onBack()
                            }
                        },
                        enabled = title.isNotBlank() && subject.isNotBlank(),
                        modifier = Modifier.testTag("save_homework_button")
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
            item {
                Card3D(elevation = 2) {
                    Text("TASK DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task / Assignment Title *") },
                        placeholder = { Text("e.g. Calculus Problem Set 4") },
                        modifier = Modifier.fillMaxWidth().testTag("homework_title_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject *") },
                        placeholder = { Text("e.g. Mathematics, Science") },
                        modifier = Modifier.fillMaxWidth().testTag("homework_subject_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher (Optional)") },
                        placeholder = { Text("e.g. Mr. Smith") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Instructions / Description") },
                        placeholder = { Text("e.g. Questions 1 to 15, show working") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            }

            // Due Date & Time
            item {
                Card3D(elevation = 2) {
                    Text("DUE DATE & TIME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick date chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val today = LocalDate.now()
                        val quickDates = listOf(
                            "Today" to today,
                            "Tomorrow" to today.plusDays(1),
                            "In 3 Days" to today.plusDays(3),
                            "Next Week" to today.plusWeeks(1)
                        )
                        items(quickDates) { (label, date) ->
                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            FilterChip(
                                selected = dueDate == dateStr,
                                onClick = { dueDate = dateStr },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = dueTime,
                            onValueChange = { dueTime = it },
                            label = { Text("Due Time (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // Reminder timing
            item {
                Card3D(elevation = 2) {
                    Text("REMINDER BEFORE DUE TIME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    val reminderOptions = listOf(
                        60 to "1 hour before",
                        360 to "6 hours before",
                        720 to "Evening before (12h)",
                        1440 to "1 day before"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        reminderOptions.forEach { (mins, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 14.sp)
                                RadioButton(
                                    selected = reminderOffsetMinutes == mins,
                                    onClick = { reminderOffsetMinutes = mins }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
