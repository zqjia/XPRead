
package com.xpread.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class HistoryProvider extends ContentProvider {
    private static HashMap<String, String> mHistoryRecordProjectionMap;
    private static HashMap<String, String> mHistoryFriendProjectionMap;

    private static final UriMatcher mUriMatcher;
    private HistoryDatabaseHelper mOpenHelper;

    private static final int RECORDS = 1;
    private static final int RECORDS_ID = 2;
    private static final int FRIENDS = 3;
    private static final int FRIENDS_ID = 4;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(History.AUTHORITY, "records", RECORDS);
        mUriMatcher.addURI(History.AUTHORITY, "records/#", RECORDS_ID);

        mUriMatcher.addURI(History.AUTHORITY, "friends", FRIENDS);
        mUriMatcher.addURI(History.AUTHORITY, "friends/#", FRIENDS_ID);

        mHistoryRecordProjectionMap = new HashMap<String, String>();
        mHistoryRecordProjectionMap.put(History.RecordsColumns._ID, History.RecordsColumns._ID);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.DATA, History.RecordsColumns.DATA);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.DISPLAY_NAME,
                History.RecordsColumns.DISPLAY_NAME);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.DISPLAY_ICON,
                History.RecordsColumns.DISPLAY_ICON);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.TYPE, History.RecordsColumns.TYPE);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.SIZE, History.RecordsColumns.SIZE);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.STATUS, History.RecordsColumns.STATUS);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.TIME_STAMP,
                History.RecordsColumns.TIME_STAMP);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.TARGET, History.RecordsColumns.TARGET);
        mHistoryRecordProjectionMap.put(History.RecordsColumns.ROLE, History.RecordsColumns.ROLE);

        mHistoryFriendProjectionMap = new HashMap<String, String>();
        mHistoryFriendProjectionMap.put(History.FriendsColumns._ID, History.FriendsColumns._ID);
        mHistoryFriendProjectionMap.put(History.FriendsColumns.USER_NAME,
                History.FriendsColumns.USER_NAME);
        mHistoryFriendProjectionMap.put(History.FriendsColumns.USER_ID, History.FriendsColumns.USER_ID);
        mHistoryFriendProjectionMap.put(History.FriendsColumns.PHOTO, History.FriendsColumns.PHOTO);
        mHistoryFriendProjectionMap.put(History.FriendsColumns.DEVICE_ID,
                History.FriendsColumns.DEVICE_ID);

    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new HistoryDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String orderBy;

        switch (mUriMatcher.match(uri)) {
            case RECORDS:
                qb.setTables(History.RecordsColumns.TABLE_NAME);
                qb.setProjectionMap(mHistoryRecordProjectionMap);
                if (TextUtils.isEmpty(sortOrder)) {
                    orderBy = History.RecordsColumns.DEFAULT_SORT_ORDER;
                } else {
                    orderBy = sortOrder;
                }
                break;

            case RECORDS_ID:
                qb.setTables(History.RecordsColumns.TABLE_NAME);
                qb.setProjectionMap(mHistoryRecordProjectionMap);
                qb.appendWhere(History.RecordsColumns._ID + " = " + uri.getPathSegments().get(1));
                if (TextUtils.isEmpty(sortOrder)) {
                    orderBy = History.RecordsColumns.DEFAULT_SORT_ORDER;
                } else {
                    orderBy = sortOrder;
                }
                break;

            case FRIENDS:
                qb.setTables(History.FriendsColumns.TABLE_NAME);
                qb.setProjectionMap(mHistoryFriendProjectionMap);

                if (TextUtils.isEmpty(sortOrder)) {
                    orderBy = History.FriendsColumns.DEFAULT_SORT_ORDER;
                } else {
                    orderBy = sortOrder;
                }
                break;

            case FRIENDS_ID:
                qb.setTables(History.FriendsColumns.TABLE_NAME);
                qb.setProjectionMap(mHistoryFriendProjectionMap);
                qb.appendWhere(History.FriendsColumns._ID + " = " + uri.getPathSegments().get(1));
                if (TextUtils.isEmpty(sortOrder)) {
                    orderBy = History.FriendsColumns.DEFAULT_SORT_ORDER;
                } else {
                    orderBy = sortOrder;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case RECORDS:
            case FRIENDS:
                return History.CONTENT_TYPE;
            case RECORDS_ID:
            case FRIENDS_ID:
                return History.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknow Uri:" + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long id = 0;
        String path;
        switch (mUriMatcher.match(uri)) {
            case RECORDS:
                id = db.insert(History.RecordsColumns.TABLE_NAME, null, values);

                if (id > 0) {
                    Uri recordsUri = ContentUris.withAppendedId(uri, id);
                    getContext().getContentResolver().notifyChange(recordsUri, null);
                    return recordsUri;
                }

                throw new SQLException("Failed to insert row into " + uri);

            case RECORDS_ID:
                id = db.insert(History.RecordsColumns.TABLE_NAME, null, values);
                if (id > 0) {
                    path = uri.toString();
                    Uri recordUri = Uri.parse(path.substring(0, path.lastIndexOf("/")) + id);
                    getContext().getContentResolver().notifyChange(recordUri, null);
                    return recordUri;
                }

                throw new SQLException("Failed to insert row into " + uri);

            case FRIENDS:
                id = db.insert(History.FriendsColumns.TABLE_NAME, null, values);
                if (id > 0) {
                    Uri friendsUri = ContentUris.withAppendedId(uri, id);
                    getContext().getContentResolver().notifyChange(friendsUri, null);
                    return friendsUri;
                }

                throw new SQLException("Failed to insert row into " + uri);

            case FRIENDS_ID:
                id = db.insert(History.FriendsColumns.TABLE_NAME, null, values);

                if (id > 0) {
                    path = uri.toString();
                    Uri friendUri = Uri.parse(path.substring(0, path.lastIndexOf("/")) + id);
                    getContext().getContentResolver().notifyChange(friendUri, null);
                    return friendUri;
                }

                throw new SQLException("Failed to insert row into " + uri);

            default:
                throw new IllegalArgumentException("Unknow Uri:" + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (mUriMatcher.match(uri)) {
            case RECORDS:
                count = db.delete(History.RecordsColumns.TABLE_NAME, selection, selectionArgs);
                Log.e("zqjia History Provider ", "delete count is " + count);
                break;

            case RECORDS_ID:
                count = db.delete(History.RecordsColumns.TABLE_NAME, History.RecordsColumns._ID
                        + " = " + uri.getPathSegments().get(1)
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;

            case FRIENDS:
                count = db.delete(History.FriendsColumns.TABLE_NAME, selection, selectionArgs);
                break;

            case FRIENDS_ID:
                count = db.delete(History.FriendsColumns.TABLE_NAME, History.FriendsColumns._ID
                        + " = " + uri.getPathSegments().get(1)
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknow Uri:" + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (mUriMatcher.match(uri)) {
            case RECORDS:
                count = db.update(History.RecordsColumns.TABLE_NAME, values, selection,
                        selectionArgs);
                break;

            case RECORDS_ID:
                count = db
                        .update(History.RecordsColumns.TABLE_NAME, values,
                                History.RecordsColumns._ID
                                        + " = "
                                        + uri.getPathSegments().get(1)
                                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                                + ")" : ""), selectionArgs);
                break;

            case FRIENDS:
                count = db.update(History.FriendsColumns.TABLE_NAME, values, selection,
                        selectionArgs);
                break;

            case FRIENDS_ID:
                count = db
                        .update(History.FriendsColumns.TABLE_NAME, values,
                                History.FriendsColumns._ID
                                        + " = "
                                        + uri.getPathSegments().get(1)
                                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                                + ")" : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknow Uri:" + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
