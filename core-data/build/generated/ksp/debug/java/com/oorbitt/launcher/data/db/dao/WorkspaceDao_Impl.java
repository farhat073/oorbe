package com.oorbitt.launcher.data.db.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.oorbitt.launcher.data.db.entity.WorkspaceItemEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WorkspaceDao_Impl implements WorkspaceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorkspaceItemEntity> __insertionAdapterOfWorkspaceItemEntity;

  private final EntityDeletionOrUpdateAdapter<WorkspaceItemEntity> __deletionAdapterOfWorkspaceItemEntity;

  private final EntityDeletionOrUpdateAdapter<WorkspaceItemEntity> __updateAdapterOfWorkspaceItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllForProfile;

  public WorkspaceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorkspaceItemEntity = new EntityInsertionAdapter<WorkspaceItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `workspace_items` (`id`,`item_type`,`container_type`,`screen_index`,`cell_x`,`cell_y`,`span_x`,`span_y`,`package_name`,`activity_name`,`app_widget_id`,`folder_id`,`folder_title`,`dock_index`,`profile_id`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkspaceItemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getItemType());
        statement.bindString(3, entity.getContainerType());
        statement.bindLong(4, entity.getScreenIndex());
        statement.bindLong(5, entity.getCellX());
        statement.bindLong(6, entity.getCellY());
        statement.bindLong(7, entity.getSpanX());
        statement.bindLong(8, entity.getSpanY());
        if (entity.getPackageName() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPackageName());
        }
        if (entity.getActivityName() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getActivityName());
        }
        statement.bindLong(11, entity.getAppWidgetId());
        if (entity.getFolderId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getFolderId());
        }
        if (entity.getFolderTitle() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getFolderTitle());
        }
        statement.bindLong(14, entity.getDockIndex());
        statement.bindLong(15, entity.getProfileId());
      }
    };
    this.__deletionAdapterOfWorkspaceItemEntity = new EntityDeletionOrUpdateAdapter<WorkspaceItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `workspace_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkspaceItemEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfWorkspaceItemEntity = new EntityDeletionOrUpdateAdapter<WorkspaceItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `workspace_items` SET `id` = ?,`item_type` = ?,`container_type` = ?,`screen_index` = ?,`cell_x` = ?,`cell_y` = ?,`span_x` = ?,`span_y` = ?,`package_name` = ?,`activity_name` = ?,`app_widget_id` = ?,`folder_id` = ?,`folder_title` = ?,`dock_index` = ?,`profile_id` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkspaceItemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getItemType());
        statement.bindString(3, entity.getContainerType());
        statement.bindLong(4, entity.getScreenIndex());
        statement.bindLong(5, entity.getCellX());
        statement.bindLong(6, entity.getCellY());
        statement.bindLong(7, entity.getSpanX());
        statement.bindLong(8, entity.getSpanY());
        if (entity.getPackageName() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPackageName());
        }
        if (entity.getActivityName() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getActivityName());
        }
        statement.bindLong(11, entity.getAppWidgetId());
        if (entity.getFolderId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getFolderId());
        }
        if (entity.getFolderTitle() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getFolderTitle());
        }
        statement.bindLong(14, entity.getDockIndex());
        statement.bindLong(15, entity.getProfileId());
        statement.bindLong(16, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workspace_items WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllForProfile = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workspace_items WHERE profile_id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final WorkspaceItemEntity item,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWorkspaceItemEntity.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final WorkspaceItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWorkspaceItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final WorkspaceItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWorkspaceItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replaceAllForProfile(final long profileId, final List<WorkspaceItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> WorkspaceDao.DefaultImpls.replaceAllForProfile(WorkspaceDao_Impl.this, profileId, items, __cont), $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllForProfile(final long profileId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllForProfile.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, profileId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllForProfile.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WorkspaceItemEntity>> getItemsForProfile(final long profileId) {
    final String _sql = "SELECT * FROM workspace_items WHERE profile_id = ? ORDER BY screen_index, cell_y, cell_x";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, profileId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workspace_items"}, new Callable<List<WorkspaceItemEntity>>() {
      @Override
      @NonNull
      public List<WorkspaceItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfItemType = CursorUtil.getColumnIndexOrThrow(_cursor, "item_type");
          final int _cursorIndexOfContainerType = CursorUtil.getColumnIndexOrThrow(_cursor, "container_type");
          final int _cursorIndexOfScreenIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "screen_index");
          final int _cursorIndexOfCellX = CursorUtil.getColumnIndexOrThrow(_cursor, "cell_x");
          final int _cursorIndexOfCellY = CursorUtil.getColumnIndexOrThrow(_cursor, "cell_y");
          final int _cursorIndexOfSpanX = CursorUtil.getColumnIndexOrThrow(_cursor, "span_x");
          final int _cursorIndexOfSpanY = CursorUtil.getColumnIndexOrThrow(_cursor, "span_y");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfActivityName = CursorUtil.getColumnIndexOrThrow(_cursor, "activity_name");
          final int _cursorIndexOfAppWidgetId = CursorUtil.getColumnIndexOrThrow(_cursor, "app_widget_id");
          final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_id");
          final int _cursorIndexOfFolderTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_title");
          final int _cursorIndexOfDockIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "dock_index");
          final int _cursorIndexOfProfileId = CursorUtil.getColumnIndexOrThrow(_cursor, "profile_id");
          final List<WorkspaceItemEntity> _result = new ArrayList<WorkspaceItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkspaceItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpItemType;
            _tmpItemType = _cursor.getString(_cursorIndexOfItemType);
            final String _tmpContainerType;
            _tmpContainerType = _cursor.getString(_cursorIndexOfContainerType);
            final int _tmpScreenIndex;
            _tmpScreenIndex = _cursor.getInt(_cursorIndexOfScreenIndex);
            final int _tmpCellX;
            _tmpCellX = _cursor.getInt(_cursorIndexOfCellX);
            final int _tmpCellY;
            _tmpCellY = _cursor.getInt(_cursorIndexOfCellY);
            final int _tmpSpanX;
            _tmpSpanX = _cursor.getInt(_cursorIndexOfSpanX);
            final int _tmpSpanY;
            _tmpSpanY = _cursor.getInt(_cursorIndexOfSpanY);
            final String _tmpPackageName;
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _tmpPackageName = null;
            } else {
              _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            final String _tmpActivityName;
            if (_cursor.isNull(_cursorIndexOfActivityName)) {
              _tmpActivityName = null;
            } else {
              _tmpActivityName = _cursor.getString(_cursorIndexOfActivityName);
            }
            final int _tmpAppWidgetId;
            _tmpAppWidgetId = _cursor.getInt(_cursorIndexOfAppWidgetId);
            final Long _tmpFolderId;
            if (_cursor.isNull(_cursorIndexOfFolderId)) {
              _tmpFolderId = null;
            } else {
              _tmpFolderId = _cursor.getLong(_cursorIndexOfFolderId);
            }
            final String _tmpFolderTitle;
            if (_cursor.isNull(_cursorIndexOfFolderTitle)) {
              _tmpFolderTitle = null;
            } else {
              _tmpFolderTitle = _cursor.getString(_cursorIndexOfFolderTitle);
            }
            final int _tmpDockIndex;
            _tmpDockIndex = _cursor.getInt(_cursorIndexOfDockIndex);
            final long _tmpProfileId;
            _tmpProfileId = _cursor.getLong(_cursorIndexOfProfileId);
            _item = new WorkspaceItemEntity(_tmpId,_tmpItemType,_tmpContainerType,_tmpScreenIndex,_tmpCellX,_tmpCellY,_tmpSpanX,_tmpSpanY,_tmpPackageName,_tmpActivityName,_tmpAppWidgetId,_tmpFolderId,_tmpFolderTitle,_tmpDockIndex,_tmpProfileId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WorkspaceItemEntity>> getDockItems(final long profileId) {
    final String _sql = "SELECT * FROM workspace_items WHERE container_type = 'DOCK' AND profile_id = ? ORDER BY dock_index";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, profileId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workspace_items"}, new Callable<List<WorkspaceItemEntity>>() {
      @Override
      @NonNull
      public List<WorkspaceItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfItemType = CursorUtil.getColumnIndexOrThrow(_cursor, "item_type");
          final int _cursorIndexOfContainerType = CursorUtil.getColumnIndexOrThrow(_cursor, "container_type");
          final int _cursorIndexOfScreenIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "screen_index");
          final int _cursorIndexOfCellX = CursorUtil.getColumnIndexOrThrow(_cursor, "cell_x");
          final int _cursorIndexOfCellY = CursorUtil.getColumnIndexOrThrow(_cursor, "cell_y");
          final int _cursorIndexOfSpanX = CursorUtil.getColumnIndexOrThrow(_cursor, "span_x");
          final int _cursorIndexOfSpanY = CursorUtil.getColumnIndexOrThrow(_cursor, "span_y");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfActivityName = CursorUtil.getColumnIndexOrThrow(_cursor, "activity_name");
          final int _cursorIndexOfAppWidgetId = CursorUtil.getColumnIndexOrThrow(_cursor, "app_widget_id");
          final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_id");
          final int _cursorIndexOfFolderTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_title");
          final int _cursorIndexOfDockIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "dock_index");
          final int _cursorIndexOfProfileId = CursorUtil.getColumnIndexOrThrow(_cursor, "profile_id");
          final List<WorkspaceItemEntity> _result = new ArrayList<WorkspaceItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkspaceItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpItemType;
            _tmpItemType = _cursor.getString(_cursorIndexOfItemType);
            final String _tmpContainerType;
            _tmpContainerType = _cursor.getString(_cursorIndexOfContainerType);
            final int _tmpScreenIndex;
            _tmpScreenIndex = _cursor.getInt(_cursorIndexOfScreenIndex);
            final int _tmpCellX;
            _tmpCellX = _cursor.getInt(_cursorIndexOfCellX);
            final int _tmpCellY;
            _tmpCellY = _cursor.getInt(_cursorIndexOfCellY);
            final int _tmpSpanX;
            _tmpSpanX = _cursor.getInt(_cursorIndexOfSpanX);
            final int _tmpSpanY;
            _tmpSpanY = _cursor.getInt(_cursorIndexOfSpanY);
            final String _tmpPackageName;
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _tmpPackageName = null;
            } else {
              _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            final String _tmpActivityName;
            if (_cursor.isNull(_cursorIndexOfActivityName)) {
              _tmpActivityName = null;
            } else {
              _tmpActivityName = _cursor.getString(_cursorIndexOfActivityName);
            }
            final int _tmpAppWidgetId;
            _tmpAppWidgetId = _cursor.getInt(_cursorIndexOfAppWidgetId);
            final Long _tmpFolderId;
            if (_cursor.isNull(_cursorIndexOfFolderId)) {
              _tmpFolderId = null;
            } else {
              _tmpFolderId = _cursor.getLong(_cursorIndexOfFolderId);
            }
            final String _tmpFolderTitle;
            if (_cursor.isNull(_cursorIndexOfFolderTitle)) {
              _tmpFolderTitle = null;
            } else {
              _tmpFolderTitle = _cursor.getString(_cursorIndexOfFolderTitle);
            }
            final int _tmpDockIndex;
            _tmpDockIndex = _cursor.getInt(_cursorIndexOfDockIndex);
            final long _tmpProfileId;
            _tmpProfileId = _cursor.getLong(_cursorIndexOfProfileId);
            _item = new WorkspaceItemEntity(_tmpId,_tmpItemType,_tmpContainerType,_tmpScreenIndex,_tmpCellX,_tmpCellY,_tmpSpanX,_tmpSpanY,_tmpPackageName,_tmpActivityName,_tmpAppWidgetId,_tmpFolderId,_tmpFolderTitle,_tmpDockIndex,_tmpProfileId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WorkspaceItemEntity>> getFolderItems(final long folderId) {
    final String _sql = "SELECT * FROM workspace_items WHERE folder_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, folderId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workspace_items"}, new Callable<List<WorkspaceItemEntity>>() {
      @Override
      @NonNull
      public List<WorkspaceItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfItemType = CursorUtil.getColumnIndexOrThrow(_cursor, "item_type");
          final int _cursorIndexOfContainerType = CursorUtil.getColumnIndexOrThrow(_cursor, "container_type");
          final int _cursorIndexOfScreenIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "screen_index");
          final int _cursorIndexOfCellX = CursorUtil.getColumnIndexOrThrow(_cursor, "cell_x");
          final int _cursorIndexOfCellY = CursorUtil.getColumnIndexOrThrow(_cursor, "cell_y");
          final int _cursorIndexOfSpanX = CursorUtil.getColumnIndexOrThrow(_cursor, "span_x");
          final int _cursorIndexOfSpanY = CursorUtil.getColumnIndexOrThrow(_cursor, "span_y");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfActivityName = CursorUtil.getColumnIndexOrThrow(_cursor, "activity_name");
          final int _cursorIndexOfAppWidgetId = CursorUtil.getColumnIndexOrThrow(_cursor, "app_widget_id");
          final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_id");
          final int _cursorIndexOfFolderTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "folder_title");
          final int _cursorIndexOfDockIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "dock_index");
          final int _cursorIndexOfProfileId = CursorUtil.getColumnIndexOrThrow(_cursor, "profile_id");
          final List<WorkspaceItemEntity> _result = new ArrayList<WorkspaceItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkspaceItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpItemType;
            _tmpItemType = _cursor.getString(_cursorIndexOfItemType);
            final String _tmpContainerType;
            _tmpContainerType = _cursor.getString(_cursorIndexOfContainerType);
            final int _tmpScreenIndex;
            _tmpScreenIndex = _cursor.getInt(_cursorIndexOfScreenIndex);
            final int _tmpCellX;
            _tmpCellX = _cursor.getInt(_cursorIndexOfCellX);
            final int _tmpCellY;
            _tmpCellY = _cursor.getInt(_cursorIndexOfCellY);
            final int _tmpSpanX;
            _tmpSpanX = _cursor.getInt(_cursorIndexOfSpanX);
            final int _tmpSpanY;
            _tmpSpanY = _cursor.getInt(_cursorIndexOfSpanY);
            final String _tmpPackageName;
            if (_cursor.isNull(_cursorIndexOfPackageName)) {
              _tmpPackageName = null;
            } else {
              _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            }
            final String _tmpActivityName;
            if (_cursor.isNull(_cursorIndexOfActivityName)) {
              _tmpActivityName = null;
            } else {
              _tmpActivityName = _cursor.getString(_cursorIndexOfActivityName);
            }
            final int _tmpAppWidgetId;
            _tmpAppWidgetId = _cursor.getInt(_cursorIndexOfAppWidgetId);
            final Long _tmpFolderId;
            if (_cursor.isNull(_cursorIndexOfFolderId)) {
              _tmpFolderId = null;
            } else {
              _tmpFolderId = _cursor.getLong(_cursorIndexOfFolderId);
            }
            final String _tmpFolderTitle;
            if (_cursor.isNull(_cursorIndexOfFolderTitle)) {
              _tmpFolderTitle = null;
            } else {
              _tmpFolderTitle = _cursor.getString(_cursorIndexOfFolderTitle);
            }
            final int _tmpDockIndex;
            _tmpDockIndex = _cursor.getInt(_cursorIndexOfDockIndex);
            final long _tmpProfileId;
            _tmpProfileId = _cursor.getLong(_cursorIndexOfProfileId);
            _item = new WorkspaceItemEntity(_tmpId,_tmpItemType,_tmpContainerType,_tmpScreenIndex,_tmpCellX,_tmpCellY,_tmpSpanX,_tmpSpanY,_tmpPackageName,_tmpActivityName,_tmpAppWidgetId,_tmpFolderId,_tmpFolderTitle,_tmpDockIndex,_tmpProfileId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
