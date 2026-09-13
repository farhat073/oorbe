package com.oorbitt.launcher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "icon_overrides")
data class IconOverrideEntity(
    @PrimaryKey val componentKey: String,
    val iconPackPackage: String?,
    val customIconUri: String?,
    val customLabel: String?,
)
