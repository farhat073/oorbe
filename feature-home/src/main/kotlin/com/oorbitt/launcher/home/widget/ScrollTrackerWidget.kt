package com.oorbitt.launcher.home.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorbitt.launcher.orbspace.ScrollTrackerService
import com.oorbitt.launcher.ui.util.rememberAppIcon

@Composable
fun ScrollTrackerWidget(
    dailyLimit: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bgOpacity: Int = 40,
    heightDp: Int = 120,
    cornerRadiusDp: Int = 20,
    accentColorHex: String = "#007AFF",
    activeIconPack: String? = null
) {
    val context = LocalContext.current
    val todayStats by ScrollTrackerService.todayStats.collectAsState()
    val totalScrolls = todayStats.values.sum()
    val isServiceRunning by ScrollTrackerService.isServiceRunning.collectAsState()

    val accentColor = remember(accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (e: Exception) {
            Color(0xFF007AFF)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = heightDp.dp)
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(Color.Black.copy(alpha = bgOpacity / 100f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f),
                        accentColor.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadiusDp.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Wellness Tracker",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isServiceRunning) "Monitoring active scrolls" else "Accessibility service off",
                        color = if (isServiceRunning) accentColor else Color.Red.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                
                Text(
                    text = "$totalScrolls scrolls",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (dailyLimit > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val progress = (totalScrolls.toFloat() / dailyLimit).coerceIn(0f, 1f)
                val progressColor = when {
                    progress >= 1f -> Color(0xFFFF2D55) // Critical Red
                    progress >= 0.75f -> Color(0xFFFF9500) // Orange Warning
                    else -> accentColor
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Limit Progress",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$totalScrolls / $dailyLimit",
                            color = progressColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = progressColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }

            if (todayStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    todayStats.entries.take(5).forEach { (pkg, count) ->
                        val appIcon = rememberAppIcon(
                            context = context,
                            packageName = pkg,
                            activeIconPack = activeIconPack
                        )
                        
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = ScrollTrackerService.friendlyEmoji(pkg),
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "$count",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
