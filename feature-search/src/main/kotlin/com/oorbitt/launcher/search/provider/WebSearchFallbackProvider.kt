package com.oorbitt.launcher.search.provider

import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchProvider
import java.net.URLEncoder

class WebSearchFallbackProvider : SearchProvider {
    override val priority: Int = 10 // Lowest priority fallback

    override suspend fun search(query: String): List<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val encodedQuery = try {
            URLEncoder.encode(trimmed, "UTF-8")
        } catch (e: Exception) {
            trimmed
        }
        val url = "https://www.google.com/search?q=$encodedQuery"

        return listOf(
            SearchResult(
                id = "web:$trimmed",
                title = "Search Google for \"$trimmed\"",
                subtitle = "Open in default browser",
                action = SearchResultAction.OpenUrl(url),
                relevanceScore = 0.3f, // low relevance score so it goes to bottom
                providerName = "Web"
            )
        )
    }
}
