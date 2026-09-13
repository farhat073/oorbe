package com.oorbitt.launcher.data.style

import com.google.common.truth.Truth.assertThat
import com.oorbitt.launcher.model.*
import org.junit.Test

class StyleBlobSerializerTest {

    @Test
    fun testExportImportRoundTrip() {
        val initialSettings = LauncherSettings(
            gridColumns = 4,
            gridRows = 6,
            dockSlots = 6,
            drawerColumns = 5,
            desktopIconSizeDp = 60,
            drawerIconSizeDp = 58,
            dockIconSizeDp = 56,
            showDesktopLabels = false,
            showDockLabels = true,
            labelSizeSp = 14,
            themeMode = ThemeMode.DARK,
            iconShape = IconShape.SQUIRCLE,
            searchBarStyle = SearchBarStyle.PILL,
            searchBarPosition = "TOP",
            orbSpaceLayoutMode = "BENTO",
            orbSpaceAccentColor = "#FF5722",
            dailyScrollLimit = 150,
            freeFormPlacement = true,
            drawerBgColor = "#121212",
            drawerBgOpacity = 85,
            drawerCornerRadiusDp = 12
        )

        val gestures = listOf(
            GestureAction(GestureType.SWIPE_UP, LauncherAction.OPEN_DRAWER),
            GestureAction(GestureType.SWIPE_DOWN, LauncherAction.OPEN_SEARCH),
            GestureAction(GestureType.DOUBLE_TAP, LauncherAction.LOCK_SCREEN)
        )

        // 1. Export
        val blob = StyleBlobSerializer.export(
            settings = initialSettings,
            gestures = gestures,
            iconPackName = "Lawnicons",
            wallpaperName = "Abstract Waves"
        )

        assertThat(blob.themeMode).isEqualTo("DARK")
        assertThat(blob.iconShape).isEqualTo("SQUIRCLE")
        assertThat(blob.gridColumns).isEqualTo(4)
        assertThat(blob.gridRows).isEqualTo(6)
        assertThat(blob.dockSlots).isEqualTo(6)
        assertThat(blob.desktopIconSize).isEqualTo(60)
        assertThat(blob.showDesktopLabels).isFalse()
        assertThat(blob.showDockLabels).isTrue()
        assertThat(blob.labelTextSize).isEqualTo(14)
        assertThat(blob.freeFormIcons).isTrue()
        assertThat(blob.drawerColumns).isEqualTo(5)
        assertThat(blob.drawerBackgroundColor).isEqualTo("#121212")
        assertThat(blob.drawerBackgroundOpacity).isEqualTo(85)
        assertThat(blob.drawerCornerRadius).isEqualTo(12)
        assertThat(blob.searchStyle).isEqualTo("PILL")
        assertThat(blob.searchPosition).isEqualTo("TOP")
        assertThat(blob.orbSpaceLayout).isEqualTo("BENTO")
        assertThat(blob.orbAccentColor).isEqualTo("#FF5722")
        assertThat(blob.dailyScrollLimit).isEqualTo(150)
        assertThat(blob.swipeUpAction).isEqualTo("OPEN_DRAWER")
        assertThat(blob.swipeDownAction).isEqualTo("OPEN_SEARCH")
        assertThat(blob.doubleTapAction).isEqualTo("LOCK_SCREEN")
        assertThat(blob.pinchAction).isEqualTo("NONE")
        assertThat(blob.iconPackName).isEqualTo("Lawnicons")
        assertThat(blob.wallpaperName).isEqualTo("Abstract Waves")

        // 2. JSON Serialization roundtrip
        val json = StyleBlobSerializer.toJson(blob)
        val deserializedBlob = StyleBlobSerializer.fromJson(json)

        assertThat(deserializedBlob).isEqualTo(blob)

        // 3. Import
        val currentSettings = LauncherSettings() // default settings
        val importedSettings = StyleBlobSerializer.import(deserializedBlob, currentSettings)

        assertThat(importedSettings.gridColumns).isEqualTo(4)
        assertThat(importedSettings.gridRows).isEqualTo(6)
        assertThat(importedSettings.dockSlots).isEqualTo(6)
        assertThat(importedSettings.desktopIconSizeDp).isEqualTo(60)
        assertThat(importedSettings.drawerIconSizeDp).isEqualTo(58)
        assertThat(importedSettings.dockIconSizeDp).isEqualTo(56)
        assertThat(importedSettings.showDesktopLabels).isFalse()
        assertThat(importedSettings.showDockLabels).isTrue()
        assertThat(importedSettings.labelSizeSp).isEqualTo(14)
        assertThat(importedSettings.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(importedSettings.iconShape).isEqualTo(IconShape.SQUIRCLE)
        assertThat(importedSettings.searchBarStyle).isEqualTo(SearchBarStyle.PILL)
        assertThat(importedSettings.searchBarPosition).isEqualTo("TOP")
        assertThat(importedSettings.orbSpaceLayoutMode).isEqualTo("BENTO")
        assertThat(importedSettings.orbSpaceAccentColor).isEqualTo("#FF5722")
        assertThat(importedSettings.dailyScrollLimit).isEqualTo(150)
        assertThat(importedSettings.freeFormPlacement).isTrue()
        assertThat(importedSettings.drawerBgColor).isEqualTo("#121212")
        assertThat(importedSettings.drawerBgOpacity).isEqualTo(85)
        assertThat(importedSettings.drawerCornerRadiusDp).isEqualTo(12)

        // 4. Import gestures
        val importedGestures = StyleBlobSerializer.importGestures(deserializedBlob)
        assertThat(importedGestures).hasSize(3)
        assertThat(importedGestures).contains(GestureAction(GestureType.SWIPE_UP, LauncherAction.OPEN_DRAWER))
        assertThat(importedGestures).contains(GestureAction(GestureType.SWIPE_DOWN, LauncherAction.OPEN_SEARCH))
        assertThat(importedGestures).contains(GestureAction(GestureType.DOUBLE_TAP, LauncherAction.LOCK_SCREEN))
    }

    @Test
    fun testDiffComputation() {
        val current = LauncherSettings(
            gridColumns = 5,
            gridRows = 5,
            themeMode = ThemeMode.SYSTEM,
            iconShape = IconShape.CIRCLE
        )

        val blob = StyleBlob(
            exportedAt = 123456L,
            themeMode = "DARK",
            iconShape = "CIRCLE", // no change
            gridColumns = 4,      // changed
            gridRows = 5          // no change
        )

        val diffs = StyleDiff.compute(current, blob)
        val themeDiff = diffs.find { it.propertyName == "Theme Mode" }!!
        assertThat(themeDiff.isChanged).isTrue()
        assertThat(themeDiff.currentValue).isEqualTo("SYSTEM")
        assertThat(themeDiff.newValue).isEqualTo("DARK")

        val shapeDiff = diffs.find { it.propertyName == "Icon Shape" }!!
        assertThat(shapeDiff.isChanged).isFalse()

        val gridDiff = diffs.find { it.propertyName == "Desktop Grid" }!!
        assertThat(gridDiff.isChanged).isTrue()
        assertThat(gridDiff.currentValue).isEqualTo("5x5")
        assertThat(gridDiff.newValue).isEqualTo("4x5")
    }

    @Test
    fun testImportFaultToleranceAndMigration() {
        val current = LauncherSettings(
            gridColumns = 5,
            gridRows = 5,
            themeMode = ThemeMode.SYSTEM
        )

        // Blob with invalid/garbage enum names or missing optional fields
        val invalidBlob = StyleBlob(
            exportedAt = 123456L,
            themeMode = "NOT_A_REAL_THEME",
            iconShape = "GARBAGE_SHAPE",
            gridColumns = 4
        )

        val imported = StyleBlobSerializer.import(invalidBlob, current)

        // Should fallback to current values for invalid enums instead of crashing
        assertThat(imported.themeMode).isEqualTo(ThemeMode.SYSTEM)
        // Should apply valid fields
        assertThat(imported.gridColumns).isEqualTo(4)
    }
}
