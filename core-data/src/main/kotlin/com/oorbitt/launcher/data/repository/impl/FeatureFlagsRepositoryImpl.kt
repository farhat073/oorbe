package com.oorbitt.launcher.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.oorbitt.launcher.data.repository.FeatureFlagsRepository
import com.oorbitt.launcher.model.Feature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeatureFlagsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : FeatureFlagsRepository {

    override fun isEnabled(feature: Feature): Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[booleanPreferencesKey(feature.key)] ?: feature.defaultEnabled
        }

    override suspend fun setEnabled(feature: Feature, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(feature.key)] = enabled
        }
    }

    override fun allFlags(): Flow<Map<Feature, Boolean>> =
        dataStore.data.map { prefs ->
            Feature.entries.associateWith { feature ->
                prefs[booleanPreferencesKey(feature.key)] ?: feature.defaultEnabled
            }
        }
}
