package com.oorbitt.launcher.search.provider

import android.content.Context
import android.provider.ContactsContract
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WhatsAppProvider(private val context: Context) : SearchProvider {
    override val priority: Int = 70

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()
        
        val projection = arrayOf(
            ContactsContract.Data._ID,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.DATA1, // WhatsApp ID/Number
            ContactsContract.Data.DATA3  // Type
        )

        // Find contacts with WhatsApp MIME type matching the query
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("vnd.android.cursor.item/vnd.com.whatsapp.profile", "%$query%")

        try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)
                val dataIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)

                while (cursor.moveToNext() && results.size < 5) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)
                    val number = cursor.getString(dataIndex)
                    
                    // The number format usually has the country code appended with @s.whatsapp.net
                    // But we can construct an intent with it or just pass it as contact URI.
                    // We can use a LaunchShortcut action for WhatsApp specific deep link or just an intent string if the model supported it.
                    // For now, let's map it to MessageContact with the number, and let the UI handle the WhatsApp specific logic, 
                    // or better, map it to a specific URL deep link format: https://api.whatsapp.com/send?phone=$number
                    
                    val cleanNumber = number.substringBefore("@")
                    
                    results.add(
                        SearchResult(
                            id = "wa_$id",
                            title = "$name (WhatsApp)",
                            subtitle = "Message on WhatsApp",
                            action = SearchResultAction.OpenUrl("https://api.whatsapp.com/send?phone=$cleanNumber"),
                            relevanceScore = if (name.startsWith(query, ignoreCase = true)) 0.8f else 0.6f,
                            providerName = "WhatsAppProvider"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore if permission not granted
        }

        return@withContext results
    }
}
