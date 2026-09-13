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
import com.oorbitt.launcher.data.db.entity.AppDietBudgetEntity;
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
public final class AppDietDao_Impl implements AppDietDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AppDietBudgetEntity> __insertionAdapterOfAppDietBudgetEntity;

  private final EntityDeletionOrUpdateAdapter<AppDietBudgetEntity> __deletionAdapterOfAppDietBudgetEntity;

  public AppDietDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAppDietBudgetEntity = new EntityInsertionAdapter<AppDietBudgetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `app_diet_budgets` (`componentKey`,`daily_limit_minutes`,`friction_enabled`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppDietBudgetEntity entity) {
        statement.bindString(1, entity.getComponentKey());
        statement.bindLong(2, entity.getDailyLimitMinutes());
        final int _tmp = entity.getFrictionEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
      }
    };
    this.__deletionAdapterOfAppDietBudgetEntity = new EntityDeletionOrUpdateAdapter<AppDietBudgetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `app_diet_budgets` WHERE `componentKey` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppDietBudgetEntity entity) {
        statement.bindString(1, entity.getComponentKey());
      }
    };
  }

  @Override
  public Object insertOrUpdate(final AppDietBudgetEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAppDietBudgetEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final AppDietBudgetEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAppDietBudgetEntity.handle(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AppDietBudgetEntity>> getAll() {
    final String _sql = "SELECT * FROM app_diet_budgets";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"app_diet_budgets"}, new Callable<List<AppDietBudgetEntity>>() {
      @Override
      @NonNull
      public List<AppDietBudgetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfComponentKey = CursorUtil.getColumnIndexOrThrow(_cursor, "componentKey");
          final int _cursorIndexOfDailyLimitMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "daily_limit_minutes");
          final int _cursorIndexOfFrictionEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "friction_enabled");
          final List<AppDietBudgetEntity> _result = new ArrayList<AppDietBudgetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AppDietBudgetEntity _item;
            final String _tmpComponentKey;
            _tmpComponentKey = _cursor.getString(_cursorIndexOfComponentKey);
            final int _tmpDailyLimitMinutes;
            _tmpDailyLimitMinutes = _cursor.getInt(_cursorIndexOfDailyLimitMinutes);
            final boolean _tmpFrictionEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFrictionEnabled);
            _tmpFrictionEnabled = _tmp != 0;
            _item = new AppDietBudgetEntity(_tmpComponentKey,_tmpDailyLimitMinutes,_tmpFrictionEnabled);
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
      final Continuation<? super AppDietBudgetEntity> $completion) {
    final String _sql = "SELECT * FROM app_diet_budgets WHERE componentKey = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AppDietBudgetEntity>() {
      @Override
      @Nullable
      public AppDietBudgetEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfComponentKey = CursorUtil.getColumnIndexOrThrow(_cursor, "componentKey");
          final int _cursorIndexOfDailyLimitMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "daily_limit_minutes");
          final int _cursorIndexOfFrictionEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "friction_enabled");
          final AppDietBudgetEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpComponentKey;
            _tmpComponentKey = _cursor.getString(_cursorIndexOfComponentKey);
            final int _tmpDailyLimitMinutes;
            _tmpDailyLimitMinutes = _cursor.getInt(_cursorIndexOfDailyLimitMinutes);
            final boolean _tmpFrictionEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFrictionEnabled);
            _tmpFrictionEnabled = _tmp != 0;
            _result = new AppDietBudgetEntity(_tmpComponentKey,_tmpDailyLimitMinutes,_tmpFrictionEnabled);
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
