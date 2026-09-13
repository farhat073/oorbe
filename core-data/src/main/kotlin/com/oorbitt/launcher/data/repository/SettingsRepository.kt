package com.oorbitt.launcher.data.repository

import com.oorbitt.launcher.model.LauncherSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<LauncherSettings>
    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings)
}
