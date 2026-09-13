package com.oorbitt.launcher.home.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    onDismiss: () -> Unit,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    modifier: Modifier = Modifier,
    widgetHostManager: WidgetHostManager = org.koin.compose.koinInject()
) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    // Fetch and group providers by app
    val groupedWidgets = remember {
        val providers = widgetHostManager.getInstalledProviders()
        providers.groupBy { info ->
            try {
                val appInfo = pm.getApplicationInfo(info.provider.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                info.provider.packageName
            }
        }.toList().sortedBy { it.first.lowercase() }
    }

    var expandedApp by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = "Add Widget",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 500.dp)
            ) {
                items(groupedWidgets) { (appName, widgets) ->
                    val isExpanded = expandedApp == appName
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedApp = if (isExpanded) null else appName }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Fetch app icon for the group
                            val appIcon = remember(appName) {
                                try {
                                    val pkg = widgets.firstOrNull()?.provider?.packageName
                                    if (pkg != null) pm.getApplicationIcon(pkg) else null
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            appIcon?.let {
                                Image(
                                    bitmap = it.toBitmap().asImageBitmap(),
                                    contentDescription = appName,
                                    modifier = Modifier.size(36.dp)
                                )
                            } ?: Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        if (isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                    .padding(bottom = 8.dp)
                            ) {
                                widgets.forEach { info ->
                                    val label = info.loadLabel(pm)
                                    val spanX = remember(info) { Math.max(1, Math.round((info.minWidth - 40) / 76f)) }
                                    val spanY = remember(info) { Math.max(1, Math.round((info.minHeight - 40) / 76f)) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onWidgetSelected(info) }
                                            .padding(horizontal = 32.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Widget preview / fallback icon
                                        val preview = remember(info) {
                                            try {
                                                info.loadPreviewImage(context, context.resources.displayMetrics.densityDpi)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }

                                        preview?.let {
                                            Image(
                                                bitmap = it.toBitmap().asImageBitmap(),
                                                contentDescription = label,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                        } ?: Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Widget", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Size: ${spanX}x${spanY}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
    }
}
