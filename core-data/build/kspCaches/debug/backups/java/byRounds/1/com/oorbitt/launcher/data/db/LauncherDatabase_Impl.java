package com.oorbitt.launcher.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.oorbitt.launcher.data.db.dao.AppDietDao;
import com.oorbitt.launcher.data.db.dao.AppDietDao_Impl;
import com.oorbitt.launcher.data.db.dao.GestureMapDao;
import com.oorbitt.launcher.data.db.dao.GestureMapDao_Impl;
import com.oorbitt.launcher.data.db.dao.HiddenAppDao;
import com.oorbitt.launcher.data.db.dao.HiddenAppDao_Impl;
import com.oorbitt.launcher.data.db.dao.IconOverrideDao;
import com.oorbitt.launcher.data.db.dao.IconOverrideDao_Impl;
import com.oorbitt.launcher.data.db.dao.LockedAppDao;
import com.oorbitt.launcher.data.db.dao.LockedAppDao_Impl;
import com.oorbitt.launcher.data.db.dao.ProfileDao;
import com.oorbitt.launcher.data.db.dao.ProfileDao_Impl;
import com.oorbitt.launcher.data.db.dao.VaultMemoDao;
import com.oorbitt.launcher.data.db.dao.VaultMemoDao_Impl;
import com.oorbitt.launcher.data.db.dao.WorkspaceDao;
import com.oorbitt.launcher.data.db.dao.WorkspaceDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LauncherDatabase_Impl extends LauncherDatabase {
  private volatile WorkspaceDao _workspaceDao;

  private volatile HiddenAppDao _hiddenAppDao;

  private volatile LockedAppDao _lockedAppDao;

  private volatile GestureMapDao _gestureMapDao;

  private volatile IconOverrideDao _iconOverrideDao;

  private volatile VaultMemoDao _vaultMemoDao;

  private volatile ProfileDao _profileDao;

  private volatile AppDietDao _appDietDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `workspace_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `item_type` TEXT NOT NULL, `container_type` TEXT NOT NULL, `screen_index` INTEGER NOT NULL, `cell_x` INTEGER NOT NULL, `cell_y` INTEGER NOT NULL, `span_x` INTEGER NOT NULL, `span_y` INTEGER NOT NULL, `package_name` TEXT, `activity_name` TEXT, `app_widget_id` INTEGER NOT NULL, `folder_id` INTEGER, `folder_title` TEXT, `dock_index` INTEGER NOT NULL, `profile_id` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `hidden_apps` (`componentKey` TEXT NOT NULL, PRIMARY KEY(`componentKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `locked_apps` (`componentKey` TEXT NOT NULL, PRIMARY KEY(`componentKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `gesture_map` (`gestureType` TEXT NOT NULL, `action` TEXT NOT NULL, `targetPackage` TEXT, `targetActivity` TEXT, PRIMARY KEY(`gestureType`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `icon_overrides` (`componentKey` TEXT NOT NULL, `iconPackPackage` TEXT, `customIconUri` TEXT, `customLabel` TEXT, PRIMARY KEY(`componentKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `vault_memos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `is_pinned` INTEGER NOT NULL, `color` TEXT NOT NULL, `blocks_json` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `wallpaper_uri` TEXT, `hidden_apps_json` TEXT NOT NULL, `gesture_overrides_json` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `app_diet_budgets` (`componentKey` TEXT NOT NULL, `daily_limit_minutes` INTEGER NOT NULL, `friction_enabled` INTEGER NOT NULL, PRIMARY KEY(`componentKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'bc8995302c942245a09134202eeb4dfb')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `workspace_items`");
        db.execSQL("DROP TABLE IF EXISTS `hidden_apps`");
        db.execSQL("DROP TABLE IF EXISTS `locked_apps`");
        db.execSQL("DROP TABLE IF EXISTS `gesture_map`");
        db.execSQL("DROP TABLE IF EXISTS `icon_overrides`");
        db.execSQL("DROP TABLE IF EXISTS `vault_memos`");
        db.execSQL("DROP TABLE IF EXISTS `profiles`");
        db.execSQL("DROP TABLE IF EXISTS `app_diet_budgets`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsWorkspaceItems = new HashMap<String, TableInfo.Column>(15);
        _columnsWorkspaceItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("item_type", new TableInfo.Column("item_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("container_type", new TableInfo.Column("container_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("screen_index", new TableInfo.Column("screen_index", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("cell_x", new TableInfo.Column("cell_x", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("cell_y", new TableInfo.Column("cell_y", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("span_x", new TableInfo.Column("span_x", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("span_y", new TableInfo.Column("span_y", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("package_name", new TableInfo.Column("package_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("activity_name", new TableInfo.Column("activity_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("app_widget_id", new TableInfo.Column("app_widget_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("folder_id", new TableInfo.Column("folder_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("folder_title", new TableInfo.Column("folder_title", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("dock_index", new TableInfo.Column("dock_index", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkspaceItems.put("profile_id", new TableInfo.Column("profile_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkspaceItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWorkspaceItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWorkspaceItems = new TableInfo("workspace_items", _columnsWorkspaceItems, _foreignKeysWorkspaceItems, _indicesWorkspaceItems);
        final TableInfo _existingWorkspaceItems = TableInfo.read(db, "workspace_items");
        if (!_infoWorkspaceItems.equals(_existingWorkspaceItems)) {
          return new RoomOpenHelper.ValidationResult(false, "workspace_items(com.oorbitt.launcher.data.db.entity.WorkspaceItemEntity).\n"
                  + " Expected:\n" + _infoWorkspaceItems + "\n"
                  + " Found:\n" + _existingWorkspaceItems);
        }
        final HashMap<String, TableInfo.Column> _columnsHiddenApps = new HashMap<String, TableInfo.Column>(1);
        _columnsHiddenApps.put("componentKey", new TableInfo.Column("componentKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHiddenApps = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHiddenApps = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHiddenApps = new TableInfo("hidden_apps", _columnsHiddenApps, _foreignKeysHiddenApps, _indicesHiddenApps);
        final TableInfo _existingHiddenApps = TableInfo.read(db, "hidden_apps");
        if (!_infoHiddenApps.equals(_existingHiddenApps)) {
          return new RoomOpenHelper.ValidationResult(false, "hidden_apps(com.oorbitt.launcher.data.db.entity.HiddenAppEntity).\n"
                  + " Expected:\n" + _infoHiddenApps + "\n"
                  + " Found:\n" + _existingHiddenApps);
        }
        final HashMap<String, TableInfo.Column> _columnsLockedApps = new HashMap<String, TableInfo.Column>(1);
        _columnsLockedApps.put("componentKey", new TableInfo.Column("componentKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLockedApps = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLockedApps = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLockedApps = new TableInfo("locked_apps", _columnsLockedApps, _foreignKeysLockedApps, _indicesLockedApps);
        final TableInfo _existingLockedApps = TableInfo.read(db, "locked_apps");
        if (!_infoLockedApps.equals(_existingLockedApps)) {
          return new RoomOpenHelper.ValidationResult(false, "locked_apps(com.oorbitt.launcher.data.db.entity.LockedAppEntity).\n"
                  + " Expected:\n" + _infoLockedApps + "\n"
                  + " Found:\n" + _existingLockedApps);
        }
        final HashMap<String, TableInfo.Column> _columnsGestureMap = new HashMap<String, TableInfo.Column>(4);
        _columnsGestureMap.put("gestureType", new TableInfo.Column("gestureType", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGestureMap.put("action", new TableInfo.Column("action", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGestureMap.put("targetPackage", new TableInfo.Column("targetPackage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGestureMap.put("targetActivity", new TableInfo.Column("targetActivity", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGestureMap = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGestureMap = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGestureMap = new TableInfo("gesture_map", _columnsGestureMap, _foreignKeysGestureMap, _indicesGestureMap);
        final TableInfo _existingGestureMap = TableInfo.read(db, "gesture_map");
        if (!_infoGestureMap.equals(_existingGestureMap)) {
          return new RoomOpenHelper.ValidationResult(false, "gesture_map(com.oorbitt.launcher.data.db.entity.GestureMapEntity).\n"
                  + " Expected:\n" + _infoGestureMap + "\n"
                  + " Found:\n" + _existingGestureMap);
        }
        final HashMap<String, TableInfo.Column> _columnsIconOverrides = new HashMap<String, TableInfo.Column>(4);
        _columnsIconOverrides.put("componentKey", new TableInfo.Column("componentKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIconOverrides.put("iconPackPackage", new TableInfo.Column("iconPackPackage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIconOverrides.put("customIconUri", new TableInfo.Column("customIconUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIconOverrides.put("customLabel", new TableInfo.Column("customLabel", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysIconOverrides = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesIconOverrides = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoIconOverrides = new TableInfo("icon_overrides", _columnsIconOverrides, _foreignKeysIconOverrides, _indicesIconOverrides);
        final TableInfo _existingIconOverrides = TableInfo.read(db, "icon_overrides");
        if (!_infoIconOverrides.equals(_existingIconOverrides)) {
          return new RoomOpenHelper.ValidationResult(false, "icon_overrides(com.oorbitt.launcher.data.db.entity.IconOverrideEntity).\n"
                  + " Expected:\n" + _infoIconOverrides + "\n"
                  + " Found:\n" + _existingIconOverrides);
        }
        final HashMap<String, TableInfo.Column> _columnsVaultMemos = new HashMap<String, TableInfo.Column>(7);
        _columnsVaultMemos.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultMemos.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultMemos.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultMemos.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultMemos.put("is_pinned", new TableInfo.Column("is_pinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultMemos.put("color", new TableInfo.Column("color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultMemos.put("blocks_json", new TableInfo.Column("blocks_json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVaultMemos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVaultMemos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVaultMemos = new TableInfo("vault_memos", _columnsVaultMemos, _foreignKeysVaultMemos, _indicesVaultMemos);
        final TableInfo _existingVaultMemos = TableInfo.read(db, "vault_memos");
        if (!_infoVaultMemos.equals(_existingVaultMemos)) {
          return new RoomOpenHelper.ValidationResult(false, "vault_memos(com.oorbitt.launcher.data.db.entity.VaultMemoEntity).\n"
                  + " Expected:\n" + _infoVaultMemos + "\n"
                  + " Found:\n" + _existingVaultMemos);
        }
        final HashMap<String, TableInfo.Column> _columnsProfiles = new HashMap<String, TableInfo.Column>(7);
        _columnsProfiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("is_active", new TableInfo.Column("is_active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("wallpaper_uri", new TableInfo.Column("wallpaper_uri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("hidden_apps_json", new TableInfo.Column("hidden_apps_json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProfiles.put("gesture_overrides_json", new TableInfo.Column("gesture_overrides_json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProfiles = new TableInfo("profiles", _columnsProfiles, _foreignKeysProfiles, _indicesProfiles);
        final TableInfo _existingProfiles = TableInfo.read(db, "profiles");
        if (!_infoProfiles.equals(_existingProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "profiles(com.oorbitt.launcher.data.db.entity.ProfileEntity).\n"
                  + " Expected:\n" + _infoProfiles + "\n"
                  + " Found:\n" + _existingProfiles);
        }
        final HashMap<String, TableInfo.Column> _columnsAppDietBudgets = new HashMap<String, TableInfo.Column>(3);
        _columnsAppDietBudgets.put("componentKey", new TableInfo.Column("componentKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppDietBudgets.put("daily_limit_minutes", new TableInfo.Column("daily_limit_minutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppDietBudgets.put("friction_enabled", new TableInfo.Column("friction_enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppDietBudgets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAppDietBudgets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAppDietBudgets = new TableInfo("app_diet_budgets", _columnsAppDietBudgets, _foreignKeysAppDietBudgets, _indicesAppDietBudgets);
        final TableInfo _existingAppDietBudgets = TableInfo.read(db, "app_diet_budgets");
        if (!_infoAppDietBudgets.equals(_existingAppDietBudgets)) {
          return new RoomOpenHelper.ValidationResult(false, "app_diet_budgets(com.oorbitt.launcher.data.db.entity.AppDietBudgetEntity).\n"
                  + " Expected:\n" + _infoAppDietBudgets + "\n"
                  + " Found:\n" + _existingAppDietBudgets);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "bc8995302c942245a09134202eeb4dfb", "5dfd83860d2913145fd60e9e1a6087f6");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "workspace_items","hidden_apps","locked_apps","gesture_map","icon_overrides","vault_memos","profiles","app_diet_budgets");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `workspace_items`");
      _db.execSQL("DELETE FROM `hidden_apps`");
      _db.execSQL("DELETE FROM `locked_apps`");
      _db.execSQL("DELETE FROM `gesture_map`");
      _db.execSQL("DELETE FROM `icon_overrides`");
      _db.execSQL("DELETE FROM `vault_memos`");
      _db.execSQL("DELETE FROM `profiles`");
      _db.execSQL("DELETE FROM `app_diet_budgets`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WorkspaceDao.class, WorkspaceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HiddenAppDao.class, HiddenAppDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LockedAppDao.class, LockedAppDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GestureMapDao.class, GestureMapDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(IconOverrideDao.class, IconOverrideDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(VaultMemoDao.class, VaultMemoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProfileDao.class, ProfileDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AppDietDao.class, AppDietDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WorkspaceDao workspaceDao() {
    if (_workspaceDao != null) {
      return _workspaceDao;
    } else {
      synchronized(this) {
        if(_workspaceDao == null) {
          _workspaceDao = new WorkspaceDao_Impl(this);
        }
        return _workspaceDao;
      }
    }
  }

  @Override
  public HiddenAppDao hiddenAppDao() {
    if (_hiddenAppDao != null) {
      return _hiddenAppDao;
    } else {
      synchronized(this) {
        if(_hiddenAppDao == null) {
          _hiddenAppDao = new HiddenAppDao_Impl(this);
        }
        return _hiddenAppDao;
      }
    }
  }

  @Override
  public LockedAppDao lockedAppDao() {
    if (_lockedAppDao != null) {
      return _lockedAppDao;
    } else {
      synchronized(this) {
        if(_lockedAppDao == null) {
          _lockedAppDao = new LockedAppDao_Impl(this);
        }
        return _lockedAppDao;
      }
    }
  }

  @Override
  public GestureMapDao gestureMapDao() {
    if (_gestureMapDao != null) {
      return _gestureMapDao;
    } else {
      synchronized(this) {
        if(_gestureMapDao == null) {
          _gestureMapDao = new GestureMapDao_Impl(this);
        }
        return _gestureMapDao;
      }
    }
  }

  @Override
  public IconOverrideDao iconOverrideDao() {
    if (_iconOverrideDao != null) {
      return _iconOverrideDao;
    } else {
      synchronized(this) {
        if(_iconOverrideDao == null) {
          _iconOverrideDao = new IconOverrideDao_Impl(this);
        }
        return _iconOverrideDao;
      }
    }
  }

  @Override
  public VaultMemoDao vaultMemoDao() {
    if (_vaultMemoDao != null) {
      return _vaultMemoDao;
    } else {
      synchronized(this) {
        if(_vaultMemoDao == null) {
          _vaultMemoDao = new VaultMemoDao_Impl(this);
        }
        return _vaultMemoDao;
      }
    }
  }

  @Override
  public ProfileDao profileDao() {
    if (_profileDao != null) {
      return _profileDao;
    } else {
      synchronized(this) {
        if(_profileDao == null) {
          _profileDao = new ProfileDao_Impl(this);
        }
        return _profileDao;
      }
    }
  }

  @Override
  public AppDietDao appDietDao() {
    if (_appDietDao != null) {
      return _appDietDao;
    } else {
      synchronized(this) {
        if(_appDietDao == null) {
          _appDietDao = new AppDietDao_Impl(this);
        }
        return _appDietDao;
      }
    }
  }
}
