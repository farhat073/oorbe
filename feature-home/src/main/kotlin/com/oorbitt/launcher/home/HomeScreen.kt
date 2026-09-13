package com.oorbitt.launcher.home

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.oorbitt.launcher.home.widget.WidgetHostManager
import com.oorbitt.launcher.home.widget.WidgetPickerSheet
import com.oorbitt.launcher.orbspace.OrbSpaceScreen
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.oorbitt.launcher.data.repository.AppRepository
import com.oorbitt.launcher.data.repository.WorkspaceRepository
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.data.repository.GestureRepository
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.model.AppInfo
import com.oorbitt.launcher.model.WorkspaceItem
import com.oorbitt.launcher.model.ItemType
import com.oorbitt.launcher.model.ContainerType
import com.oorbitt.launcher.model.GestureType
import com.oorbitt.launcher.gesture.detectLauncherGestures
import com.oorbitt.launcher.gesture.GestureHandler
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

import com.oorbitt.launcher.ui.util.rememberAppIcon
import com.oorbitt.launcher.ui.util.getIconShape
import com.oorbitt.launcher.ui.util.CustomizeAppDialog
import com.oorbitt.launcher.ui.util.FloatingContextMenu
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

private fun findEmptySlotForWidget(
    page: Int,
    spanX: Int,
    spanY: Int,
    items: List<WorkspaceItem>
): Pair<Int, Int>? {
    val cols = 5
    val rows = 5
    val occupied = Array(cols) { BooleanArray(rows) }
    for (item in items) {
        if (item.screenIndex == page && item.containerType == ContainerType.WORKSPACE) {
            for (x in item.cellX until minOf(cols, item.cellX + item.spanX)) {
                for (y in item.cellY until minOf(rows, item.cellY + item.spanY)) {
                    occupied[x][y] = true
                }
            }
        }
    }
    
    for (y in 0..(rows - spanY)) {
        for (x in 0..(cols - spanX)) {
            var fits = true
            for (dx in 0 until spanX) {
                for (dy in 0 until spanY) {
                    if (occupied[x + dx][y + dy]) {
                        fits = false
                        break
                    }
                }
                if (!fits) break
            }
            if (fits) {
                return Pair(x, y)
            }
        }
    }
    return null
}

private fun simulatePushItemsAway(
    page: Int,
    targetX: Int,
    targetY: Int,
    spanX: Int,
    spanY: Int,
    workspaceItems: List<WorkspaceItem>,
    cols: Int = 5,
    rows: Int = 5
): Boolean {
    val overlapping = workspaceItems.filter {
        it.screenIndex == page &&
        it.containerType == ContainerType.WORKSPACE &&
        targetX < it.cellX + it.spanX &&
        targetX + spanX > it.cellX &&
        targetY < it.cellY + it.spanY &&
        targetY + spanY > it.cellY
    }

    if (overlapping.isEmpty()) return true

    val grid = Array(cols) { BooleanArray(rows) }
    for (x in targetX until minOf(cols, targetX + spanX)) {
        for (y in targetY until minOf(rows, targetY + spanY)) {
            grid[x][y] = true
        }
    }

    for (item in workspaceItems) {
        if (item.screenIndex == page && item.containerType == ContainerType.WORKSPACE && !overlapping.any { it.id == item.id }) {
            for (x in item.cellX until minOf(cols, item.cellX + item.spanX)) {
                for (y in item.cellY until minOf(rows, item.cellY + item.spanY)) {
                    grid[x][y] = true
                }
            }
        }
    }

    for (item in overlapping) {
        var foundSpot = false
        for (y in 0..(rows - item.spanY)) {
            for (x in 0..(cols - item.spanX)) {
                var isFree = true
                for (dx in 0 until item.spanX) {
                    for (dy in 0 until item.spanY) {
                        if (grid[x + dx][y + dy]) {
                            isFree = false
                            break
                        }
                    }
                    if (!isFree) break
                }
                if (isFree) {
                    for (dx in 0 until item.spanX) {
                        for (dy in 0 until item.spanY) {
                            grid[x + dx][y + dy] = true
                        }
                    }
                    foundSpot = true
                    break
                }
            }
            if (foundSpot) break
        }
        if (!foundSpot) return false
    }
    return true
}

private suspend fun tryPushItemsAway(
    page: Int,
    targetX: Int,
    targetY: Int,
    spanX: Int,
    spanY: Int,
    draggedItemId: Long,
    workspaceItems: List<WorkspaceItem>,
    workspaceRepository: WorkspaceRepository,
    cols: Int = 5,
    rows: Int = 5
): Boolean {
    val overlapping = workspaceItems.filter {
        it.id != draggedItemId &&
        it.screenIndex == page &&
        it.containerType == ContainerType.WORKSPACE &&
        targetX < it.cellX + it.spanX &&
        targetX + spanX > it.cellX &&
        targetY < it.cellY + it.spanY &&
        targetY + spanY > it.cellY
    }

    if (overlapping.isEmpty()) {
        return true
    }

    val grid = Array(cols) { BooleanArray(rows) }
    for (x in targetX until minOf(cols, targetX + spanX)) {
        for (y in targetY until minOf(rows, targetY + spanY)) {
            grid[x][y] = true
        }
    }

    for (item in workspaceItems) {
        if (item.id != draggedItemId && item.screenIndex == page && item.containerType == ContainerType.WORKSPACE && !overlapping.any { it.id == item.id }) {
            for (x in item.cellX until minOf(cols, item.cellX + item.spanX)) {
                for (y in item.cellY until minOf(rows, item.cellY + item.spanY)) {
                    grid[x][y] = true
                }
            }
        }
    }

    val relocations = mutableListOf<Pair<WorkspaceItem, Pair<Int, Int>>>()
    
    for (item in overlapping) {
        var foundSpot = false
        var bestDist = Double.MAX_VALUE
        var bestSpot: Pair<Int, Int>? = null
        
        for (y in 0..(rows - item.spanY)) {
            for (x in 0..(cols - item.spanX)) {
                var isFree = true
                for (dx in 0 until item.spanX) {
                    for (dy in 0 until item.spanY) {
                        if (grid[x + dx][y + dy]) {
                            isFree = false
                            break
                        }
                    }
                    if (!isFree) break
                }
                
                if (isFree) {
                    val dist = Math.hypot((x - item.cellX).toDouble(), (y - item.cellY).toDouble())
                    if (dist < bestDist) {
                        bestDist = dist
                        bestSpot = Pair(x, y)
                        foundSpot = true
                    }
                }
            }
        }
        
        if (foundSpot && bestSpot != null) {
            for (dx in 0 until item.spanX) {
                for (dy in 0 until item.spanY) {
                    grid[bestSpot.first + dx][bestSpot.second + dy] = true
                }
            }
            relocations.add(Pair(item, bestSpot))
        } else {
            return false
        }
    }

    for (reloc in relocations) {
        val (item, spot) = reloc
        workspaceRepository.updateItem(item.copy(cellX = spot.first, cellY = spot.second))
    }
    return true
}

private suspend fun findSlotWithPush(
    page: Int,
    spanX: Int,
    spanY: Int,
    workspaceItems: List<WorkspaceItem>,
    workspaceRepository: WorkspaceRepository,
    cols: Int = 5,
    rows: Int = 5
): Pair<Int, Int>? {
    val emptySlot = findEmptySlotForWidget(page, spanX, spanY, workspaceItems)
    if (emptySlot != null) return emptySlot

    for (y in 0..(rows - spanY)) {
        for (x in 0..(cols - spanX)) {
            val canPush = simulatePushItemsAway(
                page = page,
                targetX = x,
                targetY = y,
                spanX = spanX,
                spanY = spanY,
                workspaceItems = workspaceItems,
                cols = cols,
                rows = rows
            )
            if (canPush) {
                val success = tryPushItemsAway(
                    page = page,
                    targetX = x,
                    targetY = y,
                    spanX = spanX,
                    spanY = spanY,
                    draggedItemId = -1,
                    workspaceItems = workspaceItems,
                    workspaceRepository = workspaceRepository,
                    cols = cols,
                    rows = rows
                )
                if (success) {
                    return Pair(x, y)
                }
            }
        }
    }
    return null
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isOverlayVisible: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: (String?) -> Unit,
    onOpenVault: () -> Unit,
    onOpenOrbSpace: () -> Unit,
    onOpenOrbSearch: () -> Unit,
    modifier: Modifier = Modifier,
    appRepository: AppRepository = koinInject(),
    workspaceRepository: WorkspaceRepository = koinInject(),
    widgetHostManager: WidgetHostManager = koinInject(),
    settingsRepository: SettingsRepository = koinInject(),
    authManager: com.oorbitt.launcher.security.AuthManager = koinInject()
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val allApps by appRepository.allApps.collectAsState(initial = emptyList())
    val appsByPackage = remember(allApps) {
        allApps.associateBy { it.packageName }
    }
    val workspaceItems by workspaceRepository.getItemsForProfile(0).collectAsState(initial = emptyList())
    val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
    val iconShape = remember(
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
    val gestureRepository: GestureRepository = koinInject()
    val gestureActions by gestureRepository.getAllGestureActions().collectAsState(initial = emptyList())
    
    val gestureHandler = remember(context, onOpenDrawer, onOpenSearch, onOpenSettings, onOpenVault, onOpenOrbSpace, onOpenOrbSearch) {
        GestureHandler(
            context = context,
            onOpenDrawer = onOpenDrawer,
            onOpenSearch = onOpenSearch,
            onOpenSettings = { onOpenSettings(null) },
            onOpenVault = onOpenVault,
            onOpenOrbSpace = onOpenOrbSpace,
            onOpenOrbSearch = onOpenOrbSearch
        )
    }
    
    val triggerGesture: (GestureType) -> Unit = remember(gestureActions, gestureHandler) {
        { type ->
            val action = gestureActions.firstOrNull { it.gestureType == type }
            if (action != null) {
                gestureHandler.execute(action)
            }
        }
    }

    val dragManager: com.oorbitt.launcher.ui.util.DragManager = koinInject()
    var activeContextMenuWorkspaceItem by remember { mutableStateOf<Pair<WorkspaceItem, Offset>?>(null) }
    var customizeTargetApp by remember { mutableStateOf<AppInfo?>(null) }
    val pagerState = rememberPagerState(pageCount = { 3 })

    var showWidgetPicker by remember { mutableStateOf(false) }
    var showWorkspaceMenu by remember { mutableStateOf(false) }
    var pendingWidgetInfo by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }

    var mindfulLockAppPkg by remember { mutableStateOf<String?>(null) }
    var mindfulLockAppLabel by remember { mutableStateOf("") }
    var mindfulLockAppAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val onAppLaunch = remember(context, authManager, coroutineScope, settingsState.appLockTimeoutMs, settingsState.appLockBiometricOnly) {
        { app: AppInfo ->
            val launchAction = { launchApp(context, app.packageName) }
            if (com.oorbitt.launcher.security.MindfulLockManager.isAppLocked(context, app.packageName)) {
                mindfulLockAppPkg = app.packageName
                mindfulLockAppLabel = app.displayLabel
                mindfulLockAppAction = launchAction
            } else if (app.isLocked) {
                val now = System.currentTimeMillis()
                val lastAuth = authManager.lastAuthenticatedTime
                val isBypass = lastAuth > 0L && (now - lastAuth < settingsState.appLockTimeoutMs)
                if (isBypass) {
                    launchAction()
                } else {
                    coroutineScope.launch {
                        val success = authManager.authenticate(
                            title = app.displayLabel,
                            subtitle = "Biometric validation required",
                            allowDeviceCredential = !settingsState.appLockBiometricOnly
                        )
                        if (success) {
                            launchAction()
                        }
                    }
                }
            } else {
                launchAction()
            }
            Unit
        }
    }

    val onDockAppClick = remember(context, authManager, coroutineScope, settingsState.appLockTimeoutMs, settingsState.appLockBiometricOnly) {
        { app: AppInfo ->
            val launchAction = { launchApp(context, app.packageName) }
            if (com.oorbitt.launcher.security.MindfulLockManager.isAppLocked(context, app.packageName)) {
                mindfulLockAppPkg = app.packageName
                mindfulLockAppLabel = app.displayLabel
                mindfulLockAppAction = launchAction
            } else if (app.isLocked) {
                val now = System.currentTimeMillis()
                val lastAuth = authManager.lastAuthenticatedTime
                val isBypass = lastAuth > 0L && (now - lastAuth < settingsState.appLockTimeoutMs)
                if (isBypass) {
                    launchAction()
                } else {
                    coroutineScope.launch {
                        val success = authManager.authenticate(
                            title = app.displayLabel,
                            subtitle = "Biometric validation required",
                            allowDeviceCredential = !settingsState.appLockBiometricOnly
                        )
                        if (success) {
                            launchAction()
                        }
                    }
                }
            } else {
                launchAction()
            }
            Unit
        }
    }


    val onDockAppLongClick = remember(haptic) {
        { app: AppInfo ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        widgetHostManager.startListening()
        onDispose {
            widgetHostManager.stopListening()
        }
    }

    val configureWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingWidgetId
        val info = pendingWidgetInfo
        if (result.resultCode == android.app.Activity.RESULT_OK && widgetId != null && info != null) {
            coroutineScope.launch {
                val spanX = Math.max(1, Math.round((info.minWidth - 40) / 76f))
                val spanY = Math.max(1, Math.round((info.minHeight - 40) / 76f))
                val slot = findSlotWithPush(pagerState.currentPage, spanX, spanY, workspaceItems, workspaceRepository)
                if (slot != null) {
                    workspaceRepository.addItem(
                        WorkspaceItem(
                            itemType = ItemType.WIDGET,
                            containerType = ContainerType.WORKSPACE,
                            screenIndex = pagerState.currentPage,
                            cellX = slot.first,
                            cellY = slot.second,
                            spanX = spanX,
                            spanY = spanY,
                            appWidgetId = widgetId,
                            profileId = 0
                        )
                    )
                } else {
                    widgetHostManager.deleteAppWidgetId(widgetId)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Not enough space on this screen", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            if (widgetId != null) {
                widgetHostManager.deleteAppWidgetId(widgetId)
            }
        }
        pendingWidgetId = null
        pendingWidgetInfo = null
    }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingWidgetId
        val info = pendingWidgetInfo
        if (result.resultCode == android.app.Activity.RESULT_OK && widgetId != null && info != null) {
            if (info.configure != null) {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                configureWidgetLauncher.launch(intent)
            } else {
                coroutineScope.launch {
                    val spanX = Math.max(1, Math.round((info.minWidth - 40) / 76f))
                    val spanY = Math.max(1, Math.round((info.minHeight - 40) / 76f))
                    val slot = findSlotWithPush(pagerState.currentPage, spanX, spanY, workspaceItems, workspaceRepository)
                    if (slot != null) {
                        workspaceRepository.addItem(
                            WorkspaceItem(
                                itemType = ItemType.WIDGET,
                                containerType = ContainerType.WORKSPACE,
                                screenIndex = pagerState.currentPage,
                                cellX = slot.first,
                                cellY = slot.second,
                                spanX = spanX,
                                spanY = spanY,
                                appWidgetId = widgetId,
                                profileId = 0
                            )
                        )
                    } else {
                        widgetHostManager.deleteAppWidgetId(widgetId)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Not enough space on this screen", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                pendingWidgetId = null
                pendingWidgetInfo = null
            }
        } else {
            if (widgetId != null) {
                widgetHostManager.deleteAppWidgetId(widgetId)
            }
            pendingWidgetId = null
            pendingWidgetInfo = null
        }
    }

    val onWidgetSelected: (AppWidgetProviderInfo) -> Unit = { info ->
        val appWidgetId = widgetHostManager.allocateAppWidgetId()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.profile, info.provider, null)
        } else {
            @Suppress("DEPRECATION")
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
        }
        
        if (hasPermission) {
            if (info.configure != null) {
                pendingWidgetId = appWidgetId
                pendingWidgetInfo = info
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                configureWidgetLauncher.launch(intent)
            } else {
                coroutineScope.launch {
                    val spanX = Math.max(1, Math.round((info.minWidth - 40) / 76f))
                    val spanY = Math.max(1, Math.round((info.minHeight - 40) / 76f))
                    val slot = findSlotWithPush(pagerState.currentPage, spanX, spanY, workspaceItems, workspaceRepository)
                    if (slot != null) {
                        workspaceRepository.addItem(
                            WorkspaceItem(
                                itemType = ItemType.WIDGET,
                                containerType = ContainerType.WORKSPACE,
                                screenIndex = pagerState.currentPage,
                                cellX = slot.first,
                                cellY = slot.second,
                                spanX = spanX,
                                spanY = spanY,
                                appWidgetId = appWidgetId,
                                profileId = 0
                            )
                        )
                    } else {
                        widgetHostManager.deleteAppWidgetId(appWidgetId)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Not enough space on this screen", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            pendingWidgetId = appWidgetId
            pendingWidgetInfo = info
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
                }
            }
            bindWidgetLauncher.launch(intent)
        }
    }

    val dockApps = remember(allApps, settingsState.dockSlots) {
        val maxSlots = settingsState.dockSlots
        val defaultApps = listOf(
            "com.android.dialer", "com.google.android.dialer",
            "com.android.messaging", "com.google.android.apps.messaging",
            "com.android.chrome", "com.google.android.gm",
            "com.android.camera2", "com.google.android.GoogleCamera"
        )
        val selected = mutableListOf<AppInfo>()
        for (pkg in defaultApps) {
            val app = appsByPackage[pkg]?.takeIf { !it.isHidden }
            if (app != null) {
                selected.add(app)
                if (selected.size >= maxSlots) break
            }
        }
        if (selected.size < maxSlots) {
            for (app in allApps) {
                if (!app.isHidden && selected.none { it.packageName == app.packageName }) {
                    selected.add(app)
                    if (selected.size >= maxSlots) break
                }
            }
        }
        selected
    }

    // Pre-populate home screen workspace with apps if empty
    var hasPopulated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(allApps, workspaceItems) {
        if (!hasPopulated && allApps.isNotEmpty() && workspaceItems.isEmpty()) {
            hasPopulated = true
            val defaultPackageList = listOf(
                "com.android.chrome",
                "com.google.android.youtube",
                "com.google.android.apps.maps",
                "com.google.android.apps.photos",
                "com.android.vending",
                "com.google.android.gm"
            )
            val added = mutableSetOf<String>()
            var count = 0
            
            // Try to add default apps first
            for (pkg in defaultPackageList) {
                val app = appsByPackage[pkg]?.takeIf { !it.isHidden }
                if (app != null) {
                    val cellX = count % 5
                    val cellY = count / 5
                    workspaceRepository.addItem(
                        WorkspaceItem(
                            itemType = ItemType.APP,
                            containerType = ContainerType.WORKSPACE,
                            screenIndex = 0,
                            cellX = cellX,
                            cellY = cellY,
                            packageName = pkg,
                            profileId = 0
                        )
                    )
                    added.add(pkg)
                    count++
                }
            }
            
            // If we added less than 5 apps, add some other installed apps
            if (count < 5) {
                for (app in allApps) {
                    if (!app.isHidden && app.packageName !in added) {
                        val cellX = count % 5
                        val cellY = count / 5
                        workspaceRepository.addItem(
                            WorkspaceItem(
                                itemType = ItemType.APP,
                                containerType = ContainerType.WORKSPACE,
                                screenIndex = 0,
                                cellX = cellX,
                                cellY = cellY,
                                packageName = app.packageName,
                                profileId = 0
                            )
                        )
                        added.add(app.packageName)
                        count++
                        if (count >= 10) break
                    }
                }
            }
        }
    }

    var homeScreenPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                homeScreenPositionInWindow = coords.positionInWindow()
            }
            .detectLauncherGestures(
                onSwipeUp = { triggerGesture(GestureType.SWIPE_UP) },
                onSwipeDown = { triggerGesture(GestureType.SWIPE_DOWN) },
                onDoubleTap = { triggerGesture(GestureType.DOUBLE_TAP) },
                onPinchIn = { triggerGesture(GestureType.PINCH_IN) },
                onPinchOut = { triggerGesture(GestureType.PINCH_OUT) },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showWorkspaceMenu = true
                },
                isSystemDragging = { dragManager.activeWorkspaceDragItem != null || dragManager.activeDragItem != null }
            )
    ) {
        if (settingsState.wallpaperDimPercent > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settingsState.wallpaperDimPercent / 100f))
            )
        }

        val contentAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isOverlayVisible) 0f else 1f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
            label = "homeContentAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    // slide down slightly when fading out
                    translationY = (1f - contentAlpha) * 16.dp.toPx()
                }
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Home screen workspace pages
            HorizontalPager(
                state = pagerState,

                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = dragManager.activeWorkspaceDragItem == null && dragManager.activeDragItem == null
            ) { page ->
                val pageItems = remember(workspaceItems, page) {
                    workspaceItems.filter { it.screenIndex == page && it.containerType == ContainerType.WORKSPACE }
                }
                
                var gridPositionInWindow by remember { mutableStateOf(Offset.Zero) }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (settingsState.searchBarStyle != com.oorbitt.launcher.model.SearchBarStyle.HIDDEN && settingsState.searchBarPosition == "TOP") 80.dp else 40.dp, bottom = 180.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    if (page == 0 && settingsState.showScrollTrackerWidget) {
                        com.oorbitt.launcher.home.widget.ScrollTrackerWidget(
                            dailyLimit = settingsState.dailyScrollLimit,
                            onClick = { onOpenSettings("Wellness & Stats") },
                            activeIconPack = settingsState.activeIconPack,
                            bgOpacity = settingsState.wellnessWidgetBgOpacity,
                            heightDp = settingsState.wellnessWidgetHeightDp,
                            cornerRadiusDp = settingsState.wellnessWidgetCornerRadiusDp,
                            accentColorHex = settingsState.wellnessWidgetAccentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .onGloballyPositioned { coords ->
                                gridPositionInWindow = coords.positionInWindow()
                            }
                    ) {
                    val cols = settingsState.gridColumns
                    val rows = settingsState.gridRows
                    val cellWidth = maxWidth / cols
                    val cellHeight = maxHeight / rows
                    val density = LocalDensity.current
                    val cellWidthPx = with(density) { cellWidth.toPx() }
                    val cellHeightPx = with(density) { cellHeight.toPx() }

                    // Calculate active cell hover
                    val hoveredCell = remember(dragManager.itemPosition, gridPositionInWindow, cellWidthPx, cellHeightPx, cols, rows, settingsState.freeFormPlacement) {
                        val activeItem = dragManager.activeWorkspaceDragItem
                        val activeDrawerApp = dragManager.activeDragItem
                        if (activeItem != null || activeDrawerApp != null) {
                            val gridWidthPx = cellWidthPx * cols
                            val gridHeightPx = cellHeightPx * rows
                            
                            val itemWidthPx = if (activeItem != null) {
                                if (settingsState.freeFormPlacement) {
                                    val scale = if (activeItem.spanX < 5) 1.0f else activeItem.spanX / 10f
                                    cellWidthPx * scale
                                } else {
                                    activeItem.spanX * cellWidthPx
                                }
                            } else cellWidthPx
                            
                            val itemHeightPx = if (activeItem != null) {
                                if (settingsState.freeFormPlacement) {
                                    val scale = if (activeItem.spanX < 5) 1.0f else activeItem.spanX / 10f
                                    cellHeightPx * scale
                                } else {
                                    activeItem.spanY * cellHeightPx
                                }
                            } else cellHeightPx

                            val relX = dragManager.itemPosition.x - gridPositionInWindow.x
                            val relY = dragManager.itemPosition.y - gridPositionInWindow.y

                            if (settingsState.freeFormPlacement) {
                                val pctX = (relX / gridWidthPx).coerceIn(0f, 1f)
                                val pctY = (relY / gridHeightPx).coerceIn(0f, 1f)
                                val cx = (pctX * 1000).toInt()
                                val cy = (pctY * 1000).toInt()
                                Pair(cx, cy)
                            } else {
                                val centerX = dragManager.itemPosition.x + itemWidthPx / 2
                                val centerY = dragManager.itemPosition.y + itemHeightPx / 2
                                val rX = centerX - gridPositionInWindow.x
                                val rY = centerY - gridPositionInWindow.y
                                val cx = (rX / cellWidthPx).toInt().coerceIn(0, cols - 1)
                                val cy = (rY / cellHeightPx).toInt().coerceIn(0, rows - 1)
                                Pair(cx, cy)
                            }
                        } else {
                            null
                        }
                    }

                    LaunchedEffect(hoveredCell, pagerState.currentPage) {
                        if (pagerState.currentPage == page && (dragManager.activeWorkspaceDragItem != null || dragManager.activeDragItem != null)) {
                            dragManager.dragCurrentCell = hoveredCell
                            dragManager.currentDragPage = page
                        }
                    }

                    // Render grid blueprint if dragging
                    if (dragManager.activeWorkspaceDragItem != null || dragManager.activeDragItem != null) {
                        if (!settingsState.freeFormPlacement) {
                            val activeItem = dragManager.activeWorkspaceDragItem
                            val spanX = activeItem?.spanX ?: 1
                            val spanY = activeItem?.spanY ?: 1
                            val currentCell = dragManager.dragCurrentCell
                            
                            Column(modifier = Modifier.fillMaxSize()) {
                                for (r in 0 until rows) {
                                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        for (c in 0 until cols) {
                                            val isHovered = currentCell != null &&
                                                            c >= currentCell.first && c < currentCell.first + spanX &&
                                                            r >= currentCell.second && r < currentCell.second + spanY
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(4.dp)
                                                    .background(
                                                        color = if (isHovered) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                            )
                        }
                    }

                    pageItems.forEach { item ->
                        var itemCoordinates by remember { mutableStateOf(Offset.Zero) }
                        var dragAccumulated by remember { mutableStateOf(Offset.Zero) }
                        var isDraggingMode by remember { mutableStateOf(false) }
                        var longPressOccurred by remember { mutableStateOf(false) }
                        var initialTouchOffset by remember { mutableStateOf(Offset.Zero) }
                        val currentItemCoordinates = rememberUpdatedState(itemCoordinates)

                        val dragModifier = Modifier
                            .onGloballyPositioned { coords ->
                                itemCoordinates = coords.positionInWindow()
                            }
                            .pointerInput(item) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.any { it.changedToDown() }) {
                                            longPressOccurred = false
                                        }
                                    }
                                }
                            }
                            .pointerInput(item, cellWidthPx, cellHeightPx, cols, rows) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        dragAccumulated = Offset.Zero
                                        isDraggingMode = false
                                        longPressOccurred = true
                                        initialTouchOffset = offset
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeContextMenuWorkspaceItem = Pair(item, currentItemCoordinates.value + offset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAccumulated += dragAmount
                                        if (!isDraggingMode && dragAccumulated.getDistance() > 15f) {
                                            isDraggingMode = true
                                            activeContextMenuWorkspaceItem = null
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dragManager.startWorkspaceDrag(item, currentItemCoordinates.value, initialTouchOffset)
                                        }
                                        if (isDraggingMode) {
                                            dragManager.updateDrag(dragAmount)
                                        }
                                    },
                                    onDragEnd = {
                                        if (isDraggingMode) {
                                            val targetCell = dragManager.dragCurrentCell
                                            val targetPage = dragManager.currentDragPage
                                            if (targetCell != null && targetPage != null) {
                                                coroutineScope.launch {
                                                    val success = tryPushItemsAway(
                                                        page = targetPage,
                                                        targetX = targetCell.first,
                                                        targetY = targetCell.second,
                                                        spanX = item.spanX,
                                                        spanY = item.spanY,
                                                        draggedItemId = item.id,
                                                        workspaceItems = workspaceItems,
                                                        workspaceRepository = workspaceRepository,
                                                        cols = settingsState.gridColumns,
                                                        rows = settingsState.gridRows
                                                    )
                                                    if (success) {
                                                        workspaceRepository.updateItem(item.copy(
                                                            screenIndex = targetPage,
                                                            cellX = targetCell.first,
                                                            cellY = targetCell.second
                                                        ))
                                                    } else {
                                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                            android.widget.Toast.makeText(context, "Not enough space on this screen", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                            dragManager.endDrag()
                                        }
                                        isDraggingMode = false
                                    },
                                    onDragCancel = {
                                        if (isDraggingMode) {
                                            dragManager.endDrag()
                                        }
                                        isDraggingMode = false
                                    }
                                )
                            }

                        when (item.itemType) {
                            ItemType.APP -> {
                                val app = appsByPackage[item.packageName]?.takeIf { !it.isHidden }
                                if (app != null) {
                                    val isDragging = dragManager.activeWorkspaceDragItem?.id == item.id
                                    val scale = if (settingsState.freeFormPlacement) {
                                        if (item.spanX < 5) 1.0f else item.spanX / 10f
                                    } else 1.0f

                                    Box(
                                        modifier = Modifier
                                            .offset(
                                                x = if (settingsState.freeFormPlacement) {
                                                    maxWidth * (item.cellX / 1000f)
                                                } else {
                                                    cellWidth * item.cellX
                                                },
                                                y = if (settingsState.freeFormPlacement) {
                                                    maxHeight * (item.cellY / 1000f)
                                                } else {
                                                    cellHeight * item.cellY
                                                }
                                            )
                                            .graphicsLayer {
                                                if (isDragging) {
                                                    alpha = 0.3f
                                                }
                                            }
                                            .zIndex(if (isDragging) 10f else 1f)
                                            .then(dragModifier)
                                            .clickable {
                                                if (dragManager.activeWorkspaceDragItem == null && dragManager.activeDragItem == null && !longPressOccurred) {
                                                    onAppLaunch(app)
                                                }
                                            }
                                            .size(
                                                width = if (settingsState.freeFormPlacement) {
                                                    cellWidth * scale
                                                } else {
                                                    cellWidth * item.spanX
                                                },
                                                height = if (settingsState.freeFormPlacement) {
                                                    cellHeight * scale
                                                } else {
                                                    cellHeight * item.spanY
                                                }
                                            )
                                    ) {
                                        WorkspaceAppItem(
                                            app = app,
                                            item = item,
                                            activeIconPack = settingsState.activeIconPack,
                                            iconSizeDp = settingsState.desktopIconSizeDp,
                                            labelSizeSp = settingsState.labelSizeSp,
                                            showLabels = settingsState.showDesktopLabels,
                                            iconShape = iconShape,
                                            onLaunch = onAppLaunch,
                                            onLongClick = {}
                                        )
                                    }
                                }
                            }
                            ItemType.WIDGET -> {
                                val isDragging = dragManager.activeWorkspaceDragItem?.id == item.id
                                val scale = if (settingsState.freeFormPlacement) {
                                    if (item.spanX < 5) 1.0f else item.spanX / 10f
                                } else 1.0f

                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = if (settingsState.freeFormPlacement) {
                                                maxWidth * (item.cellX / 1000f)
                                            } else {
                                                cellWidth * item.cellX
                                            },
                                            y = if (settingsState.freeFormPlacement) {
                                                maxHeight * (item.cellY / 1000f)
                                            } else {
                                                cellHeight * item.cellY
                                            }
                                        )
                                        .graphicsLayer {
                                            if (isDragging) {
                                                alpha = 0.3f
                                            }
                                        }
                                        .zIndex(if (isDragging) 10f else 1f)
                                        .then(dragModifier)
                                        .size(
                                            width = if (settingsState.freeFormPlacement) {
                                                cellWidth * item.spanX * scale
                                            } else {
                                                cellWidth * item.spanX
                                            },
                                            height = if (settingsState.freeFormPlacement) {
                                                cellHeight * item.spanY * scale
                                            } else {
                                                cellHeight * item.spanY
                                            }
                                        )
                                ) {
                                    WorkspaceWidgetItem(
                                        item = item,
                                        cellWidth = cellWidth,
                                        cellHeight = cellHeight,
                                        onLongClick = {}
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
                }
            }

            // Page indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            // Search bar
            if (settingsState.searchBarStyle != com.oorbitt.launcher.model.SearchBarStyle.HIDDEN) {
                val align = if (settingsState.searchBarPosition == "TOP") Alignment.TopCenter else Alignment.BottomCenter
                val padBottom = if (settingsState.searchBarPosition == "TOP") 0.dp else 90.dp
                val padTop = if (settingsState.searchBarPosition == "TOP") 16.dp else 0.dp
                Surface(
                    modifier = Modifier
                        .align(align)
                        .padding(top = padTop, bottom = padBottom)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onOpenSearch() },
                    shape = RoundedCornerShape(settingsState.searchBarCornerRadiusDp.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search apps, contacts, settings…",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Dock
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.take(settingsState.dockSlots).forEach { app ->
                    DockAppItem(
                        app = app,
                        activeIconPack = settingsState.activeIconPack,
                        iconSizeDp = settingsState.dockIconSizeDp,
                        labelSizeSp = settingsState.labelSizeSp,
                        showLabels = settingsState.showDockLabels,
                        iconShape = iconShape,
                        onClick = onDockAppClick,
                        onLongClick = onDockAppLongClick
                    )
                }
            }
        }
    }

    // Pager page auto-scroll logic during drag
    val screenWidthPx = context.resources.displayMetrics.widthPixels
    val edgeThresholdPx = with(density) { 60.dp.toPx() }
    
    LaunchedEffect(dragManager.activeWorkspaceDragItem != null || dragManager.activeDragItem != null) {
        val isDragging = dragManager.activeWorkspaceDragItem != null || dragManager.activeDragItem != null
        if (isDragging) {
            while (true) {
                val touchX = dragManager.touchPosition.x
                if (touchX < edgeThresholdPx) {
                    if (pagerState.currentPage > 0) {
                        kotlinx.coroutines.delay(800)
                        if (dragManager.touchPosition.x < edgeThresholdPx) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                } else if (touchX > screenWidthPx - edgeThresholdPx) {
                    if (pagerState.currentPage < pagerState.pageCount - 1) {
                        kotlinx.coroutines.delay(800)
                        if (dragManager.touchPosition.x > screenWidthPx - edgeThresholdPx) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    // Global Drag Shadow Overlay
    val activeDragItem = dragManager.activeDragItem
    val activeWorkspaceDragItem = dragManager.activeWorkspaceDragItem
    if (activeDragItem != null || activeWorkspaceDragItem != null) {
        val relativeOffset = dragManager.itemPosition - homeScreenPositionInWindow
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = relativeOffset.x
                    translationY = relativeOffset.y
                }
                .zIndex(100f)
        ) {
            if (activeDragItem != null) {
                val icon = rememberAppIcon(
                    context = context,
                    packageName = activeDragItem.packageName,
                    activityName = activeDragItem.activityName,
                    activeIconPack = settingsState.activeIconPack,
                    customIconUri = activeDragItem.customIconUri
                )
                icon?.let { img ->
                    Image(
                        bitmap = img,
                        contentDescription = null,
                        modifier = Modifier
                            .size(settingsState.desktopIconSizeDp.dp)
                            .clip(iconShape)
                            .graphicsLayer {
                                scaleX = 1.15f
                                scaleY = 1.15f
                                alpha = 0.8f
                            }
                    )
                }
            } else if (activeWorkspaceDragItem != null) {
                if (activeWorkspaceDragItem.itemType == ItemType.APP) {
                    val app = appsByPackage[activeWorkspaceDragItem.packageName]
                    val icon = rememberAppIcon(
                        context = context,
                        packageName = activeWorkspaceDragItem.packageName ?: "",
                        activityName = activeWorkspaceDragItem.activityName,
                        activeIconPack = settingsState.activeIconPack,
                        customIconUri = app?.customIconUri
                    )
                    icon?.let { img ->
                        Image(
                            bitmap = img,
                            contentDescription = null,
                            modifier = Modifier
                                .size(settingsState.desktopIconSizeDp.dp)
                                .clip(iconShape)
                                .graphicsLayer {
                                    scaleX = 1.15f
                                    scaleY = 1.15f
                                    alpha = 0.8f
                                }
                        )
                    }
                } else if (activeWorkspaceDragItem.itemType == ItemType.WIDGET) {
                    val cols = settingsState.gridColumns
                    val rows = settingsState.gridRows
                    val screenWidth = (context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density).dp
                    val screenHeight = (context.resources.displayMetrics.heightPixels / context.resources.displayMetrics.density).dp
                    val topPadding = if (settingsState.searchBarStyle != com.oorbitt.launcher.model.SearchBarStyle.HIDDEN && settingsState.searchBarPosition == "TOP") 80.dp else 40.dp
                    val gridWidth = screenWidth - 32.dp
                    val gridHeight = screenHeight - (topPadding + 180.dp)
                    val cellWidth = gridWidth / cols
                    val cellHeight = gridHeight / rows
                    
                    Box(
                        modifier = Modifier
                            .size(
                                width = cellWidth * activeWorkspaceDragItem.spanX,
                                height = cellHeight * activeWorkspaceDragItem.spanY
                            )
                            .graphicsLayer {
                                scaleX = 1.05f
                                scaleY = 1.05f
                                alpha = 0.8f
                            }
                    ) {
                        WorkspaceWidgetItem(
                            item = activeWorkspaceDragItem,
                            cellWidth = cellWidth,
                            cellHeight = cellHeight,
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }

    // Workspace Item Options Popup Context Menu
    if (activeContextMenuWorkspaceItem != null) {
        val (item, offset) = activeContextMenuWorkspaceItem!!
        val title = if (item.itemType == ItemType.APP) {
            appsByPackage[item.packageName]?.displayLabel ?: "App"
        } else {
            item.appWidgetId?.let { id ->
                widgetHostManager.getAppWidgetInfo(id)?.loadLabel(context.packageManager)
            } ?: "Widget"
        }
        
        val contextMenuOptions = remember(item, allApps, settingsState.freeFormPlacement) {
            val list = mutableListOf<Pair<String, () -> Unit>>()
            if (item.itemType == ItemType.APP) {
                val app = appsByPackage[item.packageName]
                if (app != null) {
                    list.add("Customize" to {
                        customizeTargetApp = app
                    })
                }
                list.add("Remove" to {
                    coroutineScope.launch {
                        workspaceRepository.removeItem(item.id)
                    }
                })
                if (settingsState.freeFormPlacement) {
                    val currentScale = if (item.spanX < 5) 10 else item.spanX
                    list.add("Scale Size +" to {
                        if (currentScale < 25) {
                            coroutineScope.launch {
                                workspaceRepository.updateItem(item.copy(spanX = currentScale + 1))
                            }
                        }
                    })
                    list.add("Scale Size -" to {
                        if (currentScale > 5) {
                            coroutineScope.launch {
                                workspaceRepository.updateItem(item.copy(spanX = currentScale - 1))
                            }
                        }
                    })
                }
                list.add("App Info" to {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${item.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Could not open app info", android.widget.Toast.LENGTH_SHORT).show()
                    }
                })
                list.add("Uninstall" to {
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = android.net.Uri.parse("package:${item.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Could not uninstall app", android.widget.Toast.LENGTH_SHORT).show()
                    }
                })
            } else if (item.itemType == ItemType.WIDGET) {
                list.add("Remove" to {
                    coroutineScope.launch {
                        workspaceRepository.removeItem(item.id)
                        item.appWidgetId?.let { id ->
                            widgetHostManager.deleteAppWidgetId(id)
                        }
                    }
                })
                if (settingsState.freeFormPlacement) {
                    val currentScale = if (item.spanX < 5) 10 else item.spanX
                    list.add("Scale Widget +" to {
                        if (currentScale < 25) {
                            coroutineScope.launch {
                                workspaceRepository.updateItem(item.copy(spanX = currentScale + 1))
                            }
                        }
                    })
                    list.add("Scale Widget -" to {
                        if (currentScale > 5) {
                            coroutineScope.launch {
                                workspaceRepository.updateItem(item.copy(spanX = currentScale - 1))
                            }
                        }
                    })
                } else {
                    list.add("Resize Width +" to {
                        if (item.spanX < 5) {
                            coroutineScope.launch {
                                workspaceRepository.updateItem(item.copy(spanX = item.spanX + 1))
                            }
                        }
                    })
                    list.add("Resize Width -" to {
                        if (item.spanX > 1) {
                            coroutineScope.launch {
                                workspaceRepository.updateItem(item.copy(spanX = item.spanX - 1))
                            }
                        }
                    })
                    list.add("Resize Height +" to {
                        if (item.spanY < 5) {
                            coroutineScope.launch {
                                workspaceRepository.updateItem(item.copy(spanY = item.spanY + 1))
                            }
                        }
                    })
                    list.add("Resize Height -" to {
                        if (item.spanY > 1) {
                            coroutineScope.launch {
                                workspaceRepository.updateItem(item.copy(spanY = item.spanY - 1))
                            }
                        }
                    })
                }
            }
            list
        }
        
        FloatingContextMenu(
            x = offset.x.coerceIn(16f, (context.resources.displayMetrics.widthPixels - 500).toFloat()),
            y = offset.y.coerceIn(16f, (context.resources.displayMetrics.heightPixels - 800).toFloat()),
            title = title,
            options = contextMenuOptions,
            onDismiss = { activeContextMenuWorkspaceItem = null }
        )
    }

    // Workspace Long Press Options Menu
    if (showWorkspaceMenu) {
        ModalBottomSheet(
            onDismissRequest = { showWorkspaceMenu = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Workspace Options",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
                HorizontalDivider()
                
                ListItem(
                    headlineContent = { Text("Add Widget") },
                    supportingContent = { Text("Place a widget on the home screen") },
                    modifier = Modifier.clickable {
                        showWorkspaceMenu = false
                        showWidgetPicker = true
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Launcher Settings") },
                    supportingContent = { Text("Configure theme, grid size, hidden apps, etc.") },
                    modifier = Modifier.clickable {
                        showWorkspaceMenu = false
                        onOpenSettings(null)
                    }
                )
                ListItem(
                    headlineContent = { Text("OrbSpace") },
                    supportingContent = { Text("Digital hygiene and scratchpad") },
                    modifier = Modifier.clickable {
                        showWorkspaceMenu = false
                        onOpenOrbSpace()
                    }
                )
            }
        }
    }

    if (showWidgetPicker) {
        WidgetPickerSheet(
            onDismiss = { showWidgetPicker = false },
            onWidgetSelected = { info ->
                showWidgetPicker = false
                onWidgetSelected(info)
            }
        )
    }

    customizeTargetApp?.let { app ->
        CustomizeAppDialog(
            app = app,
            activeIconPack = settingsState.activeIconPack,
            iconShape = iconShape,
            onDismiss = { customizeTargetApp = null },
            onSave = { customLabel, customIconUri ->
                customizeTargetApp = null
                coroutineScope.launch {
                    appRepository.saveIconOverride(app.componentKey, customLabel, customIconUri)
                }
            }
        )
    }

    mindfulLockAppPkg?.let { pkg ->
        val remainingMs = remember(pkg) { com.oorbitt.launcher.security.MindfulLockManager.getRemainingLockTime(context, pkg) }
        val remainingStr = remember(remainingMs) {
            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(remainingMs)
            val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

        AlertDialog(
            onDismissRequest = { mindfulLockAppPkg = null },
            title = {
                Text(
                    text = "Focus Lock Active",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "$mindfulLockAppLabel is locked to help you focus. Remaining lock time: $remainingStr.\n\nNeed to access it for a quick emergency?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            com.oorbitt.launcher.security.MindfulLockManager.triggerEmergencyUnlock(context, pkg, 5 * 60 * 1000L)
                            mindfulLockAppPkg = null
                            mindfulLockAppAction?.invoke()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFE5A93C)
                        )
                    ) {
                        Text("Unlock 5m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            com.oorbitt.launcher.security.MindfulLockManager.triggerEmergencyUnlock(context, pkg, 10 * 60 * 1000L)
                            mindfulLockAppPkg = null
                            mindfulLockAppAction?.invoke()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFE5A93C)
                        )
                    ) {
                        Text("Unlock 10m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mindfulLockAppPkg = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Close", fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Orb Button (floating search trigger) ─────────────────────────────────
    // Detect clipboard content for pulse
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    }
    val hasClipboard by remember {
        derivedStateOf {
            clipboardManager?.hasPrimaryClip() == true
        }
    }

    if (settingsState.showOrbButton) {
        OrbButton(
            onClick = onOpenOrbSearch,
            isPulsing = hasClipboard
        )
    }
}

@Composable
private fun WorkspaceWidgetItem(
    item: WorkspaceItem,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    widgetHostManager: WidgetHostManager = koinInject()
) {
    val context = LocalContext.current
    val widgetId = item.appWidgetId ?: return

    val widgetView = remember(widgetId) {
        val info = widgetHostManager.getAppWidgetInfo(widgetId)
        if (info != null) {
            widgetHostManager.createView(context, widgetId, info)
        } else {
            null
        }
    }

    LaunchedEffect(item.spanX, item.spanY, cellWidth, cellHeight) {
        val widthDp = (item.spanX * cellWidth.value).toInt()
        val heightDp = (item.spanY * cellHeight.value).toInt()
        widgetView?.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
    }

    if (widgetView != null) {
        AndroidView(
            factory = {
                widgetView.apply {
                    setPadding(0, 0, 0, 0)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Widget Unavailable",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun WorkspaceAppItem(
    app: AppInfo,
    item: WorkspaceItem,
    activeIconPack: String?,
    iconSizeDp: Int,
    labelSizeSp: Int,
    showLabels: Boolean,
    iconShape: androidx.compose.ui.graphics.Shape,
    onLaunch: (AppInfo) -> Unit,
    onLongClick: (WorkspaceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val icon = rememberAppIcon(context, app.packageName, app.activityName, activeIconPack, app.customIconUri)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon?.let {
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    bitmap = it,
                    contentDescription = app.displayLabel,
                    modifier = Modifier
                        .size(iconSizeDp.dp)
                        .clip(iconShape)
                )
                if (app.isWorkProfile) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = "Work Profile App",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        } ?: Box(
            modifier = Modifier
                .size(iconSizeDp.dp)
                .clip(iconShape)
                .background(Color.Gray.copy(alpha = 0.5f))
        )
        if (showLabels) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.displayLabel,
                color = Color.White,
                fontSize = labelSizeSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppItem(
    app: AppInfo,
    activeIconPack: String?,
    iconSizeDp: Int,
    labelSizeSp: Int,
    showLabels: Boolean,
    iconShape: androidx.compose.ui.graphics.Shape,
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = { onClick(app) },
                onLongClick = { onLongClick(app) }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        val icon = rememberAppIcon(context, app.packageName, app.activityName, activeIconPack, app.customIconUri)
        icon?.let {
            Image(
                bitmap = it,
                contentDescription = app.displayLabel,
                modifier = Modifier
                    .size(iconSizeDp.dp)
                    .clip(iconShape)
            )
        }
        if (showLabels) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.displayLabel,
                color = Color.White,
                fontSize = labelSizeSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

