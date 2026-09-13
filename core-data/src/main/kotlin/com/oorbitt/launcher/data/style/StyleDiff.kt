package com.oorbitt.launcher.data.style

import com.oorbitt.launcher.model.LauncherSettings

data class DiffItem(
    val propertyName: String,
    val currentValue: String,
    val newValue: String,
    val isChanged: Boolean
)

object StyleDiff {
    fun compute(current: LauncherSettings, blob: StyleBlob): List<DiffItem> {
        val diffs = mutableListOf<DiffItem>()

        fun addDiff(name: String, currentVal: Any, newVal: Any) {
            diffs.add(
                DiffItem(
                    propertyName = name,
                    currentValue = currentVal.toString(),
                    newValue = newVal.toString(),
                    isChanged = currentVal != newVal
                )
            )
        }

        addDiff("Theme Mode", current.themeMode.name, blob.themeMode)
        addDiff("Icon Shape", current.iconShape.name, blob.iconShape)
        addDiff("Desktop Grid", "${current.gridColumns}x${current.gridRows}", "${blob.gridColumns}x${blob.gridRows}")
        addDiff("Dock Slots", current.dockSlots, blob.dockSlots)
        addDiff("Freeform Placement", current.freeFormPlacement, blob.freeFormIcons)
        addDiff("Drawer Columns", current.drawerColumns, blob.drawerColumns)
        addDiff("Drawer BG Color", current.drawerBgColor, blob.drawerBackgroundColor)
        addDiff("Drawer BG Opacity", "${current.drawerBgOpacity}%", "${blob.drawerBackgroundOpacity}%")
        addDiff("Search Bar Style", current.searchBarStyle.name, blob.searchStyle)
        addDiff("Search Bar Position", current.searchBarPosition, blob.searchPosition)
        addDiff("OrbSpace Layout Mode", current.orbSpaceLayoutMode, blob.orbSpaceLayout)
        addDiff("OrbSpace Accent Color", current.orbSpaceAccentColor, blob.orbAccentColor)
        addDiff("Daily Scroll Limit", current.dailyScrollLimit, blob.dailyScrollLimit)

        return diffs
    }
}
