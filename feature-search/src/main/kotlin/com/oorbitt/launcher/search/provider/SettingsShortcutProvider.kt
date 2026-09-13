package com.oorbitt.launcher.search.provider

import android.provider.Settings
import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchProvider
import java.util.Locale

class SettingsShortcutProvider : SearchProvider {
    override val priority: Int = 80 // Medium-High priority

    private val shortcuts = listOf(
        SettingItem("wifi", "Wi-Fi Settings", "Configure wireless connections", Settings.ACTION_WIFI_SETTINGS),
        SettingItem("bluetooth", "Bluetooth Settings", "Pair devices & bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS),
        SettingItem("battery", "Battery Settings", "Battery usage & battery saver", Settings.ACTION_BATTERY_SAVER_SETTINGS),
        SettingItem("display", "Display Settings", "Brightness, font size, sleep timer", Settings.ACTION_DISPLAY_SETTINGS),
        SettingItem("sound", "Sound & Vibration Settings", "Ringtone, volume, do not disturb", Settings.ACTION_SOUND_SETTINGS),
        SettingItem("storage", "Storage Settings", "Free up space & storage details", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        SettingItem("apps", "App Management Settings", "Manage installed applications", Settings.ACTION_APPLICATION_SETTINGS),
        SettingItem("date", "Date & Time Settings", "Set timezone, date, format", Settings.ACTION_DATE_SETTINGS),
        SettingItem("language", "Language & Locale Settings", "System languages, locales", Settings.ACTION_LOCALE_SETTINGS),
        SettingItem("settings", "System Settings", "Open main android settings", Settings.ACTION_SETTINGS)
    )

    override suspend fun search(query: String): List<SearchResult> {
        val cleaned = query.trim().lowercase(Locale.ROOT)
        if (cleaned.length < 2) return emptyList()

        return shortcuts.filter {
            it.keyword.contains(cleaned) || it.title.lowercase(Locale.ROOT).contains(cleaned)
        }.map { item ->
            SearchResult(
                id = "setting:${item.actionIntent}",
                title = item.title,
                subtitle = item.description,
                action = SearchResultAction.OpenSetting(item.actionIntent),
                relevanceScore = 0.8f,
                providerName = "Settings"
            )
        }
    }

    private data class SettingItem(
        val keyword: String,
        val title: String,
        val description: String,
        val actionIntent: String
    )
}
