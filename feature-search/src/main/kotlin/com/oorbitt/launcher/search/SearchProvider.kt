package com.oorbitt.launcher.search

import com.oorbitt.launcher.model.SearchResult

interface SearchProvider {
    val priority: Int
    suspend fun search(query: String): List<SearchResult>
}
