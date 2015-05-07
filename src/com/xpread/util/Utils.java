
package com.xpread.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.xpread.R;

public class Utils {

    private static SharedPreferences mPreference;

    // add by zqjia
    // delete the illegel char in the default name
    private static final String[] illegalChar = {
            "/", "'", "[", "]", "%", "&", "(", ")", "~", "!", "@", "#", "$", "^", "*", "+", "=",
            "|", ",", ".", ":", ";"
    };

    public static int[] photos = new int[] {
            R.drawable.male_01, R.drawable.male_02, R.drawable.male_03, R.drawable.male_04,
            R.drawable.female_01, R.drawable.female_02, R.drawable.female_03, R.drawable.female_04
    };

    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2).denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator()).threadPoolSize(3)
                .diskCacheSize(20 * 1024 * 1024).tasksProcessingOrder(QueueProcessingType.LIFO)
                .memoryCache(new WeakMemoryCache()).writeDebugLogs().build();

        ImageLoader.getInstance().init(config);
    }

    public static byte[] createBlobData(Bitmap bitmap) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);

        return os.toByteArray();
    }

    public static Bitmap getDataFromBlob(byte[] bits) {
        return BitmapFactory.decodeByteArray(bits, 0, bits.length);
    }

    public static Bitmap getOwnerPhoto(Context context) {
        FileInputStream fis = null;

        try {
            fis = context.openFileInput(Const.IMAGE_FILE_NAME);
        } catch (FileNotFoundException e) {
            return null;
        }

        BufferedInputStream bis = new BufferedInputStream(fis);

        Bitmap photo = BitmapFactory.decodeStream(bis);

        try {
            bis.close();
        } catch (IOException e) {
            LogUtil.d("close BIS error.");
        }

        try {
            fis.close();
        } catch (IOException e) {
            LogUtil.d("close FIS error.");
        }

        return photo;
    }

    public static String getOwnerName(Context context) {
        if (mPreference == null) {
            mPreference = context.getSharedPreferences(Const.PREFERENCES_NAME,
                    Activity.MODE_PRIVATE);
        }

        // FIXME
        /*
         * add by zqjia delete the illegal char in the phone model
         */
        String defaultName = android.os.Build.MODEL.trim().toString();
        for (int i = 0; i < illegalChar.length; ++i) {
            if (defaultName.contains(illegalChar[i])) {
                defaultName.replace(illegalChar[i], "");
            }
        }

        return mPreference.getString(Const.OWNER_NAME_KEY, defaultName);
    }

    public static int getOwerIcon(Context context) {
        if (mPreference == null) {
            mPreference = context.getSharedPreferences(Const.PREFERENCES_NAME,
                    Activity.MODE_PRIVATE);
        }

        return mPreference.getInt(Const.OWNER_ICON_KEY, 0);
    }

    public static String getDeviceId(Context context) {
        if (mPreference == null) {
            mPreference = context.getSharedPreferences(Const.PREFERENCES_NAME,
                    Activity.MODE_PRIVATE);
        }

        return mPreference.getString(Const.OWNER_IMEI_KEY, "");
    }

    public static void saveDeviceId(Context context) {

        String id = getDeviceId(context);

        if (TextUtils.isEmpty(id)) {
            TelephonyManager mTm = (TelephonyManager)context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            // FIXME
            /*
             * add by zqjia compress the devices , use the radix
             */

            id = mTm.getDeviceId();

            // if the device id is null, generate it
            if (TextUtils.isEmpty(id)) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < 15; i++) {
                    sb.append((int)(Math.random() * 10));
                }

                id = sb.toString();
            }

            String imei = new BigInteger(id).toString(36);

            Editor editor = mPreference.edit();
            editor.putString(Const.OWNER_IMEI_KEY, imei);
            editor.commit();
        }

    }

    public static void saveUserInfo(Context context, String name, int icon) {
        if (mPreference == null) {
            mPreference = context.getSharedPreferences(Const.PREFERENCES_NAME,
                    Activity.MODE_PRIVATE);
        }

        Editor editor = mPreference.edit();
        if (name != null) {
            editor.putString(Const.OWNER_NAME_KEY, name);
        }

        editor.putInt(Const.OWNER_ICON_KEY, icon);
        editor.commit();
    }

    public static void saveTotalTransmission(Context context, long size) {
        if (mPreference == null) {
            mPreference = context.getSharedPreferences(Const.PREFERENCES_NAME,
                    Activity.MODE_PRIVATE);
        }
        Editor editor = mPreference.edit();
        editor.putLong(Const.OWNER_TOTAL_TRANSMISSION, size);
        editor.commit();

    }

    public static long getTotalTransmission(Context context) {
        if (mPreference == null) {
            mPreference = context.getSharedPreferences(Const.PREFERENCES_NAME,
                    Activity.MODE_PRIVATE);
        }

        return mPreference.getLong(Const.OWNER_TOTAL_TRANSMISSION, 0);
    }

    public static int getFileType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1);
        if (extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpg")
                || extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("bmp")) {
            return Const.TYPE_IMAGE;
        }

        if (extension.equalsIgnoreCase("mp4") || extension.equalsIgnoreCase("avi")
                || extension.equalsIgnoreCase("3gp") || extension.equalsIgnoreCase("asf")
                || extension.equalsIgnoreCase("f4v") || extension.equalsIgnoreCase("flv")
                || extension.equalsIgnoreCase("wmv")) {

            return Const.TYPE_VIDEO;
        }

        if (extension.equalsIgnoreCase("mp3") || extension.equalsIgnoreCase("aac")
                || extension.equalsIgnoreCase("amr") || extension.equalsIgnoreCase("ogg")
                || extension.equalsIgnoreCase("wav") || extension.equalsIgnoreCase("mid")
                || extension.equalsIgnoreCase("m4a") || extension.equalsIgnoreCase("ape")
                || extension.equalsIgnoreCase("flac")) {
            return Const.TYPE_MUSIC;
        }

        if (extension.equalsIgnoreCase("rar") || extension.equalsIgnoreCase("zip")) {
            return Const.TYPE_ZIP;
        }

        if (extension.equalsIgnoreCase("txt") || extension.equalsIgnoreCase("html")
                || extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("ini")
                || extension.equalsIgnoreCase("log")) {
            return Const.TYPE_TEXT;
        }

        if (extension.equalsIgnoreCase("apk")) {
            return Const.TYPE_APP;
        }

        return Const.TYPE_UNKNOW;

    }
    
    public static boolean isGooglePlayInstall(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                if (packageInfo.applicationInfo.packageName.contains("com.android.vending")) {
                    return true;
                }
            }
        }
        return false;
    }
}
