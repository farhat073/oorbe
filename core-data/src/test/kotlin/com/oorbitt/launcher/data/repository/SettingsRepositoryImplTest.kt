package com.oorbitt.launcher.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import com.oorbitt.launcher.data.repository.impl.SettingsRepositoryImpl
import com.oorbitt.launcher.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tmpFolder.newFolder(), "test_settings.preferences_pb") }
        )
        repository = SettingsRepositoryImpl(dataStore)
    }

    @Test
    fun defaultSettings_areReturnedInitially() = runTest {
        val settings = repository.settings.first()
        assertThat(settings.gridColumns).isEqualTo(5)
        assertThat(settings.gridRows).isEqualTo(5)
        assertThat(settings.themeMode).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun updateSettings_persistsAndEmitsUpdatedValues() = runTest {
        repository.updateSettings { current ->
            current.copy(
                gridColumns = 6,
                gridRows = 7,
                themeMode = ThemeMode.DARK
            )
        }

        val settings = repository.settings.first()
        assertThat(settings.gridColumns).isEqualTo(6)
        assertThat(settings.gridRows).isEqualTo(7)
        assertThat(settings.themeMode).isEqualTo(ThemeMode.DARK)
    }
}
