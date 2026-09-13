package com.oorbitt.launcher.data.style

import com.oorbitt.launcher.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object StyleBlobSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = false
    }

    fun export(
        settings: LauncherSettings,
        gestures: List<GestureAction> = emptyList(),
        iconPackName: String? = null,
        wallpaperName: String? = null,
        shareAppearance: Boolean = true,
        shareWorkspace: Boolean = true,
        shareDrawer: Boolean = true,
        shareOrbSpace: Boolean = true,
        shareSearch: Boolean = true,
        shareGestures: Boolean = true,
        shareWellness: Boolean = true
    ): StyleBlob {
        val swipeUp = gestures.find { it.gestureType == GestureType.SWIPE_UP }?.action?.name ?: "NONE"
        val swipeDown = gestures.find { it.gestureType == GestureType.SWIPE_DOWN }?.action?.name ?: "NONE"
        val doubleTap = gestures.find { it.gestureType == GestureType.DOUBLE_TAP }?.action?.name ?: "NONE"
        val pinch = gestures.find { it.gestureType == GestureType.PINCH_IN }?.action?.name ?: "NONE"

        return StyleBlob(
            schemaVersion = 2,
            exportedAt = System.currentTimeMillis(),
            
            // Share selection flags
            shareAppearance = shareAppearance,
            shareWorkspace = shareWorkspace,
            shareDrawer = shareDrawer,
            shareOrbSpace = shareOrbSpace,
            shareSearch = shareSearch,
            shareGestures = shareGestures,
            shareWellness = shareWellness,

            // Appearance
            themeMode = settings.themeMode.name,
            iconShape = settings.iconShape.name,
            iconCornerTopStart = settings.iconCornerTopStart,
            iconCornerTopEnd = settings.iconCornerTopEnd,
            iconCornerBottomStart = settings.iconCornerBottomStart,
            iconCornerBottomEnd = settings.iconCornerBottomEnd,
            iconCornerCut = settings.iconCornerCut,
            desktopIconSize = settings.desktopIconSizeDp,
            drawerIconSize = settings.drawerIconSizeDp,
            dockIconSize = settings.dockIconSizeDp,
            showDesktopLabels = settings.showDesktopLabels,
            showDockLabels = settings.showDockLabels,
            labelTextSize = settings.labelSizeSp,

            // Workspace & Dock
            gridColumns = settings.gridColumns,
            gridRows = settings.gridRows,
            dockSlots = settings.dockSlots,
            freeFormIcons = settings.freeFormPlacement,

            // Drawer
            drawerColumns = settings.drawerColumns,
            drawerBackgroundColor = settings.drawerBgColor,
            drawerBackgroundOpacity = settings.drawerBgOpacity,
            drawerCornerRadius = settings.drawerCornerRadiusDp,
            drawerSpaceDp = settings.drawerSpaceDp,
            drawerCornerTopStartDp = settings.drawerCornerTopStartDp,
            drawerCornerTopEndDp = settings.drawerCornerTopEndDp,
            drawerCornerBottomStartDp = settings.drawerCornerBottomStartDp,
            drawerCornerBottomEndDp = settings.drawerCornerBottomEndDp,

            // Search
            searchStyle = settings.searchBarStyle.name,
            searchPosition = settings.searchBarPosition,
            searchBarCornerRadiusDp = settings.searchBarCornerRadiusDp,
            searchResultsSpacingDp = settings.searchResultsSpacingDp,
            searchBarPaddingDp = settings.searchBarPaddingDp,

            // OrbSpace
            orbSpaceLayout = settings.orbSpaceLayoutMode,
            orbAccentColor = settings.orbSpaceAccentColor,
            orbSpaceSpacingDp = settings.orbSpaceSpacingDp,
            orbSpaceCardCornerRadiusDp = settings.orbSpaceCardCornerRadiusDp,

            // Wellness
            dailyScrollLimit = settings.dailyScrollLimit,

            // Gestures
            swipeUpAction = swipeUp,
            swipeDownAction = swipeDown,
            doubleTapAction = doubleTap,
            pinchAction = pinch,

            iconPackName = iconPackName,
            wallpaperName = wallpaperName
        )
    }

    fun import(blob: StyleBlob, current: LauncherSettings): LauncherSettings {
        var result = current

        if (blob.shareAppearance) {
            val theme = try { ThemeMode.valueOf(blob.themeMode) } catch (e: Exception) { current.themeMode }
            val shape = try { IconShape.valueOf(blob.iconShape) } catch (e: Exception) { current.iconShape }
            result = result.copy(
                themeMode = theme,
                iconShape = shape,
                iconCornerTopStart = blob.iconCornerTopStart,
                iconCornerTopEnd = blob.iconCornerTopEnd,
                iconCornerBottomStart = blob.iconCornerBottomStart,
                iconCornerBottomEnd = blob.iconCornerBottomEnd,
                iconCornerCut = blob.iconCornerCut,
                desktopIconSizeDp = blob.desktopIconSize,
                drawerIconSizeDp = blob.drawerIconSize,
                dockIconSizeDp = blob.dockIconSize,
                showDesktopLabels = blob.showDesktopLabels,
                showDockLabels = blob.showDockLabels,
                labelSizeSp = blob.labelTextSize,
                activeIconPack = blob.iconPackName
            )
        }

        if (blob.shareWorkspace) {
            result = result.copy(
                gridColumns = blob.gridColumns,
                gridRows = blob.gridRows,
                dockSlots = blob.dockSlots,
                freeFormPlacement = blob.freeFormIcons
            )
        }

        if (blob.shareDrawer) {
            result = result.copy(
                drawerColumns = blob.drawerColumns,
                drawerBgColor = blob.drawerBackgroundColor,
                drawerBgOpacity = blob.drawerBackgroundOpacity,
                drawerCornerRadiusDp = blob.drawerCornerRadius,
                drawerSpaceDp = blob.drawerSpaceDp,
                drawerCornerTopStartDp = blob.drawerCornerTopStartDp,
                drawerCornerTopEndDp = blob.drawerCornerTopEndDp,
                drawerCornerBottomStartDp = blob.drawerCornerBottomStartDp,
                drawerCornerBottomEndDp = blob.drawerCornerBottomEndDp
            )
        }

        if (blob.shareSearch) {
            val style = try { SearchBarStyle.valueOf(blob.searchStyle) } catch (e: Exception) { current.searchBarStyle }
            result = result.copy(
                searchBarStyle = style,
                searchBarPosition = blob.searchPosition,
                searchBarCornerRadiusDp = blob.searchBarCornerRadiusDp,
                searchResultsSpacingDp = blob.searchResultsSpacingDp,
                searchBarPaddingDp = blob.searchBarPaddingDp
            )
        }

        if (blob.shareOrbSpace) {
            result = result.copy(
                orbSpaceLayoutMode = blob.orbSpaceLayout,
                orbSpaceAccentColor = blob.orbAccentColor,
                orbSpaceSpacingDp = blob.orbSpaceSpacingDp,
                orbSpaceCardCornerRadiusDp = blob.orbSpaceCardCornerRadiusDp
            )
        }

        if (blob.shareWellness) {
            result = result.copy(
                dailyScrollLimit = blob.dailyScrollLimit
            )
        }

        return result
    }

    fun importGestures(blob: StyleBlob): List<GestureAction> {
        val list = mutableListOf<GestureAction>()
        if (!blob.shareGestures) return list

        fun parseAction(actionStr: String): LauncherAction {
            return try { LauncherAction.valueOf(actionStr) } catch (e: Exception) { LauncherAction.NONE }
        }

        val swipeUp = parseAction(blob.swipeUpAction)
        if (swipeUp != LauncherAction.NONE && swipeUp != LauncherAction.LAUNCH_APP) {
            list.add(GestureAction(GestureType.SWIPE_UP, swipeUp))
        }

        val swipeDown = parseAction(blob.swipeDownAction)
        if (swipeDown != LauncherAction.NONE && swipeDown != LauncherAction.LAUNCH_APP) {
            list.add(GestureAction(GestureType.SWIPE_DOWN, swipeDown))
        }

        val doubleTap = parseAction(blob.doubleTapAction)
        if (doubleTap != LauncherAction.NONE && doubleTap != LauncherAction.LAUNCH_APP) {
            list.add(GestureAction(GestureType.DOUBLE_TAP, doubleTap))
        }

        val pinch = parseAction(blob.pinchAction)
        if (pinch != LauncherAction.NONE && pinch != LauncherAction.LAUNCH_APP) {
            list.add(GestureAction(GestureType.PINCH_IN, pinch))
        }

        return list
    }

    fun toJson(blob: StyleBlob): String = json.encodeToString(blob)
    fun fromJson(jsonStr: String): StyleBlob = json.decodeFromString(jsonStr)
}
