
package com.xpread.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class History {
    public static final String AUTHORITY = "com.xpread";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.xpread";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.xpread";

    public static class RecordsColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/records");
        public static final String DEFAULT_SORT_ORDER = "time_stamp desc";
        public static final String TABLE_NAME = "records";
        public static final String DISPLAY_NAME = "display_name";
        public static final String DATA = "data";
        public static final String DISPLAY_ICON = "display_icon";
        public static final String TYPE = "type";
        public static final String STATUS = "status";
        public static final String SIZE = "size";
        public static final String TIME_STAMP = "time_stamp";
        public static final String TARGET = "target";
        public static final String ROLE = "role";
    }

    public static class FriendsColumns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/friends");
        public static final String DEFAULT_SORT_ORDER = "device_id asc";
        public static final String TABLE_NAME = "friends";
        public static final String USER_NAME = "user_name";
        public static final String PHOTO = "photo";
        public static final String DEVICE_ID = "device_id";
        public static final String USER_ID = "user_id";
    }
}
