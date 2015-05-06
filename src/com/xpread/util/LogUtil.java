
package com.xpread.util;

import android.util.Log;

public final class LogUtil {

    private static final String TAG = "LogUtil";

    private static final boolean IS_LOG = true;

    public static boolean isLog = true;

    // 不允许实例化
    private LogUtil() {
    }

    private static void l(int level, String str, boolean isLog) {
        if (isLog) {
            switch (level) {
                case Log.ERROR:
                    Log.e(TAG, str);
                    break;
                case Log.DEBUG:
                    Log.d(TAG, str);
                    break;
                case Log.WARN:
                    Log.w(TAG, str);
                    break;
                case Log.INFO:
                    Log.i(TAG, str);
                    break;
            }
        }
    }

    public static void l(int level, String str) {
        l(level, str, IS_LOG);
    }

    public static void e(String s) {
        l(Log.ERROR, s, IS_LOG);
    }

    public static void w(String s) {
        l(Log.WARN, s, IS_LOG);
    }

    public static void i(String s) {
        l(Log.INFO, s, IS_LOG);
    }

    public static void d(String s) {
        l(Log.DEBUG, s, IS_LOG);
    }
}
