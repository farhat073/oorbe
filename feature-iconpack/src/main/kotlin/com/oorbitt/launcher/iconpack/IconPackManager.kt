package com.oorbitt.launcher.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import com.oorbitt.launcher.ui.util.IconPackInfo
import com.oorbitt.launcher.ui.util.IconPackProvider
import org.xmlpull.v1.XmlPullParser
import java.io.InputStreamReader

class IconPackManager(private val context: Context) : IconPackProvider {
    private val pm = context.packageManager
    
    // Cache for loaded icon packs mappings: packageName -> Map<ComponentInfo, drawableName>
    private val iconPackCache = mutableMapOf<String, IconPackMappings>()
    
    // In-memory cache for resolved icons: "iconpack/package/activity" -> Bitmap
    private val resolvedIconCache = LruCache<String, Bitmap>(5 * 1024 * 1024) // 5 MB

    data class IconPackMappings(
        val packageName: String,
        val appfilter: Map<String, String>,
        val iconBacks: List<String>,
        val iconMask: String?,
        val iconUpon: String?,
        val scale: Float
    )

    override fun getInstalledIconPacks(): List<IconPackInfo> {
        val list = mutableListOf<IconPackInfo>()
        val intents = listOf(
            Intent("com.novalauncher.THEME"),
            Intent("org.adw.launcher.THEMES")
        )
        for (intent in intents) {
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (ri in resolveInfos) {
                val pkgName = ri.activityInfo.packageName
                if (list.none { it.packageName == pkgName }) {
                    val label = ri.loadLabel(pm).toString()
                    val icon = ri.loadIcon(pm)
                    list.add(IconPackInfo(pkgName, label, ri.loadIcon(pm)))
                }
            }
        }
        return list.sortedBy { it.label }
    }

    @Synchronized
    fun getIconPackMappings(packageName: String): IconPackMappings {
        iconPackCache[packageName]?.let { return it }
        val mappings = parseIconPack(packageName)
        iconPackCache[packageName] = mappings
        return mappings
    }

    private fun parseIconPack(packageName: String): IconPackMappings {
        val appfilter = mutableMapOf<String, String>()
        val iconBacks = mutableListOf<String>()
        var iconMask: String? = null
        var iconUpon: String? = null
        var scale = 1.0f

        try {
            val resources = pm.getResourcesForApplication(packageName)
            var inputStream = try {
                resources.assets.open("appfilter.xml")
            } catch (e: Exception) {
                null
            }

            if (inputStream == null) {
                val resId = resources.getIdentifier("appfilter", "xml", packageName)
                if (resId > 0) {
                    val xpp = resources.getXml(resId)
                    var eventType = xpp.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            val tagName = xpp.name
                            if (tagName == "item") {
                                val component = xpp.getAttributeValue(null, "component")
                                val drawable = xpp.getAttributeValue(null, "drawable")
                                if (component != null && drawable != null) {
                                    val cleaned = cleanComponent(component)
                                    appfilter[cleaned] = drawable
                                }
                            } else if (tagName.startsWith("iconback")) {
                                for (i in 0 until xpp.attributeCount) {
                                    xpp.getAttributeValue(i)?.let { iconBacks.add(it) }
                                }
                            } else if (tagName == "iconmask") {
                                if (xpp.attributeCount > 0) {
                                    iconMask = xpp.getAttributeValue(0)
                                }
                            } else if (tagName == "iconupon") {
                                if (xpp.attributeCount > 0) {
                                    iconUpon = xpp.getAttributeValue(0)
                                }
                            } else if (tagName == "scale") {
                                val factorStr = xpp.getAttributeValue(null, "factor")
                                if (factorStr != null) {
                                    scale = factorStr.toFloatOrNull() ?: 1.0f
                                }
                            }
                        }
                        eventType = xpp.next()
                    }
                    return IconPackMappings(packageName, appfilter, iconBacks, iconMask, iconUpon, scale)
                }
            }

            if (inputStream != null) {
                inputStream.use { stream ->
                    val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                    val xpp = factory.newPullParser()
                    xpp.setInput(InputStreamReader(stream))
                    var eventType = xpp.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            val tagName = xpp.name
                            if (tagName == "item") {
                                val component = xpp.getAttributeValue(null, "component")
                                val drawable = xpp.getAttributeValue(null, "drawable")
                                if (component != null && drawable != null) {
                                    val cleaned = cleanComponent(component)
                                    appfilter[cleaned] = drawable
                                }
                            } else if (tagName == "iconmask") {
                                if (xpp.attributeCount > 0) {
                                    iconMask = xpp.getAttributeValue(0)
                                }
                            } else if (tagName == "iconupon") {
                                if (xpp.attributeCount > 0) {
                                    iconUpon = xpp.getAttributeValue(0)
                                }
                            } else if (tagName == "scale") {
                                val factorStr = xpp.getAttributeValue(null, "factor")
                                if (factorStr != null) {
                                    scale = factorStr.toFloatOrNull() ?: 1.0f
                                }
                            } else if (tagName.startsWith("iconback")) {
                                for (i in 0 until xpp.attributeCount) {
                                    val valStr = xpp.getAttributeValue(i)
                                    if (valStr != null) iconBacks.add(valStr)
                                }
                            }
                        }
                        eventType = xpp.next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Failed to parse appfilter.xml for $packageName", e)
        }

        return IconPackMappings(packageName, appfilter, iconBacks, iconMask, iconUpon, scale)
    }

    private fun cleanComponent(component: String): String {
        if (component.startsWith("ComponentInfo{") && component.endsWith("}")) {
            return component.substring("ComponentInfo{".length, component.length - 1)
        }
        return component
    }

    override fun getIcon(
        packageName: String,
        activityName: String,
        iconPackPkg: String,
        fallbackDrawable: Drawable
    ): Bitmap {
        val cacheKey = "$iconPackPkg/$packageName/$activityName"
        resolvedIconCache.get(cacheKey)?.let { return it }

        val mappings = getIconPackMappings(iconPackPkg)
        val componentKey = "$packageName/$activityName"
        val drawableName = mappings.appfilter[componentKey]
            ?: mappings.appfilter[packageName]
            ?: mappings.appfilter.entries.firstOrNull { it.key.startsWith("$packageName/") }?.value

        val resultBitmap = if (drawableName != null) {
            loadBitmapFromIconPack(iconPackPkg, drawableName)
        } else {
            null
        }

        val finalBitmap = if (resultBitmap != null) {
            resultBitmap
        } else {
            generateMaskedIcon(iconPackPkg, mappings, fallbackDrawable)
        }

        resolvedIconCache.put(cacheKey, finalBitmap)
        return finalBitmap
    }

    private fun loadBitmapFromIconPack(iconPackPkg: String, drawableName: String): Bitmap? {
        try {
            val resources = pm.getResourcesForApplication(iconPackPkg)
            val resId = resources.getIdentifier(drawableName, "drawable", iconPackPkg)
            if (resId > 0) {
                val drawable = resources.getDrawable(resId, null)
                return drawableToBitmap(drawable)
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Failed to load bitmap $drawableName from $iconPackPkg", e)
        }
        return null
    }

    private fun generateMaskedIcon(
        iconPackPkg: String,
        mappings: IconPackMappings,
        fallbackDrawable: Drawable
    ): Bitmap {
        val originalBitmap = drawableToBitmap(fallbackDrawable)
        val width = originalBitmap.width
        val height = originalBitmap.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Draw IconBack (background) if defined
        if (mappings.iconBacks.isNotEmpty()) {
            val backDrawableName = mappings.iconBacks[Math.abs(fallbackDrawable.hashCode()) % mappings.iconBacks.size]
            val backBitmap = loadBitmapFromIconPack(iconPackPkg, backDrawableName)
            if (backBitmap != null) {
                val src = Rect(0, 0, backBitmap.width, backBitmap.height)
                val dst = RectF(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawBitmap(backBitmap, src, dst, paint)
            }
        }

        // 2. Draw scaled app icon masked with IconMask
        val scaledWidth = (width * mappings.scale).toInt()
        val scaledHeight = (height * mappings.scale).toInt()
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f

        val scaledAppIcon = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

        val maskBitmap = mappings.iconMask?.let { loadBitmapFromIconPack(iconPackPkg, it) }
        if (maskBitmap != null) {
            val maskScaled = Bitmap.createScaledBitmap(maskBitmap, width, height, true)
            
            val maskedLayer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val layerCanvas = Canvas(maskedLayer)
            layerCanvas.drawBitmap(scaledAppIcon, left, top, paint)
            
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            layerCanvas.drawBitmap(maskScaled, 0f, 0f, paint)
            paint.xfermode = null
            
            canvas.drawBitmap(maskedLayer, 0f, 0f, paint)
        } else {
            canvas.drawBitmap(scaledAppIcon, left, top, paint)
        }

        // 3. Draw IconUpon (overlay) if defined
        if (mappings.iconUpon != null) {
            val uponBitmap = loadBitmapFromIconPack(iconPackPkg, mappings.iconUpon)
            if (uponBitmap != null) {
                val src = Rect(0, 0, uponBitmap.width, uponBitmap.height)
                val dst = RectF(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawBitmap(uponBitmap, src, dst, paint)
            }
        }

        return result
    }

    private val MAX_ICON_SIZE = 192

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bmp = drawable.bitmap
            if (bmp.width <= MAX_ICON_SIZE && bmp.height <= MAX_ICON_SIZE) {
                return bmp
            }
            val ratio = bmp.width.toFloat() / bmp.height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (bmp.width > bmp.height) {
                newWidth = MAX_ICON_SIZE
                newHeight = (MAX_ICON_SIZE / ratio).toInt().coerceAtLeast(1)
            } else {
                newHeight = MAX_ICON_SIZE
                newWidth = (MAX_ICON_SIZE * ratio).toInt().coerceAtLeast(1)
            }
            return Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true)
        }
        val rawW = drawable.intrinsicWidth
        val rawH = drawable.intrinsicHeight
        val (width, height) = if (rawW <= 0 || rawH <= 0) {
            Pair(144, 144)
        } else if (rawW > MAX_ICON_SIZE || rawH > MAX_ICON_SIZE) {
            val ratio = rawW.toFloat() / rawH.toFloat()
            if (rawW > rawH) {
                Pair(MAX_ICON_SIZE, (MAX_ICON_SIZE / ratio).toInt().coerceAtLeast(1))
            } else {
                Pair((MAX_ICON_SIZE * ratio).toInt().coerceAtLeast(1), MAX_ICON_SIZE)
            }
        } else {
            Pair(rawW, rawH)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    override fun getAllIconPackDrawables(packageName: String): List<String> {
        val drawables = mutableSetOf<String>()
        try {
            val resources = pm.getResourcesForApplication(packageName)
            // Try to find drawable.xml in xml resources
            val drawableResId = resources.getIdentifier("drawable", "xml", packageName)
            if (drawableResId > 0) {
                val xpp = resources.getXml(drawableResId)
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val tagName = xpp.name
                        if (tagName == "item" || tagName == "drawable") {
                            val drawableName = xpp.getAttributeValue(null, "drawable") 
                                ?: xpp.getAttributeValue(null, "name")
                            if (drawableName != null) {
                                drawables.add(drawableName)
                            }
                        }
                    }
                    eventType = xpp.next()
                }
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Failed to parse drawable.xml for $packageName", e)
        }

        // If empty, fallback to appfilter mappings
        if (drawables.isEmpty()) {
            try {
                val mappings = getIconPackMappings(packageName)
                drawables.addAll(mappings.appfilter.values)
            } catch (e: Exception) {
                Log.e("IconPackManager", "Failed to load fallback appfilter drawables for $packageName", e)
            }
        }

        return drawables.toList().sorted()
    }
}
