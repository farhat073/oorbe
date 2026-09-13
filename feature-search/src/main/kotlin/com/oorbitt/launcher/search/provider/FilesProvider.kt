package com.oorbitt.launcher.search.provider

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FilesProvider(private val context: Context) : SearchProvider {
    override val priority: Int = 75 // High priority, below apps (100) and contacts (80)

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()
        
        // Use external volume to query files
        val collection = MediaStore.Files.getContentUri("external")
        
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        // Find files where the name matches the query
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        
        // Sort by most recently added
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext() && results.size < 8) { // Max 8 file results
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val mimeType = cursor.getString(mimeCol) ?: "*/*"
                    
                    // The Uri to open this specific file
                    val contentUri = Uri.withAppendedPath(collection, id.toString()).toString()
                    
                    results.add(
                        SearchResult(
                            id = "file_$id",
                            title = name,
                            subtitle = mimeType,
                            iconUri = null, // Will be resolved by the UI based on MIME type
                            action = SearchResultAction.OpenFile(contentUri, mimeType),
                            relevanceScore = calculateRelevance(name, query),
                            providerName = "FilesProvider"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore exception if READ_EXTERNAL_STORAGE or READ_MEDIA_* is not granted
        }

        return@withContext results
    }

    private fun calculateRelevance(name: String, query: String): Float {
        return if (name.startsWith(query, ignoreCase = true)) 0.8f else 0.6f
    }
}
