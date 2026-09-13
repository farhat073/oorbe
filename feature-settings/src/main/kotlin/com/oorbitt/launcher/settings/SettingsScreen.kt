package com.oorbitt.launcher.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.model.ThemeMode
import com.oorbitt.launcher.model.IconShape
import com.oorbitt.launcher.applock.AppLockScreen
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.oorbitt.launcher.ui.util.getIconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialCategory: String? = null,
    settingsRepository: SettingsRepository = koinInject(),
    authManager: com.oorbitt.launcher.security.AuthManager = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
    var activeCategory by remember { mutableStateOf<String?>(initialCategory) }
    var showWellnessAccessibilityDialog by remember { mutableStateOf(false) }
    var pendingWellnessToggleValue by remember { mutableStateOf(false) }
    var showFeatureFlags by remember { mutableStateOf(false) }
    var showHiddenApps by remember { mutableStateOf(false) }
    var showAppLock by remember { mutableStateOf(false) }
    var showIconPackSelector by remember { mutableStateOf(false) }
    var showGestureSettings by remember { mutableStateOf(false) }
    var showFirstTimeOrbSpaceLockDialog by remember { mutableStateOf(false) }
    var showStyleHub by remember { mutableStateOf(false) }
    var showFirstTimeFreeformDialog by remember { mutableStateOf(false) }
    var showFirstTimeGesturesDialog by remember { mutableStateOf(false) }
    var ticks by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            ticks++
        }
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAppLockTimeoutDialog by remember { mutableStateOf(false) }
    var showIconShapeDialog by remember { mutableStateOf(false) }
    var showWallpaperDimDialog by remember { mutableStateOf(false) }
    var showGridSizeDialog by remember { mutableStateOf(false) }
    var showIconSizeDialog by remember { mutableStateOf(false) }
    var showDesktopIconSizeDialog by remember { mutableStateOf(false) }
    var showDrawerIconSizeDialog by remember { mutableStateOf(false) }
    var showDockIconSizeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showSearchBarDialog by remember { mutableStateOf(false) }
    var showFavoriteAIAppsDialog by remember { mutableStateOf(false) }
    var showSearchBarPositionDialog by remember { mutableStateOf(false) }
    var showSearchBarCornerRadiusDialog by remember { mutableStateOf(false) }
    var showSearchResultsSpacingDialog by remember { mutableStateOf(false) }
    var showSearchBarPaddingDialog by remember { mutableStateOf(false) }
    var showOrbSpaceSpacingDialog by remember { mutableStateOf(false) }
    var showOrbSpaceCardCornerRadiusDialog by remember { mutableStateOf(false) }
    var showOrbSpaceLayoutModeDialog by remember { mutableStateOf(false) }
    var showOrbSpaceAccentColorDialog by remember { mutableStateOf(false) }
    var showOpenWithStyleDialog by remember { mutableStateOf(false) }
    var showDrawerColumnsDialog by remember { mutableStateOf(false) }
    var showDrawerBgColorDialog by remember { mutableStateOf(false) }
    var showDrawerBgOpacityDialog by remember { mutableStateOf(false) }
    var showDrawerBgImageDialog by remember { mutableStateOf(false) }
    var showDrawerCornerRadiusDialog by remember { mutableStateOf(false) }
    var showDrawerAnimationDialog by remember { mutableStateOf(false) }
    var showSortModeDialog by remember { mutableStateOf(false) }
    var showScrollLimitDialog by remember { mutableStateOf(false) }
    var showDockSlotsDialog by remember { mutableStateOf(false) }
    var showDrawerSearchStyleDialog by remember { mutableStateOf(false) }
    var showDrawerSearchPositionDialog by remember { mutableStateOf(false) }
    var showSearchResultBgColorDialog by remember { mutableStateOf(false) }
    var showSearchResultBgOpacityDialog by remember { mutableStateOf(false) }
    var showSearchResultPaddingDialog by remember { mutableStateOf(false) }
    var showSearchResultTextSizeDialog by remember { mutableStateOf(false) }
    var showWellnessWidgetBgOpacityDialog by remember { mutableStateOf(false) }
    var showWellnessWidgetHeightDialog by remember { mutableStateOf(false) }
    var showWellnessWidgetCornerRadiusDialog by remember { mutableStateOf(false) }
    var showWellnessWidgetAccentColorDialog by remember { mutableStateOf(false) }
    var installedIconPacks by remember { mutableStateOf<List<com.oorbitt.launcher.ui.util.IconPackInfo>>(emptyList()) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            coroutineScope.launch {
                settingsRepository.updateSettings { current ->
                    current.copy(drawerBgImage = it.toString())
                }
            }
        }
    }

    val iconPackProvider = remember {
        try {
            org.koin.core.context.GlobalContext.get().getOrNull<com.oorbitt.launcher.ui.util.IconPackProvider>()
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(showIconPackSelector) {
        if (showIconPackSelector && iconPackProvider != null) {
            withContext(Dispatchers.IO) {
                installedIconPacks = iconPackProvider.getInstalledIconPacks()
            }
        }
    }

    BackHandler(enabled = showFeatureFlags || showHiddenApps || showIconPackSelector || showGestureSettings || showAppLock || showStyleHub || activeCategory != null) {
        if (activeCategory != null) {
            activeCategory = null
        } else if (showFeatureFlags) {
            showFeatureFlags = false
        } else if (showHiddenApps) {
            showHiddenApps = false
        } else if (showGestureSettings) {
            showGestureSettings = false
        } else if (showAppLock) {
            showAppLock = false
        } else if (showStyleHub) {
            showStyleHub = false
        } else {
            showIconPackSelector = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (!showStyleHub) {
                    TopAppBar(
                        title = { Text(if (activeCategory != null) activeCategory!! else "Settings") },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (activeCategory != null) {
                                    activeCategory = null
                                } else {
                                    onDismiss()
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                if (activeCategory == null) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                                Text(
                                    text = "Oorbitt Settings",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Resort and perfect your launcher experience.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val categories = remember {
                                listOf(
                                    CategoryItem(
                                        "Appearance & Themes", 
                                        Icons.Default.Palette, 
                                        "Appearance & Themes", 
                                        "Customize colors, custom icon shapes, size scaling, and active icon pack themes.",
                                        listOf(Color(0xFF8A2387), Color(0xFFE94057))
                                    ),
                                    CategoryItem(
                                        "OorbStyle Marketplace", 
                                        Icons.Default.Share, 
                                        "OorbStyle Marketplace", 
                                        "Discover, download, and share launcher setups with the community.",
                                        listOf(Color(0xFF00B894), Color(0xFF0984E3))
                                    ),
                                    CategoryItem(
                                        "Workspace & Dock", 
                                        Icons.Default.GridView, 
                                        "Workspace & Dock", 
                                        "Configure home screen layout, free-form icon placement, grid counts, and dock slots.",
                                        listOf(Color(0xFF00B4DB), Color(0xFF0083B0))
                                    ),
                                    CategoryItem(
                                        "Drawer & Search", 
                                        Icons.Default.Search, 
                                        "Drawer & Search", 
                                        "Design app drawer styling, list columns, search bar presets, and quick launch triggers.",
                                        listOf(Color(0xFF11998E), Color(0xFF38EF7D))
                                    ),
                                    CategoryItem(
                                        "OrbSpace Page", 
                                        Icons.Default.ViewCarousel, 
                                        "OrbSpace Page", 
                                        "Personalize the OrbSpace overlay, custom widgets padding, layout modes, and accents.",
                                        listOf(Color(0xFFF12711), Color(0xFFF5AF19))
                                    ),
                                    CategoryItem(
                                        "Wellness & Stats", 
                                        Icons.Default.Schedule, 
                                        "Wellness & Stats", 
                                        "Track screen time, set scroll thresholds, and manage digital hygiene widgets.",
                                        listOf(Color(0xFF00FF87), Color(0xFF60EFFF))
                                    ),
                                    CategoryItem(
                                        "Security & App Lock", 
                                        Icons.Default.Lock, 
                                        "Security & App Lock", 
                                        "Secure individual applications, adjust bypass timeouts, and lock OrbSpace page.",
                                        listOf(Color(0xFFED213A), Color(0xFF93291E))
                                    ),
                                    CategoryItem(
                                        "Gestures & Advanced", 
                                        Icons.Default.Gesture, 
                                        "Gestures & Advanced", 
                                        "Assign swipe up/down triggers, enable double-tap to lock, and toggle flags.",
                                        listOf(Color(0xFF434343), Color(0xFF232526))
                                    )
                                )
                            }

                            val chunked = remember { categories.chunked(2) }
                            chunked.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(130.dp)
                                                .clickable {
                                                    if (item.key == "OorbStyle Marketplace") {
                                                        showStyleHub = true
                                                    } else {
                                                        activeCategory = item.key
                                                    }
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.White.copy(alpha = 0.04f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.08f),
                                                        Color.White.copy(alpha = 0.02f)
                                                    )
                                                )
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .background(
                                                            brush = Brush.linearGradient(item.gradientColors),
                                                            shape = RoundedCornerShape(10.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = item.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        val walkthrough = when (activeCategory) {
                            "Appearance & Themes" -> WalkthroughInfo(
                                "Design & Aesthetics",
                                "Customize the launcher's appearance. Set dark/light themes, customize icon packaging, and use the Icon Shape Designer to mold icons with custom corner radii."
                            )
                            "Workspace & Dock" -> WalkthroughInfo(
                                "Homescreen & Placements",
                                "Configure the desktop layout. You can adjust grid lines, toggles, dock limits, or enable 'Freeform Placement' to position icons anywhere without a grid, resizing them individually."
                            )
                            "Drawer & Search" -> WalkthroughInfo(
                                "Search & Navigation",
                                "Fine-tune search bar position, border parameters, and custom result spacing. Adjust list rows and columns in the app drawer, or configure the 'Open With AI' launcher shortcuts."
                            )
                            "OrbSpace Page" -> WalkthroughInfo(
                                "OrbSpace Customization",
                                "OrbSpace is your personalized dashboard page. Set background blur, dim levels, custom card layouts (like Apple Health), accents, or lock it behind biometric fingerprint verification."
                            )
                            "Wellness & Stats" -> WalkthroughInfo(
                                "Screen Time & Wellness",
                                "Help limit scrolling behaviors on social media apps. Configure daily scroll limits or turn on the Wellness widget. Note: Enabling this uses accessibility services to track swipes/gestures locally."
                            )
                            "Security & App Lock" -> WalkthroughInfo(
                                "Security & App Locking",
                                "Secure your private apps. Locked apps will require biometric identity verification upon launch. Set lock timeouts or configure fingerprint verification for the OrbSpace page."
                            )
                            "Gestures & Advanced" -> WalkthroughInfo(
                                "Gestures & Inputs",
                                "Configure system gestures like double tap to lock, swipe triggers, or toggle advanced developer flags. Double-tap to lock requires the Accessibility service to perform lock screen operations."
                            )
                            else -> null
                        }

                        walkthrough?.let { info ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = info.headline,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = info.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    if (activeCategory == "Appearance & Themes") {
                        item {
                            SettingsSection(title = "Appearance & Themes") {
                        val themeLabel = when (settingsState.themeMode) {
                            ThemeMode.SYSTEM -> "System Default"
                            ThemeMode.LIGHT -> "Light Mode"
                            ThemeMode.DARK -> "Dark Mode"
                        }
                        ListItem(
                            headlineContent = { Text("Theme Mode") },
                            supportingContent = { Text(themeLabel) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    containerColor = Color(0xFF5856D6),
                                    iconColor = Color(0xFF5856D6)
                                )
                            },
                            modifier = Modifier.clickable { showThemeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Icon Shape Designer") },
                            supportingContent = { 
                                val shapeType = if (settingsState.iconCornerCut) "Cut" else "Rounded"
                                Text("Custom $shapeType Corners (TL:${settingsState.iconCornerTopStart}% TR:${settingsState.iconCornerTopEnd}% BL:${settingsState.iconCornerBottomStart}% BR:${settingsState.iconCornerBottomEnd}%)") 
                            },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = null,
                                    containerColor = Color(0xFFFF2D55),
                                    iconColor = Color(0xFFFF2D55)
                                )
                            },
                            modifier = Modifier.clickable { showIconShapeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Icon Pack Theme") },
                            supportingContent = {
                                val activePackName = settingsState.activeIconPack
                                val label = if (activePackName != null) {
                                    val matchingPack = installedIconPacks.firstOrNull { it.packageName == activePackName }
                                    matchingPack?.label ?: activePackName
                                } else {
                                    "System Default"
                                }
                                Text(label)
                            },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    containerColor = Color(0xFF34C759),
                                    iconColor = Color(0xFF34C759)
                                )
                            },
                            modifier = Modifier.clickable { showIconPackSelector = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Wallpaper Dim Level") },
                            supportingContent = { Text("${settingsState.wallpaperDimPercent}%") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    containerColor = Color(0xFFFF9500),
                                    iconColor = Color(0xFFFF9500)
                                )
                            },
                            modifier = Modifier.clickable { showWallpaperDimDialog = true }
                        )
                    }
                }
            }

            if (activeCategory == "Workspace & Dock") {
                item {
                    SettingsSection(title = "Desktop Layout") {
                        ListItem(
                            headlineContent = { Text("Grid Size") },
                            supportingContent = { Text("${settingsState.gridColumns}x${settingsState.gridRows}") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    containerColor = Color(0xFF007AFF),
                                    iconColor = Color(0xFF007AFF)
                                )
                            },
                            modifier = Modifier.clickable { showGridSizeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Desktop Icon Size") },
                            supportingContent = { Text("${settingsState.desktopIconSizeDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    containerColor = Color(0xFF30B0C7),
                                    iconColor = Color(0xFF30B0C7)
                                )
                            },
                            modifier = Modifier.clickable { showDesktopIconSizeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Font Size") },
                            supportingContent = { Text("${settingsState.labelSizeSp} sp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    containerColor = Color(0xFF5856D6),
                                    iconColor = Color(0xFF5856D6)
                                )
                            },
                            modifier = Modifier.clickable { showFontSizeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Show Desktop Labels") },
                            supportingContent = { Text("Show text labels below home screen icons") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    containerColor = Color(0xFF00A3A3),
                                    iconColor = Color(0xFF00A3A3)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.showDesktopLabels,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { 
                                                it.copy(showDesktopLabels = checked, showLabels = checked) 
                                            }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        val searchLabel = when (settingsState.searchBarStyle) {
                            com.oorbitt.launcher.model.SearchBarStyle.BAR -> "Classic Bar"
                            com.oorbitt.launcher.model.SearchBarStyle.PILL -> "Sleek Pill"
                            com.oorbitt.launcher.model.SearchBarStyle.HIDDEN -> "Hidden (Removed)"
                        }
                        ListItem(
                            headlineContent = { Text("Search Bar Style") },
                            supportingContent = { Text(searchLabel) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    containerColor = Color(0xFF2196F3),
                                    iconColor = Color(0xFF2196F3)
                                )
                            },
                            modifier = Modifier.clickable { showSearchBarDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Search Bar Position") },
                            supportingContent = { Text(settingsState.searchBarPosition.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    containerColor = Color(0xFFFF5722),
                                    iconColor = Color(0xFFFF5722)
                                )
                            },
                            modifier = Modifier.clickable { showSearchBarPositionDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Search Bar Corner Shape") },
                            supportingContent = { Text("${settingsState.searchBarCornerRadiusDp} dp (Corner radius)") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.RoundedCorner,
                                    contentDescription = null,
                                    containerColor = Color(0xFF795548),
                                    iconColor = Color(0xFF795548)
                                )
                            },
                            modifier = Modifier.clickable { showSearchBarCornerRadiusDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        val favoriteAppsCount = remember(settingsState.favoriteAIAppsCsv) {
                            if (settingsState.favoriteAIAppsCsv.isEmpty()) "All downloaded"
                            else "${settingsState.favoriteAIAppsCsv.split(",").filter { it.isNotEmpty() }.size} selected"
                        }
                        ListItem(
                            headlineContent = { Text("Favorite AI Apps") },
                            supportingContent = { Text(favoriteAppsCount) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    containerColor = Color(0xFF4CAF50),
                                    iconColor = Color(0xFF4CAF50)
                                )
                            },
                            modifier = Modifier.clickable { showFavoriteAIAppsDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Freeform Desktop Placement") },
                            supportingContent = { Text(if (settingsState.freeFormPlacement) "Enabled (Drag icons anywhere, resize custom scale)" else "Disabled (Locked to grid cells)") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    containerColor = Color(0xFF34C759),
                                    iconColor = Color(0xFF34C759)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.freeFormPlacement,
                                    onCheckedChange = { checked ->
                                        val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                                        if (checked && !sp.getBoolean("freeform_walkthrough_shown", false)) {
                                            showFirstTimeFreeformDialog = true
                                        } else {
                                            coroutineScope.launch {
                                                settingsRepository.updateSettings { it.copy(freeFormPlacement = checked) }
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            if (activeCategory == "Drawer & Search") {
                item {
                    SettingsSection(title = "App Drawer Layout & Styling") {
                        ListItem(
                            headlineContent = { Text("App Drawer Grid Columns") },
                            supportingContent = { Text("${settingsState.drawerColumns} columns") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.ViewWeek,
                                    contentDescription = null,
                                    containerColor = Color(0xFF673AB7),
                                    iconColor = Color(0xFF673AB7)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerColumnsDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("App Drawer Icon Size") },
                            supportingContent = { Text("${settingsState.drawerIconSizeDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    containerColor = Color(0xFF03A9F4),
                                    iconColor = Color(0xFF03A9F4)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerIconSizeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Show App Drawer Labels") },
                            supportingContent = { Text("Show text labels in app drawer") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    containerColor = Color(0xFF8BC34A),
                                    iconColor = Color(0xFF8BC34A)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.showDrawerLabels,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { 
                                                it.copy(showDrawerLabels = checked) 
                                            }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        val sortLabel = when (settingsState.sortMode) {
                            com.oorbitt.launcher.model.DrawerSortMode.ALPHABETICAL -> "Alphabetical"
                            com.oorbitt.launcher.model.DrawerSortMode.USAGE -> "Most Used"
                            com.oorbitt.launcher.model.DrawerSortMode.INSTALL_DATE -> "Recently Installed"
                            else -> "Alphabetical"
                        }
                        ListItem(
                            headlineContent = { Text("App Drawer Sorting") },
                            supportingContent = { Text(sortLabel) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null,
                                    containerColor = Color(0xFF4CAF50),
                                    iconColor = Color(0xFF4CAF50)
                                )
                            },
                            modifier = Modifier.clickable { showSortModeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("App Drawer Background Color") },
                            supportingContent = { Text(settingsState.drawerBgColor) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.ColorLens,
                                    contentDescription = null,
                                    containerColor = Color(0xFF607D8B),
                                    iconColor = Color(0xFF607D8B)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerBgColorDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("App Drawer Background Opacity") },
                            supportingContent = { Text("${settingsState.drawerBgOpacity}%") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Opacity,
                                    contentDescription = null,
                                    containerColor = Color(0xFF78909C),
                                    iconColor = Color(0xFF78909C)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerBgOpacityDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        val imageLabel = if (settingsState.drawerBgImage != null) "Set (Custom Image)" else "None (Use Solid Color)"
                        ListItem(
                            headlineContent = { Text("App Drawer Background Image") },
                            supportingContent = { Text(imageLabel) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    containerColor = Color(0xFF3F51B5),
                                    iconColor = Color(0xFF3F51B5)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerBgImageDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("App Drawer Platform Layout") },
                            supportingContent = {
                                Text("Corners: ${settingsState.drawerCornerTopStartDp}-${settingsState.drawerCornerTopEndDp}-${settingsState.drawerCornerBottomStartDp}-${settingsState.drawerCornerBottomEndDp} dp | Bezel Space: ${settingsState.drawerSpaceDp} dp")
                            },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    containerColor = Color(0xFF9C27B0),
                                    iconColor = Color(0xFF9C27B0)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerCornerRadiusDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        val animLabel = when (settingsState.drawerAnimationType) {
                            "CARD" -> "Card Pop (Fluid iOS-like)"
                            "ZOOM" -> "Zoom Scale"
                            "FADE" -> "Fade Transition"
                            else -> "Slide Up (Classic)"
                        }
                        ListItem(
                            headlineContent = { Text("App Drawer Opening Animation") },
                            supportingContent = { Text(animLabel) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    containerColor = Color(0xFFFF9800),
                                    iconColor = Color(0xFFFF9800)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerAnimationDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("App Drawer Search Bar Style") },
                            supportingContent = {
                                Text("Height: ${settingsState.drawerSearchHeightDp}dp | Corners: ${settingsState.drawerSearchCornerRadiusDp}dp | Opacity: ${settingsState.drawerSearchBgOpacity}% | Border: ${settingsState.drawerSearchBorderWidthDp}dp")
                            },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    containerColor = Color(0xFF455A64),
                                    iconColor = Color(0xFF455A64)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerSearchStyleDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("App Drawer Search Position") },
                            supportingContent = { Text(settingsState.drawerSearchPosition.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    containerColor = Color(0xFF3F51B5),
                                    iconColor = Color(0xFF3F51B5)
                                )
                            },
                            modifier = Modifier.clickable { showDrawerSearchPositionDialog = true }
                        )
                    }
                }
            }

            if (activeCategory == "OrbSpace Page") {
                item {
                    SettingsSection(title = "OrbSpace & Search Layout") {
                        ListItem(
                            headlineContent = { Text("Search Results Spacing") },
                            supportingContent = { Text("${settingsState.searchResultsSpacingDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    containerColor = Color(0xFFFF5722),
                                    iconColor = Color(0xFFFF5722)
                                )
                            },
                            modifier = Modifier.clickable { showSearchResultsSpacingDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Search Bar Padding") },
                            supportingContent = { Text("${settingsState.searchBarPaddingDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    containerColor = Color(0xFF4CAF50),
                                    iconColor = Color(0xFF4CAF50)
                                )
                            },
                            modifier = Modifier.clickable { showSearchBarPaddingDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("OrbSpace Widget Spacing") },
                            supportingContent = { Text("${settingsState.orbSpaceSpacingDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    containerColor = Color(0xFF9C27B0),
                                    iconColor = Color(0xFF9C27B0)
                                )
                            },
                            modifier = Modifier.clickable { showOrbSpaceSpacingDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("OrbSpace Card Corners") },
                            supportingContent = { Text("${settingsState.orbSpaceCardCornerRadiusDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.RoundedCorner,
                                    contentDescription = null,
                                    containerColor = Color(0xFF607D8B),
                                    iconColor = Color(0xFF607D8B)
                                )
                            },
                            modifier = Modifier.clickable { showOrbSpaceCardCornerRadiusDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("OrbSpace Layout Mode") },
                            supportingContent = {
                                Text(
                                    when (settingsState.orbSpaceLayoutMode) {
                                        "APPLE_HEALTH" -> "Apple Health Grid"
                                        "COMPACT_LIST" -> "Compact List"
                                        "BENTO" -> "Bento Grid"
                                        else -> settingsState.orbSpaceLayoutMode
                                    }
                                )
                            },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    containerColor = Color(0xFF007AFF),
                                    iconColor = Color(0xFF007AFF)
                                )
                            },
                            modifier = Modifier.clickable { showOrbSpaceLayoutModeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("OrbSpace Accent Color") },
                            supportingContent = {
                                val colorName = when (settingsState.orbSpaceAccentColor) {
                                    "#007AFF" -> "Apple Blue"
                                    "#FF2D55" -> "Apple Pink"
                                    "#34C759" -> "Apple Green"
                                    "#FF9500" -> "Apple Orange"
                                    "#AF52DE" -> "Apple Purple"
                                    "#8E8E93" -> "Apple Gray"
                                    else -> settingsState.orbSpaceAccentColor
                                }
                                Text(colorName)
                            },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    containerColor = Color(
                                        try {
                                            android.graphics.Color.parseColor(settingsState.orbSpaceAccentColor)
                                        } catch (e: Exception) {
                                            0xFF007AFF.toInt()
                                        }
                                    ),
                                    iconColor = Color.White
                                )
                            },
                            modifier = Modifier.clickable { showOrbSpaceAccentColorDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Open With UI Style") },
                            supportingContent = {
                                Text(
                                    when (settingsState.openWithStyle) {
                                        "APPLE_CHIPS" -> "Apple Chips"
                                        "SPOTLIGHT_MINIMAL" -> "Spotlight Rows"
                                        "BENTO_TILES" -> "Bento Tiles"
                                        else -> settingsState.openWithStyle
                                    }
                                )
                            },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    containerColor = Color(0xFF34C759),
                                    iconColor = Color(0xFF34C759)
                                )
                            },
                            modifier = Modifier.clickable { showOpenWithStyleDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Web Search in Open With") },
                            supportingContent = { Text(if (settingsState.enableWebSearchInOpenWith) "Enabled" else "Disabled") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    containerColor = Color(0xFFFF9500),
                                    iconColor = Color(0xFFFF9500)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.enableWebSearchInOpenWith,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { it.copy(enableWebSearchInOpenWith = checked) }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("AI Apps in Open With") },
                            supportingContent = { Text(if (settingsState.enableAISearchInOpenWith) "Enabled" else "Disabled") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = null,
                                    containerColor = Color(0xFF5856D6),
                                    iconColor = Color(0xFF5856D6)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.enableAISearchInOpenWith,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { it.copy(enableAISearchInOpenWith = checked) }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Show Orb Floating Button") },
                            supportingContent = { Text(if (settingsState.showOrbButton) "Shown" else "Hidden") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    containerColor = Color(0xFF007AFF),
                                    iconColor = Color(0xFF007AFF)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.showOrbButton,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { it.copy(showOrbButton = checked) }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Enable OrbSpace Page") },
                            supportingContent = { Text(if (settingsState.enableOrbSpace) "Enabled" else "Disabled") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    containerColor = Color(0xFFE5A93C),
                                    iconColor = Color(0xFFE5A93C)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.enableOrbSpace,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { it.copy(enableOrbSpace = checked) }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Lock OrbSpace with Biometrics") },
                            supportingContent = { Text(if (settingsState.lockOrbSpace) "Required" else "Not Required") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    containerColor = Color(0xFFE5A93C),
                                    iconColor = Color(0xFFE5A93C)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.lockOrbSpace,
                                    onCheckedChange = { checked ->
                                        val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                                        if (checked && !sp.getBoolean("orbspace_lock_walkthrough_shown", false)) {
                                            showFirstTimeOrbSpaceLockDialog = true
                                        } else {
                                            coroutineScope.launch {
                                                settingsRepository.updateSettings { it.copy(lockOrbSpace = checked) }
                                            }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Search Result Background Color") },
                            supportingContent = { Text(settingsState.searchResultBgColor) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    containerColor = Color(
                                        try {
                                            android.graphics.Color.parseColor(settingsState.searchResultBgColor)
                                        } catch (e: Exception) {
                                            0xFF1C1C1E.toInt()
                                        }
                                    ),
                                    iconColor = Color.White
                                )
                            },
                            modifier = Modifier.clickable { showSearchResultBgColorDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Search Result Background Opacity") },
                            supportingContent = { Text("${settingsState.searchResultBgOpacity}%") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Opacity,
                                    contentDescription = null,
                                    containerColor = Color(0xFF673AB7),
                                    iconColor = Color(0xFF673AB7)
                                )
                            },
                            modifier = Modifier.clickable { showSearchResultBgOpacityDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Search Result Inner Padding") },
                            supportingContent = { Text("${settingsState.searchResultPaddingDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    containerColor = Color(0xFFE91E63),
                                    iconColor = Color(0xFFE91E63)
                                )
                            },
                            modifier = Modifier.clickable { showSearchResultPaddingDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Search Result Text Size") },
                            supportingContent = { Text("${settingsState.searchResultTextSizeSp} sp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    containerColor = Color(0xFF009688),
                                    iconColor = Color(0xFF009688)
                                )
                            },
                            modifier = Modifier.clickable { showSearchResultTextSizeDialog = true }
                        )
                    }
                }
            }

            if (activeCategory == "Workspace & Dock") {
                item {
                    SettingsSection(title = "Dock Settings") {
                        ListItem(
                            headlineContent = { Text("Dock Icon Slots") },
                            supportingContent = { Text("${settingsState.dockSlots} apps") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.ViewCarousel,
                                    contentDescription = null,
                                    containerColor = Color(0xFF7B1FA2),
                                    iconColor = Color(0xFF7B1FA2)
                                )
                            },
                            modifier = Modifier.clickable { showDockSlotsDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Dock Icon Size") },
                            supportingContent = { Text("${settingsState.dockIconSizeDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    containerColor = Color(0xFF00796B),
                                    iconColor = Color(0xFF00796B)
                                )
                            },
                            modifier = Modifier.clickable { showDockIconSizeDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Show Dock Labels") },
                            supportingContent = { Text("Show text labels below dock icons") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    containerColor = Color(0xFF388E3C),
                                    iconColor = Color(0xFF388E3C)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.showDockLabels,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { 
                                                it.copy(showDockLabels = checked) 
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            if (activeCategory == "Security & App Lock") {
                item {
                    SettingsSection(title = "App Management & Security") {
                        ListItem(
                            headlineContent = { Text("Hidden Apps") },
                            supportingContent = { Text("Manage hidden applications") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    containerColor = Color(0xFF455A64),
                                    iconColor = Color(0xFF455A64)
                                )
                            },
                            modifier = Modifier.clickable { showHiddenApps = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("App Lock") },
                            supportingContent = { Text("Secure individual apps with biometrics") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    containerColor = Color(0xFFD32F2F),
                                    iconColor = Color(0xFFD32F2F)
                                )
                            },
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    val authenticated = authManager.authenticate(
                                        title = "App Lock settings",
                                        subtitle = "Verify identity to manage locked apps"
                                    )
                                    if (authenticated) {
                                        showAppLock = true
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        val timeoutLabel = when (settingsState.appLockTimeoutMs) {
                            0L -> "Immediately"
                            15_000L -> "15 seconds"
                            30_000L -> "30 seconds"
                            60_000L -> "1 minute"
                            300_000L -> "5 minutes"
                            900_000L -> "15 minutes"
                            else -> "Immediately"
                        }
                        ListItem(
                            headlineContent = { Text("App Lock Timeout") },
                            supportingContent = { Text("Require authentication after $timeoutLabel") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    containerColor = Color(0xFF1E8E3E),
                                    iconColor = Color(0xFF1E8E3E)
                                )
                            },
                            modifier = Modifier.clickable { showAppLockTimeoutDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Lock on Screen Off") },
                            supportingContent = { Text("Lock apps immediately when screen turns off") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    containerColor = Color(0xFF2196F3),
                                    iconColor = Color(0xFF2196F3)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.appLockScreenOff,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { 
                                                it.copy(appLockScreenOff = checked) 
                                            }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Biometric Only") },
                            supportingContent = { Text("Disable PIN/pattern fallback for app lock") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    containerColor = Color(0xFFE91E63),
                                    iconColor = Color(0xFFE91E63)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.appLockBiometricOnly,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            settingsRepository.updateSettings { 
                                                it.copy(appLockBiometricOnly = checked) 
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            if (activeCategory == "Gestures & Advanced") {
                item {
                    SettingsSection(title = "Gestures & Advanced") {
                        ListItem(
                            headlineContent = { Text("Gestures") },
                            supportingContent = { Text("Configure swipe, pinch, and double tap actions") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Gesture,
                                    contentDescription = null,
                                    containerColor = Color(0xFFF57C00),
                                    iconColor = Color(0xFFF57C00)
                                )
                            },
                            modifier = Modifier.clickable {
                                val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                                if (!sp.getBoolean("gestures_walkthrough_shown", false)) {
                                    showFirstTimeGesturesDialog = true
                                } else {
                                    showGestureSettings = true
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Feature Flags") },
                            supportingContent = { Text("Configure and toggle individual modules") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    containerColor = Color(0xFF757575),
                                    iconColor = Color(0xFF757575)
                                )
                            },
                            modifier = Modifier.clickable { showFeatureFlags = true }
                        )
                    }
                }
            }

            if (activeCategory == "Wellness & Stats") {
                item {
                    SettingsSection(title = "Wellness & Scroll Tracker") {
                        val accessibilityEnabled = remember(ticks) { isAccessibilityServiceEnabled(context) }
                        
                        ListItem(
                            headlineContent = { Text("Scroll Tracker Service") },
                            supportingContent = { 
                                Text(
                                    if (accessibilityEnabled) "Service is active and tracking" 
                                    else "Requires accessibility permission to track swipes"
                                )
                            },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Accessibility,
                                    contentDescription = null,
                                    containerColor = Color(0xFF34C759),
                                    iconColor = Color(0xFF34C759)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = accessibilityEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            pendingWellnessToggleValue = true
                                            showWellnessAccessibilityDialog = true
                                        } else {
                                            // Direct user to accessibility settings to disable
                                            try {
                                                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Please open system settings -> Accessibility to disable.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        val limitLabel = if (settingsState.dailyScrollLimit == 0) "Disabled" else "${settingsState.dailyScrollLimit} scrolls"
                        ListItem(
                            headlineContent = { Text("Daily Scroll Limit") },
                            supportingContent = { Text("Configure daily swipe thresholds with on/off switch and slider") },
                            trailingContent = { Text(limitLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    containerColor = Color(0xFFFF9500),
                                    iconColor = Color(0xFFFF9500)
                                )
                            },
                            modifier = Modifier.clickable { showScrollLimitDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Show Wellness Widget") },
                            supportingContent = { Text("Display a premium scroll tracker card on your home screen") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    containerColor = Color(0xFF007AFF),
                                    iconColor = Color(0xFF007AFF)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settingsState.showScrollTrackerWidget,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!accessibilityEnabled) {
                                                pendingWellnessToggleValue = true
                                                showWellnessAccessibilityDialog = true
                                            } else {
                                                coroutineScope.launch {
                                                    settingsRepository.updateSettings { 
                                                        it.copy(showScrollTrackerWidget = true) 
                                                    }
                                                }
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                settingsRepository.updateSettings { 
                                                    it.copy(showScrollTrackerWidget = false) 
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Wellness Widget Opacity") },
                            supportingContent = { Text("${settingsState.wellnessWidgetBgOpacity}%") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Opacity,
                                    contentDescription = null,
                                    containerColor = Color(0xFF009688),
                                    iconColor = Color(0xFF009688)
                                )
                            },
                            modifier = Modifier.clickable { showWellnessWidgetBgOpacityDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Wellness Widget Height") },
                            supportingContent = { Text("${settingsState.wellnessWidgetHeightDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    containerColor = Color(0xFF9C27B0),
                                    iconColor = Color(0xFF9C27B0)
                                )
                            },
                            modifier = Modifier.clickable { showWellnessWidgetHeightDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Wellness Widget Corners") },
                            supportingContent = { Text("${settingsState.wellnessWidgetCornerRadiusDp} dp") },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.RoundedCorner,
                                    contentDescription = null,
                                    containerColor = Color(0xFF3F51B5),
                                    iconColor = Color(0xFF3F51B5)
                                )
                            },
                            modifier = Modifier.clickable { showWellnessWidgetCornerRadiusDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        ListItem(
                            headlineContent = { Text("Wellness Widget Accent Color") },
                            supportingContent = { Text(settingsState.wellnessWidgetAccentColor) },
                            leadingContent = {
                                SettingsIcon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    containerColor = Color(
                                        try {
                                            android.graphics.Color.parseColor(settingsState.wellnessWidgetAccentColor)
                                        } catch (e: Exception) {
                                            0xFF007AFF.toInt()
                                        }
                                    ),
                                    iconColor = Color.White
                                )
                            },
                            modifier = Modifier.clickable { showWellnessWidgetAccentColorDialog = true }
                        )
                    }
                }
            }
        }
    }

        AnimatedVisibility(
            visible = showFeatureFlags,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(250)),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(250))
        ) {
            FeatureFlagsScreen(
                onDismiss = { showFeatureFlags = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = showHiddenApps,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(250)),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(250))
        ) {
            HiddenAppsScreen(
                onDismiss = { showHiddenApps = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = showGestureSettings,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(250)),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(250))
        ) {
            GestureSettingsScreen(
                onDismiss = { showGestureSettings = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = showAppLock,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(250)),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(250))
        ) {
            AppLockScreen(
                onDismiss = { showAppLock = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showIconPackSelector) {
            ModalBottomSheet(
                onDismissRequest = { showIconPackSelector = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Select Icon Pack",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    )
                    HorizontalDivider()
                    
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
                            ListItem(
                                headlineContent = { Text("System Default", fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("Use original application icons") },
                                modifier = Modifier.clickable {
                                    showIconPackSelector = false
                                    coroutineScope.launch {
                                        settingsRepository.updateSettings { it.copy(activeIconPack = null) }
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                        items(installedIconPacks) { pack ->
                            ListItem(
                                headlineContent = { Text(pack.label, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(pack.packageName) },
                                leadingContent = {
                                    val imageBitmap = remember(pack.packageName) {
                                        val drawable = pack.icon
                                        val w = if (drawable.intrinsicWidth <= 0) 144 else drawable.intrinsicWidth
                                        val h = if (drawable.intrinsicHeight <= 0) 144 else drawable.intrinsicHeight
                                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(bmp)
                                        drawable.setBounds(0, 0, w, h)
                                        drawable.draw(canvas)
                                        bmp.asImageBitmap()
                                    }
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = pack.label,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    showIconPackSelector = false
                                    coroutineScope.launch {
                                        settingsRepository.updateSettings { it.copy(activeIconPack = pack.packageName) }
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        if (showStyleHub) {
            com.oorbitt.launcher.stylehub.ui.StyleHubScreen(
                onDismiss = { showStyleHub = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showThemeDialog) {
            SingleChoiceSettingDialog(
                title = "Theme Mode",
                options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
                selectedOption = settingsState.themeMode,
                optionLabel = { option ->
                    when (option) {
                        ThemeMode.SYSTEM -> "System Default"
                        ThemeMode.LIGHT -> "Light Mode"
                        ThemeMode.DARK -> "Dark Mode"
                    }
                },
                onDismiss = { showThemeDialog = false },
                onConfirm = { selected ->
                    showThemeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(themeMode = selected) }
                    }
                }
            )
        }

        if (showAppLockTimeoutDialog) {
            SingleChoiceSettingDialog(
                title = "App Lock Timeout",
                options = listOf(0L, 15_000L, 30_000L, 60_000L, 300_000L, 900_000L),
                selectedOption = settingsState.appLockTimeoutMs,
                optionLabel = { option ->
                    when (option) {
                        0L -> "Immediately"
                        15_000L -> "15 seconds"
                        30_000L -> "30 seconds"
                        60_000L -> "1 minute"
                        300_000L -> "5 minutes"
                        900_000L -> "15 minutes"
                        else -> "Immediately"
                    }
                },
                onDismiss = { showAppLockTimeoutDialog = false },
                onConfirm = { selected ->
                    showAppLockTimeoutDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(appLockTimeoutMs = selected) }
                    }
                }
            )
        }

        if (showIconShapeDialog) {
            IconShapeDesignerDialog(
                initialTopStart = settingsState.iconCornerTopStart,
                initialTopEnd = settingsState.iconCornerTopEnd,
                initialBottomStart = settingsState.iconCornerBottomStart,
                initialBottomEnd = settingsState.iconCornerBottomEnd,
                initialIsCut = settingsState.iconCornerCut,
                onDismiss = { showIconShapeDialog = false },
                onConfirm = { topStart, topEnd, bottomStart, bottomEnd, isCut ->
                    showIconShapeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings {
                            it.copy(
                                iconCornerTopStart = topStart,
                                iconCornerTopEnd = topEnd,
                                iconCornerBottomStart = bottomStart,
                                iconCornerBottomEnd = bottomEnd,
                                iconCornerCut = isCut
                            )
                        }
                    }
                }
            )
        }

        if (showWallpaperDimDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            SliderSettingDialog(
                title = "Wallpaper Dim Level",
                value = settingsState.wallpaperDimPercent.toFloat(),
                valueRange = 0f..100f,
                steps = 19, // Increments of 5%
                valueLabel = { "${it.toInt()}%" },
                onDismiss = { showWallpaperDimDialog = false },
                onConfirm = { selected ->
                    showWallpaperDimDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(wallpaperDimPercent = selected.toInt()) }
                    }
                },
                previewContent = { dim ->
                    MiniHomeScreenMockup(
                        iconSize = settingsState.desktopIconSizeDp.dp,
                        labelSize = settingsState.labelSizeSp.sp,
                        showLabels = settingsState.showDesktopLabels,
                        iconShape = iconShapeObj,
                        dimPercent = dim.toInt()
                    )
                }
            )
        }

        if (showGridSizeDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            GridSizeSettingDialog(
                initialColumns = settingsState.gridColumns,
                initialRows = settingsState.gridRows,
                onDismiss = { showGridSizeDialog = false },
                onConfirm = { cols, rows ->
                    showGridSizeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(gridColumns = cols, gridRows = rows) }
                    }
                },
                previewContent = { cols, rows ->
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(mockWallpaperBrush)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    repeat(cols) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(iconShapeObj)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showDesktopIconSizeDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            SliderSettingDialog(
                title = "Desktop Icon Size",
                value = settingsState.desktopIconSizeDp.toFloat(),
                valueRange = 40f..80f,
                steps = 40,
                valueLabel = { "${it.toInt()} dp" },
                onDismiss = { showDesktopIconSizeDialog = false },
                onConfirm = { selected ->
                    showDesktopIconSizeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(desktopIconSizeDp = selected.toInt()) }
                    }
                },
                previewContent = { size ->
                    MiniHomeScreenMockup(
                        iconSize = size.dp,
                        labelSize = settingsState.labelSizeSp.sp,
                        showLabels = settingsState.showDesktopLabels,
                        iconShape = iconShapeObj
                    )
                }
            )
        }

        if (showDrawerIconSizeDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            val bgColorObj = remember(settingsState.drawerBgColor) {
                try {
                    Color(android.graphics.Color.parseColor(settingsState.drawerBgColor))
                } catch (e: Exception) {
                    Color.Black
                }
            }
            SliderSettingDialog(
                title = "App Drawer Icon Size",
                value = settingsState.drawerIconSizeDp.toFloat(),
                valueRange = 40f..80f,
                steps = 40,
                valueLabel = { "${it.toInt()} dp" },
                onDismiss = { showDrawerIconSizeDialog = false },
                onConfirm = { selected ->
                    showDrawerIconSizeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(drawerIconSizeDp = selected.toInt()) }
                    }
                },
                previewContent = { size ->
                    MiniAppDrawerMockup(
                        iconSize = size.dp,
                        labelSize = settingsState.labelSizeSp.sp,
                        showLabels = settingsState.showDrawerLabels,
                        iconShape = iconShapeObj,
                        columns = settingsState.drawerColumns,
                        bgColor = bgColorObj,
                        opacity = settingsState.drawerBgOpacity,
                        topStart = settingsState.drawerCornerTopStartDp.dp,
                        topEnd = settingsState.drawerCornerTopEndDp.dp,
                        bottomStart = settingsState.drawerCornerBottomStartDp.dp,
                        bottomEnd = settingsState.drawerCornerBottomEndDp.dp,
                        space = settingsState.drawerSpaceDp.dp
                    )
                }
            )
        }

        if (showDockIconSizeDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            SliderSettingDialog(
                title = "Dock Icon Size",
                value = settingsState.dockIconSizeDp.toFloat(),
                valueRange = 40f..80f,
                steps = 40,
                valueLabel = { "${it.toInt()} dp" },
                onDismiss = { showDockIconSizeDialog = false },
                onConfirm = { selected ->
                    showDockIconSizeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(dockIconSizeDp = selected.toInt()) }
                    }
                },
                previewContent = { size ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(mockWallpaperBrush)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(settingsState.dockSlots) {
                                    Box(
                                        modifier = Modifier
                                            .size(size.dp / 2)
                                            .clip(iconShapeObj)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showFontSizeDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            SliderSettingDialog(
                title = "Font Size",
                value = settingsState.labelSizeSp.toFloat(),
                valueRange = 8f..24f,
                steps = 16, // 1 sp increments
                valueLabel = { "${it.toInt()} sp" },
                onDismiss = { showFontSizeDialog = false },
                onConfirm = { selected ->
                    showFontSizeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(labelSizeSp = selected.toInt()) }
                    }
                },
                previewContent = { size ->
                    MiniHomeScreenMockup(
                        iconSize = settingsState.desktopIconSizeDp.dp,
                        labelSize = size.sp,
                        showLabels = settingsState.showDesktopLabels,
                        iconShape = iconShapeObj
                    )
                }
            )
        }

        if (showSearchBarDialog) {
            SingleChoiceSettingDialog(
                title = "Search Bar Style",
                options = listOf(com.oorbitt.launcher.model.SearchBarStyle.BAR, com.oorbitt.launcher.model.SearchBarStyle.PILL, com.oorbitt.launcher.model.SearchBarStyle.HIDDEN),
                selectedOption = settingsState.searchBarStyle,
                optionLabel = { option ->
                    when (option) {
                        com.oorbitt.launcher.model.SearchBarStyle.BAR -> "Classic Bar"
                        com.oorbitt.launcher.model.SearchBarStyle.PILL -> "Sleek Pill"
                        com.oorbitt.launcher.model.SearchBarStyle.HIDDEN -> "Hidden"
                    }
                },
                onDismiss = { showSearchBarDialog = false },
                onConfirm = { selected ->
                    showSearchBarDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchBarStyle = selected) }
                    }
                }
            )
        }

        if (showFavoriteAIAppsDialog) {
            val installedAIAppsList = remember(settingsState.favoriteAIAppsCsv) {
                settingsState.favoriteAIAppsCsv.split(",").filter { it.isNotEmpty() }.toSet()
            }
            var selectedFavorites by remember { mutableStateOf(installedAIAppsList) }
            
            AlertDialog(
                onDismissRequest = { showFavoriteAIAppsDialog = false },
                title = { Text("Favorite AI Apps") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Choose which AI search engines should appear next to the search bar. If none are selected, all downloaded AI apps will be displayed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val availableApps = listOf(
                            Pair("ChatGPT", "com.openai.chatgpt"),
                            Pair("Gemini", "com.google.android.apps.bard"),
                            Pair("Claude", "com.anthropic.claude"),
                            Pair("Perplexity", "ai.perplexity.app.android"),
                            Pair("Copilot", "com.microsoft.copilot")
                        )
                        
                        availableApps.forEach { (name, pkg) ->
                            val isInstalled = remember(pkg) {
                                try {
                                    context.packageManager.getPackageInfo(pkg, 0)
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            
                            val isChecked = selectedFavorites.contains(pkg)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFavorites = if (isChecked) {
                                            selectedFavorites - pkg
                                        } else {
                                            if (selectedFavorites.size >= 3) {
                                                android.widget.Toast.makeText(context, "You can select up to 3 favorite AI apps", android.widget.Toast.LENGTH_SHORT).show()
                                                selectedFavorites
                                            } else {
                                                selectedFavorites + pkg
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isInstalled) "Installed" else "Not downloaded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedFavorites = if (checked) {
                                            if (selectedFavorites.size >= 3) {
                                                android.widget.Toast.makeText(context, "You can select up to 3 favorite AI apps", android.widget.Toast.LENGTH_SHORT).show()
                                                selectedFavorites
                                            } else {
                                                selectedFavorites + pkg
                                            }
                                        } else {
                                            selectedFavorites - pkg
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val csv = selectedFavorites.joinToString(",")
                            coroutineScope.launch {
                                settingsRepository.updateSettings { it.copy(favoriteAIAppsCsv = csv) }
                            }
                            showFavoriteAIAppsDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFavoriteAIAppsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDrawerColumnsDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            val bgColorObj = remember(settingsState.drawerBgColor) {
                try {
                    Color(android.graphics.Color.parseColor(settingsState.drawerBgColor))
                } catch (e: Exception) {
                    Color.Black
                }
            }
            SliderSettingDialog(
                title = "App Drawer Grid Columns",
                value = settingsState.drawerColumns.toFloat(),
                valueRange = 3f..10f,
                steps = 7, // 1 column increments
                valueLabel = { "${it.toInt()} columns" },
                onDismiss = { showDrawerColumnsDialog = false },
                onConfirm = { selected ->
                    showDrawerColumnsDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(drawerColumns = selected.toInt()) }
                    }
                },
                previewContent = { cols ->
                    MiniAppDrawerMockup(
                        iconSize = settingsState.drawerIconSizeDp.dp,
                        labelSize = settingsState.labelSizeSp.sp,
                        showLabels = settingsState.showDrawerLabels,
                        iconShape = iconShapeObj,
                        columns = cols.toInt(),
                        bgColor = bgColorObj,
                        opacity = settingsState.drawerBgOpacity,
                        topStart = settingsState.drawerCornerTopStartDp.dp,
                        topEnd = settingsState.drawerCornerTopEndDp.dp,
                        bottomStart = settingsState.drawerCornerBottomStartDp.dp,
                        bottomEnd = settingsState.drawerCornerBottomEndDp.dp,
                        space = settingsState.drawerSpaceDp.dp
                    )
                }
            )
        }

        if (showSortModeDialog) {
            SingleChoiceSettingDialog(
                title = "App Drawer Sorting",
                options = listOf(com.oorbitt.launcher.model.DrawerSortMode.ALPHABETICAL, com.oorbitt.launcher.model.DrawerSortMode.USAGE, com.oorbitt.launcher.model.DrawerSortMode.INSTALL_DATE),
                selectedOption = settingsState.sortMode,
                optionLabel = { option ->
                    when (option) {
                        com.oorbitt.launcher.model.DrawerSortMode.ALPHABETICAL -> "Alphabetical"
                        com.oorbitt.launcher.model.DrawerSortMode.USAGE -> "Most Used"
                        com.oorbitt.launcher.model.DrawerSortMode.INSTALL_DATE -> "Recently Installed"
                        else -> "Alphabetical"
                    }
                },
                onDismiss = { showSortModeDialog = false },
                onConfirm = { selected ->
                    showSortModeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(sortMode = selected) }
                    }
                }
            )
        }

        if (showScrollLimitDialog) {
            var limitEnabled by remember { mutableStateOf(settingsState.dailyScrollLimit > 0) }
            var limitValue by remember { mutableStateOf(if (settingsState.dailyScrollLimit > 0) settingsState.dailyScrollLimit.toFloat() else 1000f) }
            
            AlertDialog(
                onDismissRequest = { showScrollLimitDialog = false },
                title = { Text("Daily Scroll Limit") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Scroll Limit", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = limitEnabled,
                                onCheckedChange = { limitEnabled = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (limitEnabled) {
                            Text(
                                text = "${limitValue.toInt()} scrolls",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = limitValue,
                                onValueChange = { limitValue = it },
                                valueRange = 100f..5000f,
                                steps = 48,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "Disabled",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val finalLimit = if (limitEnabled) limitValue.toInt() else 0
                            coroutineScope.launch {
                                settingsRepository.updateSettings { it.copy(dailyScrollLimit = finalLimit) }
                            }
                            showScrollLimitDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showScrollLimitDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDockSlotsDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            SliderSettingDialog(
                title = "Dock Icon Slots",
                value = settingsState.dockSlots.toFloat(),
                valueRange = 3f..10f,
                steps = 7, // 1 slot increments
                valueLabel = { "${it.toInt()} slots" },
                onDismiss = { showDockSlotsDialog = false },
                onConfirm = { selected ->
                    showDockSlotsDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(dockSlots = selected.toInt()) }
                    }
                },
                previewContent = { slots ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(mockWallpaperBrush)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(slots.toInt()) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(iconShapeObj)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showSearchBarPositionDialog) {
            SingleChoiceSettingDialog(
                title = "Search Bar Position",
                options = listOf("TOP", "BOTTOM"),
                selectedOption = settingsState.searchBarPosition,
                optionLabel = { it.lowercase().replaceFirstChar { char -> char.uppercase() } },
                onDismiss = { showSearchBarPositionDialog = false },
                onConfirm = { selected ->
                    showSearchBarPositionDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchBarPosition = selected) }
                    }
                }
            )
        }

        if (showDrawerSearchPositionDialog) {
            SingleChoiceSettingDialog(
                title = "App Drawer Search Position",
                options = listOf("TOP", "BOTTOM"),
                selectedOption = settingsState.drawerSearchPosition,
                optionLabel = { it.lowercase().replaceFirstChar { char -> char.uppercase() } },
                onDismiss = { showDrawerSearchPositionDialog = false },
                onConfirm = { selected ->
                    showDrawerSearchPositionDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(drawerSearchPosition = selected) }
                    }
                }
            )
        }

        if (showSearchBarCornerRadiusDialog) {
            SliderSettingDialog(
                title = "Search Bar Corner Shape",
                value = settingsState.searchBarCornerRadiusDp.toFloat(),
                valueRange = 0f..28f,
                steps = 28,
                valueLabel = { "${it.toInt()} dp" },
                onDismiss = { showSearchBarCornerRadiusDialog = false },
                onConfirm = { selected ->
                    showSearchBarCornerRadiusDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchBarCornerRadiusDp = selected.toInt()) }
                    }
                },
                previewContent = { radius ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(mockWallpaperBrush)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(radius.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(radius.dp)
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Search…",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            )
        }

        if (showDrawerBgColorDialog) {
            DrawerBgColorSettingDialog(
                initialColor = settingsState.drawerBgColor,
                onDismiss = { showDrawerBgColorDialog = false },
                onConfirm = { selectedColor ->
                    showDrawerBgColorDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(drawerBgColor = selectedColor) }
                    }
                }
            )
        }

        if (showDrawerBgOpacityDialog) {
            val iconShapeObj = remember(
                settingsState.iconCornerTopStart,
                settingsState.iconCornerTopEnd,
                settingsState.iconCornerBottomStart,
                settingsState.iconCornerBottomEnd,
                settingsState.iconCornerCut
            ) {
                getIconShape(
                    topStartPercent = settingsState.iconCornerTopStart,
                    topEndPercent = settingsState.iconCornerTopEnd,
                    bottomStartPercent = settingsState.iconCornerBottomStart,
                    bottomEndPercent = settingsState.iconCornerBottomEnd,
                    isCut = settingsState.iconCornerCut
                )
            }
            val bgColorObj = remember(settingsState.drawerBgColor) {
                try {
                    Color(android.graphics.Color.parseColor(settingsState.drawerBgColor))
                } catch (e: Exception) {
                    Color.Black
                }
            }
            SliderSettingDialog(
                title = "App Drawer Background Opacity",
                value = settingsState.drawerBgOpacity.toFloat(),
                valueRange = 0f..100f,
                steps = 20,
                valueLabel = { "${it.toInt()}%" },
                onDismiss = { showDrawerBgOpacityDialog = false },
                onConfirm = { selected ->
                    showDrawerBgOpacityDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(drawerBgOpacity = selected.toInt()) }
                    }
                },
                previewContent = { opacity ->
                    MiniAppDrawerMockup(
                        iconSize = settingsState.drawerIconSizeDp.dp,
                        labelSize = settingsState.labelSizeSp.sp,
                        showLabels = settingsState.showDrawerLabels,
                        iconShape = iconShapeObj,
                        columns = settingsState.drawerColumns,
                        bgColor = bgColorObj,
                        opacity = opacity.toInt(),
                        topStart = settingsState.drawerCornerTopStartDp.dp,
                        topEnd = settingsState.drawerCornerTopEndDp.dp,
                        bottomStart = settingsState.drawerCornerBottomStartDp.dp,
                        bottomEnd = settingsState.drawerCornerBottomEndDp.dp,
                        space = settingsState.drawerSpaceDp.dp
                    )
                }
            )
        }

        if (showDrawerBgImageDialog) {
            AlertDialog(
                onDismissRequest = { showDrawerBgImageDialog = false },
                title = { Text("App Drawer Background Image") },
                text = {
                    Text(
                        if (settingsState.drawerBgImage != null) "A custom background image is currently set."
                        else "No background image is set."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDrawerBgImageDialog = false
                            try {
                                imagePickerLauncher.launch(arrayOf("image/*"))
                            } catch (e: Exception) {}
                        }
                    ) {
                        Text("Select Image")
                    }
                },
                dismissButton = {
                    if (settingsState.drawerBgImage != null) {
                        TextButton(
                            onClick = {
                                showDrawerBgImageDialog = false
                                coroutineScope.launch {
                                    settingsRepository.updateSettings { it.copy(drawerBgImage = null) }
                                }
                            }
                        ) {
                            Text("Remove Image")
                        }
                    } else {
                        TextButton(onClick = { showDrawerBgImageDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        if (showDrawerCornerRadiusDialog) {
            DrawerPlatformLayoutDialog(
                initialTopStart = settingsState.drawerCornerTopStartDp,
                initialTopEnd = settingsState.drawerCornerTopEndDp,
                initialBottomStart = settingsState.drawerCornerBottomStartDp,
                initialBottomEnd = settingsState.drawerCornerBottomEndDp,
                initialSpace = settingsState.drawerSpaceDp,
                onDismiss = { showDrawerCornerRadiusDialog = false },
                onConfirm = { topStart, topEnd, bottomStart, bottomEnd, space ->
                    showDrawerCornerRadiusDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings {
                            it.copy(
                                drawerCornerTopStartDp = topStart,
                                drawerCornerTopEndDp = topEnd,
                                drawerCornerBottomStartDp = bottomStart,
                                drawerCornerBottomEndDp = bottomEnd,
                                drawerSpaceDp = space
                            )
                        }
                    }
                }
            )
        }

        if (showDrawerSearchStyleDialog) {
            DrawerSearchStyleDialog(
                initialHeight = settingsState.drawerSearchHeightDp,
                initialCornerRadius = settingsState.drawerSearchCornerRadiusDp,
                initialBgColor = settingsState.drawerSearchBgColor,
                initialBgOpacity = settingsState.drawerSearchBgOpacity,
                initialBorderWidth = settingsState.drawerSearchBorderWidthDp,
                initialBorderColor = settingsState.drawerSearchBorderColor,
                onDismiss = { showDrawerSearchStyleDialog = false },
                onConfirm = { height, radius, bgColor, opacity, borderWidth, borderColor ->
                    showDrawerSearchStyleDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings {
                            it.copy(
                                drawerSearchHeightDp = height,
                                drawerSearchCornerRadiusDp = radius,
                                drawerSearchBgColor = bgColor,
                                drawerSearchBgOpacity = opacity,
                                drawerSearchBorderWidthDp = borderWidth,
                                drawerSearchBorderColor = borderColor
                            )
                        }
                    }
                }
            )
        }

        if (showDrawerAnimationDialog) {
            SingleChoiceSettingDialog(
                title = "App Drawer Opening Animation",
                options = listOf("SLIDE", "CARD", "ZOOM", "FADE"),
                selectedOption = settingsState.drawerAnimationType,
                optionLabel = { option ->
                    when (option) {
                        "CARD" -> "Card Pop (Fluid iOS-like)"
                        "ZOOM" -> "Zoom Scale"
                        "FADE" -> "Fade Transition"
                        else -> "Slide Up (Classic)"
                    }
                },
                onDismiss = { showDrawerAnimationDialog = false },
                onConfirm = { selected ->
                    showDrawerAnimationDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(drawerAnimationType = selected) }
                    }
                }
            )
        }

        if (showSearchResultsSpacingDialog) {
            SliderSettingDialog(
                title = "Search Results Spacing",
                value = settingsState.searchResultsSpacingDp.toFloat(),
                valueRange = 0f..32f,
                steps = 32,
                valueLabel = { "${it.toInt()} dp" },
                onDismiss = { showSearchResultsSpacingDialog = false },
                onConfirm = { selected ->
                    showSearchResultsSpacingDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchResultsSpacingDp = selected.toInt()) }
                    }
                },
                previewContent = { spacing ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(spacing.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        )
                    }
                }
            )
        }

        if (showSearchBarPaddingDialog) {
            SliderSettingDialog(
                title = "Search Bar Spacing (Padding)",
                value = settingsState.searchBarPaddingDp.toFloat(),
                valueRange = 0f..32f,
                steps = 32,
                valueLabel = { "${it.toInt()} dp" },
                onDismiss = { showSearchBarPaddingDialog = false },
                onConfirm = { selected ->
                    showSearchBarPaddingDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchBarPaddingDp = selected.toInt()) }
                    }
                },
                previewContent = { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = padding.dp, vertical = (padding * 0.5f).dp)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Search Bar Preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }

        if (showOrbSpaceSpacingDialog) {
            SliderSettingDialog(
                title = "OrbSpace Layout Spacing",
                value = settingsState.orbSpaceSpacingDp.toFloat(),
                valueRange = 4f..48f,
                steps = 44,
                valueLabel = { "${it.toInt()} dp" },
                onDismiss = { showOrbSpaceSpacingDialog = false },
                onConfirm = { selected ->
                    showOrbSpaceSpacingDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(orbSpaceSpacingDp = selected.toInt()) }
                    }
                },
                previewContent = { spacing ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        )
                    }
                }
            )
        }

        if (showOrbSpaceCardCornerRadiusDialog) {
            SliderSettingDialog(
                title = "OrbSpace Card Rounded Corners",
                value = settingsState.orbSpaceCardCornerRadiusDp.toFloat(),
                valueRange = 0f..32f,
                steps = 32,
                valueLabel = { "${it.toInt()} dp" },
                onDismiss = { showOrbSpaceCardCornerRadiusDialog = false },
                onConfirm = { selected ->
                    showOrbSpaceCardCornerRadiusDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(orbSpaceCardCornerRadiusDp = selected.toInt()) }
                    }
                },
                previewContent = { radius ->
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(radius.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }
            )
        }

        if (showOrbSpaceLayoutModeDialog) {
            SingleChoiceSettingDialog(
                title = "OrbSpace Layout Mode",
                options = listOf("APPLE_HEALTH", "COMPACT_LIST", "BENTO"),
                selectedOption = settingsState.orbSpaceLayoutMode,
                optionLabel = { option ->
                    when (option) {
                        "APPLE_HEALTH" -> "Apple Health Grid"
                        "COMPACT_LIST" -> "Compact List"
                        "BENTO" -> "Bento Grid"
                        else -> option
                    }
                },
                onDismiss = { showOrbSpaceLayoutModeDialog = false },
                onConfirm = { selected ->
                    showOrbSpaceLayoutModeDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(orbSpaceLayoutMode = selected) }
                    }
                }
            )
        }

        if (showOrbSpaceAccentColorDialog) {
            val initialColor = remember(settingsState.orbSpaceAccentColor) {
                try {
                    android.graphics.Color.parseColor(settingsState.orbSpaceAccentColor)
                } catch (e: Exception) {
                    android.graphics.Color.parseColor("#007AFF")
                }
            }
            var redVal by remember { mutableStateOf(android.graphics.Color.red(initialColor)) }
            var greenVal by remember { mutableStateOf(android.graphics.Color.green(initialColor)) }
            var blueVal by remember { mutableStateOf(android.graphics.Color.blue(initialColor)) }
            
            val liveColor = remember(redVal, greenVal, blueVal) {
                Color(redVal, greenVal, blueVal)
            }
            
            AlertDialog(
                onDismissRequest = { showOrbSpaceAccentColorDialog = false },
                title = { Text("OrbSpace Accent Color (RGB)") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Live Color Preview Box
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(liveColor)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        
                        Text(
                            text = String.format("#%02X%02X%02X", redVal, greenVal, blueVal),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Red Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Red", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                                    Text("$redVal", style = MaterialTheme.typography.bodyMedium)
                                }
                                Slider(
                                    value = redVal.toFloat(),
                                    onValueChange = { redVal = it.toInt() },
                                    valueRange = 0f..255f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Red,
                                        activeTrackColor = Color.Red.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            
                            // Green Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Green", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Green)
                                    Text("$greenVal", style = MaterialTheme.typography.bodyMedium)
                                }
                                Slider(
                                    value = greenVal.toFloat(),
                                    onValueChange = { greenVal = it.toInt() },
                                    valueRange = 0f..255f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Green,
                                        activeTrackColor = Color.Green.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            
                            // Blue Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Blue", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Blue)
                                    Text("$blueVal", style = MaterialTheme.typography.bodyMedium)
                                }
                                Slider(
                                    value = blueVal.toFloat(),
                                    onValueChange = { blueVal = it.toInt() },
                                    valueRange = 0f..255f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Blue,
                                        activeTrackColor = Color.Blue.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val hexString = String.format("#%02X%02X%02X", redVal, greenVal, blueVal)
                            coroutineScope.launch {
                                settingsRepository.updateSettings { it.copy(orbSpaceAccentColor = hexString) }
                            }
                            showOrbSpaceAccentColorDialog = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOrbSpaceAccentColorDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showSearchResultBgColorDialog) {
            CustomColorSettingDialog(
                title = "Search Result Background Color",
                initialColor = settingsState.searchResultBgColor,
                onDismiss = { showSearchResultBgColorDialog = false },
                onConfirm = { selected ->
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchResultBgColor = selected) }
                    }
                    showSearchResultBgColorDialog = false
                }
            )
        }

        if (showSearchResultBgOpacityDialog) {
            SliderSettingDialog(
                title = "Search Result Background Opacity",
                value = settingsState.searchResultBgOpacity.toFloat(),
                valueRange = 0f..100f,
                onDismiss = { showSearchResultBgOpacityDialog = false },
                onConfirm = { selected ->
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchResultBgOpacity = selected.toInt()) }
                    }
                    showSearchResultBgOpacityDialog = false
                },
                valueLabel = { "${it.toInt()}%" }
            )
        }

        if (showSearchResultPaddingDialog) {
            SliderSettingDialog(
                title = "Search Result Inner Padding",
                value = settingsState.searchResultPaddingDp.toFloat(),
                valueRange = 4f..32f,
                onDismiss = { showSearchResultPaddingDialog = false },
                onConfirm = { selected ->
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchResultPaddingDp = selected.toInt()) }
                    }
                    showSearchResultPaddingDialog = false
                },
                valueLabel = { "${it.toInt()} dp" }
            )
        }

        if (showSearchResultTextSizeDialog) {
            SliderSettingDialog(
                title = "Search Result Text Size",
                value = settingsState.searchResultTextSizeSp.toFloat(),
                valueRange = 10f..24f,
                onDismiss = { showSearchResultTextSizeDialog = false },
                onConfirm = { selected ->
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(searchResultTextSizeSp = selected.toInt()) }
                    }
                    showSearchResultTextSizeDialog = false
                },
                valueLabel = { "${it.toInt()} sp" }
            )
        }

        if (showWellnessWidgetBgOpacityDialog) {
            SliderSettingDialog(
                title = "Wellness Widget Opacity",
                value = settingsState.wellnessWidgetBgOpacity.toFloat(),
                valueRange = 10f..100f,
                onDismiss = { showWellnessWidgetBgOpacityDialog = false },
                onConfirm = { selected ->
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(wellnessWidgetBgOpacity = selected.toInt()) }
                    }
                    showWellnessWidgetBgOpacityDialog = false
                },
                valueLabel = { "${it.toInt()}%" }
            )
        }

        if (showWellnessWidgetHeightDialog) {
            SliderSettingDialog(
                title = "Wellness Widget Minimum Height",
                value = settingsState.wellnessWidgetHeightDp.toFloat(),
                valueRange = 80f..250f,
                onDismiss = { showWellnessWidgetHeightDialog = false },
                onConfirm = { selected ->
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(wellnessWidgetHeightDp = selected.toInt()) }
                    }
                    showWellnessWidgetHeightDialog = false
                },
                valueLabel = { "${it.toInt()} dp" }
            )
        }

        if (showWellnessWidgetCornerRadiusDialog) {
            SliderSettingDialog(
                title = "Wellness Widget Corner Radius",
                value = settingsState.wellnessWidgetCornerRadiusDp.toFloat(),
                valueRange = 0f..40f,
                onDismiss = { showWellnessWidgetCornerRadiusDialog = false },
                onConfirm = { selected ->
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(wellnessWidgetCornerRadiusDp = selected.toInt()) }
                    }
                    showWellnessWidgetCornerRadiusDialog = false
                },
                valueLabel = { "${it.toInt()} dp" }
            )
        }

        if (showWellnessWidgetAccentColorDialog) {
            CustomColorSettingDialog(
                title = "Wellness Widget Accent Color",
                initialColor = settingsState.wellnessWidgetAccentColor,
                onDismiss = { showWellnessWidgetAccentColorDialog = false },
                onConfirm = { selected ->
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(wellnessWidgetAccentColor = selected) }
                    }
                    showWellnessWidgetAccentColorDialog = false
                }
            )
        }

        if (showOpenWithStyleDialog) {
            SingleChoiceSettingDialog(
                title = "Open With Style",
                options = listOf("APPLE_CHIPS", "SPOTLIGHT_MINIMAL", "BENTO_TILES"),
                selectedOption = settingsState.openWithStyle,
                optionLabel = { option ->
                    when (option) {
                        "APPLE_CHIPS" -> "Apple Chips"
                        "SPOTLIGHT_MINIMAL" -> "Spotlight Rows"
                        "BENTO_TILES" -> "Bento Tiles"
                        else -> option
                    }
                },
                onDismiss = { showOpenWithStyleDialog = false },
                onConfirm = { selected ->
                    showOpenWithStyleDialog = false
                    coroutineScope.launch {
                        settingsRepository.updateSettings { it.copy(openWithStyle = selected) }
                    }
                }
            )
        }

        if (showWellnessAccessibilityDialog) {
            AlertDialog(
                onDismissRequest = { showWellnessAccessibilityDialog = false },
                title = { Text("Enable Scroll Tracker?") },
                text = { 
                    Text(
                        "Enabling this feature uses Android's Accessibility Service to track scroll/swipe finger movements inside social media apps.\n\n" +
                        "• The scroll count is approximate.\n" +
                        "• All data is processed locally on your device and is never uploaded or shared.\n\n" +
                        "Do you wish to continue?"
                    ) 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showWellnessAccessibilityDialog = false
                            coroutineScope.launch {
                                settingsRepository.updateSettings { it.copy(showScrollTrackerWidget = pendingWellnessToggleValue) }
                            }
                        }
                    ) {
                        Text("Enable")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWellnessAccessibilityDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showFirstTimeFreeformDialog) {
            var isEnabled by remember { mutableStateOf(true) }
            AlertDialog(
                onDismissRequest = { 
                    showFirstTimeFreeformDialog = false 
                    val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                    sp.edit().putBoolean("freeform_walkthrough_shown", true).apply()
                },
                title = { Text("Freeform Workspace Placement") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Freeform placement allows you to drag, drop, and resize your icons freely on the home screen, ignoring the traditional grid layout.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Freeform Mode", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                            sp.edit().putBoolean("freeform_walkthrough_shown", true).apply()
                            coroutineScope.launch {
                                settingsRepository.updateSettings { it.copy(freeFormPlacement = isEnabled) }
                            }
                            showFirstTimeFreeformDialog = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                            sp.edit().putBoolean("freeform_walkthrough_shown", true).apply()
                            showFirstTimeFreeformDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showFirstTimeOrbSpaceLockDialog) {
            var isEnabled by remember { mutableStateOf(true) }
            AlertDialog(
                onDismissRequest = { 
                    showFirstTimeOrbSpaceLockDialog = false 
                    val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                    sp.edit().putBoolean("orbspace_lock_walkthrough_shown", true).apply()
                },
                title = { Text("Lock OrbSpace Page") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Locking the OrbSpace dashboard ensures that your personalized statistics, shortcuts, and app locks are hidden behind your system's biometric security (fingerprint/face recognition).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Biometric Lock", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                            sp.edit().putBoolean("orbspace_lock_walkthrough_shown", true).apply()
                            coroutineScope.launch {
                                settingsRepository.updateSettings { it.copy(lockOrbSpace = isEnabled) }
                            }
                            showFirstTimeOrbSpaceLockDialog = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                            sp.edit().putBoolean("orbspace_lock_walkthrough_shown", true).apply()
                            showFirstTimeOrbSpaceLockDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showFirstTimeGesturesDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showFirstTimeGesturesDialog = false 
                    val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                    sp.edit().putBoolean("gestures_walkthrough_shown", true).apply()
                },
                title = { Text("System Gestures Walkthrough") },
                text = {
                    Text(
                        "Set up advanced gesture actions on your home screen, such as swipe up for drawer, swipe down for notifications, or double-tap to lock screen.\n\n" +
                        "Custom gestures enable faster control over your workflow.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                            sp.edit().putBoolean("gestures_walkthrough_shown", true).apply()
                            showGestureSettings = true
                            showFirstTimeGesturesDialog = false
                        }
                    ) {
                        Text("Configure Gestures")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                            sp.edit().putBoolean("gestures_walkthrough_shown", true).apply()
                            showFirstTimeGesturesDialog = false
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
}

private data class CategoryItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val key: String,
    val description: String,
    val gradientColors: List<Color>
)

private data class WalkthroughInfo(
    val headline: String,
    val body: String
)

@Composable
private fun SliderSettingDialog(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueLabel: (Float) -> String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
    previewContent: (@Composable (Float) -> Unit)? = null
) {
    var currentValue by remember { mutableStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (previewContent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        previewContent(currentValue)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = valueLabel(currentValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = currentValue,
                    onValueChange = { currentValue = it },
                    valueRange = valueRange,
                    steps = steps,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentValue) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GridSizeSettingDialog(
    initialColumns: Int,
    initialRows: Int,
    onDismiss: () -> Unit,
    onConfirm: (cols: Int, rows: Int) -> Unit
) {
    var cols by remember { mutableStateOf(initialColumns.toFloat()) }
    var rows by remember { mutableStateOf(initialRows.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grid Size") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Columns", style = MaterialTheme.typography.bodyLarge)
                    Text("${cols.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = cols,
                    onValueChange = { cols = it },
                    valueRange = 3f..10f,
                    steps = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rows", style = MaterialTheme.typography.bodyLarge)
                    Text("${rows.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = rows,
                    onValueChange = { rows = it },
                    valueRange = 3f..10f,
                    steps = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(cols.toInt(), rows.toInt()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun <T> SingleChoiceSettingDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit
) {
    var selected by remember { mutableStateOf(selectedOption) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(options) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selected),
                            onClick = { selected = option }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    containerColor: Color,
    iconColor: Color
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(containerColor.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerBgColorSettingDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var hexInput by remember { mutableStateOf(initialColor) }
    
    val initialRgb = remember(initialColor) { hexToRgb(initialColor) }
    var redVal by remember { mutableStateOf(initialRgb.first.toFloat()) }
    var greenVal by remember { mutableStateOf(initialRgb.second.toFloat()) }
    var blueVal by remember { mutableStateOf(initialRgb.third.toFloat()) }

    LaunchedEffect(hexInput) {
        try {
            val colorInt = android.graphics.Color.parseColor(hexInput)
            redVal = android.graphics.Color.red(colorInt).toFloat()
            greenVal = android.graphics.Color.green(colorInt).toFloat()
            blueVal = android.graphics.Color.blue(colorInt).toFloat()
        } catch (e: Exception) {
            // Ignore invalid input while user is typing
        }
    }

    val onRedChange: (Float) -> Unit = { r ->
        redVal = r
        hexInput = rgbToHex(r.toInt(), greenVal.toInt(), blueVal.toInt())
    }
    val onGreenChange: (Float) -> Unit = { g ->
        greenVal = g
        hexInput = rgbToHex(redVal.toInt(), g.toInt(), blueVal.toInt())
    }
    val onBlueChange: (Float) -> Unit = { b ->
        blueVal = b
        hexInput = rgbToHex(redVal.toInt(), greenVal.toInt(), b.toInt())
    }

    val presets = listOf(
        "#000000" to "Jet Black",
        "#121212" to "Dark Charcoal",
        "#1A1A2E" to "Deep Midnight",
        "#1B365D" to "Classic Navy",
        "#0B6623" to "Hunter Green",
        "#722F37" to "Burgundy",
        "#2C3E50" to "Slate Blue"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Drawer Background Color") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    Text("Presets:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.take(4).forEach { (hex, name) ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { hexInput = hex }
                                    .border(
                                        width = if (hexInput.uppercase() == hex.uppercase()) 3.dp else 1.dp,
                                        color = if (hexInput.uppercase() == hex.uppercase()) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.drop(4).forEach { (hex, name) ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { hexInput = hex }
                                    .border(
                                        width = if (hexInput.uppercase() == hex.uppercase()) 3.dp else 1.dp,
                                        color = if (hexInput.uppercase() == hex.uppercase()) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    Text("RGB Sliders Customization:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // R Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Red: ${redVal.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = redVal,
                        onValueChange = onRedChange,
                        valueRange = 0f..255f,
                        steps = 255,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Red,
                            thumbColor = Color.Red
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // G Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Green: ${greenVal.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = greenVal,
                        onValueChange = onGreenChange,
                        valueRange = 0f..255f,
                        steps = 255,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Green,
                            thumbColor = Color.Green
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // B Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Blue: ${blueVal.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = blueVal,
                        onValueChange = onBlueChange,
                        valueRange = 0f..255f,
                        steps = 255,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Blue,
                            thumbColor = Color.Blue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text("Custom Hex Color:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val previewColor = try {
                            Color(android.graphics.Color.parseColor(hexInput))
                        } catch (e: Exception) {
                            Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(previewColor)
                                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        )
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { hexInput = it },
                            placeholder = { Text("#000000") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            isError = try {
                                android.graphics.Color.parseColor(hexInput)
                                false
                            } catch (e: Exception) {
                                true
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val isValid = try {
                android.graphics.Color.parseColor(hexInput)
                true
            } catch (e: Exception) {
                false
            }
            TextButton(
                onClick = { if (isValid) onConfirm(hexInput) },
                enabled = isValid
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GridSizeSettingDialog(
    initialColumns: Int,
    initialRows: Int,
    onDismiss: () -> Unit,
    onConfirm: (cols: Int, rows: Int) -> Unit,
    previewContent: @Composable (cols: Int, rows: Int) -> Unit
) {
    var cols by remember { mutableStateOf(initialColumns.toFloat()) }
    var rows by remember { mutableStateOf(initialRows.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grid Size") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    previewContent(cols.toInt(), rows.toInt())
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Columns", style = MaterialTheme.typography.bodyLarge)
                    Text("${cols.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = cols,
                    onValueChange = { cols = it },
                    valueRange = 3f..10f,
                    steps = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rows", style = MaterialTheme.typography.bodyLarge)
                    Text("${rows.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = rows,
                    onValueChange = { rows = it },
                    valueRange = 3f..10f,
                    steps = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(cols.toInt(), rows.toInt()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun IconShapeDesignerDialog(
    initialTopStart: Int,
    initialTopEnd: Int,
    initialBottomStart: Int,
    initialBottomEnd: Int,
    initialIsCut: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (topStart: Int, topEnd: Int, bottomStart: Int, bottomEnd: Int, isCut: Boolean) -> Unit
) {
    var topStart by remember { mutableStateOf(initialTopStart.toFloat()) }
    var topEnd by remember { mutableStateOf(initialTopEnd.toFloat()) }
    var bottomStart by remember { mutableStateOf(initialBottomStart.toFloat()) }
    var bottomEnd by remember { mutableStateOf(initialBottomEnd.toFloat()) }
    var isCut by remember { mutableStateOf(initialIsCut) }

    val currentShape = remember(topStart, topEnd, bottomStart, bottomEnd, isCut) {
        getIconShape(topStart.toInt(), topEnd.toInt(), bottomStart.toInt(), bottomEnd.toInt(), isCut)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Icon Shape Designer") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(Color.DarkGray.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Calendar" to Color(0xFF4285F4), "Photos" to Color(0xFFEA4335), "Settings" to Color(0xFF34A853)).forEach { (label, color) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(currentShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cut Corners", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = isCut,
                            onCheckedChange = { isCut = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top-Left Corner", style = MaterialTheme.typography.bodyMedium)
                        Text("${topStart.toInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = topStart,
                        onValueChange = { topStart = it },
                        valueRange = 0f..50f,
                        steps = 50,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top-Right Corner", style = MaterialTheme.typography.bodyMedium)
                        Text("${topEnd.toInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = topEnd,
                        onValueChange = { topEnd = it },
                        valueRange = 0f..50f,
                        steps = 50,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bottom-Left Corner", style = MaterialTheme.typography.bodyMedium)
                        Text("${bottomStart.toInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = bottomStart,
                        onValueChange = { bottomStart = it },
                        valueRange = 0f..50f,
                        steps = 50,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bottom-Right Corner", style = MaterialTheme.typography.bodyMedium)
                        Text("${bottomEnd.toInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = bottomEnd,
                        onValueChange = { bottomEnd = it },
                        valueRange = 0f..50f,
                        steps = 50,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        topStart.toInt(),
                        topEnd.toInt(),
                        bottomStart.toInt(),
                        bottomEnd.toInt(),
                        isCut
                    )
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DrawerPlatformLayoutDialog(
    initialTopStart: Int,
    initialTopEnd: Int,
    initialBottomStart: Int,
    initialBottomEnd: Int,
    initialSpace: Int,
    onDismiss: () -> Unit,
    onConfirm: (topStart: Int, topEnd: Int, bottomStart: Int, bottomEnd: Int, space: Int) -> Unit
) {
    var topStart by remember { mutableStateOf(initialTopStart.toFloat()) }
    var topEnd by remember { mutableStateOf(initialTopEnd.toFloat()) }
    var bottomStart by remember { mutableStateOf(initialBottomStart.toFloat()) }
    var bottomEnd by remember { mutableStateOf(initialBottomEnd.toFloat()) }
    var space by remember { mutableStateOf(initialSpace.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Drawer Platform Layout") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 90.dp, height = 120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(mockWallpaperBrush)
                        ) {
                            val resolvedBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding((space / 4f).dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = (topStart / 4f).dp,
                                            topEnd = (topEnd / 4f).dp,
                                            bottomStart = (bottomStart / 4f).dp,
                                            bottomEnd = (bottomEnd / 4f).dp
                                        )
                                    )
                                    .background(resolvedBg)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(
                                            topStart = (topStart / 4f).dp,
                                            topEnd = (topEnd / 4f).dp,
                                            bottomStart = (bottomStart / 4f).dp,
                                            bottomEnd = (bottomEnd / 4f).dp
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Drawer Card", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bezel Margin / Floating Space", style = MaterialTheme.typography.bodyMedium)
                        Text("${space.toInt()} dp", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = space,
                        onValueChange = { space = it },
                        valueRange = 0f..40f,
                        steps = 40,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top-Left Corner", style = MaterialTheme.typography.bodyMedium)
                        Text("${topStart.toInt()} dp", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = topStart,
                        onValueChange = { topStart = it },
                        valueRange = 0f..40f,
                        steps = 40,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top-Right Corner", style = MaterialTheme.typography.bodyMedium)
                        Text("${topEnd.toInt()} dp", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = topEnd,
                        onValueChange = { topEnd = it },
                        valueRange = 0f..40f,
                        steps = 40,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bottom-Left Corner", style = MaterialTheme.typography.bodyMedium)
                        Text("${bottomStart.toInt()} dp", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = bottomStart,
                        onValueChange = { bottomStart = it },
                        valueRange = 0f..40f,
                        steps = 40,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bottom-Right Corner", style = MaterialTheme.typography.bodyMedium)
                        Text("${bottomEnd.toInt()} dp", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = bottomEnd,
                        onValueChange = { bottomEnd = it },
                        valueRange = 0f..40f,
                        steps = 40,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        topStart.toInt(),
                        topEnd.toInt(),
                        bottomStart.toInt(),
                        bottomEnd.toInt(),
                        space.toInt()
                    )
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MiniHomeScreenMockup(
    iconSize: Dp,
    labelSize: TextUnit,
    showLabels: Boolean,
    iconShape: Shape,
    dimPercent: Int = 0
) {
    Box(
        modifier = Modifier
            .size(width = 130.dp, height = 180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(mockWallpaperBrush)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimPercent / 100f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("9:41", color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Box(modifier = Modifier.size(4.dp, 3.dp).background(Color.White))
                Box(modifier = Modifier.size(4.dp, 3.dp).background(Color.White))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp, bottom = 36.dp, start = 6.dp, end = 6.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MockAppItem(iconSize / 2, labelSize * 0.7f, showLabels, iconShape, "System")
                MockAppItem(iconSize / 2, labelSize * 0.7f, showLabels, iconShape, "Store")
                MockAppItem(iconSize / 2, labelSize * 0.7f, showLabels, iconShape, "Chat")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MockAppItem(iconSize / 2, labelSize * 0.7f, showLabels, iconShape, "Music")
                MockAppItem(iconSize / 2, labelSize * 0.7f, showLabels, iconShape, "Maps")
                MockAppItem(iconSize / 2, labelSize * 0.7f, showLabels, iconShape, "Camera")
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .align(Alignment.BottomCenter)
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Box(modifier = Modifier.size(iconSize / 2.2f).clip(iconShape).background(Color.White))
                Box(modifier = Modifier.size(iconSize / 2.2f).clip(iconShape).background(Color.White))
                Box(modifier = Modifier.size(iconSize / 2.2f).clip(iconShape).background(Color.White))
                Box(modifier = Modifier.size(iconSize / 2.2f).clip(iconShape).background(Color.White))
            }
        }
    }
}

@Composable
private fun MiniAppDrawerMockup(
    iconSize: Dp,
    labelSize: TextUnit,
    showLabels: Boolean,
    iconShape: Shape,
    columns: Int,
    bgColor: Color,
    opacity: Int,
    topStart: Dp,
    topEnd: Dp,
    bottomStart: Dp,
    bottomEnd: Dp,
    space: Dp
) {
    Box(
        modifier = Modifier
            .size(width = 130.dp, height = 180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(mockWallpaperBrush)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("9:41", color = Color.White, fontSize = 6.sp)
        }
        
        val resolvedBg = bgColor.copy(alpha = opacity / 100f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding((space / 3.5f))
                .clip(RoundedCornerShape(topStart / 3.5f, topEnd / 3.5f, bottomEnd / 3.5f, bottomStart / 3.5f))
                .background(resolvedBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .padding(horizontal = 4.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(columns * 3) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(iconSize / 2.5f)
                                    .clip(iconShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            if (showLabels) {
                                Spacer(modifier = Modifier.height(1.dp))
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(2.dp)
                                        .background(Color.White.copy(alpha = 0.5f))
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
private fun MockAppItem(
    iconSize: Dp,
    labelSize: TextUnit,
    showLabels: Boolean,
    iconShape: Shape,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(iconShape)
                .background(Color.White)
        )
        if (showLabels) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(label, fontSize = 5.sp, color = Color.White)
        }
    }
}

private val mockWallpaperBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
)

private fun hexToRgb(hex: String): Triple<Int, Int, Int> {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        val r = android.graphics.Color.red(colorInt)
        val g = android.graphics.Color.green(colorInt)
        val b = android.graphics.Color.blue(colorInt)
        Triple(r, g, b)
    } catch (e: Exception) {
        Triple(0, 0, 0)
    }
}

private fun rgbToHex(r: Int, g: Int, b: Int): String {
    return String.format("#%02X%02X%02X", r, g, b)
}

@Composable
private fun DrawerSearchStyleDialog(
    initialHeight: Int,
    initialCornerRadius: Int,
    initialBgColor: String,
    initialBgOpacity: Int,
    initialBorderWidth: Int,
    initialBorderColor: String,
    onDismiss: () -> Unit,
    onConfirm: (height: Int, radius: Int, bgColor: String, opacity: Int, borderWidth: Int, borderColor: String) -> Unit
) {
    var height by remember { mutableStateOf(initialHeight) }
    var cornerRadius by remember { mutableStateOf(initialCornerRadius) }
    var bgColor by remember { mutableStateOf(initialBgColor) }
    var bgOpacity by remember { mutableStateOf(initialBgOpacity) }
    var borderWidth by remember { mutableStateOf(initialBorderWidth) }
    var borderColor by remember { mutableStateOf(initialBorderColor) }

    // BG Color RGB values
    val bgRgb = remember(bgColor) { hexToRgb(bgColor) }
    var bgR by remember { mutableStateOf(bgRgb.first.toFloat()) }
    var bgG by remember { mutableStateOf(bgRgb.second.toFloat()) }
    var bgB by remember { mutableStateOf(bgRgb.third.toFloat()) }

    // Border Color RGB values
    val borderRgb = remember(borderColor) { hexToRgb(borderColor) }
    var borderR by remember { mutableStateOf(borderRgb.first.toFloat()) }
    var borderG by remember { mutableStateOf(borderRgb.second.toFloat()) }
    var borderB by remember { mutableStateOf(borderRgb.third.toFloat()) }

    LaunchedEffect(bgColor) {
        try {
            val colorInt = android.graphics.Color.parseColor(bgColor)
            bgR = android.graphics.Color.red(colorInt).toFloat()
            bgG = android.graphics.Color.green(colorInt).toFloat()
            bgB = android.graphics.Color.blue(colorInt).toFloat()
        } catch (e: Exception) {}
    }

    LaunchedEffect(borderColor) {
        try {
            val colorInt = android.graphics.Color.parseColor(borderColor)
            borderR = android.graphics.Color.red(colorInt).toFloat()
            borderG = android.graphics.Color.green(colorInt).toFloat()
            borderB = android.graphics.Color.blue(colorInt).toFloat()
        } catch (e: Exception) {}
    }

    val presets = listOf(
        "#FFFFFF" to "Pure White",
        "#000000" to "Jet Black",
        "#121212" to "Dark Charcoal",
        "#64B5F6" to "Light Blue",
        "#81C784" to "Light Green",
        "#FF8A65" to "Deep Orange",
        "#E0E0E0" to "Light Gray"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drawer Search Bar Style") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                // Live preview at top
                Text("Preview:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val previewBg = remember(bgColor, bgOpacity) {
                    try {
                        Color(android.graphics.Color.parseColor(bgColor)).copy(alpha = bgOpacity / 100f)
                    } catch (e: Exception) {
                        Color.White.copy(alpha = 0.12f)
                    }
                }
                val previewBorder = remember(borderColor) {
                    try {
                        Color(android.graphics.Color.parseColor(borderColor))
                    } catch (e: Exception) {
                        Color.White
                    }
                }
                val previewShape = RoundedCornerShape(cornerRadius.dp)
                val borderModifier = if (borderWidth > 0) {
                    Modifier.border(width = borderWidth.dp, color = previewBorder, shape = previewShape)
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0xFF151515), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(height.dp)
                            .then(borderModifier),
                        shape = previewShape,
                        color = previewBg
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Search apps...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Corners
                    item {
                        Text("Corner Radius: ${cornerRadius} dp", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = cornerRadius.toFloat(),
                            onValueChange = { cornerRadius = it.toInt() },
                            valueRange = 0f..32f,
                            steps = 32
                        )
                    }

                    // Size / Height
                    item {
                        Text("Height Size: ${height} dp", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = height.toFloat(),
                            onValueChange = { height = it.toInt() },
                            valueRange = 36f..64f,
                            steps = 28
                        )
                    }

                    // Background Opacity
                    item {
                        Text("Background Transparency: ${bgOpacity}%", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = bgOpacity.toFloat(),
                            onValueChange = { bgOpacity = it.toInt() },
                            valueRange = 0f..100f
                        )
                    }

                    // Background Color Picker
                    item {
                        Text("Background Color (Hex code)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = bgColor,
                            onValueChange = {
                                bgColor = it
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Presets:")
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.take(4).forEach { (hex, _) ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { bgColor = hex }
                                        .border(
                                            width = if (bgColor.uppercase() == hex.uppercase()) 2.dp else 1.dp,
                                            color = if (bgColor.uppercase() == hex.uppercase()) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Custom R / G / B Sliders:")
                        Text("Red: ${bgR.toInt()}", fontSize = 12.sp)
                        Slider(
                            value = bgR,
                            onValueChange = {
                                bgR = it
                                bgColor = rgbToHex(it.toInt(), bgG.toInt(), bgB.toInt())
                            },
                            valueRange = 0f..255f
                        )
                        Text("Green: ${bgG.toInt()}", fontSize = 12.sp)
                        Slider(
                            value = bgG,
                            onValueChange = {
                                bgG = it
                                bgColor = rgbToHex(bgR.toInt(), it.toInt(), bgB.toInt())
                            },
                            valueRange = 0f..255f
                        )
                        Text("Blue: ${bgB.toInt()}", fontSize = 12.sp)
                        Slider(
                            value = bgB,
                            onValueChange = {
                                bgB = it
                                bgColor = rgbToHex(bgR.toInt(), bgG.toInt(), it.toInt())
                            },
                            valueRange = 0f..255f
                        )
                    }

                    // Border Width / Wireframe
                    item {
                        Text("Border Width: ${borderWidth} dp", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = borderWidth.toFloat(),
                            onValueChange = { borderWidth = it.toInt() },
                            valueRange = 0f..4f,
                            steps = 4
                        )
                    }

                    // Border Color Picker
                    if (borderWidth > 0) {
                        item {
                            Text("Border Color (Hex code)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = borderColor,
                                onValueChange = {
                                    borderColor = it
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Custom R / G / B Sliders:")
                            Text("Red: ${borderR.toInt()}", fontSize = 12.sp)
                            Slider(
                                value = borderR,
                                onValueChange = {
                                    borderR = it
                                    borderColor = rgbToHex(it.toInt(), borderG.toInt(), borderB.toInt())
                                },
                                valueRange = 0f..255f
                            )
                            Text("Green: ${borderG.toInt()}", fontSize = 12.sp)
                            Slider(
                                value = borderG,
                                onValueChange = {
                                    borderG = it
                                    borderColor = rgbToHex(borderR.toInt(), it.toInt(), borderB.toInt())
                                },
                                valueRange = 0f..255f
                            )
                            Text("Blue: ${borderB.toInt()}", fontSize = 12.sp)
                            Slider(
                                value = borderB,
                                onValueChange = {
                                    borderB = it
                                    borderColor = rgbToHex(borderR.toInt(), borderG.toInt(), it.toInt())
                                },
                                valueRange = 0f..255f
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(height, cornerRadius, bgColor, bgOpacity, borderWidth, borderColor)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CustomColorSettingDialog(
    title: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialColorInt = remember(initialColor) {
        try {
            android.graphics.Color.parseColor(initialColor)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#007AFF")
        }
    }
    var redVal by remember { mutableStateOf(android.graphics.Color.red(initialColorInt)) }
    var greenVal by remember { mutableStateOf(android.graphics.Color.green(initialColorInt)) }
    var blueVal by remember { mutableStateOf(android.graphics.Color.blue(initialColorInt)) }
    
    val liveColor = remember(redVal, greenVal, blueVal) {
        Color(redVal, greenVal, blueVal)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(liveColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                
                Text(
                    text = String.format("#%02X%02X%02X", redVal, greenVal, blueVal),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Red
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Red", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("$redVal", style = MaterialTheme.typography.bodyMedium)
                        }
                        Slider(
                            value = redVal.toFloat(),
                            onValueChange = { redVal = it.toInt() },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Red,
                                activeTrackColor = Color.Red.copy(alpha = 0.5f)
                            )
                        )
                    }
                    // Green
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Green", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Green)
                            Text("$greenVal", style = MaterialTheme.typography.bodyMedium)
                        }
                        Slider(
                            value = greenVal.toFloat(),
                            onValueChange = { greenVal = it.toInt() },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Green,
                                activeTrackColor = Color.Green.copy(alpha = 0.5f)
                            )
                        )
                    }
                    // Blue
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Blue", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Blue)
                            Text("$blueVal", style = MaterialTheme.typography.bodyMedium)
                        }
                        Slider(
                            value = blueVal.toFloat(),
                            onValueChange = { blueVal = it.toInt() },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Blue,
                                activeTrackColor = Color.Blue.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hexString = String.format("#%02X%02X%02X", redVal, greenVal, blueVal)
                    onConfirm(hexString)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = android.content.ComponentName(context, "com.oorbitt.launcher.orbspace.ScrollTrackerService")
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (service in enabledServices) {
        val serviceInfo = service.resolveInfo.serviceInfo
        if (serviceInfo.packageName == context.packageName && serviceInfo.name == expectedComponentName.className) {
            return true
        }
    }
    return false
}