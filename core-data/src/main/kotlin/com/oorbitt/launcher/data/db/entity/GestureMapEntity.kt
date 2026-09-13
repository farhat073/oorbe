package com.oorbitt.launcher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_map")
data class GestureMapEntity(
    @PrimaryKey val gestureType: String,
    val action: String,
    val targetPackage: String?,
    val targetActivity: String?,
)
