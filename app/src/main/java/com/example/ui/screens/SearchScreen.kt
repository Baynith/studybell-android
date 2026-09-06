package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Card3D
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.viewmodel.StudyBellUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: StudyBellUiState,
    onBack: () -> Unit,
    onSelectClass: (Long) -> Unit,
    onSelectHomework: (Long) -> Unit,
    onSelectExam: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val matchedClasses = remember(query, uiState.classes) {
        if (query.isBlank()) emptyList()
        else uiState.classes.filter {
            it.classSchedule.subject.contains(query, ignoreCase = true) ||
            it.classSchedule.teacher.contains(query, ignoreCase = true) ||
            it.classSchedule.room.contains(query, ignoreCase = true)
        }
    }

    val matchedHomework = remember(query, uiState.homework) {
        if (query.isBlank()) emptyList()
        else uiState.homework.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.subject.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    val matchedExams = remember(query, uiState.exams) {
        if (query.isBlank()) emptyList()
        else uiState.exams.filter {
            it.examName.contains(query, ignoreCase = true) ||
            it.subject.contains(query, ignoreCase = true) ||
            it.room.contains(query, ignoreCase = true)
        }
    }

    val totalMatches = matchedClasses.size + matchedHomework.size + matchedExams.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search classes, tasks, exams...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("search_query_input")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (query.isBlank()) {
                item {
                    EmptyStateCard(
                        emoji = "🔍",
                        title = "Search StudyBell",
                        subtitle = "Find any subject, teacher, homework assignment, or exam."
                    )
                }
            } else if (totalMatches == 0) {
                item {
                    EmptyStateCard(
                        emoji = "🧐",
                        title = "No results found",
                        subtitle = "No matching classes or homework found for \"$query\""
                    )
                }
            } else {
                if (matchedClasses.isNotEmpty()) {
                    item {
                        Text(
                            text = "CLASSES (${matchedClasses.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(matchedClasses) { cls ->
                        Card3D(
                            elevation = 2,
                            onClick = { onSelectClass(cls.classSchedule.id) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIconBadge(category = "CLASS", size = 40, iconSize = 20)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(cls.classSchedule.subject, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${cls.classSchedule.teacher} • ${cls.classSchedule.room}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (matchedHomework.isNotEmpty()) {
                    item {
                        Text(
                            text = "HOMEWORK (${matchedHomework.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(matchedHomework) { hw ->
                        Card3D(
                            elevation = 2,
                            onClick = { onSelectHomework(hw.id) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIconBadge(category = "HOMEWORK", size = 40, iconSize = 20)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(hw.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${hw.subject} • Due ${hw.dueDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (matchedExams.isNotEmpty()) {
                    item {
                        Text(
                            text = "EXAMS (${matchedExams.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(matchedExams) { exam ->
                        Card3D(
                            elevation = 2,
                            onClick = { onSelectExam(exam.id) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIconBadge(category = "EXAM", size = 40, iconSize = 20)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(exam.examName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${exam.subject} • ${exam.date} at ${exam.time}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
