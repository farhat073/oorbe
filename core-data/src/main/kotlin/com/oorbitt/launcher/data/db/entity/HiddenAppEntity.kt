package com.oorbitt.launcher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val componentKey: String,
)
