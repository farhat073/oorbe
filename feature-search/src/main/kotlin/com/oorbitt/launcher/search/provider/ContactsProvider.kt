package com.oorbitt.launcher.search.provider

import android.content.Context
import android.provider.ContactsContract
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsProvider(private val context: Context) : SearchProvider {
    override val priority: Int = 80 // High priority, but below apps

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()

        // 1. Query contacts database
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
        )

        val isNumeric = trimmed.any { it.isDigit() }
        val selection = if (isNumeric) {
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        } else {
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        }
        val selectionArgs = if (isNumeric) {
            arrayOf("%$trimmed%", "%$trimmed%")
        } else {
            arrayOf("%$trimmed%")
        }

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                var count = 0
                while (cursor.moveToNext() && count < 5) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)
                    val number = cursor.getString(numberIndex)
                    val photoUri = cursor.getString(photoIndex)

                    val relevance = if (name.contains(trimmed, ignoreCase = true)) {
                        if (name.startsWith(trimmed, ignoreCase = true)) 0.95f else 0.90f
                    } else {
                        0.93f // Matched by number, floats highly
                    }

                    results.add(
                        SearchResult(
                            id = "contact_$id",
                            title = name,
                            subtitle = number,
                            iconUri = photoUri,
                            action = SearchResultAction.CallContact(number),
                            relevanceScore = relevance,
                            providerName = "ContactsProvider"
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            // Ignore security exception if READ_CONTACTS is not granted
        }

        return@withContext results
    }

    private fun isPhoneNumber(query: String): Boolean {
        val trimmed = query.trim()
        val digitCount = trimmed.count { it.isDigit() }
        if (digitCount < 3) return false

        return trimmed.indices.all { i ->
            val c = trimmed[i]
            c.isDigit() || c == ' ' || c == '-' || c == '(' || c == ')' || (c == '+' && i == 0)
        }
    }

    private fun getCleanPhoneNumber(query: String): String {
        val trimmed = query.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    private fun calculateRelevance(name: String, query: String): Float {
        return if (name.startsWith(query, ignoreCase = true)) 0.9f else 0.7f
    }
}
