package com.oorbitt.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oorbitt.launcher.data.db.entity.AppDietBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDietDao {

    @Query("SELECT * FROM app_diet_budgets")
    fun getAll(): Flow<List<AppDietBudgetEntity>>

    @Query("SELECT * FROM app_diet_budgets WHERE componentKey = :key")
    suspend fun getByKey(key: String): AppDietBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: AppDietBudgetEntity)

    @Delete
    suspend fun delete(entity: AppDietBudgetEntity)
}
