package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Card3D
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExactAlarmPermissionBanner
import com.example.ui.components.SectionHeader
import com.example.ui.auth.AuthDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextUpItem
import com.example.ui.viewmodel.StudyBellUiState
import com.example.ui.viewmodel.StudyBellViewModel
import com.example.ui.viewmodel.TodayScheduleItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    uiState: StudyBellUiState,
    viewModel: StudyBellViewModel,
    onNavigateToAdd: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToItem: (String, Long) -> Unit
) {
    val authUser by viewModel.authUserState.collectAsState()
    var showAuthDialog by remember { mutableStateOf(false) }

    val currentDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    }
    val greeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // 1. Top Bar: Greeting & Logo & Test Alarm
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.studybell_icon_1788610146906),
                        contentDescription = "StudyBell Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$greeting, ${if (authUser.isLoggedIn && authUser.displayName.isNotBlank()) authUser.displayName else uiState.profile.name} 👋",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currentDate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier.testTag("home_auth_button")
                    ) {
                        if (authUser.isLoggedIn) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = "Firebase Connected (${authUser.email})",
                                tint = EmeraldGreen
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = "Sign in with Firebase",
                                tint = PurplePrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Exact Alarm & Notifications Permission Banner (Android 12+ timing compliance)
        item {
            ExactAlarmPermissionBanner(
                onPermissionGranted = {
                    viewModel.rescheduleAllAlarms()
                }
            )
        }

        // 2. Offline Test Alarm Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GoldAccent.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldDark.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = GoldDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "100% Offline Alarms",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.testAlarm(5) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GoldDark,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).testTag("test_alarm_button")
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Alarm (5s)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. NEXT UP Hero Card (as shown in design mockups!)
        item {
            NextUpHeroCard(nextUp = uiState.nextUp)
        }

        // 4. Quick Actions Row
        item {
            Column {
                Text(
                    text = "Quick Add",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    val actions = listOf(
                        Triple("Class", Icons.Default.Science, PurplePrimary),
                        Triple("Homework", Icons.Default.MenuBook, GoldDark),
                        Triple("Exam", Icons.Default.School, CoralPink),
                        Triple("Alarm", Icons.Default.Alarm, SkyBlue),
                        Triple("Reminder", Icons.Default.NotificationsActive, EmeraldGreen)
                    )
                    items(actions) { (label, icon, color) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .clickable { onNavigateToAdd(label) }
                                .testTag("quick_action_${label.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pomodoro Study Session Card
        item {
            Card3D(
                elevation = 2,
                onClick = { viewModel.setTab(com.example.ui.viewmodel.NavTab.TIMER) },
                modifier = Modifier.testTag("home_study_timer_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PurplePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = PurplePrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pomodoro Study Timer", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Focus intervals & break alerts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FilledTonalButton(
                        onClick = { viewModel.setTab(com.example.ui.viewmodel.NavTab.TIMER) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Timer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. TODAY Chronological Timeline
        item {
            SectionHeader(
                title = "Today's Schedule",
                subtitle = "${uiState.todaySchedule.size} events scheduled for today"
            )
        }

        if (uiState.todaySchedule.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🎉",
                    title = "Your day is clear",
                    subtitle = "No upcoming classes, homework, or alarms today.",
                    buttonText = "Add First Class",
                    onButtonClick = { onNavigateToAdd("Class") }
                )
            }
        } else {
            items(uiState.todaySchedule, key = { it.id }) { item ->
                TodayScheduleRow(
                    item = item,
                    onClick = {
                        onNavigateToItem(item.category, item.originalId)
                    }
                )
            }
        }

        // Sponsored AdMob Banner
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("home_admob_banner")
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ADVERTISEMENT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    com.example.ads.AdMobBanner()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAuthDialog) {
        val context = LocalContext.current
        AuthDialog(
            authManager = viewModel.authManager,
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = { displayName, email ->
                if (displayName.isNotBlank() && uiState.profile.name == "Alex") {
                    viewModel.updateProfile(uiState.profile.copy(name = displayName))
                }
                android.widget.Toast.makeText(context, "Welcome, $displayName! Firebase synced.", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun NextUpHeroCard(nextUp: NextUpItem?) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDeep),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
            .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = NavyDeep.copy(0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1E3A8A).copy(alpha = 0.5f), Color.Transparent),
                        radius = 800f
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GoldAccent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "NEXT UP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    if (nextUp != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PurplePrimary.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = nextUp.startsInText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleLight,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (nextUp != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nextUp.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (nextUp.teacher.isNotBlank() || nextUp.room.isNotBlank()) {
                                Text(
                                    text = "${nextUp.teacher} ${if (nextUp.room.isNotBlank()) "• ${nextUp.room}" else ""}",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Prominent 3D Time Badge
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = nextUp.time,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No upcoming classes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Your timetable is clear right now.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TodayScheduleRow(
    item: TodayScheduleItem,
    onClick: () -> Unit
) {
    Card3D(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_item_${item.id}"),
        onClick = onClick,
        elevation = 2
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                category = item.category,
                size = 46,
                iconSize = 24
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = item.time,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (item.isCompleted) {
                    Text(
                        text = "Completed",
                        fontSize = 11.sp,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
