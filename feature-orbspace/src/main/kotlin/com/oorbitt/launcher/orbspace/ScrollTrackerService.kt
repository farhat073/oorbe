package com.oorbitt.launcher.orbspace

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.oorbitt.launcher.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScrollTrackerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val settingsRepository: SettingsRepository by lazy {
        try {
            val context = org.koin.core.context.GlobalContext.getOrNull()
            context?.get<SettingsRepository>() ?: object : SettingsRepository {
                override val settings = kotlinx.coroutines.flow.MutableStateFlow(com.oorbitt.launcher.model.LauncherSettings())
                override suspend fun updateSettings(transform: (com.oorbitt.launcher.model.LauncherSettings) -> com.oorbitt.launcher.model.LauncherSettings) {}
            }
        } catch (e: Throwable) {
            object : SettingsRepository {
                override val settings = kotlinx.coroutines.flow.MutableStateFlow(com.oorbitt.launcher.model.LauncherSettings())
                override suspend fun updateSettings(transform: (com.oorbitt.launcher.model.LauncherSettings) -> com.oorbitt.launcher.model.LauncherSettings) {}
            }
        }
    }

    private var currentDailyLimit: Int = 0

    private data class PackageState(
        var lastEventTimestamp: Long = 0L,
        var currentCount: Int = 0,               // Total scroll count
        var currentShortFormCount: Int = 0,      // Total short-form scroll count (aligned to total scrolls for tracked social apps)
        var isActive: Boolean = false
    )

    // Keep state per package rather than globally
    private val packageStates = mutableMapOf<String, PackageState>()
    private var currentlyTrackedPackage: String? = null
    
    private var sessionFinalizeJob: Job? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off detected, finalizing active session.")
                sessionFinalizeJob?.cancel()
                val pkg = currentlyTrackedPackage
                if (pkg != null) {
                    finalizePackageSession(pkg, sync = true)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        pruneOldHistory() // Prune older than 30 days
        loadTodayStats()

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED or 
                          AccessibilityEvent.TYPE_VIEW_SELECTED or 
                          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or 
                     AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or 
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or 
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 50L
        serviceInfo = info
        
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        // Collect settings limit Flow
        serviceScope.launch {
            settingsRepository.settings.collect { settings ->
                currentDailyLimit = settings.dailyScrollLimit
            }
        }

        _isServiceRunning.value = true
        Log.d(TAG, "ScrollTrackerService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handleWindowStateChanged(packageName, event)
            return
        }

        if (type != AccessibilityEvent.TYPE_VIEW_SCROLLED && type != AccessibilityEvent.TYPE_VIEW_SELECTED) return

        if (packageName !in TRACKED_PACKAGES) {
            return
        }

        // Determine if it is a vertical swipe up (content moves up, scrolling down)
        val isSwipeUp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            event.scrollDeltaY > 0 && Math.abs(event.scrollDeltaY) > Math.abs(event.scrollDeltaX)
        } else {
            event.toIndex > event.fromIndex
        }

        if (!isSwipeUp) {
            return
        }

        val now = event.eventTime
        Log.d(TAG, "Swipe up detected in tracked app: $packageName")

        // If this is a new session because we were tracking something else, initialize it
        if (currentlyTrackedPackage != packageName) {
            sessionFinalizeJob?.cancel() // Cancel any pending finalization of the previous package
            val oldPkg = currentlyTrackedPackage
            if (oldPkg != null) {
                finalizePackageSession(oldPkg, sync = true) // Synchronous write at session boundary
            }
            currentlyTrackedPackage = packageName
            Log.d(TAG, "Now tracking: $packageName")
        }

        val state = packageStates.getOrPut(packageName) {
            PackageState(
                currentCount = prefs.getInt("${todayKey()}_$packageName", 0),
                currentShortFormCount = prefs.getInt("${todayKey()}_${packageName}_sf", 0)
            )
        }
        state.isActive = true

        val lastEventDate = if (state.lastEventTimestamp > 0L) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(state.lastEventTimestamp))
        } else {
            null
        }
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

        if (lastEventDate != null && lastEventDate != currentDate) {
            // Day rollover! Commit old counts
            val editor = prefs.edit()
            editor.putInt("${lastEventDate}_$packageName", state.currentCount)
            editor.putInt("${lastEventDate}_${packageName}_sf", state.currentShortFormCount)
            editor.commit()

            // Reset/load for the new day
            state.currentCount = prefs.getInt("${currentDate}_$packageName", 0)
            state.currentShortFormCount = prefs.getInt("${currentDate}_${packageName}_sf", 0)
            
            pruneOldHistory()
        }

        // Rolling Idle-Gap Detection
        val gap = now - state.lastEventTimestamp
        state.lastEventTimestamp = now

        Log.d(TAG, "Rolling gap for $packageName: ${gap}ms (Threshold: ${GAP_MS}ms)")

        if (gap > GAP_MS) {
            state.currentCount++
            state.currentShortFormCount = state.currentCount
            
            Log.i(TAG, "Incremented scrolls for $packageName: total=${state.currentCount}")
            updateLiveStats(packageName, state.currentCount, state.currentShortFormCount)
            
            // Persist immediately to prevent loss
            val sessionDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(state.lastEventTimestamp))
            prefs.edit()
                .putInt("${sessionDateKey}_$packageName", state.currentCount)
                .putInt("${sessionDateKey}_${packageName}_sf", state.currentShortFormCount)
                .apply()

            checkDailyScrollLimit()
        }
    }

    private fun handleWindowStateChanged(newPackageName: String, event: AccessibilityEvent) {
        val currentPackage = currentlyTrackedPackage
        if (currentPackage == null) return
        
        if (newPackageName == currentPackage) {
            sessionFinalizeJob?.cancel()
            return
        }

        if (isTransientOverlay(newPackageName)) {
            return
        }

        sessionFinalizeJob?.cancel()
        sessionFinalizeJob = serviceScope.launch {
            delay(DEBOUNCE_FINALIZE_MS)
            finalizePackageSession(currentPackage, sync = true)
        }
    }

    private fun finalizePackageSession(packageName: String, sync: Boolean) {
        val state = packageStates[packageName] ?: return
        if (!state.isActive) return
        
        Log.d(TAG, "Finalizing session for $packageName. Count: ${state.currentCount}")
        
        val sessionDateKey = if (state.lastEventTimestamp > 0) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(state.lastEventTimestamp))
        } else {
            todayKey()
        }

        val editor = prefs.edit()
        editor.putInt("${sessionDateKey}_$packageName", state.currentCount)
        editor.putInt("${sessionDateKey}_${packageName}_sf", state.currentShortFormCount)
        if (sync) {
            editor.commit()
        } else {
            editor.apply()
        }

        state.isActive = false
        if (currentlyTrackedPackage == packageName) {
            currentlyTrackedPackage = null
        }
    }

    private fun isTransientOverlay(pkg: String): Boolean {
        if (pkg == "com.android.systemui" || pkg == "android" || pkg == "com.android.permissioncontroller") return true
        if (pkg.contains("inputmethod")) return true
        
        val defaultIme = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
        val imePackage = defaultIme?.substringBefore("/")
        if (imePackage != null && pkg == imePackage) return true
        
        return false
    }

    private fun updateLiveStats(packageName: String, count: Int, shortFormCount: Int) {
        _todayStats.update { currentStats ->
            currentStats + (packageName to count)
        }
        _todayShortFormStats.update { currentStats ->
            currentStats + (packageName to shortFormCount)
        }
    }

    private fun loadTodayStats() {
        val today = todayKey()
        val stored = mutableMapOf<String, Int>()
        val storedSf = mutableMapOf<String, Int>()
        
        for (pkg in TRACKED_PACKAGES) {
            val count = prefs.getInt("${today}_$pkg", 0)
            val sfCount = prefs.getInt("${today}_${pkg}_sf", 0)
            
            if (count > 0 || sfCount > 0) {
                stored[pkg] = count
                storedSf[pkg] = sfCount
                packageStates[pkg] = PackageState(
                    currentCount = count, 
                    currentShortFormCount = sfCount,
                    isActive = false
                )
            }
        }
        _todayStats.value = stored
        _todayShortFormStats.value = storedSf
    }
    
    private fun pruneOldHistory() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val cutoffKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        
        val allKeys = prefs.all.keys
        val keysToRemove = allKeys.filter { key ->
            val datePart = key.substringBefore("_")
            datePart.length == 10 && datePart < cutoffKey
        }
        
        if (keysToRemove.isNotEmpty()) {
            val editor = prefs.edit()
            keysToRemove.forEach { editor.remove(it) }
            editor.apply()
            Log.d(TAG, "Pruned ${keysToRemove.size} old scroll history records.")
        }
    }

    private fun checkDailyScrollLimit() {
        val limit = currentDailyLimit
        if (limit <= 0) return

        var totalToday = 0
        for (pkg in TRACKED_PACKAGES) {
            val count = packageStates[pkg]?.currentCount ?: prefs.getInt("${todayKey()}_$pkg", 0)
            totalToday += count
        }

        if (totalToday >= limit) {
            val today = todayKey()
            val warnedKey = "${today}_limit_warned"
            val hasWarned = prefs.getBoolean(warnedKey, false)
            if (!hasWarned) {
                prefs.edit().putBoolean(warnedKey, true).apply()
                showLimitWarningNotification(totalToday, limit)
            }
        }
    }

    private fun showLimitWarningNotification(count: Int, limit: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "scroll_tracker_warnings"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Wellness Limits",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when scroll limits are exceeded"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val title = "Daily Scroll Limit Reached! 🚨"
        val text = "You have scrolled $count times today (Limit: $limit). Time to take a break!"
        
        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, channelId)
        } else {
            android.app.Notification.Builder(this)
        }
        
        val notification = builder
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onInterrupt() {
        Log.d(TAG, "ScrollTrackerService interrupted")
        sessionFinalizeJob?.cancel()
        val pkg = currentlyTrackedPackage
        if (pkg != null) {
            finalizePackageSession(pkg, sync = true)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "ScrollTrackerService destroyed")
        sessionFinalizeJob?.cancel()
        val pkg = currentlyTrackedPackage
        if (pkg != null) {
            finalizePackageSession(pkg, sync = true)
        }
        unregisterReceiver(screenOffReceiver)
        serviceScope.cancel()
        _isServiceRunning.value = false
        instance = null
        super.onDestroy()
    }

    companion object {
        var instance: ScrollTrackerService? = null
            private set
            
        private const val TAG = "ScrollTracker"
        private const val NOTIFICATION_ID = 1001
        const val PREFS_NAME = "orbspace_scroll_stats"

        const val GAP_MS = 800L
        const val DEBOUNCE_FINALIZE_MS = 350L

        val TRACKED_PACKAGES = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "app.revanced.android.youtube",
            "com.vanced.android.youtube",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.reddit.frontpage",
            "com.linkedin.android",
            "in.mohalla.video",
            "com.moj.shortvideoapp",
            "in.mohalla.sharechat",
            "com.roposo.android",
            "com.bigo.live",
            "com.triller.android",
            "com.pinterest",
            "com.spotify.music",
            "com.facebook.orca",
            "com.whatsapp",
            "com.google.android.apps.youtube.creator"
        )

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _todayStats = MutableStateFlow<Map<String, Int>>(emptyMap())
        val todayStats: StateFlow<Map<String, Int>> = _todayStats.asStateFlow()

        private val _todayShortFormStats = MutableStateFlow<Map<String, Int>>(emptyMap())
        val todayShortFormStats: StateFlow<Map<String, Int>> = _todayShortFormStats.asStateFlow()

        val scrollStats: StateFlow<Map<String, Int>> = _todayStats.asStateFlow()

        fun loadFromPrefs(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val today = todayKey()
            val stored = mutableMapOf<String, Int>()
            val storedSf = mutableMapOf<String, Int>()
            
            for (pkg in TRACKED_PACKAGES) {
                val count = prefs.getInt("${today}_$pkg", 0)
                val sfCount = prefs.getInt("${today}_${pkg}_sf", 0)
                
                if (count > 0) stored[pkg] = count
                if (sfCount > 0) storedSf[pkg] = sfCount
            }
            if (stored.isNotEmpty()) {
                _todayStats.update { current ->
                    if (current.isEmpty()) stored
                    else {
                        val merged = current.toMutableMap()
                        for ((pkg, count) in stored) {
                            merged[pkg] = maxOf(merged[pkg] ?: 0, count)
                        }
                        merged
                    }
                }
            }
            if (storedSf.isNotEmpty()) {
                _todayShortFormStats.update { current ->
                    if (current.isEmpty()) storedSf
                    else {
                        val merged = current.toMutableMap()
                        for ((pkg, count) in storedSf) {
                            merged[pkg] = maxOf(merged[pkg] ?: 0, count)
                        }
                        merged
                    }
                }
            }
        }
        
        fun getWeeklyBreakdown(context: Context): List<DailyScrollSummary> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val result = mutableListOf<DailyScrollSummary>()
            
            val cal = Calendar.getInstance()
            for (i in 0..6) {
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                
                val appBreakdown = mutableMapOf<String, Int>()
                val appSfBreakdown = mutableMapOf<String, Int>()
                var dailyTotal = 0
                var dailySfTotal = 0
                
                for (pkg in TRACKED_PACKAGES) {
                    val count = prefs.getInt("${dateKey}_$pkg", 0)
                    val sfCount = prefs.getInt("${dateKey}_${pkg}_sf", 0)
                    
                    if (count > 0) {
                        appBreakdown[pkg] = count
                        dailyTotal += count
                    }
                    if (sfCount > 0) {
                        appSfBreakdown[pkg] = sfCount
                        dailySfTotal += sfCount
                    }
                }
                
                result.add(DailyScrollSummary(dateKey, dailyTotal, appBreakdown, dailySfTotal, appSfBreakdown))
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            
            return result.reversed()
        }

        fun friendlyName(packageName: String): String = when {
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("youtube.creator") -> "YT Studio"
            packageName.contains("youtube") -> "YouTube"
            packageName.contains("musically") || packageName.contains("ugc.trill") -> "TikTok"
            packageName.contains("snapchat") -> "Snapchat"
            packageName.contains("facebook.orca") -> "Messenger"
            packageName.contains("facebook") -> "Facebook"
            packageName.contains("twitter") -> "X (Twitter)"
            packageName.contains("reddit") -> "Reddit"
            packageName.contains("linkedin") -> "LinkedIn"
            packageName.contains("mohalla.video") || packageName.contains("moj") -> "Moj"
            packageName.contains("sharechat") -> "ShareChat"
            packageName.contains("roposo") -> "Roposo"
            packageName.contains("bigo") -> "Bigo Live"
            packageName.contains("triller") -> "Triller"
            packageName.contains("pinterest") -> "Pinterest"
            packageName.contains("spotify") -> "Spotify"
            packageName.contains("whatsapp") -> "WhatsApp"
            else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercaseChar() }
        }

        fun friendlyEmoji(packageName: String): String = when {
            packageName.contains("instagram") -> "📸"
            packageName.contains("youtube") -> "▶️"
            packageName.contains("musically") || packageName.contains("ugc.trill") -> "🎵"
            packageName.contains("snapchat") -> "👻"
            packageName.contains("facebook.orca") -> "💬"
            packageName.contains("facebook") -> "📘"
            packageName.contains("twitter") -> "🐦"
            packageName.contains("reddit") -> "🤖"
            packageName.contains("linkedin") -> "💼"
            packageName.contains("pinterest") -> "📌"
            packageName.contains("spotify") -> "🎧"
            packageName.contains("whatsapp") -> "💬"
            else -> "📱"
        }

        private fun todayKey(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }
}

data class DailyScrollSummary(
    val date: String,
    val totalScrolls: Int,
    val appBreakdown: Map<String, Int>,
    val totalShortFormScrolls: Int,
    val appShortFormBreakdown: Map<String, Int>
)
