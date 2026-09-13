package com.oorbitt.launcher.stylehub.data

import kotlinx.serialization.Serializable

@Serializable
data class StyleHubItem(
    val id: String? = null,
    val title: String,
    val author: String,
    val description: String,
    val settings_json: String,
    val screenshots: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val install_count: Int = 0,
    val likes_count: Int = 0,
    val device_id: String,
    val status: String = "approved",
    val created_at: String? = null,
    
    // Optional field for local UI tracking
    var isLikedByUser: Boolean = false
)
