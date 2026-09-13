package com.oorbitt.launcher.ui.util

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorbitt.launcher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeAppDialog(
    app: AppInfo,
    activeIconPack: String?,
    iconShape: androidx.compose.ui.graphics.Shape,
    onDismiss: () -> Unit,
    onSave: (customLabel: String?, customIconUri: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var labelText by remember { mutableStateOf(app.customLabel ?: app.label) }
    var customIconUri by remember { mutableStateOf(app.customIconUri) }
    
    var showSourceSelector by remember { mutableStateOf(false) }
    var showIconPackSelector by remember { mutableStateOf(false) }
    var showIconGrid by remember { mutableStateOf(false) }
    var selectedIconPackPkg by remember { mutableStateOf<String?>(null) }
    var selectedIconPackLabel by remember { mutableStateOf("") }
    
    val iconPackProvider = remember {
        try {
            org.koin.core.context.GlobalContext.get().getOrNull<IconPackProvider>()
        } catch (e: Exception) {
            null
        }
    }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            customIconUri = it.toString()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize App", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon preview/selection
                val icon = rememberAppIcon(context, app.packageName, app.activityName, activeIconPack, customIconUri)
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(iconShape)
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .clickable { showSourceSelector = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = "App Icon Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Pick\nIcon", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap icon to change",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("App Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = {
                        labelText = app.label
                        customIconUri = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset to defaults")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLabel = if (labelText.trim() == app.label || labelText.isBlank()) null else labelText.trim()
                    onSave(finalLabel, customIconUri)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // 1. Choose Icon Source Bottom Sheet
    if (showSourceSelector) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSelector = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp)
            ) {
                Text(
                    text = "Change Icon Source",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                
                ListItem(
                    headlineContent = { Text("Choose from Photos") },
                    supportingContent = { Text("Select any image file from gallery") },
                    modifier = Modifier.clickable {
                        showSourceSelector = false
                        imagePicker.launch("image/*")
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Choose from Icon Packs") },
                    supportingContent = { Text("Pick from downloaded icon pack themes") },
                    modifier = Modifier.clickable {
                        showSourceSelector = false
                        showIconPackSelector = true
                    }
                )
            }
        }
    }

    // 2. Select Icon Pack Dialog
    if (showIconPackSelector) {
        var installedIconPacks by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
        LaunchedEffect(Unit) {
            if (iconPackProvider != null) {
                withContext(Dispatchers.IO) {
                    installedIconPacks = iconPackProvider.getInstalledIconPacks()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showIconPackSelector = false },
            title = { Text("Select Icon Pack") },
            text = {
                if (installedIconPacks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No installed icon packs found.", textAlign = TextAlign.Center)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(installedIconPacks) { pack ->
                            val packIcon = remember {
                                val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bmp)
                                pack.icon.setBounds(0, 0, 96, 96)
                                pack.icon.draw(canvas)
                                bmp.asImageBitmap()
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedIconPackPkg = pack.packageName
                                        selectedIconPackLabel = pack.label
                                        showIconPackSelector = false
                                        showIconGrid = true
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = packIcon,
                                    contentDescription = pack.label,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pack.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPackSelector = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 3. Grid of Icons from Selected Pack
    if (showIconGrid && selectedIconPackPkg != null) {
        val packPkg = selectedIconPackPkg!!
        var allDrawables by remember { mutableStateOf<List<String>>(emptyList()) }
        var filterText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(packPkg) {
            isLoading = true
            if (iconPackProvider != null) {
                withContext(Dispatchers.IO) {
                    allDrawables = iconPackProvider.getAllIconPackDrawables(packPkg)
                }
            }
            isLoading = false
        }

        val filteredDrawables = remember(allDrawables, filterText) {
            if (filterText.isBlank()) allDrawables
            else allDrawables.filter { it.contains(filterText, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showIconGrid = false },
            title = { Text(selectedIconPackLabel) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Search icons...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (filteredDrawables.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No icons match search query.")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredDrawables, key = { it }) { drawableName ->
                                val iconBitmap = rememberIconPackIcon(context, packPkg, drawableName)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            customIconUri = "iconpack://$packPkg/$drawableName"
                                            showIconGrid = false
                                        }
                                        .padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap,
                                            contentDescription = drawableName,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.Gray.copy(alpha = 0.2f))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = drawableName.substringAfterLast("_"),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconGrid = false }) {
                    Text("Back")
                }
            }
        )
    }
}

@Composable
fun rememberIconPackIcon(
    context: Context,
    iconPackPkg: String,
    drawableName: String
): ImageBitmap? {
    var icon by remember(iconPackPkg, drawableName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(iconPackPkg, drawableName) {
        val loaded = withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val resources = pm.getResourcesForApplication(iconPackPkg)
                val resId = resources.getIdentifier(drawableName, "drawable", iconPackPkg)
                if (resId > 0) {
                    val drawable = resources.getDrawable(resId, null)
                    val bmp = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                        drawable.bitmap
                    } else {
                        val w = if (drawable.intrinsicWidth <= 0) 144 else drawable.intrinsicWidth
                        val h = if (drawable.intrinsicHeight <= 0) 144 else drawable.intrinsicHeight
                        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(b)
                        drawable.setBounds(0, 0, w, h)
                        drawable.draw(canvas)
                        b
                    }
                    bmp.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        }
        icon = loaded
    }
    return icon
}
