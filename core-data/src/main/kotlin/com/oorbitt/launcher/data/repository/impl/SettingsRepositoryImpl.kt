package com.oorbitt.launcher.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.model.DrawerSortMode
import com.oorbitt.launcher.model.IconShape
import com.oorbitt.launcher.model.LauncherSettings
import com.oorbitt.launcher.model.SearchBarStyle
import com.oorbitt.launcher.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private companion object {
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val GRID_ROWS = intPreferencesKey("grid_rows")
        val DOCK_SLOTS = intPreferencesKey("dock_slots")
        val DRAWER_COLUMNS = intPreferencesKey("drawer_columns")
        val ICON_SIZE_DP = intPreferencesKey("icon_size_dp")
        val LABEL_SIZE_SP = intPreferencesKey("label_size_sp")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val SHOW_DOCK_LABELS = booleanPreferencesKey("show_dock_labels")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val WALLPAPER_DIM = intPreferencesKey("wallpaper_dim_percent")
        val SEARCH_BAR_STYLE = stringPreferencesKey("search_bar_style")
        val SORT_MODE = stringPreferencesKey("sort_mode")
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        val ACTIVE_ICON_PACK = stringPreferencesKey("active_icon_pack")
        
        val DESKTOP_ICON_SIZE_DP = intPreferencesKey("desktop_icon_size_dp")
        val DRAWER_ICON_SIZE_DP = intPreferencesKey("drawer_icon_size_dp")
        val DOCK_ICON_SIZE_DP = intPreferencesKey("dock_icon_size_dp")
        val SHOW_DESKTOP_LABELS = booleanPreferencesKey("show_desktop_labels")
        val SHOW_DRAWER_LABELS = booleanPreferencesKey("show_drawer_labels")
        val DRAWER_BG_COLOR = stringPreferencesKey("drawer_bg_color")
        val DRAWER_BG_OPACITY = intPreferencesKey("drawer_bg_opacity")
        val DRAWER_BG_IMAGE = stringPreferencesKey("drawer_bg_image")
        val DRAWER_CORNER_RADIUS_DP = intPreferencesKey("drawer_corner_radius_dp")
        val DRAWER_ANIMATION_TYPE = stringPreferencesKey("drawer_animation_type")
        val SEARCH_BAR_POSITION = stringPreferencesKey("search_bar_position")
        val SEARCH_BAR_CORNER_RADIUS_DP = intPreferencesKey("search_bar_corner_radius_dp")

        val ICON_CORNER_TOP_START = intPreferencesKey("icon_corner_top_start")
        val ICON_CORNER_TOP_END = intPreferencesKey("icon_corner_top_end")
        val ICON_CORNER_BOTTOM_START = intPreferencesKey("icon_corner_bottom_start")
        val ICON_CORNER_BOTTOM_END = intPreferencesKey("icon_corner_bottom_end")
        val ICON_CORNER_CUT = booleanPreferencesKey("icon_corner_cut")
        
        val DRAWER_CORNER_TOP_START = intPreferencesKey("drawer_corner_top_start")
        val DRAWER_CORNER_TOP_END = intPreferencesKey("drawer_corner_top_end")
        val DRAWER_CORNER_BOTTOM_START = intPreferencesKey("drawer_corner_bottom_start")
        val DRAWER_CORNER_BOTTOM_END = intPreferencesKey("drawer_corner_bottom_end")
        val DRAWER_SPACE_DP = intPreferencesKey("drawer_space_dp")
        val DRAWER_GROUPS_JSON = stringPreferencesKey("drawer_groups_json")
        
        val DRAWER_SEARCH_HEIGHT_DP = intPreferencesKey("drawer_search_height_dp")
        val DRAWER_SEARCH_CORNER_RADIUS_DP = intPreferencesKey("drawer_search_corner_radius_dp")
        val DRAWER_SEARCH_BG_COLOR = stringPreferencesKey("drawer_search_bg_color")
        val DRAWER_SEARCH_BG_OPACITY = intPreferencesKey("drawer_search_bg_opacity")
        val DRAWER_SEARCH_BORDER_WIDTH_DP = intPreferencesKey("drawer_search_border_width_dp")
        val DRAWER_SEARCH_BORDER_COLOR = stringPreferencesKey("drawer_search_border_color")
        
        val APP_LOCK_TIMEOUT_MS = longPreferencesKey("app_lock_timeout_ms")
        val APP_LOCK_SCREEN_OFF = booleanPreferencesKey("app_lock_screen_off")
        val APP_LOCK_BIOMETRIC_ONLY = booleanPreferencesKey("app_lock_biometric_only")
        val DAILY_SCROLL_LIMIT = intPreferencesKey("daily_scroll_limit")
        val SHOW_SCROLL_TRACKER_WIDGET = booleanPreferencesKey("show_scroll_tracker_widget")
        val DRAWER_SEARCH_POSITION = stringPreferencesKey("drawer_search_position")
        val FAVORITE_AI_APPS_CSV = stringPreferencesKey("favorite_ai_apps_csv")
        val SEARCH_RESULTS_SPACING_DP = intPreferencesKey("search_results_spacing_dp")
        val ORB_SPACE_SPACING_DP = intPreferencesKey("orb_space_spacing_dp")
        val SEARCH_BAR_PADDING_DP = intPreferencesKey("search_bar_padding_dp")
        val ORB_SPACE_CARD_CORNER_RADIUS_DP = intPreferencesKey("orb_space_card_corner_radius_dp")
        val ORB_SPACE_LAYOUT_MODE = stringPreferencesKey("orb_space_layout_mode")
        val ORB_SPACE_ACCENT_COLOR = stringPreferencesKey("orb_space_accent_color")
        val OPEN_WITH_STYLE = stringPreferencesKey("open_with_style")
        val ENABLE_WEB_SEARCH_IN_OPEN_WITH = booleanPreferencesKey("enable_web_search_in_open_with")
        val ENABLE_AI_SEARCH_IN_OPEN_WITH = booleanPreferencesKey("enable_ai_search_in_open_with")
        val SHOW_ORB_BUTTON = booleanPreferencesKey("show_orb_button")
        val ENABLE_ORB_SPACE = booleanPreferencesKey("enable_orb_space")
        val LOCK_ORB_SPACE = booleanPreferencesKey("lock_orb_space")
        val FREE_FORM_PLACEMENT = booleanPreferencesKey("free_form_placement")
        val SEARCH_RESULT_BG_COLOR = stringPreferencesKey("search_result_bg_color")
        val SEARCH_RESULT_BG_OPACITY = intPreferencesKey("search_result_bg_opacity")
        val SEARCH_RESULT_PADDING_DP = intPreferencesKey("search_result_padding_dp")
        val SEARCH_RESULT_TEXT_SIZE_SP = intPreferencesKey("search_result_text_size_sp")
        val WELLNESS_WIDGET_BG_OPACITY = intPreferencesKey("wellness_widget_bg_opacity")
        val WELLNESS_WIDGET_HEIGHT_DP = intPreferencesKey("wellness_widget_height_dp")
        val WELLNESS_WIDGET_CORNER_RADIUS_DP = intPreferencesKey("wellness_widget_corner_radius_dp")
        val WELLNESS_WIDGET_ACCENT_COLOR = stringPreferencesKey("wellness_widget_accent_color")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    override val settings: Flow<LauncherSettings> = dataStore.data.map { prefs ->
        LauncherSettings(
            gridColumns = prefs[GRID_COLUMNS] ?: 5,
            gridRows = prefs[GRID_ROWS] ?: 5,
            dockSlots = prefs[DOCK_SLOTS] ?: 5,
            drawerColumns = prefs[DRAWER_COLUMNS] ?: 4,
            iconSizeDp = prefs[ICON_SIZE_DP] ?: 52,
            labelSizeSp = prefs[LABEL_SIZE_SP] ?: 12,
            showLabels = prefs[SHOW_LABELS] ?: true,
            showDockLabels = prefs[SHOW_DOCK_LABELS] ?: false,
            themeMode = prefs[THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            iconShape = prefs[ICON_SHAPE]?.let { IconShape.valueOf(it) } ?: IconShape.CIRCLE,
            wallpaperDimPercent = prefs[WALLPAPER_DIM] ?: 0,
            searchBarStyle = prefs[SEARCH_BAR_STYLE]?.let { SearchBarStyle.valueOf(it) } ?: SearchBarStyle.BAR,
            sortMode = prefs[SORT_MODE]?.let { DrawerSortMode.valueOf(it) } ?: DrawerSortMode.ALPHABETICAL,
            activeProfileId = prefs[ACTIVE_PROFILE_ID] ?: 0,
            activeIconPack = prefs[ACTIVE_ICON_PACK],
            desktopIconSizeDp = prefs[DESKTOP_ICON_SIZE_DP] ?: prefs[ICON_SIZE_DP] ?: 52,
            drawerIconSizeDp = prefs[DRAWER_ICON_SIZE_DP] ?: prefs[ICON_SIZE_DP] ?: 52,
            dockIconSizeDp = prefs[DOCK_ICON_SIZE_DP] ?: prefs[ICON_SIZE_DP] ?: 52,
            showDesktopLabels = prefs[SHOW_DESKTOP_LABELS] ?: prefs[SHOW_LABELS] ?: true,
            showDrawerLabels = prefs[SHOW_DRAWER_LABELS] ?: prefs[SHOW_LABELS] ?: true,
            drawerBgColor = prefs[DRAWER_BG_COLOR] ?: "#000000",
            drawerBgOpacity = prefs[DRAWER_BG_OPACITY] ?: 92,
            drawerBgImage = prefs[DRAWER_BG_IMAGE],
            drawerCornerRadiusDp = prefs[DRAWER_CORNER_RADIUS_DP] ?: 0,
            drawerAnimationType = prefs[DRAWER_ANIMATION_TYPE] ?: "SLIDE",
            searchBarPosition = prefs[SEARCH_BAR_POSITION] ?: "BOTTOM",
            searchBarCornerRadiusDp = prefs[SEARCH_BAR_CORNER_RADIUS_DP] ?: 24,
            iconCornerTopStart = prefs[ICON_CORNER_TOP_START] ?: 50,
            iconCornerTopEnd = prefs[ICON_CORNER_TOP_END] ?: 50,
            iconCornerBottomStart = prefs[ICON_CORNER_BOTTOM_START] ?: 50,
            iconCornerBottomEnd = prefs[ICON_CORNER_BOTTOM_END] ?: 50,
            iconCornerCut = prefs[ICON_CORNER_CUT] ?: false,
            drawerCornerTopStartDp = prefs[DRAWER_CORNER_TOP_START] ?: 16,
            drawerCornerTopEndDp = prefs[DRAWER_CORNER_TOP_END] ?: 16,
            drawerCornerBottomStartDp = prefs[DRAWER_CORNER_BOTTOM_START] ?: 0,
            drawerCornerBottomEndDp = prefs[DRAWER_CORNER_BOTTOM_END] ?: 0,
            drawerSpaceDp = prefs[DRAWER_SPACE_DP] ?: 0,
            drawerGroupsJson = prefs[DRAWER_GROUPS_JSON] ?: "[]",
            drawerSearchHeightDp = prefs[DRAWER_SEARCH_HEIGHT_DP] ?: 48,
            drawerSearchCornerRadiusDp = prefs[DRAWER_SEARCH_CORNER_RADIUS_DP] ?: 24,
            drawerSearchBgColor = prefs[DRAWER_SEARCH_BG_COLOR] ?: "#FFFFFF",
            drawerSearchBgOpacity = prefs[DRAWER_SEARCH_BG_OPACITY] ?: 12,
            drawerSearchBorderWidthDp = prefs[DRAWER_SEARCH_BORDER_WIDTH_DP] ?: 0,
            drawerSearchBorderColor = prefs[DRAWER_SEARCH_BORDER_COLOR] ?: "#FFFFFF",
            appLockTimeoutMs = prefs[APP_LOCK_TIMEOUT_MS] ?: 0L,
            appLockScreenOff = prefs[APP_LOCK_SCREEN_OFF] ?: true,
            appLockBiometricOnly = prefs[APP_LOCK_BIOMETRIC_ONLY] ?: false,
            dailyScrollLimit = prefs[DAILY_SCROLL_LIMIT] ?: 0,
            showScrollTrackerWidget = prefs[SHOW_SCROLL_TRACKER_WIDGET] ?: false,
            drawerSearchPosition = prefs[DRAWER_SEARCH_POSITION] ?: "TOP",
            favoriteAIAppsCsv = prefs[FAVORITE_AI_APPS_CSV] ?: "",
            searchResultsSpacingDp = prefs[SEARCH_RESULTS_SPACING_DP] ?: 8,
            orbSpaceSpacingDp = prefs[ORB_SPACE_SPACING_DP] ?: 16,
            searchBarPaddingDp = prefs[SEARCH_BAR_PADDING_DP] ?: 12,
            orbSpaceCardCornerRadiusDp = prefs[ORB_SPACE_CARD_CORNER_RADIUS_DP] ?: 16,
            orbSpaceLayoutMode = prefs[ORB_SPACE_LAYOUT_MODE] ?: "APPLE_HEALTH",
            orbSpaceAccentColor = prefs[ORB_SPACE_ACCENT_COLOR] ?: "#007AFF",
            openWithStyle = prefs[OPEN_WITH_STYLE] ?: "APPLE_CHIPS",
            enableWebSearchInOpenWith = prefs[ENABLE_WEB_SEARCH_IN_OPEN_WITH] ?: true,
            enableAISearchInOpenWith = prefs[ENABLE_AI_SEARCH_IN_OPEN_WITH] ?: true,
            showOrbButton = prefs[SHOW_ORB_BUTTON] ?: true,
            enableOrbSpace = prefs[ENABLE_ORB_SPACE] ?: true,
            lockOrbSpace = prefs[LOCK_ORB_SPACE] ?: false,
            freeFormPlacement = prefs[FREE_FORM_PLACEMENT] ?: false,
            searchResultBgColor = prefs[SEARCH_RESULT_BG_COLOR] ?: "#1C1C1E",
            searchResultBgOpacity = prefs[SEARCH_RESULT_BG_OPACITY] ?: 100,
            searchResultPaddingDp = prefs[SEARCH_RESULT_PADDING_DP] ?: 12,
            searchResultTextSizeSp = prefs[SEARCH_RESULT_TEXT_SIZE_SP] ?: 14,
            wellnessWidgetBgOpacity = prefs[WELLNESS_WIDGET_BG_OPACITY] ?: 40,
            wellnessWidgetHeightDp = prefs[WELLNESS_WIDGET_HEIGHT_DP] ?: 120,
            wellnessWidgetCornerRadiusDp = prefs[WELLNESS_WIDGET_CORNER_RADIUS_DP] ?: 20,
            wellnessWidgetAccentColor = prefs[WELLNESS_WIDGET_ACCENT_COLOR] ?: "#007AFF",
            hasCompletedOnboarding = prefs[HAS_COMPLETED_ONBOARDING] ?: false
        )
    }

    override suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.edit { prefs ->
            val current = LauncherSettings(
                gridColumns = prefs[GRID_COLUMNS] ?: 5,
                gridRows = prefs[GRID_ROWS] ?: 5,
                dockSlots = prefs[DOCK_SLOTS] ?: 5,
                drawerColumns = prefs[DRAWER_COLUMNS] ?: 4,
                iconSizeDp = prefs[ICON_SIZE_DP] ?: 52,
                labelSizeSp = prefs[LABEL_SIZE_SP] ?: 12,
                showLabels = prefs[SHOW_LABELS] ?: true,
                showDockLabels = prefs[SHOW_DOCK_LABELS] ?: false,
                themeMode = prefs[THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
                iconShape = prefs[ICON_SHAPE]?.let { IconShape.valueOf(it) } ?: IconShape.CIRCLE,
                wallpaperDimPercent = prefs[WALLPAPER_DIM] ?: 0,
                searchBarStyle = prefs[SEARCH_BAR_STYLE]?.let { SearchBarStyle.valueOf(it) } ?: SearchBarStyle.BAR,
                sortMode = prefs[SORT_MODE]?.let { DrawerSortMode.valueOf(it) } ?: DrawerSortMode.ALPHABETICAL,
                activeProfileId = prefs[ACTIVE_PROFILE_ID] ?: 0,
                activeIconPack = prefs[ACTIVE_ICON_PACK],
                desktopIconSizeDp = prefs[DESKTOP_ICON_SIZE_DP] ?: prefs[ICON_SIZE_DP] ?: 52,
                drawerIconSizeDp = prefs[DRAWER_ICON_SIZE_DP] ?: prefs[ICON_SIZE_DP] ?: 52,
                dockIconSizeDp = prefs[DOCK_ICON_SIZE_DP] ?: prefs[ICON_SIZE_DP] ?: 52,
                showDesktopLabels = prefs[SHOW_DESKTOP_LABELS] ?: prefs[SHOW_LABELS] ?: true,
                showDrawerLabels = prefs[SHOW_DRAWER_LABELS] ?: prefs[SHOW_LABELS] ?: true,
                drawerBgColor = prefs[DRAWER_BG_COLOR] ?: "#000000",
                drawerBgOpacity = prefs[DRAWER_BG_OPACITY] ?: 92,
                drawerBgImage = prefs[DRAWER_BG_IMAGE],
                drawerCornerRadiusDp = prefs[DRAWER_CORNER_RADIUS_DP] ?: 0,
                drawerAnimationType = prefs[DRAWER_ANIMATION_TYPE] ?: "SLIDE",
                searchBarPosition = prefs[SEARCH_BAR_POSITION] ?: "BOTTOM",
                searchBarCornerRadiusDp = prefs[SEARCH_BAR_CORNER_RADIUS_DP] ?: 24,
                iconCornerTopStart = prefs[ICON_CORNER_TOP_START] ?: 50,
                iconCornerTopEnd = prefs[ICON_CORNER_TOP_END] ?: 50,
                iconCornerBottomStart = prefs[ICON_CORNER_BOTTOM_START] ?: 50,
                iconCornerBottomEnd = prefs[ICON_CORNER_BOTTOM_END] ?: 50,
                iconCornerCut = prefs[ICON_CORNER_CUT] ?: false,
                drawerCornerTopStartDp = prefs[DRAWER_CORNER_TOP_START] ?: 16,
                drawerCornerTopEndDp = prefs[DRAWER_CORNER_TOP_END] ?: 16,
                drawerCornerBottomStartDp = prefs[DRAWER_CORNER_BOTTOM_START] ?: 0,
                drawerCornerBottomEndDp = prefs[DRAWER_CORNER_BOTTOM_END] ?: 0,
                drawerSpaceDp = prefs[DRAWER_SPACE_DP] ?: 0,
                drawerGroupsJson = prefs[DRAWER_GROUPS_JSON] ?: "[]",
                drawerSearchHeightDp = prefs[DRAWER_SEARCH_HEIGHT_DP] ?: 48,
                drawerSearchCornerRadiusDp = prefs[DRAWER_SEARCH_CORNER_RADIUS_DP] ?: 24,
                drawerSearchBgColor = prefs[DRAWER_SEARCH_BG_COLOR] ?: "#FFFFFF",
                drawerSearchBgOpacity = prefs[DRAWER_SEARCH_BG_OPACITY] ?: 12,
                drawerSearchBorderWidthDp = prefs[DRAWER_SEARCH_BORDER_WIDTH_DP] ?: 0,
                drawerSearchBorderColor = prefs[DRAWER_SEARCH_BORDER_COLOR] ?: "#FFFFFF",
                appLockTimeoutMs = prefs[APP_LOCK_TIMEOUT_MS] ?: 0L,
                appLockScreenOff = prefs[APP_LOCK_SCREEN_OFF] ?: true,
                appLockBiometricOnly = prefs[APP_LOCK_BIOMETRIC_ONLY] ?: false,
                dailyScrollLimit = prefs[DAILY_SCROLL_LIMIT] ?: 0,
                showScrollTrackerWidget = prefs[SHOW_SCROLL_TRACKER_WIDGET] ?: false,
                drawerSearchPosition = prefs[DRAWER_SEARCH_POSITION] ?: "TOP",
                favoriteAIAppsCsv = prefs[FAVORITE_AI_APPS_CSV] ?: "",
                searchResultsSpacingDp = prefs[SEARCH_RESULTS_SPACING_DP] ?: 8,
                orbSpaceSpacingDp = prefs[ORB_SPACE_SPACING_DP] ?: 16,
                searchBarPaddingDp = prefs[SEARCH_BAR_PADDING_DP] ?: 12,
                orbSpaceCardCornerRadiusDp = prefs[ORB_SPACE_CARD_CORNER_RADIUS_DP] ?: 16,
                orbSpaceLayoutMode = prefs[ORB_SPACE_LAYOUT_MODE] ?: "APPLE_HEALTH",
                orbSpaceAccentColor = prefs[ORB_SPACE_ACCENT_COLOR] ?: "#007AFF",
                openWithStyle = prefs[OPEN_WITH_STYLE] ?: "APPLE_CHIPS",
                enableWebSearchInOpenWith = prefs[ENABLE_WEB_SEARCH_IN_OPEN_WITH] ?: true,
                enableAISearchInOpenWith = prefs[ENABLE_AI_SEARCH_IN_OPEN_WITH] ?: true,
                showOrbButton = prefs[SHOW_ORB_BUTTON] ?: true,
                enableOrbSpace = prefs[ENABLE_ORB_SPACE] ?: true,
                lockOrbSpace = prefs[LOCK_ORB_SPACE] ?: false,
                freeFormPlacement = prefs[FREE_FORM_PLACEMENT] ?: false,
                searchResultBgColor = prefs[SEARCH_RESULT_BG_COLOR] ?: "#1C1C1E",
                searchResultBgOpacity = prefs[SEARCH_RESULT_BG_OPACITY] ?: 100,
                searchResultPaddingDp = prefs[SEARCH_RESULT_PADDING_DP] ?: 12,
                searchResultTextSizeSp = prefs[SEARCH_RESULT_TEXT_SIZE_SP] ?: 14,
                wellnessWidgetBgOpacity = prefs[WELLNESS_WIDGET_BG_OPACITY] ?: 40,
                wellnessWidgetHeightDp = prefs[WELLNESS_WIDGET_HEIGHT_DP] ?: 120,
                wellnessWidgetCornerRadiusDp = prefs[WELLNESS_WIDGET_CORNER_RADIUS_DP] ?: 20,
                wellnessWidgetAccentColor = prefs[WELLNESS_WIDGET_ACCENT_COLOR] ?: "#007AFF",
                hasCompletedOnboarding = prefs[HAS_COMPLETED_ONBOARDING] ?: false
            )
            val updated = transform(current)
            prefs[GRID_COLUMNS] = updated.gridColumns
            prefs[GRID_ROWS] = updated.gridRows
            prefs[DOCK_SLOTS] = updated.dockSlots
            prefs[DRAWER_COLUMNS] = updated.drawerColumns
            prefs[ICON_SIZE_DP] = updated.iconSizeDp
            prefs[LABEL_SIZE_SP] = updated.labelSizeSp
            prefs[SHOW_LABELS] = updated.showLabels
            prefs[SHOW_DOCK_LABELS] = updated.showDockLabels
            prefs[THEME_MODE] = updated.themeMode.name
            prefs[ICON_SHAPE] = updated.iconShape.name
            prefs[WALLPAPER_DIM] = updated.wallpaperDimPercent
            prefs[SEARCH_BAR_STYLE] = updated.searchBarStyle.name
            prefs[SORT_MODE] = updated.sortMode.name
            prefs[ACTIVE_PROFILE_ID] = updated.activeProfileId
            
            prefs[DESKTOP_ICON_SIZE_DP] = updated.desktopIconSizeDp
            prefs[DRAWER_ICON_SIZE_DP] = updated.drawerIconSizeDp
            prefs[DOCK_ICON_SIZE_DP] = updated.dockIconSizeDp
            prefs[SHOW_DESKTOP_LABELS] = updated.showDesktopLabels
            prefs[SHOW_DRAWER_LABELS] = updated.showDrawerLabels
            prefs[DRAWER_BG_COLOR] = updated.drawerBgColor
            prefs[DRAWER_BG_OPACITY] = updated.drawerBgOpacity
            prefs[DRAWER_CORNER_RADIUS_DP] = updated.drawerCornerRadiusDp
            prefs[DRAWER_ANIMATION_TYPE] = updated.drawerAnimationType
            prefs[SEARCH_BAR_POSITION] = updated.searchBarPosition
            prefs[SEARCH_BAR_CORNER_RADIUS_DP] = updated.searchBarCornerRadiusDp

            prefs[ICON_CORNER_TOP_START] = updated.iconCornerTopStart
            prefs[ICON_CORNER_TOP_END] = updated.iconCornerTopEnd
            prefs[ICON_CORNER_BOTTOM_START] = updated.iconCornerBottomStart
            prefs[ICON_CORNER_BOTTOM_END] = updated.iconCornerBottomEnd
            prefs[ICON_CORNER_CUT] = updated.iconCornerCut
            prefs[DRAWER_CORNER_TOP_START] = updated.drawerCornerTopStartDp
            prefs[DRAWER_CORNER_TOP_END] = updated.drawerCornerTopEndDp
            prefs[DRAWER_CORNER_BOTTOM_START] = updated.drawerCornerBottomStartDp
            prefs[DRAWER_CORNER_BOTTOM_END] = updated.drawerCornerBottomEndDp
            prefs[DRAWER_SPACE_DP] = updated.drawerSpaceDp
            prefs[DRAWER_GROUPS_JSON] = updated.drawerGroupsJson
            
            prefs[DRAWER_SEARCH_HEIGHT_DP] = updated.drawerSearchHeightDp
            prefs[DRAWER_SEARCH_CORNER_RADIUS_DP] = updated.drawerSearchCornerRadiusDp
            prefs[DRAWER_SEARCH_BG_COLOR] = updated.drawerSearchBgColor
            prefs[DRAWER_SEARCH_BG_OPACITY] = updated.drawerSearchBgOpacity
            prefs[DRAWER_SEARCH_BORDER_WIDTH_DP] = updated.drawerSearchBorderWidthDp
            prefs[DRAWER_SEARCH_BORDER_COLOR] = updated.drawerSearchBorderColor
            
            prefs[APP_LOCK_TIMEOUT_MS] = updated.appLockTimeoutMs
            prefs[APP_LOCK_SCREEN_OFF] = updated.appLockScreenOff
            prefs[APP_LOCK_BIOMETRIC_ONLY] = updated.appLockBiometricOnly
            prefs[DAILY_SCROLL_LIMIT] = updated.dailyScrollLimit
            prefs[SHOW_SCROLL_TRACKER_WIDGET] = updated.showScrollTrackerWidget
            prefs[DRAWER_SEARCH_POSITION] = updated.drawerSearchPosition
            prefs[FAVORITE_AI_APPS_CSV] = updated.favoriteAIAppsCsv
            prefs[SEARCH_RESULTS_SPACING_DP] = updated.searchResultsSpacingDp
            prefs[ORB_SPACE_SPACING_DP] = updated.orbSpaceSpacingDp
            prefs[SEARCH_BAR_PADDING_DP] = updated.searchBarPaddingDp
            prefs[ORB_SPACE_CARD_CORNER_RADIUS_DP] = updated.orbSpaceCardCornerRadiusDp
            prefs[ORB_SPACE_LAYOUT_MODE] = updated.orbSpaceLayoutMode
            prefs[ORB_SPACE_ACCENT_COLOR] = updated.orbSpaceAccentColor
            prefs[OPEN_WITH_STYLE] = updated.openWithStyle
            prefs[ENABLE_WEB_SEARCH_IN_OPEN_WITH] = updated.enableWebSearchInOpenWith
            prefs[ENABLE_AI_SEARCH_IN_OPEN_WITH] = updated.enableAISearchInOpenWith
            prefs[SHOW_ORB_BUTTON] = updated.showOrbButton
            prefs[ENABLE_ORB_SPACE] = updated.enableOrbSpace
            prefs[LOCK_ORB_SPACE] = updated.lockOrbSpace
            prefs[FREE_FORM_PLACEMENT] = updated.freeFormPlacement
            prefs[SEARCH_RESULT_BG_COLOR] = updated.searchResultBgColor
            prefs[SEARCH_RESULT_BG_OPACITY] = updated.searchResultBgOpacity
            prefs[SEARCH_RESULT_PADDING_DP] = updated.searchResultPaddingDp
            prefs[SEARCH_RESULT_TEXT_SIZE_SP] = updated.searchResultTextSizeSp
            prefs[WELLNESS_WIDGET_BG_OPACITY] = updated.wellnessWidgetBgOpacity
            prefs[WELLNESS_WIDGET_HEIGHT_DP] = updated.wellnessWidgetHeightDp
            prefs[WELLNESS_WIDGET_CORNER_RADIUS_DP] = updated.wellnessWidgetCornerRadiusDp
            prefs[WELLNESS_WIDGET_ACCENT_COLOR] = updated.wellnessWidgetAccentColor
            prefs[HAS_COMPLETED_ONBOARDING] = updated.hasCompletedOnboarding

            val activeIconPack = updated.activeIconPack
            if (activeIconPack != null) {
                prefs[ACTIVE_ICON_PACK] = activeIconPack
            } else {
                prefs.remove(ACTIVE_ICON_PACK)
            }

            val drawerBgImage = updated.drawerBgImage
            if (drawerBgImage != null) {
                prefs[DRAWER_BG_IMAGE] = drawerBgImage
            } else {
                prefs.remove(DRAWER_BG_IMAGE)
            }
        }
    }
}
