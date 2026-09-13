package com.oorbitt.launcher.model

/** Master toggle enum — every feature in Oorbitt has a corresponding entry here. */
enum class Feature(val key: String, val defaultEnabled: Boolean, val description: String) {
    // Home screen
    SMART_SURFACES("smart_surfaces", false, "Context-aware home screen"),
    DOCK("dock", true, "Bottom dock bar"),
    PAGE_INDICATORS("page_indicators", true, "Page dot indicators"),
    EDGE_PANEL("edge_panel", false, "Side panel with quick tools"),
    ONE_HAND_MODE("one_hand_mode", false, "Reachability mode"),

    // Drawer
    HIDDEN_APPS("hidden_apps", true, "Hide apps from drawer"),
    WORK_PROFILE_SECTION("work_profile", true, "Work profile tab"),
    AUTO_CATEGORIZE("auto_categorize", true, "Automatic app categories"),
    SUGGESTED_APPS("suggested_apps", false, "Usage-based app suggestions"),

    // Search
    SEARCH_APPS("search_apps", true, "App search"),
    SEARCH_CALCULATOR("search_calculator", true, "Calculator in search"),
    SEARCH_UNIT_CONVERTER("search_unit_converter", true, "Unit converter in search"),
    SEARCH_CONTACTS("search_contacts", false, "Contact search"),
    SEARCH_SETTINGS("search_settings", true, "Settings shortcut search"),
    SEARCH_SHORTCUTS("search_shortcuts", true, "App shortcut search"),
    SEARCH_WEB_FALLBACK("search_web", true, "Web search fallback"),
    COMMAND_BAR("command_bar", false, "Natural language commands"),

    // Icons & Theme
    ICON_PACKS("icon_packs", true, "Icon pack support"),
    CUSTOM_ICON_SHAPES("icon_shapes", true, "Adaptive icon masking"),
    MATERIAL_YOU("material_you", true, "Dynamic theming"),
    WALLPAPER_PARALLAX("wallpaper_parallax", true, "Wallpaper scroll effect"),
    CUSTOM_TRANSITIONS("custom_transitions", false, "Custom app animations"),

    // Gestures
    GESTURE_SWIPE_UP("gesture_swipe_up", true, "Swipe up gesture"),
    GESTURE_SWIPE_DOWN("gesture_swipe_down", true, "Swipe down gesture"),
    GESTURE_DOUBLE_TAP("gesture_double_tap", true, "Double tap gesture"),
    GESTURE_PINCH("gesture_pinch", true, "Pinch gesture"),
    GESTURE_LONG_PRESS("gesture_long_press", true, "Long press on empty space"),

    // Notifications
    NOTIFICATION_BADGES("notif_badges", true, "Badge dots/counts"),
    NOTIFICATION_PREVIEW("notif_preview", false, "Notification preview on home"),

    // Shizuku
    SHIZUKU_FREEZE("shizuku_freeze", true, "Freeze/unfreeze apps"),
    SHIZUKU_FORCE_STOP("shizuku_force_stop", true, "Force stop from popup"),
    SHIZUKU_CLEAR_CACHE("shizuku_clear_cache", true, "Clear cache from popup"),
    SHIZUKU_FLOATING_WINDOW("shizuku_floating", false, "Open in popup window"),
    SHIZUKU_RECENT_TASKS("shizuku_recents", false, "Recent apps panel"),

    // Intelligence
    APP_DIET("app_diet", false, "Screen time controls"),
    APP_HYGIENE("app_hygiene", false, "Battery/data/permission dashboard"),
    CLIPBOARD_HISTORY("clipboard_history", false, "Clipboard manager"),

    // Privacy & Security
    GHOST_MODE("ghost_mode", false, "Secret clean layout"),
    APP_LOCK("app_lock", false, "Per-app biometric lock"),
    SECURE_VAULT("secure_vault", true, "Private encrypted vault"),

    // Profiles
    LAUNCHER_PROFILES("launcher_profiles", false, "Multiple home configurations"),

    // Backup
    LOCAL_BACKUP("local_backup", true, "Export/import layouts"),
    COMMUNITY_SHARING("community_sharing", false, "Share layouts with community");

    companion object {
        fun fromKey(key: String): Feature? = entries.find { it.key == key }
    }
}
