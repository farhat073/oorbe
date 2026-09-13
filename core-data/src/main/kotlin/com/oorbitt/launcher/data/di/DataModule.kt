package com.oorbitt.launcher.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.oorbitt.launcher.data.db.LauncherDatabase
import com.oorbitt.launcher.data.repository.AppRepository
import com.oorbitt.launcher.data.repository.FeatureFlagsRepository
import com.oorbitt.launcher.data.repository.SettingsRepository
import com.oorbitt.launcher.data.repository.WorkspaceRepository
import com.oorbitt.launcher.data.repository.GestureRepository
import com.oorbitt.launcher.data.repository.VaultRepository
import com.oorbitt.launcher.data.repository.impl.AppRepositoryImpl
import com.oorbitt.launcher.data.repository.impl.FeatureFlagsRepositoryImpl
import com.oorbitt.launcher.data.repository.impl.SettingsRepositoryImpl
import com.oorbitt.launcher.data.repository.impl.WorkspaceRepositoryImpl
import com.oorbitt.launcher.data.repository.impl.GestureRepositoryImpl
import com.oorbitt.launcher.data.repository.impl.VaultRepositoryImpl
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "oorbitt_prefs")

val dataModule = module {
    // Database
    single {
        Room.databaseBuilder(
            get<Context>().applicationContext,
            LauncherDatabase::class.java,
            "oorbitt.db"
        ).fallbackToDestructiveMigration().build()
    }

    // DAOs
    single { get<LauncherDatabase>().workspaceDao() }
    single { get<LauncherDatabase>().hiddenAppDao() }
    single { get<LauncherDatabase>().lockedAppDao() }
    single { get<LauncherDatabase>().gestureMapDao() }
    single { get<LauncherDatabase>().iconOverrideDao() }
    single { get<LauncherDatabase>().vaultMemoDao() }
    single { get<LauncherDatabase>().profileDao() }
    single { get<LauncherDatabase>().appDietDao() }

    // DataStore
    single<DataStore<Preferences>> { get<Context>().applicationContext.dataStore }

    // Repositories
    single<AppRepository> { AppRepositoryImpl(get(), get(), get(), get()) }
    single<FeatureFlagsRepository> { FeatureFlagsRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<WorkspaceRepository> { WorkspaceRepositoryImpl(get()) }
    single<GestureRepository> { GestureRepositoryImpl(get()) }
    single<VaultRepository> { VaultRepositoryImpl(get(), get()) }
}

