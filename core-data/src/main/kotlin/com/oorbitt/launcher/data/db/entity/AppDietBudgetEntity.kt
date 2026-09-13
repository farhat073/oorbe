package com.oorbitt.launcher.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_diet_budgets")
data class AppDietBudgetEntity(
    @PrimaryKey val componentKey: String,
    @ColumnInfo(name = "daily_limit_minutes") val dailyLimitMinutes: Int,
    @ColumnInfo(name = "friction_enabled") val frictionEnabled: Boolean,
)
