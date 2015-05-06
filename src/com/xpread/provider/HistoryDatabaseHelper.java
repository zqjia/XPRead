
package com.xpread.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "history.db";

    private static final int DATABASE_VERSION = 1;

    public HistoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + History.RecordsColumns.TABLE_NAME + " ("
                + History.RecordsColumns._ID + " INTEGER PRIMARY KEY,"
                + History.RecordsColumns.DATA + " TEXT," + History.RecordsColumns.DISPLAY_NAME
                + " TEXT," + History.RecordsColumns.DISPLAY_ICON + " BLOB,"
                + History.RecordsColumns.TYPE + " INTEGER," + History.RecordsColumns.SIZE
                + " INTEGER," + History.RecordsColumns.STATUS + " INTEGER,"
                + History.RecordsColumns.TIME_STAMP + " INTEGER," + History.RecordsColumns.TARGET
                + " TEXT," + History.RecordsColumns.ROLE + " INTEGER" + ");");

        db.execSQL("CREATE TABLE " + History.FriendsColumns.TABLE_NAME + " ("
                + History.FriendsColumns._ID + " INTEGER PRIMARY KEY,"
                + History.FriendsColumns.USER_NAME + " TEXT," + History.FriendsColumns.USER_ID
                + " TEXT," + History.FriendsColumns.PHOTO + " INTEGER,"
                + History.FriendsColumns.DEVICE_ID + " TEXT" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + History.RecordsColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + History.FriendsColumns.TABLE_NAME);

        onCreate(db);
    }

}
