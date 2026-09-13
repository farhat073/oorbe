package com.oorbitt.launcher.data.repository

import com.oorbitt.launcher.model.Feature
import kotlinx.coroutines.flow.Flow

interface FeatureFlagsRepository {
    fun isEnabled(feature: Feature): Flow<Boolean>
    suspend fun setEnabled(feature: Feature, enabled: Boolean)
    fun allFlags(): Flow<Map<Feature, Boolean>>
}
