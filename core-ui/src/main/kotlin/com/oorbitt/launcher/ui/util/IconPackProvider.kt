package com.oorbitt.launcher.ui.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

interface IconPackProvider {
    fun getInstalledIconPacks(): List<IconPackInfo>
    fun getIcon(
        packageName: String,
        activityName: String,
        iconPackPkg: String,
        fallbackDrawable: Drawable
    ): Bitmap
    fun getAllIconPackDrawables(packageName: String): List<String>
}
