package com.oorbitt.launcher.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.ui.util.rememberAppIcon
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    settingsRepository: SettingsRepository = koinInject(),
    appRepository: com.oorbitt.launcher.data.repository.AppRepository = koinInject(),
    authManager: com.oorbitt.launcher.security.AuthManager = koinInject()
) {
    val context = LocalContext.current
    val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
    val allApps by appRepository.allApps.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var mindfulLockAppPkg by remember { mutableStateOf<String?>(null) }
    var mindfulLockAppLabel by remember { mutableStateOf("") }
    var mindfulLockAppAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val aggregator: SearchAggregator = koinInject()
    val focusRequester = remember { FocusRequester() }
    var showFirstTimeAISearchHintDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
        if (!sp.getBoolean("search_ai_hint_shown", false)) {
            showFirstTimeAISearchHintDialog = true
        }
    }

    val listState = rememberLazyListState()

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

    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            try {
                listState.scrollToItem(0)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
        } else {
            delay(150) // debounce keystrokes
            results = aggregator.search(query)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
            Color(0xFF007AFF) // Default Apple Blue
        }
    }

    val primaryTextColor = if (isLightBg) Color(0xFF1C1C1E) else Color.White
    val secondaryTextColor = if (isLightBg) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val dividerColor = if (isLightBg) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)
    val cardBgColor = if (isLightBg) Color.White else Color(0xFF1C1C1E)
    val screenBgColor = if (isLightBg) Color(0xFFF2F2F7) else Color(0xFF0A0A0C)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val searchBarAndOpenWith = @Composable {
            val options = remember(query, settingsState, installedAIApps) {
                if (query.isBlank()) return@remember emptyList<OpenWithOption>()
                
                val list = mutableListOf<OpenWithOption>()
                
                if (settingsState.enableWebSearchInOpenWith) {
                    list.add(OpenWithOption("Google", isWeb = true) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com/search?q=${Uri.encode(query)}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            onDismiss()
                        } catch (_: Exception) {}
                    })
                    list.add(OpenWithOption("DuckDuckGo", isWeb = true) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://duckduckgo.com/?q=${Uri.encode(query)}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            onDismiss()
                        } catch (_: Exception) {}
                    })
                    list.add(OpenWithOption("Bing", isWeb = true) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bing.com/search?q=${Uri.encode(query)}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            onDismiss()
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
                            launchAISearch(context, aiApp, query)
                            onDismiss()
                        })
                    }
                }
                
                list
            }

            val showOpenWithPill = query.isNotEmpty() && options.isNotEmpty()

            Row(
                modifier = Modifier
                    .padding(horizontal = settingsState.searchBarPaddingDp.dp, vertical = (settingsState.searchBarPaddingDp * 0.75f).toInt().dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(if (showOpenWithPill) 1.2f else 1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, dividerColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = primaryTextColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = TextStyle(
                                color = primaryTextColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(themeAccentColor),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search apps, contacts, settings…",
                                        color = secondaryTextColor.copy(alpha = 0.6f),
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        )
                        
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = secondaryTextColor
                                )
                            }
                        }
                    }
                }

                if (showOpenWithPill) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val fadeLineBrush = remember(dividerColor) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                dividerColor.copy(alpha = 0.4f),
                                dividerColor,
                                dividerColor.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(56.dp)
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
                                        .background(primaryTextColor.copy(alpha = 0.05f))
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
                                            else -> themeAccentColor
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

        val resultsList: @Composable ColumnScope.() -> Unit = {
            val canScrollBackward by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
            }
            val canScrollForward by remember {
                derivedStateOf {
                    listState.canScrollForward
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = if (settingsState.searchBarPosition.equals("BOTTOM", ignoreCase = true)) {
                        Arrangement.spacedBy(settingsState.searchResultsSpacingDp.dp, Alignment.Bottom)
                    } else {
                        Arrangement.spacedBy(settingsState.searchResultsSpacingDp.dp, Alignment.Top)
                    },
                    reverseLayout = settingsState.searchBarPosition.equals("BOTTOM", ignoreCase = true)
                ) {
                    val showDialer = isPhoneNumber(query)

                    if (showDialer && settingsState.searchBarPosition.equals("BOTTOM", ignoreCase = true)) {
                        item(key = "quick_dialer") {
                            QuickDialerCard(query = query, context = context) {
                                onDismiss()
                            }
                        }
                    }

                    items(
                        items = if (settingsState.searchBarPosition.equals("BOTTOM", ignoreCase = true)) results.reversed() else results,
                        key = { it.id }
                    ) { result ->
                        val isLocked = remember(result, allApps) {
                            val action = result.action
                            if (action is SearchResultAction.LaunchApp) {
                                allApps.any { it.packageName == action.packageName && it.activityName == action.activityName && it.isLocked }
                            } else {
                                false
                            }
                        }
                        SearchResultItem(
                            result = result,
                            activeIconPack = settingsState.activeIconPack,
                            isLocked = isLocked,
                            onClick = {
                                val action = result.action
                                if (action is SearchResultAction.LaunchApp) {
                                    val matchedApp = allApps.find {
                                        it.packageName == action.packageName && it.activityName == action.activityName
                                    }
                                    val launchAction = {
                                        executeSearchAction(context, action)
                                        onDismiss()
                                    }
                                    if (com.oorbitt.launcher.security.MindfulLockManager.isAppLocked(context, action.packageName)) {
                                        mindfulLockAppPkg = action.packageName
                                        mindfulLockAppLabel = matchedApp?.displayLabel ?: action.packageName
                                        mindfulLockAppAction = launchAction
                                    } else if (matchedApp?.isLocked == true) {
                                        val now = System.currentTimeMillis()
                                        val lastAuth = authManager.lastAuthenticatedTime
                                        val isBypass = lastAuth > 0L && (now - lastAuth < settingsState.appLockTimeoutMs)
                                        if (isBypass) {
                                            launchAction()
                                        } else {
                                            coroutineScope.launch {
                                                val success = authManager.authenticate(
                                                    title = matchedApp.displayLabel,
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
                                } else {
                                    executeSearchAction(context, action)
                                    onDismiss()
                                }
                            }
                        )
                    }

                    if (showDialer && !settingsState.searchBarPosition.equals("BOTTOM", ignoreCase = true)) {
                        item(key = "quick_dialer") {
                            QuickDialerCard(query = query, context = context) {
                                onDismiss()
                            }
                        }
                    }
                }

                // Sleek Arrow up indicator
                androidx.compose.animation.AnimatedVisibility(
                    visible = canScrollBackward,
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
                    visible = canScrollForward,
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
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (settingsState.searchBarPosition.equals("BOTTOM", ignoreCase = true)) {
                resultsList()
                searchBarAndOpenWith()
            } else {
                searchBarAndOpenWith()
                resultsList()
            }
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

        if (showFirstTimeAISearchHintDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showFirstTimeAISearchHintDialog = false 
                    val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                    sp.edit().putBoolean("search_ai_hint_shown", true).apply()
                },
                title = { Text("AI Search") },
                text = {
                    Text(
                        "You can search with AI directly from the search bar! Type your query and quickly launch it in ChatGPT, Gemini, Claude, or other installed assistant apps.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val sp = context.getSharedPreferences("launcher_walkthrough", Context.MODE_PRIVATE)
                            sp.edit().putBoolean("search_ai_hint_shown", true).apply()
                            showFirstTimeAISearchHintDialog = false
                        }
                    ) {
                        Text("Got It")
                    }
                }
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    activeIconPack: String?,
    isLocked: Boolean,
    onClick: () -> Unit,
    settingsRepository: SettingsRepository = koinInject()
) {
    val context = LocalContext.current
    val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
    
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

    val primaryTextColor = if (isLightBg) Color(0xFF1C1C1E) else Color.White
    val secondaryTextColor = if (isLightBg) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val dividerColor = if (isLightBg) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)
    val cardBgColor = if (isLightBg) Color.White else Color(0xFF1C1C1E)
    val themeAccentColor = remember(settingsState.orbSpaceAccentColor) {
        try {
            Color(android.graphics.Color.parseColor(settingsState.orbSpaceAccentColor))
        } catch (e: Exception) {
            Color(0xFF007AFF)
        }
    }

    // Customizable Search Result Styles
    val customCardBgColor = remember(settingsState.searchResultBgColor, settingsState.searchResultBgOpacity, cardBgColor) {
        try {
            Color(android.graphics.Color.parseColor(settingsState.searchResultBgColor))
                .copy(alpha = settingsState.searchResultBgOpacity / 100f)
        } catch (e: Exception) {
            cardBgColor
        }
    }
    val verticalPadding = settingsState.searchResultPaddingDp.dp
    val primaryTextSize = settingsState.searchResultTextSizeSp.sp
    val secondaryTextSize = (settingsState.searchResultTextSizeSp - 2).coerceAtLeast(10).sp
    val itemIconSize = (settingsState.searchResultTextSizeSp * 2.8f).coerceIn(32f, 64f).dp
    val itemInnerIconSize = (settingsState.searchResultTextSizeSp * 1.4f).coerceIn(16f, 32f).dp

    // Choose styling based on provider type
    val isCalculator = result.providerName == "Calculator"
    
    if (isCalculator) {
        // Special highlighted card for mathematical answers
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = customCardBgColor
            ),
            border = BorderStroke(1.dp, themeAccentColor),
            shape = RoundedCornerShape(settingsState.orbSpaceCardCornerRadiusDp.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(verticalPadding * 1.3f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(itemIconSize)
                        .clip(CircleShape)
                        .background(themeAccentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Math",
                        tint = Color.White,
                        modifier = Modifier.size(itemInnerIconSize)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        color = primaryTextColor,
                        fontSize = (settingsState.searchResultTextSizeSp + 6).coerceAtLeast(16).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.subtitle ?: "Calculator result (Tap to copy)",
                        color = secondaryTextColor,
                        fontSize = secondaryTextSize
                    )
                }
            }
        }
    } else {
        // Normal list items (Apps, Settings, Web Fallback)
        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, dividerColor.copy(alpha = if (settingsState.searchResultBgOpacity < 20) 0.15f else 1f), RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(horizontal = 4.dp, vertical = (settingsState.searchResultPaddingDp - 6).coerceAtLeast(0).dp),
            colors = ListItemDefaults.colors(
                containerColor = customCardBgColor,
                headlineColor = primaryTextColor,
                supportingColor = secondaryTextColor
            ),
            leadingContent = {
                val action = result.action
                when (action) {
                    is SearchResultAction.LaunchApp -> {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            val icon = rememberAppIcon(context, action.packageName, action.activityName, activeIconPack)
                            icon?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = result.title,
                                    modifier = Modifier
                                        .size(itemIconSize)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } ?: Box(
                                modifier = Modifier
                                    .size(itemIconSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(secondaryTextColor.copy(alpha = 0.2f))
                            )
                            if (isLocked) {
                                Box(
                                    modifier = Modifier
                                        .size((itemIconSize * 0.4f).coerceIn(12.dp, 24.dp))
                                        .clip(CircleShape)
                                        .background(themeAccentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.White,
                                        modifier = Modifier.size((itemInnerIconSize * 0.5f).coerceIn(8.dp, 16.dp))
                                    )
                                }
                            }
                        }
                    }
                    is SearchResultAction.OpenSetting -> {
                        Box(
                            modifier = Modifier
                                .size(itemIconSize)
                                .clip(RoundedCornerShape(8.dp))
                                .background(dividerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = primaryTextColor,
                                modifier = Modifier.size(itemInnerIconSize)
                            )
                        }
                    }
                    is SearchResultAction.CallContact -> {
                        val isQuickAction = result.id.startsWith("phone_")
                        if (isQuickAction) {
                            Box(
                                modifier = Modifier
                                    .size(itemIconSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF34C759)), // Apple Green
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(itemInnerIconSize)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(itemIconSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeAccentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = result.title.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = primaryTextSize,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is SearchResultAction.MessageContact -> {
                        Box(
                            modifier = Modifier
                                .size(itemIconSize)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF007AFF)), // Apple Blue
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "SMS",
                                tint = Color.White,
                                modifier = Modifier.size(itemInnerIconSize)
                            )
                        }
                    }
                    is SearchResultAction.OpenUrl -> {
                        val isWhatsApp = result.id.startsWith("phone_wa_") || action.url.contains("whatsapp.com")
                        if (isWhatsApp) {
                            Box(
                                modifier = Modifier
                                    .size(itemIconSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF34C759)), // WhatsApp/Green
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(itemInnerIconSize)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(itemIconSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(dividerColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Web",
                                    tint = primaryTextColor,
                                    modifier = Modifier.size(itemInnerIconSize)
                                )
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(itemIconSize)
                                .clip(RoundedCornerShape(8.dp))
                                .background(dividerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = primaryTextColor,
                                modifier = Modifier.size(itemInnerIconSize)
                            )
                        }
                    }
                }
            },
            headlineContent = {
                Text(
                    text = result.title,
                    color = primaryTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = primaryTextSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                result.subtitle?.let {
                    Text(
                        text = it,
                        color = secondaryTextColor,
                        fontSize = secondaryTextSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
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
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(action.url)).apply {
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
        is SearchResultAction.LaunchShortcut -> {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
                    launcherApps.startShortcut(action.packageName, action.shortcutId, null, null, android.os.Process.myUserHandle())
                }
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
