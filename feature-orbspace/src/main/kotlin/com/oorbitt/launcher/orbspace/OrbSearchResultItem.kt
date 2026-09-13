package com.oorbitt.launcher.orbspace

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Search result item composable ────────────────────────────────────────────

@Composable
fun OrbSearchResultItem(
    result: SearchResult,
    onAction: (SearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Load app icon for app results
    var appIconBitmap by remember(result.id) { mutableStateOf<ImageBitmap?>(null) }
    if (result.action is SearchResultAction.LaunchApp) {
        LaunchedEffect(result.id) {
            appIconBitmap = withContext(Dispatchers.IO) {
                try {
                    val pkgName = (result.action as SearchResultAction.LaunchApp).packageName
                    val drawable = context.packageManager.getApplicationIcon(pkgName)
                    drawable.toBitmap(48, 48).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onAction(result) }
            .padding(14.dp)
    ) {
        when (val action = result.action) {
            is SearchResultAction.LaunchApp -> AppResultRow(result, appIconBitmap)
            is SearchResultAction.ShowAnswer -> AnswerResultRow(result, action)
            is SearchResultAction.CallContact -> ContactResultRow(result, action, context)
            is SearchResultAction.OpenSetting -> SettingResultRow(result)
            is SearchResultAction.OpenUrl -> WebResultRow(result)
            is SearchResultAction.LaunchShortcut -> ShortcutResultRow(result)
            is SearchResultAction.MessageContact -> ContactMessageRow(result, action, context)
            is SearchResultAction.ExecuteCommand -> CommandResultRow(result)
            is SearchResultAction.OpenFile -> FileResultRow(result, action, context)
        }
    }
}

// ── App result ───────────────────────────────────────────────────────────────

@Composable
private fun AppResultRow(result: SearchResult, iconBitmap: ImageBitmap?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // App icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = result.title,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        // "Open" chip
        ActionChip(
            label = "Open",
            icon = Icons.Default.OpenInNew,
            color = Color(0xFF6C5CE7)
        )
    }
}

// ── Calculator / Unit converter answer ───────────────────────────────────────

@Composable
private fun AnswerResultRow(result: SearchResult, action: SearchResultAction.ShowAnswer) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00B894), Color(0xFF55EFC4))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Calculate,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.answer,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        ActionChip(
            label = "Copy",
            icon = Icons.Default.ContentCopy,
            color = Color(0xFF00B894)
        )
    }
}

// ── Contact result with Call/Message chips ────────────────────────────────────

@Composable
private fun ContactResultRow(
    result: SearchResult,
    action: SearchResultAction.CallContact,
    context: Context
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = result.title.take(1).uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = result.subtitle ?: action.phoneNumber,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        // Action chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionChip(
                label = "Call",
                icon = Icons.Default.Call,
                color = Color(0xFF00B894),
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.phoneNumber}"))
                    context.startActivity(intent)
                }
            )
            ActionChip(
                label = "SMS",
                icon = Icons.AutoMirrored.Filled.Send,
                color = Color(0xFF0984E3),
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${action.phoneNumber}"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun ContactMessageRow(
    result: SearchResult,
    action: SearchResultAction.MessageContact,
    context: Context
) {
    ContactResultRow(
        result = result,
        action = SearchResultAction.CallContact(action.contactUri),
        context = context
    )
}

// ── Settings result ──────────────────────────────────────────────────────────

@Composable
private fun SettingResultRow(result: SearchResult) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        ActionChip(
            label = "Open",
            icon = Icons.Default.OpenInNew,
            color = Color(0xFF636E72)
        )
    }
}

// ── Web search fallback ──────────────────────────────────────────────────────

@Composable
private fun WebResultRow(result: SearchResult) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0984E3), Color(0xFF74B9FF))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Open in default browser",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Shortcut result ──────────────────────────────────────────────────────────

@Composable
private fun ShortcutResultRow(result: SearchResult) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Shortcut,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

// ── Command result ───────────────────────────────────────────────────────────

@Composable
private fun CommandResultRow(result: SearchResult) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFF7675).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Terminal,
                contentDescription = null,
                tint = Color(0xFFFF7675),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

// ── Reusable action chip ─────────────────────────────────────────────────────

@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    val chipModifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(color.copy(alpha = 0.15f))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 10.dp, vertical = 6.dp)

    Row(
        modifier = chipModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FileResultRow(result: SearchResult, action: SearchResultAction.OpenFile, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(action.uri), action.mimeType)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ActivityNotFoundException etc
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(action.uri), action.mimeType)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
        ) {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            result.subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
