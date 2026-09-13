package com.oorbitt.launcher.model

data class LauncherProfile(
    val id: Long = 0,
    val name: String,
    val type: ProfileType = ProfileType.CUSTOM,
    val isActive: Boolean = false,
    val wallpaperUri: String? = null,
    val hiddenApps: Set<String> = emptySet(),
    val gestureOverrides: Map<GestureType, LauncherAction> = emptyMap()
)

enum class ProfileType {
    DEFAULT, WORK, MINIMAL, FOCUS, CUSTOM
}
