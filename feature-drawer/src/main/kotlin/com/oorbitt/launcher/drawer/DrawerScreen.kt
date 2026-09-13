package com.oorbitt.launcher.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.oorbitt.launcher.search.SearchAggregator
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import kotlinx.coroutines.delay

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.core.graphics.drawable.toBitmap
import com.oorbitt.launcher.data.repository.AppRepository
import com.oorbitt.launcher.data.repository.WorkspaceRepository
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.model.AppInfo
import com.oorbitt.launcher.model.WorkspaceItem
import com.oorbitt.launcher.model.ItemType
import com.oorbitt.launcher.model.ContainerType
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.model.DrawerSortMode
import org.koin.compose.koinInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.oorbitt.launcher.ui.util.rememberAppIcon
import com.oorbitt.launcher.ui.util.getIconShape
import com.oorbitt.launcher.ui.util.CustomizeAppDialog
import com.oorbitt.launcher.ui.util.FloatingContextMenu
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridItemSpan

sealed interface DrawerGridItem {
    data class App(val app: AppInfo) : DrawerGridItem
    data class Group(val group: com.oorbitt.launcher.model.DrawerGroup) : DrawerGridItem
    data class Calculator(val expression: String, val result: String) : DrawerGridItem
    data class WebSearch(val query: String, val engineName: String, val url: String) : DrawerGridItem
    data class UniversalResult(val result: SearchResult) : DrawerGridItem
    data class QuickDialer(val query: String) : DrawerGridItem
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DrawerScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    appRepository: AppRepository = koinInject(),
    workspaceRepository: WorkspaceRepository = koinInject(),
    settingsRepository: SettingsRepository = koinInject(),
    authManager: com.oorbitt.launcher.security.AuthManager = koinInject(),
    searchAggregator: SearchAggregator = koinInject()
) {
    val context = LocalContext.current
    val allApps by appRepository.allApps.collectAsState(initial = emptyList())
    val workspaceItems by workspaceRepository.getItemsForProfile(0).collectAsState(initial = emptyList())
    val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
    val dragManager: com.oorbitt.launcher.ui.util.DragManager = koinInject()
    val appsByPackage = remember(allApps) { allApps.associateBy { it.packageName } }
    val sortMode = settingsState.sortMode
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasRequestedPermissions by remember { mutableStateOf(false) }
    var showFirstTimeOrbSpaceHintDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
        if (settingsState.enableOrbSpace && !sp.getBoolean("drawer_orbspace_hint_shown", false)) {
            showFirstTimeOrbSpaceHintDialog = true
            sp.edit().putBoolean("drawer_orbspace_hint_shown", true).apply()
        }
    }

    val gridState = rememberLazyGridState()

    val installedAIApps = remember(context, allApps) {
        val specs = listOf(
            AIAppSpec("ChatGPT", "com.openai.chatgpt", "https://chatgpt.com/?q="),
            AIAppSpec("Gemini", "com.google.android.apps.bard", "https://gemini.google.com/app?q="),
            AIAppSpec("Claude", "com.anthropic.claude", "https://claude.ai/new?q="),
            AIAppSpec("Perplexity", "ai.perplexity.app.android", "https://www.perplexity.ai/?q="),
            AIAppSpec("Copilot", "com.microsoft.copilot", "https://copilot.microsoft.com/?q=")
        )
        specs.filter { spec ->
            try {
                context.packageManager.getPackageInfo(spec.packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    val canScrollBackward by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollForward by remember {
        derivedStateOf {
            gridState.canScrollForward
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            try {
                gridState.scrollToItem(0)
            } catch (_: Exception) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ ->
            if (searchQuery.isNotEmpty()) {
                coroutineScope.launch {
                    searchResults = searchAggregator.search(searchQuery)
                }
            }
        }
    )

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        if (!hasRequestedPermissions && searchQuery.length > 1) {
            hasRequestedPermissions = true
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            permissionLauncher.launch(permissions)
        }

        isSearching = true
        delay(150) // debounce
        searchResults = searchAggregator.search(searchQuery)
        isSearching = false
    }
    var activeContextMenuApp by remember { mutableStateOf<Pair<AppInfo, Offset>?>(null) }
    var customizeTargetApp by remember { mutableStateOf<AppInfo?>(null) }

    var manageGroupsApp by remember { mutableStateOf<AppInfo?>(null) }
    var groupToCustomise by remember { mutableStateOf<com.oorbitt.launcher.model.DrawerGroup?>(null) }
    var selectedGroupForDetail by remember { mutableStateOf<com.oorbitt.launcher.model.DrawerGroup?>(null) }

    val drawerGroups = remember(settingsState.drawerGroupsJson) {
        com.oorbitt.launcher.model.DrawerGroup.fromJsonString(settingsState.drawerGroupsJson)
    }

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

    var mindfulLockAppPkg by remember { mutableStateOf<String?>(null) }
    var mindfulLockAppLabel by remember { mutableStateOf("") }
    var mindfulLockAppAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val onAppClick = remember(context, authManager, coroutineScope, settingsState.appLockTimeoutMs, settingsState.appLockBiometricOnly) {
        { app: AppInfo ->
            val launchAction = { launchApp(context, app.packageName, app.activityName) }
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

    val onDrawerItemDragEnd = remember(workspaceItems, workspaceRepository, context, coroutineScope, onDismiss) {
        {
            val targetCell = dragManager.dragCurrentCell
            val targetPage = dragManager.currentDragPage
            val app = dragManager.activeDragItem
            if (targetCell != null && targetPage != null && app != null) {
                coroutineScope.launch {
                    val targetX = targetCell.first
                    val targetY = targetCell.second
                    val cols = 5
                    val rows = 5
                    
                    val overlapping = workspaceItems.filter {
                        it.screenIndex == targetPage &&
                        it.containerType == ContainerType.WORKSPACE &&
                        targetX < it.cellX + it.spanX &&
                        targetX + 1 > it.cellX &&
                        targetY < it.cellY + it.spanY &&
                        targetY + 1 > it.cellY
                    }
                    
                    var success = true
                    if (overlapping.isNotEmpty()) {
                        val grid = Array(cols) { BooleanArray(rows) }
                        grid[targetX][targetY] = true
                        
                        for (item in workspaceItems) {
                            if (item.screenIndex == targetPage && item.containerType == ContainerType.WORKSPACE && !overlapping.any { it.id == item.id }) {
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
                                success = false
                                break
                            }
                        }
                        
                        if (success) {
                            for (reloc in relocations) {
                                val (item, spot) = reloc
                                workspaceRepository.updateItem(item.copy(cellX = spot.first, cellY = spot.second))
                            }
                        }
                    }
                    
                    if (success) {
                        workspaceRepository.addItem(
                            WorkspaceItem(
                                id = 0,
                                itemType = ItemType.APP,
                                containerType = ContainerType.WORKSPACE,
                                screenIndex = targetPage,
                                cellX = targetX,
                                cellY = targetY,
                                spanX = 1,
                                spanY = 1,
                                packageName = app.packageName,
                                activityName = app.activityName,
                                profileId = 0
                            )
                        )
                    } else {
                        android.widget.Toast.makeText(context, "Not enough space on this screen", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            dragManager.endDrag()
            onDismiss()
        }
    }

    var dragAccumulator by remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (available.y > 0f) {
                    dragAccumulator += available.y
                    if (dragAccumulator > 150f) {
                        dragAccumulator = 0f
                        onDismiss()
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                } else if (available.y < 0f) {
                    dragAccumulator = 0f
                }
                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                dragAccumulator = 0f
                if (available.y > 100f) {
                    onDismiss()
                    return available
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    LaunchedEffect(Unit) {
        appRepository.refreshAppList()
    }

    val hasWorkProfile = remember(allApps) {
        allApps.any { it.isWorkProfile }
    }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Personal, 1 = Work

    val filteredApps by remember {
        derivedStateOf {
            val baseApps = if (hasWorkProfile) {
                if (selectedTab == 0) allApps.filter { !it.isWorkProfile }
                else allApps.filter { it.isWorkProfile }
            } else {
                allApps
            }
            val nonHidden = baseApps.filter { !it.isHidden }
            val searched = if (searchQuery.isBlank()) nonHidden
            else nonHidden.filter { it.displayLabel.contains(searchQuery, ignoreCase = true) }

            when (sortMode) {
                DrawerSortMode.ALPHABETICAL -> searched.sortedBy { it.displayLabel.lowercase() }
                DrawerSortMode.USAGE -> searched.sortedWith(
                    compareByDescending<AppInfo> { it.usageCount }
                        .thenByDescending { it.lastUsedTime }
                        .thenBy { it.displayLabel.lowercase() }
                )
                DrawerSortMode.INSTALL_DATE -> searched.sortedWith(
                    compareByDescending<AppInfo> { it.installTime }
                        .thenBy { it.displayLabel.lowercase() }
                )
                else -> searched.sortedBy { it.displayLabel.lowercase() }
            }
        }
    }

    val drawerGridItems by remember {
        derivedStateOf {
            val items = mutableListOf<DrawerGridItem>()
            if (searchQuery.isNotEmpty()) {
                if (searchResults.isNotEmpty()) {
                    searchResults.forEach { result ->
                        val action = result.action
                        if (action is SearchResultAction.LaunchApp) {
                            val matchedApp = allApps.find {
                                it.packageName == action.packageName &&
                                (action.activityName.isEmpty() || it.activityName == action.activityName)
                            }
                            if (matchedApp != null) {
                                items.add(DrawerGridItem.App(matchedApp))
                            } else {
                                items.add(DrawerGridItem.UniversalResult(result))
                            }
                        } else {
                            items.add(DrawerGridItem.UniversalResult(result))
                        }
                    }
                } else {
                    filteredApps.forEach { items.add(DrawerGridItem.App(it)) }
                }
                if (isPhoneNumber(searchQuery)) {
                    items.add(DrawerGridItem.QuickDialer(searchQuery))
                }
            } else {
                val groupedPackages = drawerGroups.flatMap { it.appPackageNames }.toSet()
                filteredApps.forEach { app ->
                    if (app.packageName !in groupedPackages) {
                        items.add(DrawerGridItem.App(app))
                    }
                }
                drawerGroups.forEach { group ->
                    items.add(DrawerGridItem.Group(group))
                }
                items.sortWith(
                    compareBy<DrawerGridItem> {
                        when (it) {
                            is DrawerGridItem.Group -> 1 // Groups first? wait, if we want apps first it should be 1
                            is DrawerGridItem.App -> 0
                            else -> 2
                        }
                    }.thenBy {
                        when (it) {
                            is DrawerGridItem.Group -> it.group.title.lowercase()
                            is DrawerGridItem.App -> it.app.displayLabel.lowercase()
                            else -> ""
                        }
                    }
                )
            }
            items
        }
    }

    val saveGroup = remember(drawerGroups, settingsRepository, coroutineScope) {
        { updatedGroup: com.oorbitt.launcher.model.DrawerGroup ->
            val index = drawerGroups.indexOfFirst { it.id == updatedGroup.id }
            val newGroups = if (index >= 0) {
                drawerGroups.toMutableList().apply { set(index, updatedGroup) }
            } else {
                drawerGroups.toMutableList().apply { add(updatedGroup) }
            }
            coroutineScope.launch {
                settingsRepository.updateSettings {
                    it.copy(drawerGroupsJson = com.oorbitt.launcher.model.DrawerGroup.toJsonString(newGroups))
                }
            }
            Unit
        }
    }

    val deleteGroup = remember(drawerGroups, settingsRepository, coroutineScope) {
        { groupId: String ->
            val newGroups = drawerGroups.filter { it.id != groupId }
            coroutineScope.launch {
                settingsRepository.updateSettings {
                    it.copy(drawerGroupsJson = com.oorbitt.launcher.model.DrawerGroup.toJsonString(newGroups))
                }
            }
            Unit
        }
    }

    val bgColor = remember(settingsState.drawerBgColor, settingsState.drawerBgOpacity) {
        val parsed = try {
            Color(android.graphics.Color.parseColor(settingsState.drawerBgColor))
        } catch (e: Exception) {
            Color.Black
        }
        parsed.copy(alpha = settingsState.drawerBgOpacity / 100f)
    }
    val drawerShape = remember(
        settingsState.drawerCornerTopStartDp,
        settingsState.drawerCornerTopEndDp,
        settingsState.drawerCornerBottomStartDp,
        settingsState.drawerCornerBottomEndDp
    ) {
        RoundedCornerShape(
            topStart = settingsState.drawerCornerTopStartDp.dp,
            topEnd = settingsState.drawerCornerTopEndDp.dp,
            bottomStart = settingsState.drawerCornerBottomStartDp.dp,
            bottomEnd = settingsState.drawerCornerBottomEndDp.dp
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .nestedScroll(nestedScrollConnection)
    ) {
        // App drawer background card platform
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (dragManager.isDraggingFromDrawer) 0f else 1f
                }
                .padding(
                    start = settingsState.drawerSpaceDp.dp,
                    end = settingsState.drawerSpaceDp.dp,
                    bottom = settingsState.drawerSpaceDp.dp,
                    top = settingsState.drawerSpaceDp.dp
                )
                .clip(drawerShape)
                .background(bgColor)
        ) {
            if (!settingsState.drawerBgImage.isNullOrBlank()) {
                var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(settingsState.drawerBgImage) {
                    imageBitmap = withContext(Dispatchers.IO) {
                        try {
                            val uri = android.net.Uri.parse(settingsState.drawerBgImage)
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            bmp?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = settingsState.drawerBgOpacity / 100f
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
        val searchBarBlockAndOpenWith = @Composable {
            val options = remember(searchQuery, settingsState, installedAIApps) {
                if (searchQuery.isBlank()) return@remember emptyList<OpenWithOption>()
                
                val list = mutableListOf<OpenWithOption>()
                
                if (settingsState.enableWebSearchInOpenWith) {
                    list.add(OpenWithOption("Google", isWeb = true) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com/search?q=${Uri.encode(searchQuery)}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    })
                    list.add(OpenWithOption("DuckDuckGo", isWeb = true) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://duckduckgo.com/?q=${Uri.encode(searchQuery)}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    })
                    list.add(OpenWithOption("Bing", isWeb = true) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bing.com/search?q=${Uri.encode(searchQuery)}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    })
                }
                
                if (settingsState.enableAISearchInOpenWith) {
                    val favCsv = settingsState.favoriteAIAppsCsv
                    val filteredAIApps = if (favCsv.isEmpty()) {
                        installedAIApps
                    } else {
                        val favSet = favCsv.split(",").filter { it.isNotEmpty() }.toSet()
                        installedAIApps.filter { it.packageName in favSet }
                    }
                    filteredAIApps.forEach { aiApp ->
                        list.add(OpenWithOption(aiApp.name) {
                            launchAISearch(context, aiApp, searchQuery)
                        })
                    }
                }
                
                list
            }

            val showOpenWithPill = searchQuery.isNotEmpty() && options.isNotEmpty()

            val searchBarBgColorObj = remember(settingsState.drawerSearchBgColor, settingsState.drawerSearchBgOpacity) {
                try {
                    Color(android.graphics.Color.parseColor(settingsState.drawerSearchBgColor))
                        .copy(alpha = settingsState.drawerSearchBgOpacity / 100f)
                } catch (e: Exception) {
                    Color.White.copy(alpha = 0.12f)
                }
            }

            val searchBarBorderColorObj = remember(settingsState.drawerSearchBorderColor) {
                try {
                    Color(android.graphics.Color.parseColor(settingsState.drawerSearchBorderColor))
                } catch (e: Exception) {
                    Color.White
                }
            }

            val searchBarShape = remember(settingsState.drawerSearchCornerRadiusDp) {
                RoundedCornerShape(settingsState.drawerSearchCornerRadiusDp.dp)
            }

            val searchBarBorderModifier = if (settingsState.drawerSearchBorderWidthDp > 0) {
                Modifier.border(
                    width = settingsState.drawerSearchBorderWidthDp.dp,
                    color = searchBarBorderColorObj,
                    shape = searchBarShape
                )
            } else {
                Modifier
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 25f) {
                                onDismiss()
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = settingsState.searchBarPaddingDp.dp, vertical = (settingsState.searchBarPaddingDp * 0.6f).toInt().dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(if (showOpenWithPill) 1.2f else 1f)
                            .height(settingsState.drawerSearchHeightDp.dp)
                            .then(searchBarBorderModifier),
                        shape = searchBarShape,
                        color = searchBarBgColorObj
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
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search apps…",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    if (showOpenWithPill) {
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val fadeLineBrush = remember(searchBarBorderColorObj) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                                            Color.Transparent,
                                                            searchBarBorderColorObj.copy(alpha = 0.4f),
                                                            searchBarBorderColorObj,
                                                            searchBarBorderColorObj.copy(alpha = 0.4f),
                                                            Color.Transparent
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.8f)
                                .height(settingsState.drawerSearchHeightDp.dp)
                                .drawBehind {
                                    // Top line
                                    drawLine(
                                        brush = fadeLineBrush,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    // Bottom line
                                    drawLine(
                                        brush = fadeLineBrush,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val lazyListState = rememberLazyListState()
                            LazyRow(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                itemsIndexed(options) { index, option ->
                                    val aiApp = installedAIApps.find { it.name == option.name }
                                    val activityName = aiApp?.let { app ->
                                        allApps.find { it.packageName == app.packageName }?.activityName
                                    }
                                    val icon = aiApp?.let { app ->
                                        rememberAppIcon(context, app.packageName, activityName, settingsState.activeIconPack)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                val layoutInfo = lazyListState.layoutInfo
                                                val visibleItems = layoutInfo.visibleItemsInfo
                                                val itemInfo = visibleItems.find { it.index == index }
                                                if (itemInfo != null) {
                                                    val viewportCenter = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
                                                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                                    val distanceFromCenter = kotlin.math.abs(viewportCenter - itemCenter)
                                                    val fraction = (1f - (distanceFromCenter / viewportCenter)).coerceIn(0f, 1f)
                                                    
                                                    scaleX = 0.8f + 0.35f * fraction
                                                    scaleY = 0.8f + 0.35f * fraction
                                                    alpha = 0.4f + 0.6f * fraction
                                                } else {
                                                    scaleX = 0.8f
                                                    scaleY = 0.8f
                                                    alpha = 0.4f
                                                }
                                            }
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .clickable { option.action() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (icon != null) {
                                            Image(
                                                bitmap = icon,
                                                contentDescription = option.name,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            val initials = when (option.name) {
                                                "Google" -> "G"
                                                "DuckDuckGo" -> "D"
                                                "Bing" -> "B"
                                                else -> option.name.take(1).uppercase()
                                            }
                                            val circleColor = when (option.name) {
                                                "Google" -> Color(0xFF4285F4)
                                                "DuckDuckGo" -> Color(0xFFFF5722)
                                                "Bing" -> Color(0xFF00837B)
                                                "ChatGPT" -> Color(0xFF10A37F)
                                                "Gemini" -> Color(0xFF1A73E8)
                                                "Claude" -> Color(0xFFD97706)
                                                "Copilot" -> Color(0xFF2563EB)
                                                "Perplexity" -> Color(0xFF00A3A0)
                                                else -> Color.White
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(circleColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = initials,
                                                    color = circleColor,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (settingsState.drawerSearchPosition != "BOTTOM") {
            searchBarBlockAndOpenWith()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(settingsState.drawerColumns),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (searchQuery.isNotEmpty()) settingsState.searchResultsSpacingDp.dp else 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = drawerGridItems,
                    key = {
                        when (it) {
                            is DrawerGridItem.App -> "app:${it.app.componentKey}"
                            is DrawerGridItem.Group -> "group:${it.group.id}"
                            is DrawerGridItem.Calculator -> "calculator:${it.expression}"
                            is DrawerGridItem.WebSearch -> "web:${it.engineName}"
                            is DrawerGridItem.UniversalResult -> "universal:${it.result.id}"
                            is DrawerGridItem.QuickDialer -> "quick_dialer:${it.query}"
                        }
                    },
                    span = { item ->
                        val span = when (item) {
                            is DrawerGridItem.App -> 1
                            is DrawerGridItem.Group -> item.group.spanX
                            is DrawerGridItem.Calculator -> settingsState.drawerColumns
                            is DrawerGridItem.WebSearch -> settingsState.drawerColumns
                            is DrawerGridItem.UniversalResult -> settingsState.drawerColumns
                            is DrawerGridItem.QuickDialer -> settingsState.drawerColumns
                        }
                        GridItemSpan(span.coerceIn(1, settingsState.drawerColumns))
                    },
                    contentType = {
                        when (it) {
                            is DrawerGridItem.App -> "app_icon"
                            is DrawerGridItem.Group -> "group_card"
                            is DrawerGridItem.Calculator -> "calculator"
                            is DrawerGridItem.WebSearch -> "web_search"
                            is DrawerGridItem.UniversalResult -> "universal_result"
                            is DrawerGridItem.QuickDialer -> "quick_dialer"
                        }
                    }
                ) { item ->
                    when (item) {
                        is DrawerGridItem.App -> {
                            var gridItemCoordinates by remember { mutableStateOf(Offset.Zero) }
                            AppGridItem(
                                app = item.app,
                                activeIconPack = settingsState.activeIconPack,
                                iconSizeDp = settingsState.drawerIconSizeDp,
                                labelSizeSp = settingsState.labelSizeSp,
                                showLabels = settingsState.showDrawerLabels,
                                iconShape = iconShape,
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    gridItemCoordinates = coords.positionInWindow()
                                },
                                onClick = onAppClick,
                                onLongClick = { app, touchOffset ->
                                    activeContextMenuApp = Pair(app, gridItemCoordinates + touchOffset)
                                },
                                onDragStart = { app, touchOffset ->
                                    activeContextMenuApp = null
                                    dragManager.startDrawerDrag(app, gridItemCoordinates, touchOffset)
                                },
                                onDrag = { dragAmount ->
                                    dragManager.updateDrag(dragAmount)
                                },
                                onDragEnd = onDrawerItemDragEnd
                            )
                        }
                        is DrawerGridItem.Group -> {
                            GroupGridItem(
                                group = item.group,
                                allApps = allApps,
                                activeIconPack = settingsState.activeIconPack,
                                iconShape = iconShape,
                                onClick = { selectedGroupForDetail = item.group },
                                onLongClick = { groupToCustomise = item.group }
                            )
                        }
                        is DrawerGridItem.Calculator -> {
                            CalculatorItem(
                                expression = item.expression,
                                result = item.result,
                                onResultClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Calculation Result", item.result)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Copied result: ${item.result}", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {}
                                }
                            )
                        }
                        is DrawerGridItem.WebSearch -> {
                            WebSearchItem(
                                query = item.query,
                                engineName = item.engineName,
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No web browser found", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        is DrawerGridItem.UniversalResult -> {
                            UniversalResultItem(
                                result = item.result,
                                activeIconPack = settingsState.activeIconPack,
                                settingsState = settingsState,
                                onClick = {
                                    executeSearchAction(context, item.result.action)
                                    if (item.result.action is SearchResultAction.LaunchApp ||
                                        item.result.action is SearchResultAction.OpenUrl ||
                                        item.result.action is SearchResultAction.OpenSetting ||
                                        item.result.action is SearchResultAction.LaunchShortcut
                                    ) {
                                        onDismiss()
                                    }
                                }
                            )
                        }
                        is DrawerGridItem.QuickDialer -> {
                            QuickDialerCard(
                                query = item.query,
                                context = context,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
            
            // Sleek Arrow up indicator
            androidx.compose.animation.AnimatedVisibility(
                visible = canScrollBackward && searchQuery.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "More results above",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(20.dp),
                        tint = Color.White
                    )
                }
            }

            // Sleek Arrow down indicator
            androidx.compose.animation.AnimatedVisibility(
                visible = canScrollForward && searchQuery.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "More results below",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(20.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        if (settingsState.drawerSearchPosition == "BOTTOM") {
            searchBarBlockAndOpenWith()
        }
    }
}

    manageGroupsApp?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { manageGroupsApp = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Manage Groups: ${app.displayLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
                HorizontalDivider()

                if (drawerGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No groups created yet",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    drawerGroups.forEach { group ->
                        val isMember = group.appPackageNames.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newApps = if (isMember) {
                                        group.appPackageNames.filter { it != app.packageName }
                                    } else {
                                        group.appPackageNames + app.packageName
                                    }
                                    saveGroup(group.copy(appPackageNames = newApps))
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isMember,
                                onCheckedChange = { checked ->
                                    val newApps = if (checked) {
                                        group.appPackageNames + app.packageName
                                    } else {
                                        group.appPackageNames.filter { it != app.packageName }
                                    }
                                    saveGroup(group.copy(appPackageNames = newApps))
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                HorizontalDivider()
                
                ListItem(
                    headlineContent = { Text("Create New Group", color = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        val newGroup = com.oorbitt.launcher.model.DrawerGroup(
                            title = "New Group",
                            appPackageNames = listOf(app.packageName)
                        )
                        saveGroup(newGroup)
                        manageGroupsApp = null
                        groupToCustomise = newGroup
                    }
                )
            }
        }
    }

    selectedGroupForDetail?.let { group ->
        val groupApps = remember(group, allApps) {
            group.appPackageNames.mapNotNull { pkg ->
                appsByPackage[pkg]
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = { selectedGroupForDetail = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = {
                            selectedGroupForDetail = null
                            groupToCustomise = group
                        }
                    ) {
                        Text("Customise")
                    }
                }
                
                HorizontalDivider()
                
                if (groupApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps inside this group",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settingsState.drawerColumns),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupApps) { app ->
                            var gridItemCoordinates by remember { mutableStateOf(Offset.Zero) }
                            AppGridItem(
                                app = app,
                                activeIconPack = settingsState.activeIconPack,
                                iconSizeDp = settingsState.drawerIconSizeDp,
                                labelSizeSp = settingsState.labelSizeSp,
                                showLabels = settingsState.showDrawerLabels,
                                iconShape = iconShape,
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    gridItemCoordinates = coords.positionInWindow()
                                },
                                onClick = { clickedApp ->
                                    selectedGroupForDetail = null
                                    onDismiss()
                                    onAppClick(clickedApp)
                                },
                                onLongClick = { clickedApp, touchOffset ->
                                    selectedGroupForDetail = null
                                    activeContextMenuApp = Pair(clickedApp, gridItemCoordinates + touchOffset)
                                },
                                onDragStart = { clickedApp, touchOffset ->
                                    selectedGroupForDetail = null
                                    activeContextMenuApp = null
                                    dragManager.startDrawerDrag(clickedApp, gridItemCoordinates, touchOffset)
                                },
                                onDrag = { dragAmount ->
                                    dragManager.updateDrag(dragAmount)
                                },
                                onDragEnd = onDrawerItemDragEnd
                            )
                        }
                    }
                }
            }
        }
    }

    groupToCustomise?.let { group ->
        CustomizeGroupSheet(
            group = group,
            allApps = allApps,
            maxColumns = settingsState.drawerColumns,
            onDismiss = { groupToCustomise = null },
            onSave = { updatedGroup ->
                saveGroup(updatedGroup)
                groupToCustomise = null
            },
            onDelete = { groupId ->
                deleteGroup(groupId)
                groupToCustomise = null
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

    if (activeContextMenuApp != null) {
        val (app, offset) = activeContextMenuApp!!
        val contextMenuOptions: List<Pair<String, () -> Unit>> = listOf(
            "Add to Home" to {
                coroutineScope.launch {
                    var foundSlot = false
                    for (page in 0..2) {
                        for (y in 0..4) {
                            for (x in 0..4) {
                                val occupied = workspaceItems.any {
                                    it.screenIndex == page &&
                                    it.containerType == ContainerType.WORKSPACE &&
                                    x < it.cellX + it.spanX &&
                                    x + 1 > it.cellX &&
                                    y < it.cellY + it.spanY &&
                                    y + 1 > it.cellY
                                }
                                if (!occupied) {
                                    workspaceRepository.addItem(
                                        WorkspaceItem(
                                            id = 0,
                                            itemType = ItemType.APP,
                                            containerType = ContainerType.WORKSPACE,
                                            screenIndex = page,
                                            cellX = x,
                                            cellY = y,
                                            spanX = 1,
                                            spanY = 1,
                                            packageName = app.packageName,
                                            activityName = app.activityName
                                        )
                                    )
                                    foundSlot = true
                                    break
                                }
                            }
                            if (foundSlot) break
                        }
                        if (foundSlot) break
                    }
                    if (!foundSlot) {
                        android.widget.Toast.makeText(context, "No space on home screen", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                Unit
            },
            "Customize" to {
                customizeTargetApp = app
                Unit
            },
            "Add to Group" to {
                manageGroupsApp = app
                Unit
            },
            "App Info" to {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Could not open app info", android.widget.Toast.LENGTH_SHORT).show()
                }
                Unit
            },
            "Uninstall" to {
                try {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${app.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Could not uninstall app", android.widget.Toast.LENGTH_SHORT).show()
                }
                Unit
            },
            "Hide" to {
                coroutineScope.launch {
                    appRepository.hideApp(app.componentKey)
                }
                Unit
            }
        )

        FloatingContextMenu(
            x = offset.x.coerceIn(16f, (context.resources.displayMetrics.widthPixels - 500).toFloat()),
            y = offset.y.coerceIn(16f, (context.resources.displayMetrics.heightPixels - 800).toFloat()),
            title = app.displayLabel,
            options = contextMenuOptions,
            onDismiss = { activeContextMenuApp = null }
        )
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
                        Text("Emergency 5m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text("Emergency 10m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    if (showFirstTimeOrbSpaceHintDialog) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(
                    color = Color(0xFF1E1E2E).copy(alpha = 0.98f),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF00B894).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OrbSpace Dashboard",
                    color = Color(0xFF00B894),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Swipe left on the home screen to access your personalized OrbSpace dashboard, wellness stats, and productivity cards.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        showFirstTimeOrbSpaceHintDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00B894),
                        contentColor = Color(0xFF1E1E2E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp).fillMaxWidth(0.6f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("Got it", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: AppInfo,
    activeIconPack: String?,
    iconSizeDp: Int,
    labelSizeSp: Int,
    showLabels: Boolean,
    iconShape: androidx.compose.ui.graphics.Shape,
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo, Offset) -> Unit,
    onDragStart: (AppInfo, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val icon = rememberAppIcon(context, app.packageName, app.activityName, activeIconPack, app.customIconUri)
    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }
    var isDraggingMode by remember { mutableStateOf(false) }
    var longPressOccurred by remember { mutableStateOf(false) }
    var initialTouchOffset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .pointerInput(app) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.changedToDown() }) {
                            longPressOccurred = false
                        }
                    }
                }
            }
            .pointerInput(app) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragAccumulated = Offset.Zero
                        isDraggingMode = false
                        longPressOccurred = true
                        initialTouchOffset = offset
                        onLongClick(app, offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                        if (!isDraggingMode && dragAccumulated.getDistance() > 15f) {
                            isDraggingMode = true
                            onDragStart(app, initialTouchOffset)
                        }
                        if (isDraggingMode) {
                            onDrag(dragAmount)
                        }
                    },
                    onDragEnd = {
                        if (isDraggingMode) {
                            onDragEnd()
                        }
                        isDraggingMode = false
                    },
                    onDragCancel = {
                        if (isDraggingMode) {
                            onDragEnd()
                        }
                        isDraggingMode = false
                    }
                )
            }
            .clickable {
                if (!isDraggingMode && !longPressOccurred) {
                    onClick(app)
                }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                .background(Color.Gray)
        )
        if (showLabels) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.displayLabel,
                color = Color.White,
                fontSize = labelSizeSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(iconSizeDp.dp + 16.dp)
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

private fun launchApp(context: Context, packageName: String, activityName: String) {
    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(packageName, activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) { }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupGridItem(
    group: com.oorbitt.launcher.model.DrawerGroup,
    allApps: List<AppInfo>,
    activeIconPack: String?,
    iconShape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedBgColor = remember(group.bgColor) {
        try {
            Color(android.graphics.Color.parseColor(group.bgColor))
        } catch (e: Exception) {
            Color(0x22FFFFFF)
        }
    }
    val parsedTextColor = remember(group.textColor) {
        try {
            Color(android.graphics.Color.parseColor(group.textColor))
        } catch (e: Exception) {
            Color.White
        }
    }

    val previewApps = remember(group.appPackageNames, allApps) {
        val indexed = allApps.associateBy { it.packageName }
        group.appPackageNames.mapNotNull { pkg ->
            indexed[pkg]
        }.take(4)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height((90 * group.spanY).dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(group.cornerRadiusDp.dp),
        colors = CardDefaults.cardColors(containerColor = parsedBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (previewApps.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            previewApps.take(2).forEach { app ->
                                val icon = rememberAppIcon(context, app.packageName, app.activityName, activeIconPack, app.customIconUri)
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(iconShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(iconShape)
                                            .background(Color.Gray.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                        if (previewApps.size > 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                previewApps.drop(2).take(2).forEach { app ->
                                    val icon = rememberAppIcon(context, app.packageName, app.activityName, activeIconPack, app.customIconUri)
                                    if (icon != null) {
                                        Image(
                                            bitmap = icon,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(iconShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(iconShape)
                                                .background(Color.Gray.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = parsedTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = group.title,
                color = parsedTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeGroupSheet(
    group: com.oorbitt.launcher.model.DrawerGroup,
    allApps: List<AppInfo>,
    maxColumns: Int,
    onDismiss: () -> Unit,
    onSave: (com.oorbitt.launcher.model.DrawerGroup) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf(group.title) }
    var spanX by remember { mutableStateOf(group.spanX.toFloat()) }
    var spanY by remember { mutableStateOf(group.spanY.toFloat()) }
    var bgColor by remember { mutableStateOf(group.bgColor) }
    var textColor by remember { mutableStateOf(group.textColor) }
    var cornerRadius by remember { mutableStateOf(group.cornerRadiusDp.toFloat()) }
    val selectedApps = remember { mutableStateListOf<String>().apply { addAll(group.appPackageNames) } }
    
    val predefinedColors = listOf(
        "#22FFFFFF", "#44888888", "#88000000",
        "#440096F3", "#444CAF50", "#44FF9800",
        "#44F44336", "#449C27B0"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize Group",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(
                    onClick = { onDelete(group.id); onDismiss() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Group")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Columns Width (Breadth): ${spanX.toInt()}")
            Slider(
                value = spanX,
                onValueChange = { spanX = it },
                valueRange = 1f..maxColumns.toFloat(),
                steps = if (maxColumns > 2) maxColumns - 2 else 0
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Row Height: ${spanY.toInt()}")
            Slider(
                value = spanY,
                onValueChange = { spanY = it },
                valueRange = 1f..3f,
                steps = 1
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Text("Corner Radius: ${cornerRadius.toInt()} dp")
            Slider(
                value = cornerRadius,
                onValueChange = { cornerRadius = it },
                valueRange = 0f..48f
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Background Color (Hex code)")
            OutlinedTextField(
                value = bgColor,
                onValueChange = { bgColor = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                predefinedColors.forEach { colorStr ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorStr)))
                            .clickable { bgColor = colorStr }
                            .border(
                                width = if (bgColor == colorStr) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Text Color (Hex code)")
            OutlinedTextField(
                value = textColor,
                onValueChange = { textColor = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Apps inside this group", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    allApps.sortedBy { it.displayLabel.lowercase() }.forEach { app ->
                        val isChecked = selectedApps.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedApps.remove(app.packageName)
                                    else selectedApps.add(app.packageName)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedApps.add(app.packageName)
                                    else selectedApps.remove(app.packageName)
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = app.displayLabel, color = Color.White)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    onSave(
                        group.copy(
                            title = title.ifBlank { "Group" },
                            spanX = spanX.toInt(),
                            spanY = spanY.toInt(),
                            bgColor = bgColor,
                            textColor = textColor,
                            cornerRadiusDp = cornerRadius.toInt(),
                            appPackageNames = selectedApps.toList()
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}

@Composable
private fun CalculatorItem(
    expression: String,
    result: String,
    onResultClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clickable { onResultClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "=",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expression,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
                Text(
                    text = result,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Copy",
                color = Color(0xFF64B5F6),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun WebSearchItem(
    query: String,
    engineName: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = "Search $engineName for \"$query\"",
                color = Color.White,
                fontSize = 15.sp
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search icon",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
}

@Composable
private fun UniversalResultItem(
    result: SearchResult,
    activeIconPack: String?,
    settingsState: com.oorbitt.launcher.model.LauncherSettings,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isCalculator = result.providerName == "Calculator"

    val isLightBg = remember(settingsState.drawerBgColor, settingsState.drawerBgOpacity) {
        val parsed = try {
            Color(android.graphics.Color.parseColor(settingsState.drawerBgColor))
        } catch (e: Exception) {
            Color.Black
        }
        if (settingsState.drawerBgOpacity < 35) {
            false
        } else {
            parsed.luminance() > 0.5f
        }
    }

    val themeAccentColor = remember(settingsState.orbSpaceAccentColor) {
        try {
            Color(android.graphics.Color.parseColor(settingsState.orbSpaceAccentColor))
        } catch (e: Exception) {
            Color(0xFF007AFF)
        }
    }

    val primaryTextColor = if (isLightBg) Color(0xFF1C1C1E) else Color.White
    val secondaryTextColor = if (isLightBg) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val dividerColor = if (isLightBg) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)
    val cardBgColor = if (isLightBg) Color.White else Color(0xFF1C1C1E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        border = BorderStroke(1.dp, dividerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leadingIcon = @Composable {
                if (isCalculator) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeAccentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "=",
                            color = themeAccentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val action = result.action
                    when (action) {
                        is SearchResultAction.LaunchApp -> {
                            val icon = rememberAppIcon(context, action.packageName, action.activityName, activeIconPack)
                            icon?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = result.title,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } ?: Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(secondaryTextColor.copy(alpha = 0.15f))
                            )
                        }
                        is SearchResultAction.OpenSetting -> {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(secondaryTextColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = primaryTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        is SearchResultAction.CallContact -> {
                            val isQuickAction = result.id.startsWith("phone_")
                            val color = if (isQuickAction) Color(0xFF00B894) else themeAccentColor
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        is SearchResultAction.MessageContact -> {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0984E3).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "SMS",
                                    tint = Color(0xFF0984E3),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        is SearchResultAction.OpenUrl -> {
                            val isWhatsApp = result.id.startsWith("phone_wa_") || action.url.contains("whatsapp.com")
                            val color = if (isWhatsApp) Color(0xFF25D366) else themeAccentColor
                            val icon = if (isWhatsApp) Icons.Default.Call else Icons.Default.Search
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(secondaryTextColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = primaryTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            leadingIcon()
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    color = primaryTextColor,
                    fontSize = if (isCalculator) 20.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitleText = if (isCalculator) (result.subtitle ?: "Calculator result") else result.subtitle
                subtitleText?.let {
                    Text(
                        text = it,
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (isCalculator) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Copy",
                    color = themeAccentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(themeAccentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun executeSearchAction(context: Context, action: SearchResultAction) {
    when (action) {
        is SearchResultAction.LaunchApp -> {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(action.packageName, action.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
        is SearchResultAction.OpenUrl -> {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
        is SearchResultAction.OpenSetting -> {
            try {
                val intent = Intent(action.settingAction).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
        is SearchResultAction.ShowAnswer -> {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Calculation Result", action.answer)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "Copied result: ${action.answer}", android.widget.Toast.LENGTH_SHORT).show()
        }
        is SearchResultAction.LaunchShortcut -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
                    launcherApps.startShortcut(action.packageName, action.shortcutId, null, null, android.os.Process.myUserHandle())
                }
            } catch (_: Exception) {}
        }
        is SearchResultAction.CallContact -> {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.phoneNumber}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
        is SearchResultAction.MessageContact -> {
            try {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(action.contactUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
        is SearchResultAction.OpenFile -> {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(action.uri), action.mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
        else -> {}
    }
}

private data class AIAppSpec(
    val name: String,
    val packageName: String,
    val searchUrl: String
)

private data class OpenWithOption(
    val name: String,
    val isWeb: Boolean = false,
    val action: () -> Unit
)

private fun launchAISearch(context: Context, aiApp: AIAppSpec, query: String) {
    val encodedQuery = Uri.encode(query)
    val url = "${aiApp.searchUrl}$encodedQuery"
    

    
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val defaultBrowser = context.packageManager.resolveActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://")),
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        if (defaultBrowser != null && defaultBrowser != "android") {
            browserIntent.setPackage(defaultBrowser)
        }
        try {
            context.startActivity(browserIntent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "Failed to open ${aiApp.name}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun QuickDialerCard(
    query: String,
    context: Context,
    onDismiss: () -> Unit
) {
    val cleanNumber = remember(query) {
        val trimmed = query.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        if (hasPlus) "+$digits" else digits
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Quick Action",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cleanNumber,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickDialButton(
                    label = "Call",
                    icon = Icons.Default.Call,
                    color = Color(0xFF00B894),
                    modifier = Modifier.weight(1f)
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        onDismiss()
                    } catch (_: Exception) {}
                }
                QuickDialButton(
                    label = "SMS",
                    icon = Icons.AutoMirrored.Filled.Send,
                    color = Color(0xFF0984E3),
                    modifier = Modifier.weight(1f)
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        onDismiss()
                    } catch (_: Exception) {}
                }
                QuickDialButton(
                    label = "WhatsApp",
                    icon = Icons.Default.Call,
                    color = Color(0xFF25D366),
                    modifier = Modifier.weight(1f)
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        onDismiss()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}

@Composable
private fun QuickDialButton(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun isPhoneNumber(query: String): Boolean {
    val trimmed = query.trim()
    val digitCount = trimmed.count { it.isDigit() }
    if (digitCount < 3) return false

    return trimmed.indices.all { i ->
        val c = trimmed[i]
        c.isDigit() || c == ' ' || c == '-' || c == '(' || c == ')' || (c == '+' && i == 0)
    }
}

