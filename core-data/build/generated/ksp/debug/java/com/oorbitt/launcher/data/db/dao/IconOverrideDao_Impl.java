package com.oorbitt.launcher.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.oorbitt.launcher.data.db.entity.IconOverrideEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class IconOverrideDao_Impl implements IconOverrideDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<IconOverrideEntity> __insertionAdapterOfIconOverrideEntity;

  private final EntityDeletionOrUpdateAdapter<IconOverrideEntity> __deletionAdapterOfIconOverrideEntity;

  public IconOverrideDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfIconOverrideEntity = new EntityInsertionAdapter<IconOverrideEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `icon_overrides` (`componentKey`,`iconPackPackage`,`customIconUri`,`customLabel`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IconOverrideEntity entity) {
        statement.bindString(1, entity.getComponentKey());
        if (entity.getIconPackPackage() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getIconPackPackage());
        }
        if (entity.getCustomIconUri() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCustomIconUri());
        }
        if (entity.getCustomLabel() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCustomLabel());
        }
      }
    };
    this.__deletionAdapterOfIconOverrideEntity = new EntityDeletionOrUpdateAdapter<IconOverrideEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `icon_overrides` WHERE `componentKey` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IconOverrideEntity entity) {
        statement.bindString(1, entity.getComponentKey());
      }
    };
  }

  @Override
  public Object insertOrUpdate(final IconOverrideEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfIconOverrideEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final IconOverrideEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfIconOverrideEntity.handle(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<IconOverrideEntity>> getAll() {
    final String _sql = "SELECT * FROM icon_overrides";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"icon_overrides"}, new Callable<List<IconOverrideEntity>>() {
      @Override
      @NonNull
      public List<IconOverrideEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfComponentKey = CursorUtil.getColumnIndexOrThrow(_cursor, "componentKey");
          final int _cursorIndexOfIconPackPackage = CursorUtil.getColumnIndexOrThrow(_cursor, "iconPackPackage");
          final int _cursorIndexOfCustomIconUri = CursorUtil.getColumnIndexOrThrow(_cursor, "customIconUri");
          final int _cursorIndexOfCustomLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "customLabel");
          final List<IconOverrideEntity> _result = new ArrayList<IconOverrideEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IconOverrideEntity _item;
            final String _tmpComponentKey;
            _tmpComponentKey = _cursor.getString(_cursorIndexOfComponentKey);
            final String _tmpIconPackPackage;
            if (_cursor.isNull(_cursorIndexOfIconPackPackage)) {
              _tmpIconPackPackage = null;
            } else {
              _tmpIconPackPackage = _cursor.getString(_cursorIndexOfIconPackPackage);
            }
            final String _tmpCustomIconUri;
            if (_cursor.isNull(_cursorIndexOfCustomIconUri)) {
              _tmpCustomIconUri = null;
            } else {
              _tmpCustomIconUri = _cursor.getString(_cursorIndexOfCustomIconUri);
            }
            final String _tmpCustomLabel;
            if (_cursor.isNull(_cursorIndexOfCustomLabel)) {
              _tmpCustomLabel = null;
            } else {
              _tmpCustomLabel = _cursor.getString(_cursorIndexOfCustomLabel);
            }
            _item = new IconOverrideEntity(_tmpComponentKey,_tmpIconPackPackage,_tmpCustomIconUri,_tmpCustomLabel);
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
  public Object getByKey(final String key,
      final Continuation<? super IconOverrideEntity> $completion) {
    final String _sql = "SELECT * FROM icon_overrides WHERE componentKey = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<IconOverrideEntity>() {
      @Override
      @Nullable
      public IconOverrideEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfComponentKey = CursorUtil.getColumnIndexOrThrow(_cursor, "componentKey");
          final int _cursorIndexOfIconPackPackage = CursorUtil.getColumnIndexOrThrow(_cursor, "iconPackPackage");
          final int _cursorIndexOfCustomIconUri = CursorUtil.getColumnIndexOrThrow(_cursor, "customIconUri");
          final int _cursorIndexOfCustomLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "customLabel");
          final IconOverrideEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpComponentKey;
            _tmpComponentKey = _cursor.getString(_cursorIndexOfComponentKey);
            final String _tmpIconPackPackage;
            if (_cursor.isNull(_cursorIndexOfIconPackPackage)) {
              _tmpIconPackPackage = null;
            } else {
              _tmpIconPackPackage = _cursor.getString(_cursorIndexOfIconPackPackage);
            }
            final String _tmpCustomIconUri;
            if (_cursor.isNull(_cursorIndexOfCustomIconUri)) {
              _tmpCustomIconUri = null;
            } else {
              _tmpCustomIconUri = _cursor.getString(_cursorIndexOfCustomIconUri);
            }
            final String _tmpCustomLabel;
            if (_cursor.isNull(_cursorIndexOfCustomLabel)) {
              _tmpCustomLabel = null;
            } else {
              _tmpCustomLabel = _cursor.getString(_cursorIndexOfCustomLabel);
            }
            _result = new IconOverrideEntity(_tmpComponentKey,_tmpIconPackPackage,_tmpCustomIconUri,_tmpCustomLabel);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
