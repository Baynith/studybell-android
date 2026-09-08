package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.NextOccurrenceEngine
import com.example.ads.AdMobBanner
import com.example.data.model.Homework
import com.example.data.model.TaskStatus
import com.example.ui.components.Card3D
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldDark
import com.example.ui.theme.PurplePrimary
import com.example.ui.viewmodel.StudyBellUiState
import com.example.ui.viewmodel.StudyBellViewModel

@Composable
fun TasksScreen(
    uiState: StudyBellUiState,
    viewModel: StudyBellViewModel,
    onAddHomework: () -> Unit,
    onEditHomework: (Long) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Pending") } // "All", "Pending", "Completed"
    var selectedSubject by remember { mutableStateOf("All") }

    val allSubjects = remember(uiState.homework) {
        listOf("All") + uiState.homework.map { it.subject }.distinct()
    }

    val filteredHomework = uiState.homework.filter { hw ->
        val statusMatches = when (selectedFilter) {
            "Pending" -> hw.status != TaskStatus.COMPLETED
            "Completed" -> hw.status == TaskStatus.COMPLETED
            else -> true
        }
        val subjectMatches = selectedSubject == "All" || hw.subject.equals(selectedSubject, ignoreCase = true)
        statusMatches && subjectMatches
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tasks & Homework",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${uiState.homework.count { it.status == TaskStatus.COMPLETED }}/${uiState.homework.size} Completed",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onAddHomework,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("add_homework_top_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Task")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Tabs (All, Pending, Completed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Pending", "All", "Completed").forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    modifier = Modifier.testTag("task_filter_$filter")
                )
            }
        }

        // Subject Filter row if multiple subjects exist
        if (allSubjects.size > 2) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(allSubjects) { subj ->
                    val isSelected = selectedSubject == subj
                    SuggestionChip(
                        onClick = { selectedSubject = subj },
                        label = { Text(subj, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) PurplePrimary.copy(alpha = 0.15f) else Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredHomework.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    emoji = if (selectedFilter == "Completed") "🎯" else "✨",
                    title = if (selectedFilter == "Completed") "No completed tasks yet" else "All caught up!",
                    subtitle = if (selectedFilter == "Completed") "Complete homework by checking the box." else "You have zero pending homework or tasks.",
                    buttonText = "Add Homework",
                    onButtonClick = onAddHomework
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                filteredHomework.forEachIndexed { index, hw ->
                    item(key = hw.id) {
                        HomeworkCard(
                            homework = hw,
                            onToggle = { viewModel.toggleHomeworkStatus(hw) },
                            onEdit = { onEditHomework(hw.id) },
                            onDelete = { viewModel.deleteHomework(hw.id) }
                        )
                    }

                    // Insert ad banner between homework tasks
                    if (index > 0 && index % 2 == 1) {
                        item(key = "tasks_ad_between_$index") {
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

                item(key = "tasks_bottom_ad") {
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

@Composable
fun HomeworkCard(
    homework: Homework,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = homework.status == TaskStatus.COMPLETED
    var showMenu by remember { mutableStateOf(false) }

    Card3D(
        elevation = 2,
        modifier = Modifier.testTag("homework_card_${homework.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldGreen,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.testTag("homework_checkbox_${homework.id}")
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = homework.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )

                Text(
                    text = homework.subject + if (homework.teacher.isNotBlank()) " • ${homework.teacher}" else "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (homework.description.isNotBlank()) {
                    Text(
                        text = homework.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDone) EmeraldGreen.copy(alpha = 0.15f) else GoldDark.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Due: ${NextOccurrenceEngine.formatDisplayDate(homework.dueDate)} at ${NextOccurrenceEngine.formatDisplayTime(homework.dueTime)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDone) EmeraldGreen else GoldDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

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
                        text = { Text("Edit Task") },
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
