package com.oorbitt.launcher

import android.app.Application
import com.oorbitt.launcher.data.di.dataModule
import com.oorbitt.launcher.ui.util.AppIconCache
import com.oorbitt.launcher.security.di.securityModule
import com.oorbitt.launcher.search.di.searchModule
import com.oorbitt.launcher.home.di.homeModule
import com.oorbitt.launcher.iconpack.di.iconPackModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class OorbittApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppIconCache.init(this)
        setupCrashHandler()
        initKoin()
    }

    private fun initKoin() {
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@OorbittApp)
            modules(
                dataModule,
                securityModule,
                searchModule,
                homeModule,
                iconPackModule,
                com.oorbitt.launcher.stylehub.di.styleHubModule
            )
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log crash, then restart gracefully
            try {
                // TODO: Phase 9 — implement crash logging
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
