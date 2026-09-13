package com.oorbitt.launcher.data.db.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.oorbitt.launcher.data.db.entity.GestureMapEntity;
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
public final class GestureMapDao_Impl implements GestureMapDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GestureMapEntity> __insertionAdapterOfGestureMapEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public GestureMapDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGestureMapEntity = new EntityInsertionAdapter<GestureMapEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `gesture_map` (`gestureType`,`action`,`targetPackage`,`targetActivity`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GestureMapEntity entity) {
        statement.bindString(1, entity.getGestureType());
        statement.bindString(2, entity.getAction());
        if (entity.getTargetPackage() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTargetPackage());
        }
        if (entity.getTargetActivity() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTargetActivity());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM gesture_map";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final GestureMapEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGestureMapEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GestureMapEntity>> getAll() {
    final String _sql = "SELECT * FROM gesture_map";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"gesture_map"}, new Callable<List<GestureMapEntity>>() {
      @Override
      @NonNull
      public List<GestureMapEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGestureType = CursorUtil.getColumnIndexOrThrow(_cursor, "gestureType");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfTargetPackage = CursorUtil.getColumnIndexOrThrow(_cursor, "targetPackage");
          final int _cursorIndexOfTargetActivity = CursorUtil.getColumnIndexOrThrow(_cursor, "targetActivity");
          final List<GestureMapEntity> _result = new ArrayList<GestureMapEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GestureMapEntity _item;
            final String _tmpGestureType;
            _tmpGestureType = _cursor.getString(_cursorIndexOfGestureType);
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpTargetPackage;
            if (_cursor.isNull(_cursorIndexOfTargetPackage)) {
              _tmpTargetPackage = null;
            } else {
              _tmpTargetPackage = _cursor.getString(_cursorIndexOfTargetPackage);
            }
            final String _tmpTargetActivity;
            if (_cursor.isNull(_cursorIndexOfTargetActivity)) {
              _tmpTargetActivity = null;
            } else {
              _tmpTargetActivity = _cursor.getString(_cursorIndexOfTargetActivity);
            }
            _item = new GestureMapEntity(_tmpGestureType,_tmpAction,_tmpTargetPackage,_tmpTargetActivity);
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
