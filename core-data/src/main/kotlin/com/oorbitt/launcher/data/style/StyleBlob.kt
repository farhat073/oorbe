package com.oorbitt.launcher.data.style

import kotlinx.serialization.Serializable

@Serializable
data class StyleBlob(
    val schemaVersion: Int = 2,
    val exportedAt: Long,

    // Granular sharing flags
    val shareAppearance: Boolean = true,
    val shareWorkspace: Boolean = true,
    val shareDrawer: Boolean = true,
    val shareOrbSpace: Boolean = true,
    val shareSearch: Boolean = true,
    val shareGestures: Boolean = true,
    val shareWellness: Boolean = true,

    // Appearance
    val themeMode: String,
    val iconShape: String,
    val iconCornerTopStart: Int = 50,
    val iconCornerTopEnd: Int = 50,
    val iconCornerBottomStart: Int = 50,
    val iconCornerBottomEnd: Int = 50,
    val iconCornerCut: Boolean = false,
    val desktopIconSize: Int = 52,
    val drawerIconSize: Int = 52,
    val dockIconSize: Int = 52,
    val showDesktopLabels: Boolean = true,
    val showDockLabels: Boolean = false,
    val labelTextSize: Int = 12,

    // Workspace & Dock
    val gridColumns: Int = 5,
    val gridRows: Int = 5,
    val dockSlots: Int = 5,
    val freeFormIcons: Boolean = false,

    // Drawer (including bezel gap spacing)
    val drawerColumns: Int = 4,
    val drawerBackgroundColor: String = "#000000",
    val drawerBackgroundOpacity: Int = 92,
    val drawerCornerRadius: Int = 0,
    val drawerSpaceDp: Int = 0,
    val drawerCornerTopStartDp: Int = 16,
    val drawerCornerTopEndDp: Int = 16,
    val drawerCornerBottomStartDp: Int = 0,
    val drawerCornerBottomEndDp: Int = 0,

    // Search Layout & Spacing
    val searchStyle: String = "BAR",
    val searchPosition: String = "BOTTOM",
    val searchBarCornerRadiusDp: Int = 24,
    val searchResultsSpacingDp: Int = 8,
    val searchBarPaddingDp: Int = 12,

    // OrbSpace Layout & Spacing
    val orbSpaceLayout: String = "APPLE_HEALTH",
    val orbAccentColor: String = "#007AFF",
    val orbSpaceSpacingDp: Int = 16,
    val orbSpaceCardCornerRadiusDp: Int = 16,

    // Wellness
    val dailyScrollLimit: Int = 0,

    // Gestures
    val swipeUpAction: String = "NONE",
    val swipeDownAction: String = "NONE",
    val doubleTapAction: String = "NONE",
    val pinchAction: String = "NONE",

    // Informational only
    val iconPackName: String? = null,
    val wallpaperName: String? = null
)
