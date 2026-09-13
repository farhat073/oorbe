package com.oorbitt.launcher.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "wallpaper_uri") val wallpaperUri: String?,
    @ColumnInfo(name = "hidden_apps_json") val hiddenAppsJson: String,
    @ColumnInfo(name = "gesture_overrides_json") val gestureOverridesJson: String,
)
