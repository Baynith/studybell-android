package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.alarm.AlarmPermissionHelper
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyCard
import com.example.ui.theme.PurplePrimary

/**
 * Interactive banner shown to prompt users when exact alarm scheduling or notifications are disabled.
 * Dynamically re-checks permissions on lifecycle resume (when returning from System Settings).
 */
@Composable
fun ExactAlarmPermissionBanner(
    modifier: Modifier = Modifier,
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var canScheduleExact by remember { mutableStateOf(AlarmPermissionHelper.canScheduleExactAlarms(context)) }
    var hasNotificationPermission by remember { mutableStateOf(AlarmPermissionHelper.isNotificationPermissionGranted(context)) }
    var isBatteryOptimizedIgnored by remember { mutableStateOf(AlarmPermissionHelper.isBatteryOptimizationIgnored(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var isDismissed by remember { mutableStateOf(false) }

    // Re-check permissions when returning to the app from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val previousExact = canScheduleExact
                canScheduleExact = AlarmPermissionHelper.canScheduleExactAlarms(context)
                hasNotificationPermission = AlarmPermissionHelper.isNotificationPermissionGranted(context)
                isBatteryOptimizedIgnored = AlarmPermissionHelper.isBatteryOptimizationIgnored(context)

                if (!previousExact && canScheduleExact) {
                    onPermissionGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val needsPermission = (!canScheduleExact || !hasNotificationPermission) && !isDismissed

    AnimatedVisibility(
        visible = needsPermission,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            modifier = modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(GoldAccent, Color(0xFFEF4444))),
                    RoundedCornerShape(20.dp)
                )
                .testTag("exact_alarm_permission_banner"),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOn,
                            contentDescription = "Exact Timing Required",
                            tint = GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!canScheduleExact) "High-Precision Exact Alarm Scheduling Authorization Required" else "High-Priority Notification Delivery Permission Needed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (!canScheduleExact) {
                                "Required on Android 12 (API level 31) and higher so that school timetable bells, class commencement chimes, and assignment reminders ring at the exact scheduled second without power-saving delays."
                            } else {
                                "Enable high-priority notification delivery permissions so you never miss an upcoming class period, homework deadline countdown, or examination alert."
                            },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 17.sp
                        )
                    }

                    IconButton(
                        onClick = { isDismissed = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDialog = true },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            "Why this is needed?",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                AlarmPermissionHelper.openExactAlarmSettings(context)
                            } else {
                                AlarmPermissionHelper.openNotificationSettings(context)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("grant_exact_alarm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Grant Permission",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        ExactAlarmExplanationDialog(
            canScheduleExact = canScheduleExact,
            hasNotificationPermission = hasNotificationPermission,
            isBatteryOptimizedIgnored = isBatteryOptimizedIgnored,
            onDismiss = { showDialog = false },
            onOpenExactAlarm = {
                AlarmPermissionHelper.openExactAlarmSettings(context)
                showDialog = false
            },
            onOpenNotifications = {
                AlarmPermissionHelper.openNotificationSettings(context)
                showDialog = false
            },
            onIgnoreBattery = {
                AlarmPermissionHelper.requestIgnoreBatteryOptimizations(context)
                showDialog = false
            }
        )
    }
}

/**
 * Educational dialog explaining Android exact alarm timing policies and requirements.
 */
@Composable
fun ExactAlarmExplanationDialog(
    canScheduleExact: Boolean,
    hasNotificationPermission: Boolean,
    isBatteryOptimizedIgnored: Boolean,
    onDismiss: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onOpenNotifications: () -> Unit,
    onIgnoreBattery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PurplePrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "System Permissions, Device Authorizations & High-Precision Alarm Accuracy",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "StudyBell operates as a 100% offline-first timetable and school bell scheduler. To ensure that your classes, homework countdown timers, and examination chimes trigger reliably with second-level precision, the Android operating system mandates the following explicit user permissions:",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Item 1: Exact Alarms
                PermissionStatusRow(
                    icon = Icons.Default.Schedule,
                    title = "Exact Alarm Scheduling Authorization (SCHEDULE_EXACT_ALARM)",
                    desc = "Authorizes StudyBell to schedule exact, unbatched alarms using the system AlarmManager, preventing Android from deferring or delaying school bell chimes by up to 30 minutes.",
                    isGranted = canScheduleExact,
                    onAction = onOpenExactAlarm,
                    actionText = "Grant Permission"
                )

                // Item 2: Notifications
                PermissionStatusRow(
                    icon = Icons.Default.Notifications,
                    title = "High-Priority Lock-Screen Notifications (POST_NOTIFICATIONS)",
                    desc = "Permits heads-up alert banners, custom ringtone chimes, and full-screen alarm screens when your mobile device is locked, motionless, or in standby mode.",
                    isGranted = hasNotificationPermission,
                    onAction = onOpenNotifications,
                    actionText = "Allow Banners"
                )

                // Item 3: Battery Saver
                PermissionStatusRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Optimization Exemption Whitelist (Doze Mode Exemption)",
                    desc = "Exempts background alarm scheduling from aggressive operating system power management and deep sleep Doze mode restrictions so morning bells ring on time.",
                    isGranted = isBatteryOptimizedIgnored,
                    onAction = onIgnoreBattery,
                    actionText = "Unrestrict App"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun PermissionStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    onAction: () -> Unit,
    actionText: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
