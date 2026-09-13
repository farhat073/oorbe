package com.oorbitt.launcher.model

data class GestureAction(
    val gestureType: GestureType,
    val action: LauncherAction,
    val targetPackage: String? = null,
    val targetActivity: String? = null
)

enum class GestureType {
    SWIPE_UP, SWIPE_DOWN, DOUBLE_TAP, PINCH_IN, PINCH_OUT,
    SWIPE_UP_TWO_FINGER, LONG_PRESS_EMPTY, SWIPE_LEFT, SWIPE_RIGHT
}

enum class LauncherAction {
    OPEN_DRAWER, OPEN_SEARCH, OPEN_NOTIFICATIONS, OPEN_QUICK_SETTINGS,
    OPEN_RECENT_APPS, LOCK_SCREEN, TOGGLE_TORCH, OPEN_SETTINGS,
    OPEN_EDGE_PANEL, OPEN_PRIVACY_DASHBOARD, SWITCH_PROFILE,
    SCREEN_OFF, OPEN_VAULT, OPEN_CLIPBOARD, OPEN_APP_HYGIENE, LAUNCH_APP, NONE,
    OPEN_ORB_SPACE, OPEN_ORB_SEARCH
}

