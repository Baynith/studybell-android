package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskStatus
import com.example.ui.components.Card3D
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudyBellUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    uiState: StudyBellUiState,
    onBack: () -> Unit
) {
    val totalClasses = uiState.classes.size
    val totalHomework = uiState.homework.size
    val completedHomework = uiState.homework.count { it.status == TaskStatus.COMPLETED }
    val pendingHomework = totalHomework - completedHomework
    val upcomingExams = uiState.exams.size

    val completionRate = if (totalHomework > 0) {
        ((completedHomework.toFloat() / totalHomework.toFloat()) * 100).toInt()
    } else 100

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productivity & Stats", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Score Card
            item {
                Card3D(
                    elevation = 3,
                    backgroundColor = NavyDeep
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HOMEWORK COMPLETION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$completionRate%",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "$completedHomework of $totalHomework tasks completed",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Key Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Classes / Week",
                        value = "${uiState.classes.sumOf { it.days.size }}",
                        accentColor = PurplePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pending Tasks",
                        value = "$pendingHomework",
                        accentColor = GoldDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Exams Ahead",
                        value = "$upcomingExams",
                        accentColor = CoralPink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Activity Bar Chart
            item {
                Card3D(elevation = 2) {
                    Text("WEEKLY CLASS LOAD", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Number of classes scheduled per day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val dayCounts = (1..7).map { dayNum ->
                        uiState.classes.sumOf { cls -> cls.days.count { it.dayOfWeek == dayNum } }
                    }
                    val maxCount = (dayCounts.maxOrNull() ?: 1).coerceAtLeast(1)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayCounts.forEachIndexed { index, count ->
                            val heightFraction = count.toFloat() / maxCount.toFloat()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (count > 0) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((90 * heightFraction).coerceAtLeast(6f).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (count > 0) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dayNames[index],
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card3D(modifier = modifier, elevation = 2) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
    }
}
