package com.oorbitt.launcher.home.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

class LauncherAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return super.onCreateView(context, appWidgetId, appWidget)
    }
}

class WidgetHostManager(private val context: Context) {
    companion object {
        const val HOST_ID = 1024
    }

    val host = LauncherAppWidgetHost(context, HOST_ID)
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun startListening() {
        try {
            host.startListening()
        } catch (_: Exception) {}
    }

    fun stopListening() {
        try {
            host.stopListening()
        } catch (_: Exception) {}
    }

    fun allocateAppWidgetId(): Int {
        return host.allocateAppWidgetId()
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        host.deleteAppWidgetId(appWidgetId)
    }

    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return appWidgetManager.getAppWidgetInfo(appWidgetId)
    }

    fun createView(context: Context, appWidgetId: Int, appWidgetInfo: AppWidgetProviderInfo): AppWidgetHostView {
        return host.createView(context, appWidgetId, appWidgetInfo)
    }

    fun getInstalledProviders(): List<AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders
    }
}
