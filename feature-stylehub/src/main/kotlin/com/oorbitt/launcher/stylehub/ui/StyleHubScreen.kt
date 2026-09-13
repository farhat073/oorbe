package com.oorbitt.launcher.stylehub.ui

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.oorbitt.launcher.data.repository.GestureRepository
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.data.style.StyleBlobSerializer
import com.oorbitt.launcher.data.style.StyleDiff
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.stylehub.data.CloudinaryUploader
import com.oorbitt.launcher.stylehub.data.StyleHubItem
import com.oorbitt.launcher.stylehub.data.StyleHubRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// Brand Colors (Elegant Emerald and Slate Blue)
val PrimaryPurple = Color(0xFF00B894)
val SecondaryBlue = Color(0xFF0984E3)
val TertiaryPink = Color(0xFF00D2D3)
val EmeraldGreen = Color(0xFF00B894)
val RedMuted = Color(0xFFF38BA8)
val DarkBackground = Color(0xFF1E1E2E)
val CardBackground = Color(0xFF313244)
val TextPrimary = Color(0xFFCDD6F4)
val TextMuted = Color(0xFFBAC2DE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleHubScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository: StyleHubRepository = koinInject()
    val settingsRepo: SettingsRepository = koinInject()
    val gestureRepo: GestureRepository = koinInject()

    val currentSettings by settingsRepo.settings.collectAsState(initial = LauncherSettings())
    val currentGestures by gestureRepo.getAllGestureActions().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Browse, 1: My Uploads
    var approvedStyles by remember { mutableStateOf<List<StyleHubItem>>(emptyList()) }
    var myStyles by remember { mutableStateOf<List<StyleHubItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("All") }

    var selectedStyle by remember { mutableStateOf<StyleHubItem?>(null) }
    var showUploadWizard by remember { mutableStateOf(false) }

    val onLikeToggle = { style: StyleHubItem ->
        scope.launch {
            val isCurrentlyLiked = style.isLikedByUser
            val success = repository.toggleLikeStyle(style.id ?: "", isCurrentlyLiked)
            if (success) {
                val updateItem = { item: StyleHubItem ->
                    if (item.id == style.id) {
                        val diff = if (isCurrentlyLiked) -1 else 1
                        item.copy(
                            likes_count = (item.likes_count + diff).coerceAtLeast(0),
                            isLikedByUser = !isCurrentlyLiked
                        )
                    } else item
                }
                approvedStyles = approvedStyles.map(updateItem)
                myStyles = myStyles.map(updateItem)
                
                if (selectedStyle?.id == style.id) {
                    selectedStyle = selectedStyle?.copy(
                        likes_count = ((selectedStyle?.likes_count ?: 0) + (if (isCurrentlyLiked) -1 else 1)).coerceAtLeast(0)
                    )?.apply {
                        isLikedByUser = !isCurrentlyLiked
                    }
                }
            } else {
                Toast.makeText(context, "Failed to update like status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val tagsList = listOf("All", "Minimal", "Neon", "Light", "Dark", "Catppuccin")

    // Fetch lists
    val refreshLists = {
        scope.launch {
            isLoading = true
            approvedStyles = repository.getApprovedStyles()
            myStyles = repository.getMyStyles()
            isLoading = false
        }
    }

    LaunchedEffect(selectedTab) {
        refreshLists()
    }

    val filteredStyles = remember(selectedTab, approvedStyles, myStyles, searchQuery, selectedTag) {
        val baseList = if (selectedTab == 0) approvedStyles else myStyles
        baseList.filter { item ->
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                    item.author.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            val matchesTag = selectedTag == "All" || item.tags.any { it.equals(selectedTag, ignoreCase = true) }
            matchesSearch && matchesTag
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OorbStyle Marketplace",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    IconButton(onClick = { refreshLists() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    text = { Text("Share Layout", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = { showUploadWizard = true },
                    containerColor = PrimaryPurple,
                    contentColor = DarkBackground
                )
            }
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Switcher
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBackground,
                contentColor = PrimaryPurple,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryPurple
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Browse Community", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = PrimaryPurple,
                    unselectedContentColor = TextMuted
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My Shared Styles", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = PrimaryPurple,
                    unselectedContentColor = TextMuted
                )
            }

            // Search and Tag filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search styles...", color = TextMuted.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CardBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = CardBackground.copy(alpha = 0.3f),
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = CardBackground
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags Scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tagsList.forEach { tag ->
                        val selected = selectedTag == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) PrimaryPurple else CardBackground.copy(alpha = 0.6f))
                                .clickable { selectedTag = tag }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tag,
                                color = if (selected) DarkBackground else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Styles Grid
            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            } else if (filteredStyles.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No styles found",
                            color = TextMuted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredStyles) { style ->
                        StyleCard(
                            style = style,
                            onClick = { selectedStyle = style },
                            onLikeClick = { onLikeToggle(style) }
                        )
                    }
                }
            }
        }
    }

    // Details Dialog
    selectedStyle?.let { style ->
        StyleDetailsDialog(
            style = style,
            currentSettings = currentSettings,
            onDismiss = { selectedStyle = null },
            onLike = { onLikeToggle(style) },
            onApply = {
                scope.launch {
                    val blob = StyleBlobSerializer.fromJson(style.settings_json)
                    // Apply visual configuration
                    settingsRepo.updateSettings { StyleBlobSerializer.import(blob, it) }

                    // Apply gesture actions
                    val gestures = StyleBlobSerializer.importGestures(blob)
                    gestures.forEach { gestureAction ->
                        gestureRepo.setGestureAction(gestureAction)
                    }

                    // Increment download count
                    repository.incrementInstallCount(style.id ?: "")
                    
                    Toast.makeText(context, "Theme Applied Successfully!", Toast.LENGTH_SHORT).show()
                    selectedStyle = null
                    refreshLists()
                }
            },
            onFlag = { reason ->
                scope.launch {
                    val success = repository.flagStyle(style.id ?: "", reason)
                    if (success) {
                        Toast.makeText(context, "Layout Reported", Toast.LENGTH_SHORT).show()
                    }
                    selectedStyle = null
                    refreshLists()
                }
            }
        )
    }

    // Share Wizard Dialog
    if (showUploadWizard) {
        ShareStyleWizardDialog(
            currentSettings = currentSettings,
            onDismiss = { showUploadWizard = false },
            onUpload = { title, author, desc, tags, iconPack, wallpaper, screenshots,
                         shareApp, shareWork, shareDraw, shareOrb, shareSearch, shareGest, shareWell ->
                scope.launch {
                    isLoading = true
                    showUploadWizard = false
                    val blob = StyleBlobSerializer.export(
                        settings = currentSettings,
                        gestures = currentGestures,
                        iconPackName = iconPack,
                        wallpaperName = wallpaper,
                        shareAppearance = shareApp,
                        shareWorkspace = shareWork,
                        shareDrawer = shareDraw,
                        shareOrbSpace = shareOrb,
                        shareSearch = shareSearch,
                        shareGestures = shareGest,
                        shareWellness = shareWell
                    )
                    val settingsJson = StyleBlobSerializer.toJson(blob)

                    val finalScreenshots = if (screenshots.isEmpty()) {
                        listOf("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500")
                    } else {
                        screenshots
                    }

                    val success = repository.uploadStyle(
                        title = title,
                        author = author,
                        description = desc,
                        settingsJson = settingsJson,
                        screenshots = finalScreenshots,
                        tags = tags,
                        iconPackName = iconPack,
                        wallpaperName = wallpaper
                    )
                    
                    isLoading = false
                    if (success) {
                        Toast.makeText(context, "Shared successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to share theme", Toast.LENGTH_SHORT).show()
                    }
                    refreshLists()
                }
            }
        )
    }
}

@Composable
fun StyleCard(
    style: StyleHubItem,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            // Screenshot Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(DarkBackground.copy(alpha = 0.5f))
            ) {
                AsyncImage(
                    model = style.screenshots.firstOrNull() ?: "https://picsum.photos/300/400",
                    contentDescription = style.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Like overlay (Top End)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(DarkBackground.copy(alpha = 0.8f))
                        .clickable { onLikeClick() }
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = if (style.isLikedByUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (style.isLikedByUser) RedMuted else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Download/Install & Likes badges (Bottom End)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = style.install_count.toString(),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (style.isLikedByUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (style.isLikedByUser) RedMuted else TextPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = style.likes_count.toString(),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Info Area
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = style.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "by ${style.author}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    style.tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CardBackground.copy(alpha = 0.8f))
                                .border(1.dp, TextMuted.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StyleDetailsDialog(
    style: StyleHubItem,
    currentSettings: LauncherSettings,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onFlag: (String) -> Unit,
    onLike: () -> Unit
) {
    val blob = remember(style.settings_json) { StyleBlobSerializer.fromJson(style.settings_json) }
    val diffItems = remember(currentSettings, blob) { StyleDiff.compute(currentSettings, blob) }
    val pagerState = rememberPagerState(pageCount = { style.screenshots.size.coerceAtLeast(1) })
    
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                    Text(
                        text = "Layout Details",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onLike) {
                            Icon(
                                imageVector = if (style.isLikedByUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (style.isLikedByUser) RedMuted else TextPrimary
                            )
                        }
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Outlined.Flag, contentDescription = "Report", tint = RedMuted)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Screenshots Slider
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(CardBackground)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                AsyncImage(
                                    model = style.screenshots.getOrNull(page) ?: "https://picsum.photos/400/800",
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            // Indicators overlay
                            if (style.screenshots.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    repeat(style.screenshots.size) { index ->
                                        val active = pagerState.currentPage == index
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (active) PrimaryPurple else CardBackground)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Title & Description
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = style.title,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Shared by ${style.author}",
                                color = SecondaryBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${style.install_count} installs",
                                        color = TextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (style.isLikedByUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (style.isLikedByUser) RedMuted else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${style.likes_count} likes",
                                        color = TextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = style.description,
                                color = TextMuted,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Tags row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                style.tags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CardBackground)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Wallpaper & Icon Pack Details
                            if (!blob.iconPackName.isNullOrEmpty() || !blob.wallpaperName.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!blob.iconPackName.isNullOrEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📦", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Icon Pack: ${blob.iconPackName}",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        if (!blob.wallpaperName.isNullOrEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🖼️", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Wallpaper: ${blob.wallpaperName}",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Setup Difference Analysis
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = "Layout Diff Analysis",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    diffItems.forEach { diff ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = diff.propertyName,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1.2f)
                                            )
                                            
                                            if (diff.isChanged) {
                                                Row(
                                                    modifier = Modifier.weight(2f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    Text(
                                                        text = diff.currentValue,
                                                        color = RedMuted,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronRight,
                                                        contentDescription = null,
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = diff.newValue,
                                                        color = EmeraldGreen,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = diff.newValue,
                                                    color = TextMuted.copy(alpha = 0.6f),
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.weight(2f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                // Apply Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apply OorbStyle Layout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Flag/Report Dialog Overlay
            if (showReportDialog) {
                AlertDialog(
                    onDismissRequest = { showReportDialog = false },
                    title = { Text("Report Layout", color = TextPrimary) },
                    text = {
                        Column {
                            Text("Please specify why you are reporting this layout submission:", color = TextMuted, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = reportReason,
                                onValueChange = { reportReason = it },
                                placeholder = { Text("e.g. Broken links, offensive description, spam...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = CardBackground
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (reportReason.isNotEmpty()) {
                                    onFlag(reportReason)
                                    showReportDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedMuted)
                        ) {
                            Text("Submit Report", color = DarkBackground)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReportDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                    },
                    containerColor = CardBackground
                )
            }
        }
    }
}

@Composable
fun ShareStyleWizardDialog(
    currentSettings: LauncherSettings,
    onDismiss: () -> Unit,
    onUpload: (
        title: String,
        author: String,
        description: String,
        tags: List<String>,
        iconPack: String?,
        wallpaper: String?,
        screenshots: List<String>,
        shareAppearance: Boolean,
        shareWorkspace: Boolean,
        shareDrawer: Boolean,
        shareOrbSpace: Boolean,
        shareSearch: Boolean,
        shareGestures: Boolean,
        shareWellness: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var iconPack by remember { mutableStateOf(currentSettings.activeIconPack ?: "") }
    var wallpaper by remember { mutableStateOf("") }

    // Uris selected from the device
    var thumbnailUri by remember { mutableStateOf<Uri?>(null) }
    val screenshotUris = remember { mutableStateListOf<Uri>() }

    val thumbnailPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val size = getUriFileSize(context, uri)
            if (size > 2 * 1024 * 1024) {
                Toast.makeText(context, "Thumbnail exceeds 2MB limit!", Toast.LENGTH_SHORT).show()
            } else {
                thumbnailUri = uri
            }
        }
    }

    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val size = getUriFileSize(context, uri)
            
            // Calculate current total size of selected screenshots
            var currentTotalSize = 0L
            screenshotUris.forEach { u ->
                currentTotalSize += getUriFileSize(context, u)
            }
            
            if (currentTotalSize + size > 5 * 1024 * 1024) {
                Toast.makeText(context, "Total screenshots size exceeds 5MB limit!", Toast.LENGTH_SHORT).show()
            } else {
                if (screenshotUris.size >= 4) {
                    Toast.makeText(context, "Max 4 screenshots allowed!", Toast.LENGTH_SHORT).show()
                } else {
                    screenshotUris.add(uri)
                }
            }
        }
    }

    var shareAppearance by remember { mutableStateOf(true) }
    var shareWorkspace by remember { mutableStateOf(true) }
    var shareDrawer by remember { mutableStateOf(true) }
    var shareOrbSpace by remember { mutableStateOf(true) }
    var shareSearch by remember { mutableStateOf(true) }
    var shareGestures by remember { mutableStateOf(true) }
    var shareWellness by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header (Pinned)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share Your Style",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
                
                Text(
                    text = "Export your customized launcher setup to the community hub.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Layout Title") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = CardBackground,
                            focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                            unfocusedContainerColor = CardBackground.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Author Name") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = CardBackground,
                            focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                            unfocusedContainerColor = CardBackground.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = CardBackground,
                            focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                            unfocusedContainerColor = CardBackground.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = iconPack,
                            onValueChange = { iconPack = it },
                            label = { Text("Icon Pack") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = CardBackground,
                                focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                                unfocusedContainerColor = CardBackground.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = wallpaper,
                            onValueChange = { wallpaper = it },
                            label = { Text("Wallpaper") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = CardBackground,
                                focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                                unfocusedContainerColor = CardBackground.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("Minimal, Dark, Neon") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = CardBackground,
                            focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                            unfocusedContainerColor = CardBackground.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Visual Assets Card Section
                    Text(
                        text = "VISUAL ASSETS",
                        color = PrimaryPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Thumbnail Picker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cover Thumbnail", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (thumbnailUri != null) "Selected: ${thumbnailUri!!.lastPathSegment ?: "image"}" else "Required (Max 2MB)",
                                        color = if (thumbnailUri != null) EmeraldGreen else TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { thumbnailPicker.launch("image/*") },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(CardBackground, RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = if (thumbnailUri != null) Icons.Default.Edit else Icons.Default.Add,
                                        contentDescription = "Pick Thumbnail",
                                        tint = SecondaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                            // Screenshots Picker
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Screenshots (${screenshotUris.size}/4)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Max 5MB combined", color = TextMuted, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = { screenshotPicker.launch("image/*") },
                                        enabled = screenshotUris.size < 4,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (screenshotUris.size < 4) CardBackground else CardBackground.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "Add Screenshot",
                                            tint = if (screenshotUris.size < 4) SecondaryBlue else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (screenshotUris.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    screenshotUris.forEachIndexed { index, uri ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "screenshot_${index + 1}.png",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { screenshotUris.removeAt(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = RedMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Share Configurations Card Section
                    Text(
                        text = "CONFIGURATIONS TO EXPORT",
                        color = PrimaryPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            ShareCategoryRow(
                                title = "Appearance & Themes",
                                checked = shareAppearance,
                                onCheckedChange = { shareAppearance = it }
                            )
                            ShareCategoryRow(
                                title = "Workspace Grid & Dock",
                                checked = shareWorkspace,
                                onCheckedChange = { shareWorkspace = it }
                            )
                            ShareCategoryRow(
                                title = "App Drawer Spacing & Columns",
                                checked = shareDrawer,
                                onCheckedChange = { shareDrawer = it }
                            )
                            ShareCategoryRow(
                                title = "Search Bar Layout",
                                checked = shareSearch,
                                onCheckedChange = { shareSearch = it }
                            )
                            ShareCategoryRow(
                                title = "OrbSpace Dashboard",
                                checked = shareOrbSpace,
                                onCheckedChange = { shareOrbSpace = it }
                            )
                            ShareCategoryRow(
                                title = "Gesture Actions",
                                checked = shareGestures,
                                onCheckedChange = { shareGestures = it }
                            )
                            ShareCategoryRow(
                                title = "Wellness Scroll limits",
                                checked = shareWellness,
                                onCheckedChange = { shareWellness = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons (Pinned at Bottom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextMuted, fontWeight = FontWeight.Bold)
                    }

                    var isUploading by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && author.isNotEmpty() && !isUploading) {
                                isUploading = true
                                scope.launch {
                                    try {
                                        val screenshotsList = mutableListOf<String>()
                                        
                                        // 1. Upload thumbnail
                                        val thumbUrl = if (thumbnailUri != null) {
                                            uploadUriToCloudinary(context, thumbnailUri!!)
                                        } else {
                                            null
                                        }
                                        
                                        if (thumbUrl != null) {
                                            screenshotsList.add(thumbUrl)
                                        } else {
                                            screenshotsList.add("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500")
                                        }

                                        // 2. Upload screenshots
                                        screenshotUris.forEach { uri ->
                                            val url = uploadUriToCloudinary(context, uri)
                                            if (url != null) {
                                                screenshotsList.add(url)
                                            }
                                        }

                                        val tagsList = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        
                                        onUpload(
                                            title,
                                            author,
                                            description,
                                            tagsList,
                                            iconPack.ifEmpty { null },
                                            wallpaper.ifEmpty { null },
                                            screenshotsList,
                                            shareAppearance,
                                            shareWorkspace,
                                            shareDrawer,
                                            shareOrbSpace,
                                            shareSearch,
                                            shareGestures,
                                            shareWellness
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            }
                        },
                        enabled = title.isNotEmpty() && author.isNotEmpty() && !isUploading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(2f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple,
                            contentColor = DarkBackground,
                            disabledContainerColor = PrimaryPurple.copy(alpha = 0.3f),
                            disabledContentColor = TextMuted.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DarkBackground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publishing...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Publish Layout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareCategoryRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryPurple,
                checkedTrackColor = PrimaryPurple.copy(alpha = 0.4f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardBackground
            )
        )
    }
}

private suspend fun uploadUriToCloudinary(context: android.content.Context, uri: Uri): String? {
    val bitmap = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("StyleHubScreen", "Failed to load bitmap from uri: $uri", e)
        null
    } ?: return null

    return suspendCancellableCoroutine { continuation ->
        CloudinaryUploader.uploadScreenshot(bitmap) { url ->
            if (continuation.isActive) {
                continuation.resume(url)
            }
        }
    }
}

private fun getUriFileSize(context: android.content.Context, uri: Uri): Long {
    return try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            it.length
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}
