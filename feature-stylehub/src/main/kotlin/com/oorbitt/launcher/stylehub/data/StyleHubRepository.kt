package com.oorbitt.launcher.stylehub.data

import android.content.Context
import android.util.Log
import com.oorbitt.launcher.data.style.StyleBlob
import com.oorbitt.launcher.data.style.StyleBlobSerializer
import com.oorbitt.launcher.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class StyleHubRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val sharedPrefs = context.getSharedPreferences("stylehub_prefs", Context.MODE_PRIVATE)

    // Supabase Configuration - placeholders.
    // If these are empty, we gracefully fall back to the premium mock list.
    private val supabaseUrl = ""
    private val supabaseKey = ""

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private var localStyles = mutableListOf<StyleHubItem>()

    init {
        // Initialize mock styles so there is rich content out-of-the-box
        val myId = getDeviceId()
        localStyles.add(
            StyleHubItem(
                id = "mock-1",
                title = "Mocha Dream",
                author = "Oorbitt Team",
                description = "Catppuccin-inspired dark setup. Relaxing pastel hues with a clean 5x5 layout.",
                settings_json = createMockBlobJson(ThemeMode.DARK, "SQUIRCLE", "#CBA6F7"),
                screenshots = listOf("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop"),
                tags = listOf("Minimal", "Catppuccin", "Dark"),
                install_count = 1420,
                device_id = "team-id",
                status = "approved"
            )
        )
        localStyles.add(
            StyleHubItem(
                id = "mock-2",
                title = "Nordic Clean",
                author = "Sven",
                description = "E-ink style minimalism with ice-blue details. High readability, zero clutter.",
                settings_json = createMockBlobJson(ThemeMode.LIGHT, "CIRCLE", "#89B4FA"),
                screenshots = listOf("https://images.unsplash.com/photo-1604871000636-074fa5117945?w=500&auto=format&fit=crop"),
                tags = listOf("Clean", "Light", "Nord"),
                install_count = 890,
                device_id = "team-id",
                status = "approved"
            )
        )
        localStyles.add(
            StyleHubItem(
                id = "mock-3",
                title = "Neon Tokyo",
                author = "Akira",
                description = "Vibrant synthwave themes. Hot pink highlights inside modular Bento layout grids.",
                settings_json = createMockBlobJson(ThemeMode.DARK, "ROUNDED_RECT", "#F5C2E7"),
                screenshots = listOf("https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop"),
                tags = listOf("Neon", "Tokyo", "Cyber"),
                install_count = 2105,
                device_id = "team-id",
                status = "approved"
            )
        )
    }

    private fun createMockBlobJson(theme: ThemeMode, shape: String, accent: String): String {
        val blob = StyleBlob(
            exportedAt = System.currentTimeMillis(),
            themeMode = theme.name,
            iconShape = shape,
            orbAccentColor = accent,
            gridColumns = 5,
            gridRows = 5,
            dockSlots = 5,
            searchStyle = "PILL",
            orbSpaceLayout = "APPLE_HEALTH"
        )
        return StyleBlobSerializer.toJson(blob)
    }

    fun getDeviceId(): String {
        var id = sharedPrefs.getString("device_uuid", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            sharedPrefs.edit().putString("device_uuid", id).apply()
        }
        return id
    }

    suspend fun getLikedStyleIds(): Set<String> = withContext(Dispatchers.IO) {
        val myDeviceId = getDeviceId()
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            return@withContext emptySet()
        }
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/style_likes?device_id=eq.$myDeviceId&select=style_id")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptySet()
                val responseBody = response.body?.string() ?: "[]"
                val array = JSONArray(responseBody)
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    set.add(array.getJSONObject(i).getString("style_id"))
                }
                return@withContext set
            }
        } catch (e: Exception) {
            Log.e("StyleHubRepository", "Failed to fetch liked styles", e)
            return@withContext emptySet()
        }
    }

    suspend fun getApprovedStyles(): List<StyleHubItem> = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            // Return mock list
            return@withContext localStyles.filter { it.status == "approved" }
        }

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/styles?status=eq.approved&order=created_at.desc")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("x-device-id", getDeviceId())
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseBody = response.body?.string() ?: "[]"
                val list = json.decodeFromString<List<StyleHubItem>>(responseBody)
                val likedIds = getLikedStyleIds()
                list.forEach { it.isLikedByUser = it.id in likedIds }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("StyleHubRepository", "Failed to fetch remote styles, falling back to local mock list", e)
            return@withContext localStyles.filter { it.status == "approved" }
        }
    }

    suspend fun getMyStyles(): List<StyleHubItem> = withContext(Dispatchers.IO) {
        val myDeviceId = getDeviceId()
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            return@withContext localStyles.filter { it.device_id == myDeviceId }
        }

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/styles?device_id=eq.$myDeviceId&order=created_at.desc")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("x-device-id", myDeviceId)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseBody = response.body?.string() ?: "[]"
                val list = json.decodeFromString<List<StyleHubItem>>(responseBody)
                val likedIds = getLikedStyleIds()
                list.forEach { it.isLikedByUser = it.id in likedIds }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("StyleHubRepository", "Failed to fetch my styles, falling back to memory", e)
            return@withContext localStyles.filter { it.device_id == myDeviceId }
        }
    }

    suspend fun uploadStyle(
        title: String,
        author: String,
        description: String,
        settingsJson: String,
        screenshots: List<String>,
        tags: List<String>,
        iconPackName: String? = null,
        wallpaperName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val myDeviceId = getDeviceId()
        val item = StyleHubItem(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            description = description,
            settings_json = settingsJson,
            screenshots = screenshots,
            tags = tags,
            device_id = myDeviceId,
            status = "approved" // Auto-approved in mock environment
        )

        // Add to local list in memory
        localStyles.add(item)

        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            return@withContext true
        }

        // We manually build the payload so we insert exactly what supabase expects
        val bodyObj = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("author", item.author)
            put("description", item.description)
            put("settings_json", item.settings_json)
            put("screenshots", JSONArray(item.screenshots))
            put("tags", JSONArray(item.tags))
            put("device_id", item.device_id)
            put("status", "pending") // Send to pending queue for admin review
            put("icon_pack_name", iconPackName ?: JSONObject.NULL)
            put("wallpaper_name", wallpaperName ?: JSONObject.NULL)
        }
        val body = bodyObj.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/styles")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-device-id", myDeviceId)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("StyleHubRepository", "Failed to upload style online, saved locally", e)
            return@withContext true
        }
    }

    suspend fun toggleLikeStyle(styleId: String, currentLiked: Boolean): Boolean = withContext(Dispatchers.IO) {
        val myDeviceId = getDeviceId()
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            // Local fallback toggle
            return@withContext true
        }

        if (currentLiked) {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/style_likes?style_id=eq.$styleId&device_id=eq.$myDeviceId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("x-device-id", myDeviceId)
                .delete()
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    return@withContext response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e("StyleHubRepository", "Failed to unlike style", e)
                return@withContext false
            }
        } else {
            val bodyObj = JSONObject().apply {
                put("style_id", styleId)
                put("device_id", myDeviceId)
            }
            val body = bodyObj.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/style_likes")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-device-id", myDeviceId)
                .post(body)
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    return@withContext response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e("StyleHubRepository", "Failed to like style", e)
                return@withContext false
            }
        }
    }

    suspend fun incrementInstallCount(styleId: String): Boolean = withContext(Dispatchers.IO) {
        // Increment locally
        localStyles.find { it.id == styleId }?.let {
            val updated = it.copy(install_count = it.install_count + 1)
            localStyles.remove(it)
            localStyles.add(updated)
        }

        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            return@withContext true
        }

        val body = "{\"style_id\":\"$styleId\"}".toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/increment_install_count")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-device-id", getDeviceId())
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("StyleHubRepository", "Failed to increment download count online", e)
            return@withContext false
        }
    }

    suspend fun flagStyle(styleId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            // Remove locally so it disappears from view immediately
            localStyles.removeAll { it.id == styleId }
            return@withContext true
        }

        val bodyObj = JSONObject().apply {
            put("style_id", styleId)
            put("device_id", getDeviceId())
            put("reason", reason)
        }
        val body = bodyObj.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/style_flags")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-device-id", getDeviceId())
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("StyleHubRepository", "Failed to flag style online", e)
            return@withContext false
        }
    }
}
