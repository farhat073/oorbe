package com.oorbitt.launcher.search.provider

import com.oorbitt.launcher.data.repository.AppRepository
import com.oorbitt.launcher.model.AppInfo
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchProvider
import kotlinx.coroutines.flow.first
import java.util.Locale

class AppSearchProvider(
    private val appRepository: AppRepository
) : SearchProvider {
    override val priority: Int = 100 // Highest priority

    override suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val cleanedQuery = query.trim().lowercase(Locale.ROOT)
        
        // Grab all installed apps
        val apps = appRepository.allApps.first().filter { !it.isHidden }
        
        val results = mutableListOf<Pair<AppInfo, Float>>()
        
        for (app in apps) {
            val label = app.displayLabel.lowercase(Locale.ROOT)
            val score = calculateScore(label, cleanedQuery)
            if (score > 0f) {
                results.add(app to score)
            }
        }
        
        return results
            .sortedByDescending { it.second }
            .map { (app, score) ->
                SearchResult(
                    id = "app:${app.componentKey}",
                    title = app.displayLabel,
                    subtitle = "Application",
                    action = SearchResultAction.LaunchApp(app.packageName, app.activityName),
                    relevanceScore = score,
                    providerName = "Apps"
                )
            }
    }

    private fun calculateScore(label: String, query: String): Float {
        if (label == query) return 1.0f
        if (label.startsWith(query)) return 0.9f
        if (label.contains(query)) return 0.7f
        
        // Initials matching (e.g., "g m" or "gm" -> Google Maps / Gmail / etc.)
        val initials = label.split(" ", "-", "_")
            .filter { it.isNotEmpty() }
            .map { it[0] }
            .joinToString("")
        if (initials.startsWith(query) || initials.contains(query)) {
            return 0.8f
        }
        
        // Fuzzy Match (Levenshtein distance)
        val limit = 2 // max edits
        if (query.length > 2) {
            val dist = levenshtein(label.take(query.length + 1), query)
            if (dist <= limit) {
                return 0.5f - (dist.toFloat() / 10f) // higher distance = lower score
            }
        }
        
        return 0f
    }

    private fun levenshtein(s: String, t: String): Int {
        val m = s.length
        val n = t.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[m][n]
    }
}
