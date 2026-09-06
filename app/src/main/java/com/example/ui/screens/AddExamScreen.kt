package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.Exam
import com.example.ui.components.Card3D
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExamScreen(
    initialExamId: Long? = null,
    existingExams: List<Exam>,
    onSave: (Exam) -> Unit,
    onBack: () -> Unit
) {
    val existing = remember(initialExamId, existingExams) {
        if (initialExamId != null && initialExamId > 0) {
            existingExams.firstOrNull { it.id == initialExamId }
        } else null
    }

    var examName by remember { mutableStateOf(existing?.examName ?: "") }
    var subject by remember { mutableStateOf(existing?.subject ?: "") }
    var date by remember {
        mutableStateOf(existing?.date ?: LocalDate.now().plusWeeks(2).format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
    var time by remember { mutableStateOf(existing?.time ?: "09:00") }
    var room by remember { mutableStateOf(existing?.room ?: "") }
    var teacher by remember { mutableStateOf(existing?.teacher ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var reminderOptions by remember { mutableStateOf(existing?.reminderOptions ?: "7d,3d,1d,1h") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add Exam" else "Edit Exam", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (examName.isNotBlank() && subject.isNotBlank()) {
                                onSave(
                                    Exam(
                                        id = existing?.id ?: 0,
                                        examName = examName.trim(),
                                        subject = subject.trim(),
                                        date = date.trim(),
                                        time = time.trim(),
                                        room = room.trim(),
                                        teacher = teacher.trim(),
                                        notes = notes.trim(),
                                        reminderOptions = reminderOptions,
                                        isEnabled = true
                                    )
                                )
                                onBack()
                            }
                        },
                        enabled = examName.isNotBlank() && subject.isNotBlank(),
                        modifier = Modifier.testTag("save_exam_button")
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
                    Text("EXAM INFORMATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("Exam Name *") },
                        placeholder = { Text("e.g. Midterm Physics, Final Calculus") },
                        modifier = Modifier.fillMaxWidth().testTag("exam_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject *") },
                        placeholder = { Text("e.g. Physics, History") },
                        modifier = Modifier.fillMaxWidth().testTag("exam_subject_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room / Examination Hall") },
                        placeholder = { Text("e.g. Hall B, Gym") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher / Examiner") },
                        placeholder = { Text("e.g. Mr. John") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Topics / Exam Rules") },
                        placeholder = { Text("e.g. Bring scientific calculator & ID card") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }

            // Date & Time
            item {
                Card3D(elevation = 2) {
                    Text("EXAM SCHEDULE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Time (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // Auto Reminders (7d, 3d, 1d, 1h)
            item {
                Card3D(elevation = 2) {
                    Text("COUNTDOWN REMINDERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Receive multiple preparation reminders ahead of the exam.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    val options = listOf(
                        "7d" to "7 days before",
                        "3d" to "3 days before",
                        "1d" to "1 day before",
                        "1h" to "1 hour before"
                    )

                    val currentList = reminderOptions.split(",").map { it.trim() }

                    options.forEach { (code, label) ->
                        val isSelected = currentList.contains(code)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 14.sp)
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val updated = currentList.toMutableList()
                                    if (checked) {
                                        if (!updated.contains(code)) updated.add(code)
                                    } else {
                                        updated.remove(code)
                                    }
                                    reminderOptions = updated.joinToString(",")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
