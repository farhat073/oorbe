package com.oorbitt.launcher.data.repository.impl

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.oorbitt.launcher.data.db.dao.HiddenAppDao
import com.oorbitt.launcher.data.db.dao.LockedAppDao
import com.oorbitt.launcher.data.db.entity.HiddenAppEntity
import com.oorbitt.launcher.data.db.entity.LockedAppEntity
import com.oorbitt.launcher.data.repository.AppRepository
import com.oorbitt.launcher.model.AppCategory
import com.oorbitt.launcher.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import com.oorbitt.launcher.data.db.dao.IconOverrideDao
import com.oorbitt.launcher.data.db.entity.IconOverrideEntity

class AppRepositoryImpl(
    private val context: Context,
    private val hiddenAppDao: HiddenAppDao,
    private val lockedAppDao: LockedAppDao,
    private val iconOverrideDao: IconOverrideDao
) : AppRepository {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    private val _rawInstalledApps = MutableStateFlow<List<AppInfo>>(emptyList())

    override val hiddenApps: Flow<Set<String>> = hiddenAppDao.getAll().map { list ->
        list.map { it.componentKey }.toSet()
    }

    override val lockedApps: Flow<Set<String>> = lockedAppDao.getAll().map { list ->
        list.map { it.componentKey }.toSet()
    }

    override val allApps: Flow<List<AppInfo>> = combine(
        _rawInstalledApps,
        hiddenApps,
        lockedApps,
        iconOverrideDao.getAll().map { it.associateBy { e -> e.componentKey } }
    ) { installed, hidden, locked, overrides ->
        installed.map { app ->
            val override = overrides[app.componentKey]
            app.copy(
                isHidden = hidden.contains(app.componentKey),
                isLocked = locked.contains(app.componentKey),
                customLabel = override?.customLabel,
                customIconUri = override?.customIconUri
            )
        }
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            scope.launch { refreshAppList() }
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            scope.launch { refreshAppList() }
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            scope.launch { refreshAppList() }
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scope.launch { refreshAppList() }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scope.launch { refreshAppList() }
        }
    }

    init {
        // Register launcher apps callback to monitor install/uninstall events
        launcherApps.registerCallback(callback)
        scope.launch {
            refreshAppList()
        }
    }

    override suspend fun hideApp(componentKey: String) {
        hiddenAppDao.insert(HiddenAppEntity(componentKey))
    }

    override suspend fun unhideApp(componentKey: String) {
        hiddenAppDao.deleteByKey(componentKey)
    }

    override suspend fun lockApp(componentKey: String) {
        lockedAppDao.insert(LockedAppEntity(componentKey))
    }

    override suspend fun unlockApp(componentKey: String) {
        lockedAppDao.deleteByKey(componentKey)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    override suspend fun refreshAppList() {
        val appsList = mutableListOf<AppInfo>()
        val pm = context.packageManager
        
        try {
            val userProfiles = userManager.userProfiles
            val myUserHandle = Process.myUserHandle()
            
            val usageStatsMap = if (hasUsageStatsPermission()) {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 30L * 24 * 60 * 60 * 1000 // Last 30 days
                usm?.queryAndAggregateUsageStats(startTime, endTime)
            } else {
                null
            }
            
            for (profile in userProfiles) {
                val isWorkProfile = profile != myUserHandle
                val activityList = launcherApps.getActivityList(null, profile)
                
                for (activityInfo in activityList) {
                    val packageName = activityInfo.applicationInfo.packageName
                    val activityName = activityInfo.name
                    val label = activityInfo.label.toString()
                    
                    var installTime: Long = 0
                    try {
                        val packageInfo = pm.getPackageInfo(packageName, 0)
                        installTime = packageInfo.firstInstallTime
                    } catch (e: Exception) {
                        // Fallback if packages are uninstalling
                    }

                    // Guess category (can be expanded later)
                    val category = determineCategory(activityInfo)

                    val stats = usageStatsMap?.get(packageName)
                    val lastUsed = stats?.lastTimeUsed ?: 0L
                    val usageCount = (stats?.totalTimeInForeground ?: 0L).toInt()

                    appsList.add(
                        AppInfo(
                            packageName = packageName,
                            activityName = activityName,
                            label = label,
                            userHandle = profile.hashCode(),
                            isWorkProfile = isWorkProfile,
                            installTime = installTime,
                            lastUsedTime = lastUsed,
                            usageCount = usageCount,
                            category = category
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Log/handle error
        }

        _rawInstalledApps.value = appsList.sortedBy { it.label.lowercase() }
    }

    private fun determineCategory(info: LauncherActivityInfo): AppCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemCategory = info.applicationInfo.category
            return when (systemCategory) {
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO,
                android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> AppCategory.MEDIA
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                android.content.pm.ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
                android.content.pm.ApplicationInfo.CATEGORY_MAPS -> AppCategory.TRAVEL
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
                else -> AppCategory.UNCATEGORIZED
            }
        }
        return AppCategory.UNCATEGORIZED
    }

    override suspend fun saveIconOverride(componentKey: String, customLabel: String?, customIconUri: String?) {
        iconOverrideDao.insertOrUpdate(
            IconOverrideEntity(
                componentKey = componentKey,
                iconPackPackage = null,
                customIconUri = customIconUri,
                customLabel = customLabel
            )
        )
        refreshAppList()
    }

    override suspend fun deleteIconOverride(componentKey: String) {
        val existing = iconOverrideDao.getByKey(componentKey)
        if (existing != null) {
            iconOverrideDao.delete(existing)
        }
        refreshAppList()
    }
}
