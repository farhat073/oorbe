package com.oorbitt.launcher.data.repository

import com.oorbitt.launcher.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val allApps: Flow<List<AppInfo>>
    val hiddenApps: Flow<Set<String>>
    val lockedApps: Flow<Set<String>>
    suspend fun hideApp(componentKey: String)
    suspend fun unhideApp(componentKey: String)
    suspend fun lockApp(componentKey: String)
    suspend fun unlockApp(componentKey: String)
    suspend fun refreshAppList()
    suspend fun saveIconOverride(componentKey: String, customLabel: String?, customIconUri: String?)
    suspend fun deleteIconOverride(componentKey: String)
}
