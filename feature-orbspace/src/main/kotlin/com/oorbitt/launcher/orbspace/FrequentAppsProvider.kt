package com.oorbitt.launcher.orbspace

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import kotlin.math.exp

/**
 * Represents an app that the user launches frequently.
 *
 * @property packageName  Application package name.
 * @property label        Human-readable app label.
 * @property launchCount  Aggregated launch count over the query window.
 * @property lastUsed     Epoch millis of the most recent foreground use.
 */
data class FrequentApp(
    val packageName: String,
    val label: String,
    val launchCount: Long,
    val lastUsed: Long
)

/**
 * Provides the user's most-frequently-used apps by querying
 * [UsageStatsManager] over the last 7 days and ranking with an
 * exponential-decay recency weighting.
 *
 * Requires the `android.permission.PACKAGE_USAGE_STATS` permission
 * which must be granted via **Settings → Apps → Special access →
 * Usage access**. When the permission is missing the provider
 * gracefully returns an empty list.
 */
class FrequentAppsProvider(private val context: Context) {

    companion object {
        /** How many top apps to return. */
        private const val TOP_COUNT = 8

        /** Days of usage history to query. */
        private const val WINDOW_DAYS = 7L

        /** Decay factor per day since last use (higher = faster decay). */
        private const val DECAY_LAMBDA = 0.3

        /** Packages to always exclude regardless of other heuristics. */
        private val EXCLUDED_PREFIXES = listOf(
            "com.android.",          // core system packages
            "com.google.android.gms", // Play Services
            "com.google.android.gsf", // Google Services Framework
            "com.google.android.ext.", // shared libraries
            "com.google.android.onetimeinitializer",
            "com.google.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.google.android.providers.",
            "android"                // bare system UID package
        )

        /** Our own launcher package — never suggest ourselves. */
        private const val OWN_PACKAGE = "com.oorbitt.launcher"
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Returns up to [TOP_COUNT] frequently-used apps sorted by a
     * recency-weighted score.
     *
     * Returns an empty list when:
     * - Usage-stats permission is not granted.
     * - No qualifying apps were used in the past [WINDOW_DAYS] days.
     */
    fun getFrequentApps(): List<FrequentApp> {
        if (!hasUsageStatsPermission()) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as? UsageStatsManager ?: return emptyList()

        val now = System.currentTimeMillis()
        val windowStart = now - WINDOW_DAYS * 24 * 60 * 60 * 1000

        val dailyStats: List<UsageStats> = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            windowStart,
            now
        ) ?: return emptyList()

        // Aggregate per package across all daily buckets
        val aggregated = mutableMapOf<String, AggregatedUsage>()
        for (stat in dailyStats) {
            val pkg = stat.packageName ?: continue
            // Rough heuristic — count each day with > 0 totalTimeInForeground as one "launch".
            val launches = if (stat.totalTimeInForeground > 0) 1L else 0L
            val lastUsed = stat.lastTimeUsed

            val entry = aggregated.getOrPut(pkg) { AggregatedUsage() }
            entry.totalLaunches += launches
            if (lastUsed > entry.lastUsed) entry.lastUsed = lastUsed
        }

        val pm = context.packageManager

        return aggregated
            .filterKeys { pkg -> !isExcluded(pkg) && hasLaunchIntent(pm, pkg) }
            .map { (pkg, usage) ->
                val daysSinceLastUse = ((now - usage.lastUsed).coerceAtLeast(0))
                    .toDouble() / (24 * 60 * 60 * 1000)
                val score = usage.totalLaunches * exp(-daysSinceLastUse * DECAY_LAMBDA)
                ScoredApp(pkg, usage.totalLaunches, usage.lastUsed, score)
            }
            .filter { it.score > 0.0 && it.totalLaunches > 0 }
            .sortedByDescending { it.score }
            .take(TOP_COUNT)
            .mapNotNull { scored -> toFrequentApp(pm, scored) }
    }

    // ── Internal types ──────────────────────────────────────────────────

    private class AggregatedUsage(
        var totalLaunches: Long = 0L,
        var lastUsed: Long = 0L
    )

    private data class ScoredApp(
        val packageName: String,
        val totalLaunches: Long,
        val lastUsed: Long,
        val score: Double
    )

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE)
                    as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks whether [pkg] should be filtered out.
     */
    private fun isExcluded(pkg: String): Boolean {
        if (pkg == OWN_PACKAGE || pkg.startsWith("$OWN_PACKAGE.")) return true
        return EXCLUDED_PREFIXES.any { prefix -> pkg.startsWith(prefix) }
    }

    /**
     * Returns `true` when the app has a launchable main activity
     * (i.e. it's not a background-only system component).
     */
    private fun hasLaunchIntent(pm: PackageManager, pkg: String): Boolean {
        val intent = pm.getLaunchIntentForPackage(pkg) ?: return false
        // Double-check the intent actually resolves
        return intent.resolveActivity(pm) != null
    }

    private fun toFrequentApp(pm: PackageManager, scored: ScoredApp): FrequentApp? {
        return try {
            val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(
                    scored.packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(scored.packageName, 0)
            }
            val label = pm.getApplicationLabel(ai).toString()
            FrequentApp(
                packageName = scored.packageName,
                label = label,
                launchCount = scored.totalLaunches,
                lastUsed = scored.lastUsed
            )
        } catch (_: PackageManager.NameNotFoundException) {
            // App was uninstalled between query and label lookup
            null
        }
    }
}
