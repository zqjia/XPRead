
package com.xpread.transfer.exception;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.uc.base.wa.WaEntry;
import com.xpread.R;
import com.xpread.control.Controller;
import com.xpread.control.WifiAdmin;
import com.xpread.control.WifiApAdmin;
import com.xpread.provider.UserInfo;
import com.xpread.util.LogUtil;
import com.xpread.util.NetworkUtil;
import com.xpread.util.SDCardUtil;
import com.xpread.util.Utils;
import com.xpread.util.WaUtils;
import com.xpread.wa.WaKeys;

/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 * 
 * @author user
 */
public class CrashHandler implements UncaughtExceptionHandler {

    public static final String TAG = "CrashHandler";

    public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;

    private final static String SAVE_PATH = SDCardUtil.getSDCardPath() + "xpreadLog";

    // 系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    // CrashHandler实例
    private static CrashHandler INSTANCE = new CrashHandler();

    // 程序的Context对象
    private Context mContext;

    // 用来存储设备信息和异常信息
    private Map<String, String> mInfos = new HashMap<String, String>();

    // 用于格式化日期,作为日志文件名的一部分
    private DateFormat mFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    private Controller mController;

    /** 保证只有一个CrashHandler实例 */
    private CrashHandler() {
    }

    /** 获取CrashHandler实例 ,单例模式 */
    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化
     * 
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);

        mController = Controller.getInstance(mContext);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_CRASH);
        WaEntry.handleMsg(WaEntry.MSG_CRASHED);
        ex.printStackTrace();
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "error : ", e);
                }
            }
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     * 
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 使用Toast来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, R.string.exception_application, Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();
        // 收集设备参数信息
        collectDeviceInfo(mContext);
        // 保存日志文件
        saveCrashInfo2File(ex);

        // 断开已连接上的好友
        WifiAdmin wifiAdmin = mController.getWifiAdmin();
        UserInfo userInfo = mController.getUerInfo();
        String connectedFriendSsid = mController.getUerInfo().getConnectedFriendSsid();
        if (connectedFriendSsid != null) {
            // 第一步，如果与好友连接，则先断开
            if (NetworkUtil.isConnected(mContext)
                    && wifiAdmin.getConnectedSsid().startsWith("xpread")) {
                wifiAdmin.disconnectCurrentWifi();
            }

            // 第二步，移除好友的网络设置
            if (android.os.Build.VERSION.SDK_INT != 21) {

                if (connectedFriendSsid != null) {
                    wifiAdmin.removeWifiConfiguration(connectedFriendSsid);
                }
            } else {
                wifiAdmin.disableNetWork(connectedFriendSsid);
            }

            // 第三步，恢復wifi打开状态
            int isWifiConnectedBefore = userInfo.getIsWifiConnectedBefore();
            if (isWifiConnectedBefore == 1) {
                if (openWifi()) {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "wifi open success");
                    }
                } else {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "wifi open fail");
                    }
                }
                wifiAdmin.reconnect();
            } else if (isWifiConnectedBefore == 0) {
                wifiAdmin.closeWifi();
            }

            userInfo.setIsWifiConnectedBefore(-1);
        }

        return true;
    }

    /**
     * 收集设备参数信息
     * 
     * @param context
     */
    public void collectDeviceInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                mInfos.put("versionName", versionName);
                mInfos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            if (LogUtil.isLog) {

                Log.e(TAG, "an error occured when collect package info", e);
            }
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                mInfos.put(field.getName(), field.get(null).toString());
                if (LogUtil.isLog) {
                    Log.d(TAG, field.getName() + " : " + field.get(null));
                }
            } catch (Exception e) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "an error occured when collect crash info", e);
                }
            }
        }
    }

    /**
     * 保存错误信息到文件中
     * 
     * @param ex
     * @return 返回文件名称,便于将文件传送到服务器
     */
    private String saveCrashInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : mInfos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            String time = mFormatter.format(new Date());
            String fileName = "xpread_" + "_V" + WaUtils.getVersionName() + "_"
                    + Utils.getOwnerName() + "_" + time + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = SAVE_PATH;
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + "/" + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            if (LogUtil.isLog) {
                Log.e(TAG, "an error occured while writing file...", e);
            }
        }
        return null;
    }

    private boolean openWifi() {
        WifiAdmin wifiAdmin = this.mController.getWifiAdmin();
        WifiApAdmin wifiApAdmin = this.mController.getWifiApAdmin();

        int wifiState = wifiAdmin.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED
                || wifiState == WifiManager.WIFI_STATE_ENABLING) {
            if (LogUtil.isLog) {
                Log.d(TAG, "wifi is enbale or enbaling, it's ok");
            }
            return true;
        } else {
            if (wifiApAdmin.getWifiApState() == WIFI_AP_STATE_ENABLING
                    || wifiApAdmin.getWifiApState() == WIFI_AP_STATE_ENABLED) {
                if (LogUtil.isLog) {
                    Log.d(TAG, "wifi ap is enbale , close it");
                }

                WifiConfiguration wcg = wifiApAdmin.getWifiApConfiguration();
                if (!wifiApAdmin.setWifiApEnabled(wcg, false)) {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "close wifi ap fail");
                        return false;
                    }
                }
            }

            if (!wifiAdmin.openWifi()) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "open wifi fail");
                }
                return false;
            }

            return true;
        }
    }
}
