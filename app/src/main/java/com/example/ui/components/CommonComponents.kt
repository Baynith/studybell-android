package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun Card3D(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    elevation: Int = 4,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation.dp,
                shape = shape,
                ambientColor = NavyDeep.copy(alpha = 0.08f),
                spotColor = NavyDeep.copy(alpha = 0.15f)
            )
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
fun CategoryIconBadge(
    category: String,
    modifier: Modifier = Modifier,
    size: Int = 46,
    iconSize: Int = 26
) {
    val (icon, bgGradient) = when (category) {
        "CLASS", "CLASS_REMINDER" -> Pair(
            Icons.Default.Science,
            Brush.linearGradient(listOf(PurplePrimary, Color(0xFF8B5CF6)))
        )
        "CLASS_END" -> Pair(
            Icons.Default.NotificationsNone,
            Brush.linearGradient(listOf(SkyBlue, Color(0xFF0284C7)))
        )
        "HOMEWORK" -> Pair(
            Icons.Default.MenuBook,
            Brush.linearGradient(listOf(GoldAccent, GoldDark))
        )
        "EXAM" -> Pair(
            Icons.Default.School,
            Brush.linearGradient(listOf(CoralPink, Color(0xFFE11D48)))
        )
        "WAKE_UP" -> Pair(
            Icons.Default.Alarm,
            Brush.linearGradient(listOf(GoldAccent, Color(0xFFF59E0B)))
        )
        "STUDY" -> Pair(
            Icons.Default.AutoStories,
            Brush.linearGradient(listOf(EmeraldGreen, Color(0xFF059669)))
        )
        else -> Pair(
            Icons.Default.NotificationsActive,
            Brush.linearGradient(listOf(PurplePrimary, SkyBlue))
        )
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgGradient)
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = Color.White,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PurplePrimary
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    emoji: String = "📅",
    title: String,
    subtitle: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 42.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onButtonClick,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
