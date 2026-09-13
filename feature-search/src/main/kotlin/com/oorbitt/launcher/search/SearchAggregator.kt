package com.oorbitt.launcher.search

import com.oorbitt.launcher.model.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class SearchAggregator(
    private val providers: List<SearchProvider>
) {
    suspend fun search(query: String): List<SearchResult> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()

        val deferredResults = providers.map { provider ->
            async {
                try {
                    // Impose an 800ms timeout per provider so one slow provider doesn't block the rest
                    withTimeoutOrNull(800) {
                        provider.search(query)
                    } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

        // Wait for all to complete
        val allResults = deferredResults.flatMap { it.await() }

        // Sort by relevance score descending, then by provider priority descending
        return@coroutineScope allResults.sortedWith(
            compareByDescending<SearchResult> { it.relevanceScore }
                .thenByDescending { getProviderPriority(it.providerName) }
        )
    }

    private fun getProviderPriority(providerName: String): Int {
        val provider = providers.firstOrNull { it.javaClass.simpleName.startsWith(providerName, ignoreCase = true) }
        return provider?.priority ?: 0
    }
}
