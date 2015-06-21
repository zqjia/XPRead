
package com.xpread.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.xpread.XApplication;
import com.xpread.control.Controller;

/**
 * @Title: WaUtils.java
 * @Package com.xpread.wa.util
 * @Description: TODO(用一句话描述该文件做什么)
 * @author zhanhl@ucweb.com
 * @date 2015-1-16 下午5:45:32
 * @version V1.0
 */
public class WaUtils {
    private static Context mContext;

    private static SharedPreferences mPreference;

    static {
        mContext = XApplication.getContext();
        mPreference = mContext.getSharedPreferences(Const.PREFERENCES_NAME, Activity.MODE_PRIVATE);
    }

    // 获取cpu的核心数
    public static String getCpuArch() {

        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {

                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        int coreCount = 1;

        try {

            File dir = new File("/sys/devices/system/cpu/");

            File[] files = dir.listFiles(new CpuFilter());

            // Return the number of cores (virtual CPU devices)
            coreCount = files.length;
        } catch (Exception e) {
            // Print exception
            e.printStackTrace();
        }

        return String.valueOf(coreCount);

    }

    // 获取设备的MAC地址
    public static String getMacAddress() {
        WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);

        WifiInfo info = wifi.getConnectionInfo();

        return info == null ? "" : info.getMacAddress();
    }

    // 获取Android版本信息
    public static String getVersionName() {

        PackageManager packageManager = mContext.getPackageManager();

        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        String version = packInfo == null ? "" : packInfo.versionName;
        return version;
    }

    // 获取屏幕的宽度或高度
    public static String getScreenSize(boolean isWidth) {
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        int info = isWidth ? metrics.widthPixels : metrics.heightPixels;

        return String.valueOf(info);
    }

    // 获取系统语言
    public static String getSystemLanguage() {
        Locale locale = Locale.getDefault();
        return String.format("%1$s-%2$s", locale.getLanguage(), locale.getCountry());
    }

    // 获取剩余内存
    public static String getFreeMemory() {
        ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        float size = mi.availMem / 1024f / 1024f;

        return String.format(Locale.ENGLISH, "%1$.2fMB", size);
    }

    // 获取设备id
    public static String getDeviceId() {
        String id = Utils.getDeviceId();
        if (TextUtils.isEmpty(id)) {
            return "000000000000000";
        }

        return Long.valueOf(id, 36).toString();
    }

    // 获取设备id
    public static int getPhoneType() {
        TelephonyManager mTm = (TelephonyManager)mContext
                .getSystemService(Context.TELEPHONY_SERVICE);

        return mTm.getPhoneType();
    }

    // 网络类型
    public static boolean isWifiNetwork() {
        ConnectivityManager connectMgr = (ConnectivityManager)mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectMgr.getActiveNetworkInfo();

        if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
            return !Controller.getInstance(mContext).isConnected();
        }

        return false;
    }

    public static boolean isMobileNetwork() {
        ConnectivityManager connectMgr = (ConnectivityManager)mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectMgr.getActiveNetworkInfo();

        if (info != null) {
            return info.getType() == ConnectivityManager.TYPE_MOBILE;
        }

        return false;
    }

    public static void setLongValue(String key, long value) {
        Editor editor = mPreference.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static long getLongValue(String key) {
        long value = 0;
        try {
            value = mPreference.getLong(key, 0);
        } catch (ClassCastException e) {
            // do nothing
        }

        return value;

    }

    public static boolean writeBytes(File file, byte[] headData, byte[] bodyData, int offset,
            int len) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        if (file == null || file.isDirectory()) {
            return false;
        }

        try {
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(headData);
            bos.write(bodyData);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return true;
    }

    public static byte[] readBytes(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        byte[] buffer = null;
        try {
            int cacheSize = 1024;

            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(cacheSize);
            byte[] b = new byte[cacheSize];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }

            buffer = bos.toByteArray();

            fis.close();
            bos.close();
        } catch (FileNotFoundException e) {
            // TODO
        } catch (IOException e) {
            // TODO: handle exception
        }

        return buffer;
    }
}
