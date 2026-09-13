package com.oorbitt.launcher.security

import android.content.Context
import org.json.JSONObject

/**
 * Manages Mindful Lock/Focus Lock durations and emergency bypass windows.
 * Saves values in SharedPreferences to make status queries fast and globally accessible.
 */
object MindfulLockManager {
    private const val PREFS_NAME = "orbspace_data"
    private const val KEY_LOCKED_APPS = "mindful_locks_json"
    private const val KEY_EMERGENCY_UNLOCKS = "emergency_locks_json"

    /**
     * Checks if an app is currently locked under Mindful Lock.
     * Returns true if locked and not temporarily emergency unlocked.
     */
    fun isAppLocked(context: Context, packageName: String): Boolean {
        val now = System.currentTimeMillis()
        
        // 1. Check if the app has an active lock
        val locksJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCKED_APPS, "{}") ?: "{}"
        val locksObj = try { JSONObject(locksJson) } catch (e: Exception) { JSONObject() }
        val lockExpiry = locksObj.optLong(packageName, 0L)
        
        if (lockExpiry <= now) {
            if (lockExpiry > 0L) {
                removeLock(context, packageName)
            }
            return false
        }

        // 2. Check if the app is currently in an active emergency unlock period
        val emergencyJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMERGENCY_UNLOCKS, "{}") ?: "{}"
        val emergencyObj = try { JSONObject(emergencyJson) } catch (e: Exception) { JSONObject() }
        val emergencyExpiry = emergencyObj.optLong(packageName, 0L)

        if (emergencyExpiry > now) {
            return false
        } else if (emergencyExpiry > 0L) {
            removeEmergencyUnlock(context, packageName)
        }

        return true
    }

    /**
     * Gets the remaining lock duration in milliseconds.
     */
    fun getRemainingLockTime(context: Context, packageName: String): Long {
        val locksJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCKED_APPS, "{}") ?: "{}"
        val locksObj = try { JSONObject(locksJson) } catch (e: Exception) { JSONObject() }
        val expiry = locksObj.optLong(packageName, 0L)
        return (expiry - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * Gets the remaining emergency unlock duration in milliseconds.
     */
    fun getRemainingEmergencyTime(context: Context, packageName: String): Long {
        val emergencyJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMERGENCY_UNLOCKS, "{}") ?: "{}"
        val emergencyObj = try { JSONObject(emergencyJson) } catch (e: Exception) { JSONObject() }
        val expiry = emergencyObj.optLong(packageName, 0L)
        return (expiry - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun lockApp(context: Context, packageName: String, durationMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val locksJson = prefs.getString(KEY_LOCKED_APPS, "{}") ?: "{}"
        val locksObj = try { JSONObject(locksJson) } catch (e: Exception) { JSONObject() }
        
        locksObj.put(packageName, System.currentTimeMillis() + durationMs)
        prefs.edit().putString(KEY_LOCKED_APPS, locksObj.toString()).apply()
    }

    fun unlockAppCompletely(context: Context, packageName: String) {
        removeLock(context, packageName)
        removeEmergencyUnlock(context, packageName)
    }

    fun triggerEmergencyUnlock(context: Context, packageName: String, durationMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val emergencyJson = prefs.getString(KEY_EMERGENCY_UNLOCKS, "{}") ?: "{}"
        val emergencyObj = try { JSONObject(emergencyJson) } catch (e: Exception) { JSONObject() }
        
        emergencyObj.put(packageName, System.currentTimeMillis() + durationMs)
        prefs.edit().putString(KEY_EMERGENCY_UNLOCKS, emergencyObj.toString()).apply()
    }

    private fun removeLock(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val locksJson = prefs.getString(KEY_LOCKED_APPS, "{}") ?: "{}"
        val locksObj = try { JSONObject(locksJson) } catch (e: Exception) { JSONObject() }
        if (locksObj.has(packageName)) {
            locksObj.remove(packageName)
            prefs.edit().putString(KEY_LOCKED_APPS, locksObj.toString()).apply()
        }
    }

    private fun removeEmergencyUnlock(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val emergencyJson = prefs.getString(KEY_EMERGENCY_UNLOCKS, "{}") ?: "{}"
        val emergencyObj = try { JSONObject(emergencyJson) } catch (e: Exception) { JSONObject() }
        if (emergencyObj.has(packageName)) {
            emergencyObj.remove(packageName)
            prefs.edit().putString(KEY_EMERGENCY_UNLOCKS, emergencyObj.toString()).apply()
        }
    }

    fun getLockedApps(context: Context): Map<String, Long> {
        val locksJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCKED_APPS, "{}") ?: "{}"
        val locksObj = try { JSONObject(locksJson) } catch (e: Exception) { JSONObject() }
        val map = mutableMapOf<String, Long>()
        val keys = locksObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = locksObj.getLong(key)
        }
        return map
    }

    fun getEmergencyUnlockedApps(context: Context): Map<String, Long> {
        val emergencyJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMERGENCY_UNLOCKS, "{}") ?: "{}"
        val emergencyObj = try { JSONObject(emergencyJson) } catch (e: Exception) { JSONObject() }
        val map = mutableMapOf<String, Long>()
        val keys = emergencyObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = emergencyObj.getLong(key)
        }
        return map
    }
}
