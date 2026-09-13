package com.oorbitt.launcher.orbspace

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchAggregator
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.model.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * OrbSearchSurface — the full-screen search overlay opened by tapping the Orb.
 *
 * Displays:
 * - Dim background (60% black)
 * - Bottom-positioned search bar with auto-focus
 * - Contextual suggestions when query is empty (frequent apps + clipboard actions)
 * - Live search results as user types
 * - Staggered result animations
 */
@Composable
fun OrbSearchSurface(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    searchAggregator: SearchAggregator = koinInject(),
    settingsRepository: SettingsRepository = koinInject()
) {
    val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current



    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ ->
            // Re-trigger search to pick up new results if permissions granted
            if (query.isNotEmpty()) {
                searchJob?.cancel()
                searchJob = coroutineScope.launch {
                    val searchResults = searchAggregator.search(query)
                    results = searchResults
                }
            }
        }
    )

    // Clipboard detection
    var clipboardAction by remember { mutableStateOf<ClipboardAction?>(null) }
    LaunchedEffect(Unit) {
        clipboardAction = withContext(Dispatchers.IO) {
            ClipboardDetector(context).detect()
        }
    }

    // Frequent apps
    var frequentApps by remember { mutableStateOf<List<FrequentApp>>(emptyList()) }
    LaunchedEffect(Unit) {
        frequentApps = withContext(Dispatchers.IO) {
            FrequentAppsProvider(context).getFrequentApps()
        }
    }

    // Auto-focus search bar
    LaunchedEffect(Unit) {
        delay(150) // let animation settle
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Search as user types (debounced 150ms)
    LaunchedEffect(query) {
        searchJob?.cancel()
        if (query.isBlank()) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        
        if (!hasRequestedPermissions && query.length > 1) {
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
        searchJob = coroutineScope.launch {
            delay(150) // debounce
            val searchResults = searchAggregator.search(query)
            results = searchResults
            isSearching = false
        }
    }

    // Dismiss on swipe down
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragAccumulator > 200f) {
                            keyboardController?.hide()
                            onDismiss()
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = { dragAccumulator = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0) dragAccumulator += dragAmount
                    }
                )
            }
            .clickable(interactionSource = null, indication = null) {
                // Tap on dim area dismisses
                if (query.isBlank()) {
                    keyboardController?.hide()
                    onDismiss()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            // ── Results area (scrollable, fills available space above search bar) ──
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(settingsState.searchResultsSpacingDp.dp),
                reverseLayout = true // results grow upward from search bar
            ) {
                if (query.isBlank()) {
                    // ── Empty state: Contextual suggestions ──────────────────────

                    // Clipboard action card
                    clipboardAction?.let { action ->
                        if (action !is ClipboardAction.PlainText) {
                            item(key = "clipboard") {
                                ClipboardSuggestionCard(
                                    action = action,
                                    context = context,
                                    onDismiss = { clipboardAction = null }
                                )
                            }
                        }
                    }

                    // Frequent apps section
                    if (frequentApps.isNotEmpty()) {
                        item(key = "frequent_apps") {
                            FrequentAppsRow(
                                apps = frequentApps,
                                context = context,
                                onAppClick = { app ->
                                    val intent = context.packageManager
                                        .getLaunchIntentForPackage(app.packageName)
                                    if (intent != null) {
                                        context.startActivity(intent)
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }

                    // Hint text
                    item(key = "hint") {
                        Text(
                            text = "Search apps, contacts, settings…",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                } else {
                    // ── Search results ───────────────────────────────────────────
                    itemsIndexed(
                        items = results,
                        key = { _, result -> result.id }
                    ) { index, result ->
                        // Staggered entry animation
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(index * 40L)
                            visible = true
                        }

                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn(animationSpec = tween(200))
                        ) {
                            OrbSearchResultItem(
                                result = result,
                                onAction = { handleSearchAction(it, context, onDismiss) }
                            )
                        }
                    }

                    // Loading indicator
                    if (isSearching) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White.copy(alpha = 0.5f),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }

            // ── Search bar (bottom-positioned) ──────────────────────────────────
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onClear = {
                    query = ""
                    results = emptyList()
                },
                onDismiss = {
                    keyboardController?.hide()
                    onDismiss()
                },
                focusRequester = focusRequester,
                modifier = Modifier.padding(horizontal = settingsState.searchBarPaddingDp.dp, vertical = (settingsState.searchBarPaddingDp * 0.75f).toInt().dp)
            )
        }
    }
}

// ── Search bar composable ────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Search",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(Color.White.copy(alpha = 0.7f)),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Search…",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Clipboard suggestion card ────────────────────────────────────────────────

@Composable
private fun ClipboardSuggestionCard(
    action: ClipboardAction,
    context: Context,
    onDismiss: () -> Unit
) {
    val (emoji, label, description) = when (action) {
        is ClipboardAction.OpenUrl -> Triple("🔗", "Open URL", action.url)
        is ClipboardAction.CallNumber -> Triple("📞", "Call number", action.number)
        is ClipboardAction.SendEmail -> Triple("✉️", "Send email", action.email)
        is ClipboardAction.OpenMap -> Triple("📍", "Open in Maps", action.address)
        is ClipboardAction.MathExpression -> Triple("🧮", "= ${action.result}", action.expression)
        is ClipboardAction.PlainText -> return // shouldn't reach here
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6C5CE7).copy(alpha = 0.2f),
                        Color(0xFFA29BFE).copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Color(0xFF6C5CE7).copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                handleClipboardAction(action, context)
                onDismiss()
            }
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "📋 Clipboard",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp
            )
        }
    }
}

// ── Frequent apps row ────────────────────────────────────────────────────────

@Composable
private fun FrequentAppsRow(
    apps: List<FrequentApp>,
    context: Context,
    onAppClick: (FrequentApp) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Frequent",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                FrequentAppChip(app = app, context = context, onClick = { onAppClick(app) })
            }
        }
    }
}

@Composable
private fun FrequentAppChip(
    app: FrequentApp,
    context: Context,
    onClick: () -> Unit
) {
    var iconBitmap by remember(app.packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(app.packageName) {
        iconBitmap = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
                    .toBitmap(48, 48)
                    .asImageBitmap()
            } catch (e: Exception) { null }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = iconBitmap!!,
                    contentDescription = app.label,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 56.dp)
        )
    }
}

// ── Action handlers ──────────────────────────────────────────────────────────

private fun handleSearchAction(result: SearchResult, context: Context, onDismiss: () -> Unit) {
    try {
        when (val action = result.action) {
            is SearchResultAction.LaunchApp -> {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(action.packageName, action.activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                context.startActivity(intent)
                onDismiss()
            }
            is SearchResultAction.OpenUrl -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                context.startActivity(intent)
                onDismiss()
            }
            is SearchResultAction.OpenFile -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(action.uri), action.mimeType)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
                onDismiss()
            }
            is SearchResultAction.ShowAnswer -> {
                // Copy to clipboard
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("result", action.answer)
                )
            }
            is SearchResultAction.OpenSetting -> {
                val intent = Intent(action.settingAction)
                context.startActivity(intent)
                onDismiss()
            }
            is SearchResultAction.CallContact -> {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.phoneNumber}"))
                context.startActivity(intent)
                onDismiss()
            }
            is SearchResultAction.MessageContact -> {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${action.contactUri}"))
                context.startActivity(intent)
                onDismiss()
            }
            is SearchResultAction.LaunchShortcut -> {
                onDismiss()
            }
            is SearchResultAction.ExecuteCommand -> {
                onDismiss()
            }
        }
    } catch (e: Exception) {
        // Silently handle any launch failures
    }
}

private fun handleClipboardAction(action: ClipboardAction, context: Context) {
    try {
        when (action) {
            is ClipboardAction.OpenUrl -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(action.url)))
            }
            is ClipboardAction.CallNumber -> {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.number}")))
            }
            is ClipboardAction.SendEmail -> {
                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${action.email}")))
            }
            is ClipboardAction.OpenMap -> {
                val encodedAddress = Uri.encode(action.address)
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedAddress")))
            }
            is ClipboardAction.MathExpression -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("result", action.result)
                )
            }
            is ClipboardAction.PlainText -> { /* no-op */ }
        }
    } catch (e: Exception) {
        // Silently handle
    }
}
