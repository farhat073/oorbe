package com.oorbitt.launcher.gesture

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.oorbitt.launcher.model.GestureAction
import com.oorbitt.launcher.model.LauncherAction

class GestureHandler(
    private val context: Context,
    private val onOpenDrawer: () -> Unit,
    private val onOpenSearch: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenVault: () -> Unit,
    private val onOpenOrbSpace: () -> Unit,
    private val onOpenOrbSearch: () -> Unit
) {
    fun execute(action: GestureAction) {
        when (action.action) {
            LauncherAction.OPEN_DRAWER -> onOpenDrawer()
            LauncherAction.OPEN_SEARCH -> onOpenSearch()
            LauncherAction.OPEN_SETTINGS -> onOpenSettings()
            LauncherAction.OPEN_NOTIFICATIONS -> expandNotifications()
            LauncherAction.OPEN_QUICK_SETTINGS -> expandQuickSettings()
            LauncherAction.TOGGLE_TORCH -> toggleTorch()
            LauncherAction.OPEN_VAULT -> onOpenVault()
            LauncherAction.OPEN_ORB_SPACE -> onOpenOrbSpace()
            LauncherAction.OPEN_ORB_SEARCH -> onOpenOrbSearch()
            LauncherAction.LAUNCH_APP -> launchApp(action.targetPackage, action.targetActivity)
            LauncherAction.LOCK_SCREEN, LauncherAction.SCREEN_OFF -> lockScreen()
            LauncherAction.NONE -> { /* Do nothing */ }
            else -> {
                // Show a friendly toast for feature-specific placeholders
                Toast.makeText(context, "Action: ${action.action.name.replace('_', ' ').lowercase().capitalize()}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun lockScreen() {
        try {
            val serviceClass = Class.forName("com.oorbitt.launcher.orbspace.ScrollTrackerService")
            val companionField = serviceClass.getDeclaredField("Companion")
            companionField.isAccessible = true
            val companionObj = companionField.get(null)
            
            val serviceInstance = try {
                val method = companionObj.javaClass.getMethod("getInstance")
                method.invoke(companionObj)
            } catch (e: Exception) {
                val field = companionObj.javaClass.getDeclaredField("instance")
                field.isAccessible = true
                field.get(companionObj)
            } as? android.accessibilityservice.AccessibilityService
            
            if (serviceInstance != null) {
                val success = serviceInstance.performGlobalAction(8) // GLOBAL_ACTION_LOCK_SCREEN
                if (!success) {
                    Toast.makeText(context, "Lock screen failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                showAccessibilityPrompt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showAccessibilityPrompt()
        }
    }

    private fun showAccessibilityPrompt() {
        Toast.makeText(context, "Please enable Accessibility Service for Oorbitt in settings to allow Lock Screen", Toast.LENGTH_LONG).show()
        try {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("WrongConstant")
    private fun expandNotifications() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
            // Broadcast fallback
            val statusBarIntent = Intent("android.intent.action.SHOW_NOTIFICATION_INPUT")
            context.sendBroadcast(statusBarIntent)
        }
    }

    @SuppressLint("WrongConstant")
    private fun expandQuickSettings() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandSettingsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchApp(pkg: String?, activity: String?) {
        if (pkg != null) {
            try {
                val intent = if (activity != null) {
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setClassName(pkg, activity)
                    }
                } else {
                    context.packageManager.getLaunchIntentForPackage(pkg)
                }
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch $pkg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleTorch() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                isTorchOn = !isTorchOn
                cameraManager.setTorchMode(cameraId, isTorchOn)
            } else {
                Toast.makeText(context, "Flashlight not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not toggle flashlight", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun String.capitalize(): String =
        this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    companion object {
        private var isTorchOn = false
    }
}
