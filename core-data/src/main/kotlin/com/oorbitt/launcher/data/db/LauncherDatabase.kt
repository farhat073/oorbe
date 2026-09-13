package com.oorbitt.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.oorbitt.launcher.data.db.dao.AppDietDao
import com.oorbitt.launcher.data.db.dao.GestureMapDao
import com.oorbitt.launcher.data.db.dao.HiddenAppDao
import com.oorbitt.launcher.data.db.dao.IconOverrideDao
import com.oorbitt.launcher.data.db.dao.ProfileDao
import com.oorbitt.launcher.data.db.dao.VaultMemoDao
import com.oorbitt.launcher.data.db.dao.WorkspaceDao
import com.oorbitt.launcher.data.db.dao.LockedAppDao
import com.oorbitt.launcher.data.db.entity.AppDietBudgetEntity
import com.oorbitt.launcher.data.db.entity.GestureMapEntity
import com.oorbitt.launcher.data.db.entity.HiddenAppEntity
import com.oorbitt.launcher.data.db.entity.IconOverrideEntity
import com.oorbitt.launcher.data.db.entity.LockedAppEntity
import com.oorbitt.launcher.data.db.entity.ProfileEntity
import com.oorbitt.launcher.data.db.entity.VaultMemoEntity
import com.oorbitt.launcher.data.db.entity.WorkspaceItemEntity

@Database(
    entities = [
        WorkspaceItemEntity::class,
        HiddenAppEntity::class,
        LockedAppEntity::class,
        GestureMapEntity::class,
        IconOverrideEntity::class,
        VaultMemoEntity::class,
        ProfileEntity::class,
        AppDietBudgetEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun gestureMapDao(): GestureMapDao
    abstract fun iconOverrideDao(): IconOverrideDao
    abstract fun vaultMemoDao(): VaultMemoDao
    abstract fun profileDao(): ProfileDao
    abstract fun appDietDao(): AppDietDao
}
