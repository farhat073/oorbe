package com.oorbitt.launcher.model

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val customLabel: String? = null,
    val userHandle: Int = 0,
    val isWorkProfile: Boolean = false,
    val installTime: Long = 0,
    val lastUsedTime: Long = 0,
    val usageCount: Int = 0,
    val isHidden: Boolean = false,
    val isFrozen: Boolean = false,
    val isLocked: Boolean = false,
    val category: AppCategory = AppCategory.UNCATEGORIZED,
    val customIconUri: String? = null
) {
    val componentKey: String get() = "$packageName/$activityName"
    val displayLabel: String get() = customLabel ?: label
}

enum class AppCategory {
    SOCIAL, GAMES, PRODUCTIVITY, MEDIA, UTILITIES, COMMUNICATION,
    SHOPPING, FINANCE, HEALTH, EDUCATION, TRAVEL, FOOD, NEWS,
    UNCATEGORIZED
}
