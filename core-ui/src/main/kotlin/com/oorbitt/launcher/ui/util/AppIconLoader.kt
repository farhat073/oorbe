package com.oorbitt.launcher.ui.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Singleton cache for application icons to prevent memory thrashing and GC pauses.
 * Default max size: 12 MB, auto-scales based on device memory class.
 */
object AppIconCache {
    private var cache: LruCache<String, ImageBitmap>? = null

    private fun getOrCreateCache(): LruCache<String, ImageBitmap> {
        return cache ?: synchronized(this) {
            cache ?: createDefaultCache().also { cache = it }
        }
    }

    /** Call once from Application.onCreate() with context for optimal sizing. */
    fun init(context: Context) {
        synchronized(this) {
            if (cache == null) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryClass = am.memoryClass // in MB
                val maxCacheMb = (memoryClass / 8).coerceIn(4, 12)
                cache = object : LruCache<String, ImageBitmap>(maxCacheMb * 1024 * 1024) {
                    override fun sizeOf(key: String, value: ImageBitmap): Int {
                        return value.width * value.height * 4
                    }
                }
            }
        }
    }

    private fun createDefaultCache(): LruCache<String, ImageBitmap> {
        return object : LruCache<String, ImageBitmap>(12 * 1024 * 1024) {
            override fun sizeOf(key: String, value: ImageBitmap): Int {
                return value.width * value.height * 4
            }
        }
    }

    fun get(key: String): ImageBitmap? = getOrCreateCache().get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        getOrCreateCache().put(key, bitmap)
    }

    fun evictAll() {
        cache?.evictAll()
    }
}

private const val MAX_ICON_SIZE_PX = 144

private fun Bitmap.scaleDownIfTooLarge(): Bitmap {
    if (width <= MAX_ICON_SIZE_PX && height <= MAX_ICON_SIZE_PX) return this
    val ratio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int
    if (width > height) {
        newWidth = MAX_ICON_SIZE_PX
        newHeight = (MAX_ICON_SIZE_PX / ratio).toInt().coerceAtLeast(1)
    } else {
        newHeight = MAX_ICON_SIZE_PX
        newWidth = (MAX_ICON_SIZE_PX * ratio).toInt().coerceAtLeast(1)
    }
    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

private fun renderDrawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap.scaleDownIfTooLarge()
    }
    val rawW = drawable.intrinsicWidth
    val rawH = drawable.intrinsicHeight
    val (w, h) = if (rawW <= 0 || rawH <= 0) {
        Pair(144, 144)
    } else if (rawW > MAX_ICON_SIZE_PX || rawH > MAX_ICON_SIZE_PX) {
        val ratio = rawW.toFloat() / rawH.toFloat()
        if (rawW > rawH) {
            Pair(MAX_ICON_SIZE_PX, (MAX_ICON_SIZE_PX / ratio).toInt().coerceAtLeast(1))
        } else {
            Pair((MAX_ICON_SIZE_PX * ratio).toInt().coerceAtLeast(1), MAX_ICON_SIZE_PX)
        }
    } else {
        Pair(rawW, rawH)
    }
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return bmp
}

/**
 * Asynchronously loads and caches the application icon.
 * Resolves the icon using the active icon pack or falls back to system icon.
 */
@Composable
fun rememberAppIcon(
    context: Context,
    packageName: String,
    activityName: String? = null,
    activeIconPack: String? = null,
    customIconUri: String? = null
): ImageBitmap? {
    val iconPackProvider = remember {
        try {
            org.koin.core.context.GlobalContext.get().getOrNull<IconPackProvider>()
        } catch (e: Exception) {
            null
        }
    }

    val cacheKey = if (!customIconUri.isNullOrBlank()) {
        "custom/$customIconUri"
    } else {
        "${activeIconPack ?: "system"}/$packageName/${activityName ?: ""}"
    }
    var icon by remember(cacheKey) { mutableStateOf(AppIconCache.get(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (icon == null) {
            val loadedIcon = withContext(Dispatchers.IO) {
                try {
                    val bitmap = if (!customIconUri.isNullOrBlank()) {
                        try {
                            if (customIconUri.startsWith("iconpack://")) {
                                val uri = android.net.Uri.parse(customIconUri)
                                val iconPackPkg = uri.host
                                val drawableName = uri.path?.removePrefix("/")
                                if (iconPackPkg != null && drawableName != null) {
                                    val pm = context.packageManager
                                    val resources = pm.getResourcesForApplication(iconPackPkg)
                                    val resId = resources.getIdentifier(drawableName, "drawable", iconPackPkg)
                                    if (resId > 0) {
                                        val drawable = resources.getDrawable(resId, null)
                                        renderDrawableToBitmap(drawable)
                                    } else null
                                } else null
                            } else {
                                val uri = android.net.Uri.parse(customIconUri)
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                                inputStream?.close()
                                bmp?.scaleDownIfTooLarge()
                            }
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    val finalBitmap = bitmap ?: run {
                        val pm = context.packageManager
                        val fallbackDrawable = if (activityName != null) {
                            try {
                                val comp = android.content.ComponentName(packageName, activityName)
                                pm.getActivityIcon(comp)
                            } catch (e: Exception) {
                                pm.getApplicationIcon(packageName)
                            }
                        } else {
                            pm.getApplicationIcon(packageName)
                        }

                        if (activeIconPack != null && iconPackProvider != null) {
                            iconPackProvider.getIcon(packageName, activityName ?: "", activeIconPack, fallbackDrawable).scaleDownIfTooLarge()
                        } else {
                            renderDrawableToBitmap(fallbackDrawable)
                        }
                    }
                    finalBitmap?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            
            loadedIcon?.let {
                AppIconCache.put(cacheKey, it)
                icon = it
            }
        }
    }

    return icon
}

