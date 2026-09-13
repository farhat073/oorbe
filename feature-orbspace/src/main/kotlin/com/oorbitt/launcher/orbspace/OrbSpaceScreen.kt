package com.oorbitt.launcher.orbspace

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.security.AuthManager
import androidx.compose.material.icons.filled.Fingerprint
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.model.AppInfo
import org.koin.compose.koinInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ── Data models ──────────────────────────────────────────────────────────────

private data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Persistence helpers (SharedPreferences) ──────────────────────────────────

private const val ORBSPACE_PREFS = "orbspace_data"
private const val KEY_MEMO = "quick_memo"
private const val KEY_REMINDERS = "reminders_json"

private fun loadMemo(context: Context): String {
    return context.getSharedPreferences(ORBSPACE_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_MEMO, "") ?: ""
}

private fun saveMemo(context: Context, text: String) {
    context.getSharedPreferences(ORBSPACE_PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_MEMO, text).apply()
}

private fun loadReminders(context: Context): List<ReminderItem> {
    val json = context.getSharedPreferences(ORBSPACE_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_REMINDERS, "[]") ?: "[]"
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ReminderItem(
                id = obj.optString("id", UUID.randomUUID().toString()),
                title = obj.getString("title"),
                completed = obj.optBoolean("completed", false),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveReminders(context: Context, items: List<ReminderItem>) {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("completed", item.completed)
            put("createdAt", item.createdAt)
        })
    }
    context.getSharedPreferences(ORBSPACE_PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_REMINDERS, arr.toString()).apply()
}

// ── Accessibility service check ──────────────────────────────────────────────

private fun isScrollTrackerEnabled(context: Context): Boolean {
    val expectedComponent = ComponentName(context, ScrollTrackerService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponent = ComponentName.unflattenFromString(componentNameString)
        if (enabledComponent != null && enabledComponent == expectedComponent) {
            return true
        }
    }
    return false
}

private data class OrbSpaceColors(
    val isLightBg: Boolean,
    val base: Color,
    val secondary: Color,
    val tertiary: Color,
    val subtleBorder: Color,
    val cardBgStart: Color,
    val cardBgEnd: Color,
    val divider: Color,
    val accent: Color,
    val alertBg: Color,
    val textButtonContent: Color,
    val sfProgress: Color,
    val feedProgress: Color
)

private val LocalOrbSpaceColors = staticCompositionLocalOf<OrbSpaceColors> {
    error("No OrbSpaceColors provided")
}

// ── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun OrbSpaceScreen(
    modifier: Modifier = Modifier,
    isPageVisible: Boolean = true,
    settingsRepository: SettingsRepository = koinInject(),
    authManager: AuthManager = koinInject()
) {
    val context = LocalContext.current
    val settingsState by settingsRepository.settings.collectAsState(initial = LauncherSettings())
    val todayStats by ScrollTrackerService.todayStats.collectAsState()
    val todayShortFormStats by ScrollTrackerService.todayShortFormStats.collectAsState()
    val isServiceRunning by ScrollTrackerService.isServiceRunning.collectAsState()
    var isAccessibilityEnabled by remember { mutableStateOf(isScrollTrackerEnabled(context)) }
    
    val coroutineScope = rememberCoroutineScope()
    var isUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(isPageVisible) {
        if (!isPageVisible) {
            isUnlocked = false
        } else if (settingsState.lockOrbSpace && !isUnlocked) {
            val success = authManager.authenticate(
                title = "OrbSpace Lock",
                subtitle = "Verify identity to access OrbSpace"
            )
            if (success) {
                isUnlocked = true
            }
        }
    }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isScrollTrackerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // We consider it enabled if either the setting is on or the service is actively running
    val isServiceEnabled = isAccessibilityEnabled || isServiceRunning

    LaunchedEffect(Unit) {
        ScrollTrackerService.loadFromPrefs(context)
    }

    var showWeeklyDialog by remember { mutableStateOf(false) }
    val weeklyBreakdown = remember(showWeeklyDialog) {
        if (showWeeklyDialog) {
            ScrollTrackerService.getWeeklyBreakdown(context)
        } else {
            emptyList<DailyScrollSummary>()
        }
    }

    // Memo state
    var memoText by remember { mutableStateOf(loadMemo(context)) }

    // Reminders state
    var reminders by remember { mutableStateOf(loadReminders(context)) }
    var newReminderText by remember { mutableStateOf("") }
    var showAddReminder by remember { mutableStateOf(false) }

    // ── Drawer-matching visual properties ────────────────────────────────────
    val bgColor = remember(settingsState.drawerBgColor, settingsState.drawerBgOpacity) {
        val parsed = try {
            Color(android.graphics.Color.parseColor(settingsState.drawerBgColor))
        } catch (e: Exception) {
            Color.Black
        }
        parsed.copy(alpha = settingsState.drawerBgOpacity / 100f)
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

    val totalScrollsToday = todayStats.values.sum()
    val isLimitExceeded = settingsState.dailyScrollLimit > 0 && totalScrollsToday > settingsState.dailyScrollLimit

    val colors = remember(isLightBg, settingsState.orbSpaceAccentColor, isLimitExceeded) {
        val base = if (isLightBg) Color(0xFF1C1B1F) else Color.White
        val parsedAccent = try {
            Color(android.graphics.Color.parseColor(settingsState.orbSpaceAccentColor))
        } catch (e: Exception) {
            if (isLightBg) Color(0xFF007AFF) else Color(0xFF0A84FF) // Apple Blue default
        }
        val progressColor = if (isLimitExceeded) Color(0xFFFF3B30) else parsedAccent
        val feedColor = if (isLimitExceeded) Color(0xFFFF453A) else (if (isLightBg) Color(0xFF34C759) else Color(0xFF30D158))
        OrbSpaceColors(
            isLightBg = isLightBg,
            base = base,
            secondary = base.copy(alpha = 0.6f),
            tertiary = base.copy(alpha = 0.35f),
            subtleBorder = base.copy(alpha = 0.12f),
            cardBgStart = base.copy(alpha = 0.12f),
            cardBgEnd = base.copy(alpha = 0.04f),
            divider = base.copy(alpha = 0.08f),
            accent = progressColor,
            alertBg = if (isLightBg) Color(0xFFFFFFFF) else Color(0xFF1E1E1E),
            textButtonContent = progressColor,
            sfProgress = progressColor,
            feedProgress = feedColor
        )
    }

    val containerShape = remember(
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

    val spaceDp = settingsState.drawerSpaceDp

    CompositionLocalProvider(LocalOrbSpaceColors provides colors) {
        // ── Content (no manual swipe — pager handles it) ─────────────────────────
        Box(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (settingsState.lockOrbSpace && !isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spaceDp.dp)
                        .clip(containerShape)
                        .background(bgColor)
                        .clickable {
                            coroutineScope.launch {
                                val success = authManager.authenticate(
                                    title = "OrbSpace Lock",
                                    subtitle = "Verify identity to access OrbSpace"
                                )
                                if (success) {
                                    isUnlocked = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint",
                            tint = colors.accent,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "OrbSpace Locked",
                            color = colors.base,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to unlock using biometrics",
                            color = colors.secondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // ── Container matching the drawer's card style ───────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spaceDp.dp)
                        .clip(containerShape)
                        .background(bgColor)
                ) {
            // Background image (matching drawer)
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
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = settingsState.drawerBgOpacity / 100f
                    )
                }
            }

            // ── Dashboard content ────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(settingsState.orbSpaceSpacingDp.dp),
                verticalArrangement = Arrangement.spacedBy(settingsState.orbSpaceSpacingDp.dp)
            ) {
                // ── Header ───────────────────────────────────────────────────
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "OrbSpace",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.base
                        )
                        Text(
                            text = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                                .format(Date()),
                            fontSize = 13.sp,
                            color = colors.secondary
                        )
                    }
                }

                // ── Digital Hygiene Card ─────────────────────────────────────
                item(key = "hygiene") {
                    val cardCornerRadius = settingsState.orbSpaceCardCornerRadiusDp
                    
                    if (!isServiceEnabled) {
                        DashboardCard(
                            title = "Digital Hygiene",
                            cornerRadiusDp = cardCornerRadius
                        ) {
                            Text(
                                text = "Enable the accessibility service to track scroll activity across apps.",
                                color = colors.secondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.subtleBorder
                                )
                            ) {
                                Text(
                                    "Enable Accessibility Service",
                                    color = colors.base,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else if (todayStats.isEmpty()) {
                        DashboardCard(
                            title = "Digital Hygiene",
                            cornerRadiusDp = cardCornerRadius
                        ) {
                            Text(
                                text = "No scrolls tracked today. Open your social apps and start browsing.",
                                color = colors.secondary,
                                fontSize = 13.sp
                             )
                        }
                    } else {
                        val totalScrollsToday = todayStats.values.sum()
                        val totalSfScrollsToday = todayShortFormStats.values.sum()
                        val totalFeedScrollsToday = (totalScrollsToday - totalSfScrollsToday).coerceAtLeast(0)
                        
                        when (settingsState.orbSpaceLayoutMode) {
                            "APPLE_HEALTH" -> {
                                DashboardCard(
                                    title = "Digital Hygiene",
                                    cornerRadiusDp = cardCornerRadius,
                                    trailingAction = {
                                        TextButton(
                                            onClick = { showWeeklyDialog = true },
                                            colors = ButtonDefaults.textButtonColors(contentColor = colors.textButtonContent),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Details →", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                ) {
                                    if (isLimitExceeded) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                                .background(Color(0xFFFF3B30).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("⚠️", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Daily limit passed! You have scrolled $totalScrollsToday times today (Limit: ${settingsState.dailyScrollLimit}).",
                                                    color = Color(0xFFFF453A),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(130.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.size(120.dp)) {
                                                val strokeWidth = 12.dp.toPx()
                                                val diameter = size.minDimension - strokeWidth
                                                val center = Offset(size.width / 2f, size.height / 2f)
                                                
                                                // Track 1 (Outer - Feed)
                                                drawCircle(
                                                    color = colors.feedProgress.copy(alpha = 0.08f),
                                                    radius = diameter / 2f,
                                                    center = center,
                                                    style = Stroke(width = strokeWidth)
                                                )
                                                // Arc 1 (Outer - Feed)
                                                val feedTarget = 500f
                                                val feedPercent = totalFeedScrollsToday.toFloat() / feedTarget
                                                drawArc(
                                                    color = colors.feedProgress,
                                                    startAngle = -90f,
                                                    sweepAngle = (feedPercent * 360f).coerceIn(0f, 360f),
                                                    useCenter = false,
                                                    topLeft = Offset(center.x - diameter / 2f, center.y - diameter / 2f),
                                                    size = Size(diameter, diameter),
                                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                                )

                                                // Track 2 (Inner - Short-form)
                                                val innerDiameter = diameter - strokeWidth * 2.4f
                                                drawCircle(
                                                    color = colors.sfProgress.copy(alpha = 0.08f),
                                                    radius = innerDiameter / 2f,
                                                    center = center,
                                                    style = Stroke(width = strokeWidth)
                                                )
                                                // Arc 2 (Inner - Short-form)
                                                val sfTarget = 500f
                                                val sfPercent = totalSfScrollsToday.toFloat() / sfTarget
                                                drawArc(
                                                    color = colors.sfProgress,
                                                    startAngle = -90f,
                                                    sweepAngle = (sfPercent * 360f).coerceIn(0f, 360f),
                                                    useCenter = false,
                                                    topLeft = Offset(center.x - innerDiameter / 2f, center.y - innerDiameter / 2f),
                                                    size = Size(innerDiameter, innerDiameter),
                                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                                )
                                            }
                                            
                                            // Center display
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$totalScrollsToday",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = colors.base
                                                )
                                                Text(
                                                    text = "total",
                                                    fontSize = 10.sp,
                                                    color = colors.secondary
                                                )
                                            }
                                        }
                                        
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.sfProgress))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Short-form", color = colors.base, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Text("$totalSfScrollsToday scrolls", color = colors.secondary, fontSize = 13.sp, modifier = Modifier.padding(start = 14.dp))
                                            }
                                            HorizontalDivider(color = colors.divider)
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.feedProgress))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Feed scrolls", color = colors.base, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Text("$totalFeedScrollsToday scrolls", color = colors.secondary, fontSize = 13.sp, modifier = Modifier.padding(start = 14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            "COMPACT_LIST" -> {
                                DashboardCard(
                                    title = "Digital Hygiene",
                                    cornerRadiusDp = cardCornerRadius,
                                    trailingAction = {
                                        TextButton(
                                            onClick = { showWeeklyDialog = true },
                                            colors = ButtonDefaults.textButtonColors(contentColor = colors.textButtonContent),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Details →", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                ) {
                                    if (isLimitExceeded) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                                .background(Color(0xFFFF3B30).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("⚠️", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Daily limit passed! You have scrolled $totalScrollsToday times today (Limit: ${settingsState.dailyScrollLimit}).",
                                                    color = Color(0xFFFF453A),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Total Activity", color = colors.base, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            Text("$totalScrollsToday", color = colors.base, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        val sfRatio = if (totalScrollsToday > 0) totalSfScrollsToday.toFloat() / totalScrollsToday.toFloat() else 0f
                                        val feedRatio = if (totalScrollsToday > 0) totalFeedScrollsToday.toFloat() / totalScrollsToday.toFloat() else 0f
                                        
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Short-form", color = colors.secondary, fontSize = 12.sp)
                                                Text("$totalSfScrollsToday (${(sfRatio * 100).toInt()}%)", color = colors.base, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            LinearProgressIndicator(
                                                progress = { sfRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = colors.sfProgress,
                                                trackColor = colors.divider
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Feed scrolls", color = colors.secondary, fontSize = 12.sp)
                                                Text("$totalFeedScrollsToday (${(feedRatio * 100).toInt()}%)", color = colors.base, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            LinearProgressIndicator(
                                                progress = { feedRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = colors.feedProgress,
                                                trackColor = colors.divider
                                            )
                                        }
                                    }
                                }
                            }
                            else -> { // "BENTO"
                                val bentoCardBg = if (colors.isLightBg) Color(0xFFF2F2F7) else Color(0xFF2C2C2E)
                                val bentoBorder = if (colors.isLightBg) Color(0xFFE5E5EA) else Color(0xFF3A3A3C)
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(settingsState.orbSpaceSpacingDp.dp)
                                ) {
                                    // Total Bento Card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(cardCornerRadius.dp))
                                            .background(bentoCardBg)
                                            .border(1.dp, bentoBorder, RoundedCornerShape(cardCornerRadius.dp))
                                            .clickable { showWeeklyDialog = true }
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Total Today", color = colors.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("$totalScrollsToday scrolls", color = colors.base, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                            }
                                            Text("Details →", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    // Row with 2 Bento Tiles
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(settingsState.orbSpaceSpacingDp.dp)
                                    ) {
                                        // Short-form Tile
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(cardCornerRadius.dp))
                                                .background(bentoCardBg)
                                                .border(1.dp, bentoBorder, RoundedCornerShape(cardCornerRadius.dp))
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.sfProgress))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Short-form", color = colors.secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("$totalSfScrollsToday", color = colors.base, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                        
                                        // Feed Tile
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(cardCornerRadius.dp))
                                                .background(bentoCardBg)
                                                .border(1.dp, bentoBorder, RoundedCornerShape(cardCornerRadius.dp))
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.feedProgress))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Feed", color = colors.secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("$totalFeedScrollsToday", color = colors.base, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Mindful App Lock Card ─────────────────────────────────────
                item(key = "mindful_lock") {
                    val appRepository: com.oorbitt.launcher.data.repository.AppRepository = koinInject()
                    val allApps by appRepository.allApps.collectAsState(initial = emptyList())
                    
                    var showLockAppDialog by remember { mutableStateOf(false) }
                    var showFocusLockWalkthroughDialog by remember { mutableStateOf(false) }
                    
                    // Trigger a recomposition every second to update countdown timers!
                    var ticks by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(1000L)
                            ticks++
                        }
                    }
                    
                    val lockedApps = remember(ticks) {
                        com.oorbitt.launcher.security.MindfulLockManager.getLockedApps(context)
                            .filter { it.value > System.currentTimeMillis() }
                    }
                    
                    val emergencyUnlockedApps = remember(ticks) {
                        com.oorbitt.launcher.security.MindfulLockManager.getEmergencyUnlockedApps(context)
                            .filter { it.value > System.currentTimeMillis() }
                    }
                    
                    DashboardCard(
                        title = "Focus Lock",
                        cornerRadiusDp = settingsState.orbSpaceCardCornerRadiusDp,
                        trailingAction = {
                            IconButton(
                                onClick = {
                                    val shown = context.getSharedPreferences("orbspace_data", Context.MODE_PRIVATE)
                                        .getBoolean("focus_lock_walkthrough_shown", false)
                                    if (shown) {
                                        showLockAppDialog = true
                                    } else {
                                        showFocusLockWalkthroughDialog = true
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Lock",
                                    tint = colors.accent
                                )
                            }
                        }
                    ) {
                        if (lockedApps.isEmpty()) {
                            Text(
                                text = "No apps locked. Use Focus Lock to restrict access to distracting apps and boost productivity.",
                                color = colors.secondary,
                                fontSize = 13.sp
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                lockedApps.forEach { (packageName, expiryTime) ->
                                    val matchedApp = allApps.find { it.packageName == packageName }
                                    val label = matchedApp?.displayLabel ?: packageName
                                    
                                    val appIcon = rememberAppIcon(context, packageName)
                                    
                                    val remainingMs = expiryTime - System.currentTimeMillis()
                                    val remainingStr = run {
                                        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(remainingMs)
                                        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
                                        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
                                        if (hours > 0) "${hours}h ${minutes}m" else if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
                                    }
                                    
                                    val emergencyExpiry = emergencyUnlockedApps[packageName] ?: 0L
                                    val isEmergencyActive = emergencyExpiry > System.currentTimeMillis()
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (appIcon != null) {
                                                Image(
                                                    bitmap = appIcon,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(colors.subtleBorder),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label.take(1), color = colors.base, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = label,
                                                    color = colors.base,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (isEmergencyActive) {
                                                    val emRemainingMs = emergencyExpiry - System.currentTimeMillis()
                                                    val emSecs = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(emRemainingMs)
                                                    Text(
                                                        text = "Emergency unlock: ${emSecs / 60}:${"%02d".format(emSecs % 60)} left",
                                                        color = Color(0xFF34C759),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else {
                                                    Text(
                                                        text = "$remainingStr remaining",
                                                        color = colors.secondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                        
                                        if (!isEmergencyActive) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Emergency Unlock",
                                                    color = colors.secondary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Button(
                                                        onClick = {
                                                            com.oorbitt.launcher.security.MindfulLockManager.triggerEmergencyUnlock(context, packageName, 5 * 60 * 1000L)
                                                            ticks++
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = colors.subtleBorder
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("5m", color = colors.base, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            com.oorbitt.launcher.security.MindfulLockManager.triggerEmergencyUnlock(context, packageName, 10 * 60 * 1000L)
                                                            ticks++
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = colors.subtleBorder
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("10m", color = colors.base, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF34C759).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0xFF34C759).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Unlocked", color = Color(0xFF34C759), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (showFocusLockWalkthroughDialog) {
                        AlertDialog(
                            onDismissRequest = { showFocusLockWalkthroughDialog = false },
                            title = {
                                Text("About Focus Lock", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Focus Lock helps you restrict distracting applications to keep you fully productive.",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "• Apps will be locked immediately for the duration you set.\n" +
                                               "• Standard bypasses are blocked during this time.\n" +
                                               "• If you need urgent access, you can use the 'Emergency Unlock' options directly from the block overlay or settings.\n" +
                                               "• Use focus lock responsibly to build healthy digital habits.",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        context.getSharedPreferences("orbspace_data", Context.MODE_PRIVATE)
                                            .edit()
                                            .putBoolean("focus_lock_walkthrough_shown", true)
                                            .apply()
                                        showFocusLockWalkthroughDialog = false
                                        showLockAppDialog = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                                ) {
                                    Text("Got It, Continue", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showFocusLockWalkthroughDialog = false }) {
                                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                                }
                            },
                            containerColor = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    if (showLockAppDialog) {
                        var selectedPkg by remember { mutableStateOf<String?>(null) }
                        var lockDurationMinutes by remember { mutableStateOf(60) } // Default 1 hour
                        
                        AlertDialog(
                            onDismissRequest = { showLockAppDialog = false },
                            title = {
                                Text("New Focus Lock", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 350.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("Select app and duration to restrict access. You will not be able to bypass it except via emergency unlocks.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                    
                                    // App list dropdown / selector
                                    var showAppList by remember { mutableStateOf(false) }
                                    val selectedAppLabel = allApps.find { it.packageName == selectedPkg }?.displayLabel ?: "Select App"
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .clickable { showAppList = !showAppList }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(selectedAppLabel, color = Color.White, fontSize = 14.sp)
                                            Text("▼", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                    }
                                    
                                    if (showAppList) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        ) {
                                            items(allApps.filter { !it.packageName.contains("launcher") && !it.packageName.contains("android") }) { app ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedPkg = app.packageName
                                                            showAppList = false
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val appIcon = rememberAppIcon(context, app.packageName)
                                                    if (appIcon != null) {
                                                        Image(appIcon, null, modifier = Modifier.size(24.dp).clip(CircleShape))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    Text(app.displayLabel, color = Color.White, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Duration slider or selection
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val labelText = when {
                                            lockDurationMinutes < 60 -> "$lockDurationMinutes mins"
                                            lockDurationMinutes % 60 == 0 -> "${lockDurationMinutes / 60} hours"
                                            else -> "${lockDurationMinutes / 60}h ${lockDurationMinutes % 60}m"
                                        }
                                        Text("Duration: $labelText", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Slider(
                                            value = lockDurationMinutes.toFloat(),
                                            onValueChange = { lockDurationMinutes = ((it / 15).toInt() * 15).coerceAtLeast(15) },
                                            valueRange = 15f..1440f, // 15 mins to 24 hours
                                            colors = SliderDefaults.colors(
                                                thumbColor = colors.accent,
                                                activeTrackColor = colors.accent
                                            )
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        selectedPkg?.let { pkg ->
                                            com.oorbitt.launcher.security.MindfulLockManager.lockApp(
                                                context,
                                                pkg,
                                                lockDurationMinutes * 60 * 1000L
                                            )
                                        }
                                        showLockAppDialog = false
                                        ticks++
                                    },
                                    enabled = selectedPkg != null,
                                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                                ) {
                                    Text("Lock App", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLockAppDialog = false }) {
                                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                                }
                            },
                            containerColor = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // ── Quick Memo Card ──────────────────────────────────────────
                item(key = "memo") {
                    val memoBgColor = if (colors.isLightBg) Color(0xFFFFFDF9) else Color(0xFF1E1C19)
                    val memoBorderColor = if (colors.isLightBg) Color(0xFFE5DDCB) else Color(0xFF332F2A)
                    val notesAccentColor = Color(0xFFE5A93C) // Warm Apple Notes Orange
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(settingsState.orbSpaceCardCornerRadiusDp.dp))
                            .background(memoBgColor)
                            .border(1.dp, memoBorderColor, RoundedCornerShape(settingsState.orbSpaceCardCornerRadiusDp.dp))
                    ) {
                        // Notepad top strip indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(notesAccentColor)
                        )
                        
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Quick Memo",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.base,
                                    letterSpacing = 0.4.sp
                                )
                                if (memoText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            memoText = ""
                                            saveMemo(context, "")
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = colors.tertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            
                            BasicTextField(
                                value = memoText,
                                onValueChange = { newValue ->
                                    memoText = newValue
                                    saveMemo(context, newValue)
                                },
                                textStyle = TextStyle(
                                    color = colors.base,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                ),
                                cursorBrush = SolidColor(notesAccentColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp, max = 220.dp),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (memoText.isEmpty()) {
                                            Text(
                                                "Jot down thoughts, ideas, quick notes…",
                                                color = colors.tertiary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                // ── Reminders Card ───────────────────────────────────────────
                item(key = "reminders_header") {
                    DashboardCard(
                        title = "Reminders",
                        cornerRadiusDp = settingsState.orbSpaceCardCornerRadiusDp,
                        trailingAction = {
                            IconButton(
                                onClick = { showAddReminder = !showAddReminder },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        colors.divider,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add reminder",
                                    tint = colors.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.animateContentSize()
                        ) {
                            // Add new reminder input
                            if (showAddReminder) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .background(
                                            colors.divider,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = newReminderText,
                                        onValueChange = { newReminderText = it },
                                        textStyle = TextStyle(
                                            color = colors.base,
                                            fontSize = 14.sp
                                        ),
                                        cursorBrush = SolidColor(colors.secondary),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        decorationBox = { innerTextField ->
                                            Box {
                                                if (newReminderText.isEmpty()) {
                                                    Text(
                                                        "Add a reminder…",
                                                        color = colors.tertiary,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (newReminderText.isNotBlank()) {
                                                val newItem = ReminderItem(title = newReminderText.trim())
                                                reminders = reminders + newItem
                                                saveReminders(context, reminders)
                                                newReminderText = ""
                                                showAddReminder = false
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Save",
                                            tint = if (colors.isLightBg) Color(0xFF2E7D32) else Color(0xFF4ADE80),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (reminders.isEmpty()) {
                                Text(
                                    "No reminders yet. Tap + to add one.",
                                    color = colors.tertiary,
                                    fontSize = 13.sp
                                )
                            } else {
                                // Active reminders first, then completed
                                val sorted = reminders.sortedWith(
                                    compareBy<ReminderItem> { it.completed }
                                        .thenByDescending { it.createdAt }
                                )
                                sorted.forEach { item ->
                                    ReminderRow(
                                        item = item,
                                        onToggle = {
                                            reminders = reminders.map {
                                                if (it.id == item.id) it.copy(completed = !it.completed)
                                                else it
                                            }
                                            saveReminders(context, reminders)
                                        },
                                        onDelete = {
                                            reminders = reminders.filter { it.id != item.id }
                                            saveReminders(context, reminders)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom spacer
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        if (showWeeklyDialog) {
            AlertDialog(
                onDismissRequest = { showWeeklyDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scroll Statistics",
                            color = colors.base,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showWeeklyDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.secondary
                            )
                        }
                    }
                },
                text = {
                    var expandedDays by remember { mutableStateOf(setOf<String>()) }
                    var selectedTab by remember { mutableStateOf(0) } // 0 = Today, 1 = History
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Custom Tab Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.divider)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Today", "History").forEachIndexed { index, label ->
                                val isSelected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.subtleBorder else Color.Transparent)
                                        .clickable { selectedTab = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) colors.base else colors.secondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Tab Content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (selectedTab == 0) {
                                    // Today Breakdown
                                    if (todayStats.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No scroll activity recorded today.",
                                                    color = colors.tertiary,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    } else {
                                        items(todayStats.entries.sortedByDescending { it.value }.toList()) { (pkg, count) ->
                                            val sfCount = todayShortFormStats[pkg] ?: 0
                                            val feedCount = (count - sfCount).coerceAtLeast(0)
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(colors.cardBgStart)
                                                    .border(1.dp, colors.subtleBorder, RoundedCornerShape(16.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AppIconMonogram(packageName = pkg)
                                                
                                                Spacer(modifier = Modifier.width(12.dp))
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = ScrollTrackerService.friendlyName(pkg),
                                                        color = colors.base,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.9f)
                                                            .height(5.dp)
                                                            .clip(RoundedCornerShape(2.5.dp))
                                                            .background(colors.divider)
                                                    ) {
                                                        val sfRatio = sfCount.toFloat() / count.coerceAtLeast(1)
                                                        val feedRatio = feedCount.toFloat() / count.coerceAtLeast(1)
                                                        if (sfRatio > 0f) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxHeight()
                                                                    .weight(sfRatio)
                                                                    .background(colors.sfProgress)
                                                            )
                                                        }
                                                        if (feedRatio > 0f) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxHeight()
                                                                    .weight(feedRatio)
                                                                    .background(colors.feedProgress)
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "$count",
                                                        color = colors.base,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = if (sfCount > 0) "$sfCount shorts" else "feed",
                                                        color = colors.secondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // History Breakdown
                                    if (weeklyBreakdown.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No history data available.",
                                                    color = colors.tertiary,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    } else {
                                        // 7-day visual bar chart card at the top
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(colors.cardBgStart)
                                                    .border(1.dp, colors.subtleBorder, RoundedCornerShape(16.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "Last 7 Days Visual Breakdown",
                                                    color = colors.base,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                )
                                                
                                                val maxScrolls = weeklyBreakdown.maxOfOrNull { it.totalScrolls }?.coerceAtLeast(1) ?: 1
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(110.dp)
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    weeklyBreakdown.forEach { summary ->
                                                        val barHeightRatio = summary.totalScrolls.toFloat() / maxScrolls
                                                        val sfRatio = if (summary.totalScrolls > 0) summary.totalShortFormScrolls.toFloat() / summary.totalScrolls else 0f
                                                        val feedRatio = 1f - sfRatio
                                                        
                                                        val dayName = try {
                                                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(summary.date)
                                                            if (date != null) {
                                                                SimpleDateFormat("E", Locale.getDefault()).format(date).take(3)
                                                            } else ""
                                                        } catch (e: Exception) { "" }

                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.Bottom
                                                        ) {
                                                            if (summary.totalScrolls > 0) {
                                                                Text(
                                                                    text = "${summary.totalScrolls}",
                                                                    color = colors.base,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                                )
                                                            } else {
                                                                Spacer(modifier = Modifier.height(12.dp))
                                                            }
                                                            
                                                            Box(
                                                                modifier = Modifier
                                                                    .height(65.dp * barHeightRatio)
                                                                    .width(12.dp)
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(colors.divider),
                                                                contentAlignment = Alignment.BottomCenter
                                                            ) {
                                                                if (summary.totalScrolls > 0) {
                                                                    Column(modifier = Modifier.fillMaxSize()) {
                                                                        if (sfRatio > 0f) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .weight(sfRatio)
                                                                                    .background(colors.sfProgress)
                                                                            )
                                                                        }
                                                                        if (feedRatio > 0f) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .weight(feedRatio)
                                                                                    .background(colors.feedProgress)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = dayName,
                                                                color = colors.secondary,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        // Expandable days list
                                        items(weeklyBreakdown) { summary ->
                                            val isExpanded = expandedDays.contains(summary.date)
                                            val friendlyDate = formatFriendlyDate(summary.date)
                                            
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(colors.cardBgStart)
                                                    .border(
                                                        width = 1.dp,
                                                        color = colors.subtleBorder,
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable {
                                                        expandedDays = if (isExpanded) expandedDays - summary.date else expandedDays + summary.date
                                                    }
                                                    .padding(14.dp)
                                                    .animateContentSize()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = friendlyDate,
                                                            color = colors.base,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        if (summary.totalScrolls == 0) {
                                                            Text(
                                                                text = "No activity",
                                                                color = colors.tertiary,
                                                                fontSize = 11.sp
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "${summary.totalShortFormScrolls} shorts • ${summary.totalScrolls - summary.totalShortFormScrolls} feed",
                                                                color = colors.secondary,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "${summary.totalScrolls}",
                                                        color = if (summary.totalScrolls > 0) colors.accent else colors.tertiary,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                
                                                if (isExpanded && summary.totalScrolls > 0) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    HorizontalDivider(color = colors.divider)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    summary.appBreakdown.entries
                                                        .sortedByDescending { it.value }
                                                        .forEach { (pkg, count) ->
                                                            val sfCount = summary.appShortFormBreakdown[pkg] ?: 0
                                                            val feedCount = (count - sfCount).coerceAtLeast(0)
                                                            
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(colors.cardBgEnd)
                                                                    .padding(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                AppIconMonogram(packageName = pkg, modifier = Modifier.size(28.dp))
                                                                Spacer(modifier = Modifier.width(10.dp))
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = ScrollTrackerService.friendlyName(pkg),
                                                                        color = colors.base.copy(alpha = 0.9f),
                                                                        fontSize = 12.sp,
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth(0.9f)
                                                                            .height(3.dp)
                                                                            .clip(RoundedCornerShape(1.5.dp))
                                                                            .background(colors.divider)
                                                                    ) {
                                                                        val sfRatio = sfCount.toFloat() / count.coerceAtLeast(1)
                                                                        val feedRatio = feedCount.toFloat() / count.coerceAtLeast(1)
                                                                        if (sfRatio > 0f) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .fillMaxHeight()
                                                                                    .weight(sfRatio)
                                                                                    .background(colors.sfProgress)
                                                                            )
                                                                        }
                                                                        if (feedRatio > 0f) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .fillMaxHeight()
                                                                                    .weight(feedRatio)
                                                                                    .background(colors.feedProgress)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.width(10.dp))
                                                                Column(horizontalAlignment = Alignment.End) {
                                                                    Text(
                                                                        text = "$count",
                                                                        color = colors.base.copy(alpha = 0.9f),
                                                                        fontSize = 12.sp,
                                                                        fontWeight = FontWeight.SemiBold
                                                                    )
                                                                    Text(
                                                                        text = if (sfCount > 0) "$sfCount shorts" else "feed",
                                                                        color = colors.tertiary,
                                                                        fontSize = 9.sp
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                        }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showWeeklyDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = colors.alertBg,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.padding(16.dp)
            )
        }
        } // end of else
    }
    }
}

private fun formatFriendlyDate(dateStr: String): String {
    return try {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (dateStr == todayStr) return "Today"
        
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
        if (date != null) {
            SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(date)
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}

// ── Reusable dashboard card ──────────────────────────────────────────────────

@Composable
private fun DashboardCard(
    title: String,
    cornerRadiusDp: Int,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalOrbSpaceColors.current
    val cardBg = if (colors.isLightBg) Color.White else Color(0xFF1C1C1E)
    val borderCol = if (colors.isLightBg) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                color = borderCol,
                shape = RoundedCornerShape(cornerRadiusDp.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.base.copy(alpha = 0.9f),
                    letterSpacing = 0.4.sp
                )
                trailingAction?.invoke()
            }
            content()
        }
    }
}

// ── Helper UI components ─────────────────────────────────────────────

@Composable
private fun LegendItem(color: Color, label: String) {
    val colors = LocalOrbSpaceColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.secondary
        )
    }
}

@Composable
private fun AppIconMonogram(packageName: String, modifier: Modifier = Modifier) {
    val colors = LocalOrbSpaceColors.current
    val brand = remember(packageName, colors.base) { getAppBrand(packageName, colors.base) }
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(brand.color.copy(alpha = 0.15f))
            .border(1.dp, brand.color.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = brand.initials,
            color = brand.color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = (-0.5).sp
        )
    }
}

private data class AppBrand(
    val initials: String,
    val color: Color
)

private fun getAppBrand(packageName: String, fallbackColor: Color): AppBrand {
    return when {
        packageName.contains("instagram") -> AppBrand("IG", Color(0xFFE1306C))
        packageName.contains("youtube.creator") -> AppBrand("YS", Color(0xFFF43F5E))
        packageName.contains("youtube") -> AppBrand("YT", Color(0xFFEF4444))
        packageName.contains("musically") || packageName.contains("ugc.trill") -> AppBrand("TK", Color(0xFF00F2FE))
        packageName.contains("snapchat") -> AppBrand("SC", Color(0xFFFFE000))
        packageName.contains("facebook.orca") -> AppBrand("MS", Color(0xFF0084FF))
        packageName.contains("facebook") -> AppBrand("FB", Color(0xFF1877F2))
        packageName.contains("twitter") -> AppBrand("X", fallbackColor)
        packageName.contains("reddit") -> AppBrand("RD", Color(0xFFFF4500))
        packageName.contains("linkedin") -> AppBrand("LN", Color(0xFF0A66C2))
        packageName.contains("pinterest") -> AppBrand("PT", Color(0xFFBD081C))
        packageName.contains("spotify") -> AppBrand("SP", Color(0xFF1DB954))
        packageName.contains("whatsapp") -> AppBrand("WA", Color(0xFF25D366))
        else -> {
            val name = ScrollTrackerService.friendlyName(packageName)
            val initials = name.take(2).uppercase()
            AppBrand(initials, fallbackColor.copy(alpha = 0.6f))
        }
    }
}

// ── Reminder row ─────────────────────────────────────────────────────────────

@Composable
private fun ReminderRow(
    item: ReminderItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalOrbSpaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (item.completed) colors.accent else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (item.completed) colors.accent else colors.secondary.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.completed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (colors.isLightBg) Color.White else Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.title,
            color = if (item.completed) colors.tertiary else colors.base.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = colors.base.copy(alpha = 0.2f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun rememberAppIcon(context: Context, packageName: String): androidx.compose.ui.graphics.ImageBitmap? {
    var bitmap by remember(packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap(48, 48).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    return bitmap
}
