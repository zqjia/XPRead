/**
 * <p>Title: ucweb</p>
 *
 * <p>Description: </p>
 * 解析中间件协议
 * <p>Copyright: Copyright (c) 2010</p>
 *
 * <p>Company: ucweb.com</p>
 *
 * @author wujm@ucweb.com
 * @version 1.0
 */

package com.xpread.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.uc.base.wa.WaBodyBuilder;
import com.uc.base.wa.WaEntry;
import com.xpread.wa.WaKeys;

/**
 * 用于实验室数据
 * 
 * @author Administrator
 */
public class LaboratoryData {

    private final static String TAG = LaboratoryData.class.getSimpleName();

    private static HashMap<String, String> cache = new HashMap<String, String>();

    // public static Context gContext;

    // 传送文件总大小
    public static long gTransferFileTotalSize;

    public static long gTransferSendFileTotalSize;

    public static long gTransferReceiveFileTotalSize;

    // 总的CPU使用率
    public static long gAppBeginCPUTime;

    public static long gAppEndCPUTime;

    public static long gTotalAPPBeginCPUTime;

    public static long gTotalAPPEndCPUTime;

    public static long gServiceBeginCPUTime;

    public static long gServiceEndCPUTime;

    public static long gTotalServiceBeginCPUTime;

    public static long gTotalServiceEndCPUTime;

    // 文件传送速度
    public static long gSDWriteSize;

    public static long gSDReadSize;

    public static long gSDWriteTime;

    public static long gSDReadTime;

    public static long gNetWorkWriteTime;

    public static long gNetWorkReadTime;

    public static long gNetWorkWriteSize;

    public static long gNetWorkReadSize;

    public static long gSendFileTotalSize;

    public static long gSendFileTotalTime;

    public static long gReceiveFileTotalSize;

    public static long gReceiveFileTotalTime;

    public static long gTotalSize;

    public static long gTotalTime;

    // wifi建立
    public static long gWifiAPEstablishBeginTime;

    public static long gWifiAPEstablishEndSuccessTime;

    public static long gWifiEstablishBeginTime;

    public static long gWifiEstablishEndSuccessTime;

    private static boolean isWa = false;

    // 实验室数据
    // 传送
    public static final String KEY_XPREAD_DATA_TRANSFER_BUFFER = "transfer_buffer";

    public static final String KEY_XPREAD_DATA_TRANSFER_HEAP_IDEA = "heap_idea";

    public static final String KEY_XPREAD_DATA_TRANSFER_HEAP_ONE_FILE = "heap_one_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_HEAP_TWO_FILE = "heap_two_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_HEAP_THREE_FILE = "heap_three_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_HEAP_MAIN_ACTIVITY = "heap_main_activity";

    public static final String KEY_XPREAD_DATA_TRANSFER_HEAP_FILE_PICK_ACTIVITY = "heap_file_pick_activity";

    public static final String KEY_XPREAD_DATA_TRANSFER_HEAP_RECORD_ACTIVITY = "heap_read_activity";

    /**
     * http post connect请求超时
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_101 = "error_101";

    /**
     * 请求token通道不成功
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_102 = "error_102";

    /**
     * 发送完文件后getResponse失败
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_103 = "error_103";

    /**
     * 文件不存在
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_104 = "error_104";

    /**
     * 取消发送
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_105 = "error_105";

    /**
     * 已经有相同的文件正在传送
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_106 = "error_106";

    /**
     * 其他原因
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_ERROR_107 = "error_107";

    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_TOTAL_FILES = "s_count_total_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_FAIL_FILES = "s_count_fail_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_SUCCESS_FILES = "s_count_success_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_CANCEL_FILES = "s_count_cancel_file";

    // public static final String
    // KEY_XPREAD_DATA_TRANSFER_SEND_RATE_FILES_SUCCESS = "s_file_rate_success";

    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_SPEED_SD_READ = "s_speed_sd_read";

    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_SPEED_NET_WRITE = "s_net_write";

    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_SPEED_TOTAL = "s_speed_total";

    public static final String KEY_XPREAD_DATA_TRANSFER_SEND_SUCCESS_RATE = "s_success_rate";

    // 接收
    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_FILES = "r_count_file";

    // public static final String
    // KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_TOTAL_FILES =
    // "r_count_total_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_FAIL_FILES = "r_count_fail_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_SUCCESS_FILES = "r_count_success_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_CANCEL_FILES = "r_count_cancel_file";

    // public static final String
    // KEY_XPREAD_DATA_TRANSFER_RECEIVE_RATE_FILES_SUCCESS =
    // "r_file_rate_success";

    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_SPEED_NET_READ = "r_speed_net_read";

    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_SPEED_SD_WRITE = "r_speed_sd_write";

    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_SPEED_TOTAL = "r_speed_total";

    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_SUCCESS_RATE = "r_success_rate";

    /**
     * 服务停止，置错所有任务
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_201 = "error_201";

    /**
     * token信道消息不成功
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_202 = "error_202";

    /**
     * 文件取消
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_203 = "error_203";

    /**
     * getReposes code ！= 200
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_204 = "error_204";

    /**
     * 其他原因
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_205 = "error_205";

    /**
     * http头解析错误
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_206 = "error_206";

    /**
     * 接收文件初始化失败（文件不存在）
     */
    public static final String KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_207 = "error_207";

    public static final String KEY_XPREAD_DATA_TRANSFER_TOTAL_FILE = "total_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_TOTAL_SUCCESS_FILE = "total_success_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_TOTAL_CANCEL_FILE = "total_cancel_file";

    public static final String KEY_XPREAD_DATA_TRANSFER_TOTAL_FAIL_FILE = "total_fail_file";

    public static final String KEY_XPRAD_DATA_TRANSFER_TOTAL_SPEED = "total_speed";

    public static final String KEY_XPREAD_DATA_TRANSFER_TOTAL_RATE_SUCCESS = "total_rate_success";

    public static final String KEY_XPREAD_DATA_TRANSFER_TOTAL_SIZE = "total_size";

    public static final String KEY_XPREAD_DATA_TRANSFER_TOTAL_SEND_SIZE = "total_s_size";

    public static final String KEY_XPREAD_DATA_TRANSFER_TOTAL_RECEIVE_SIZE = "total_r_size";

    // 连接
    public static final String KEY_XPREAD_DATA_CONNECT_DISCONNECT_ACK_TIME_OUT = "disc_ack_time_out";

    public static final String KEY_XPREAD_DATA_CONNECT_DISCONNECT_ACK_SUCCESS = "disc_ack_success";

    public static final String KEY_XPREAD_DATA_CONNECT_DISCONNECT_COUNT = "disc_count";

    public static final String KEY_XPREAD_DATA_CONNECT_DISCONNECT_NO_FILE_SEND_TIME_OUT = "disc_no_s_time_out";

    // wifi
    public static final String KEY_XPREAD_DATA_WIFI_AP_ESTABLISH_SUCCESS = "ap_est_success";

    public static final String KEY_XPREAD_DATA_WIFI_AP_ESTABLISH_SUCCESS_TIME = "ap_est_success_time";

    public static final String KEY_XPREAD_DATA_WIFI_AP_ESTABLISH_FAILURE = "ap_est_failure";

    public static final String KEY_XPREAD_DATA_WIFI_AP_ESTABLISH_TOTAL_COUNT = "ap_est_total_count";

    public static final String KEY_XPREAD_DATA_WIFI_AP_WAIT_WIFI_ESTABLISH_TIME_OUT = "ap_wait_est_time_out";

    public static final String KEY_XPREAD_DATA_WIFI_ESTABLISH_SUCCESS = "wifi_est_success";

    public static final String KEY_XPREAD_DATA_WIFI_ESTABLISH_TOTAL_COUNT = "wifi_est_total_count";

    public static final String KEY_XPREAD_DATA_WIFI_ESTABLISH_SUCCESS_TIME = "wifi_est_success_time";

    public static final String KEY_XPREAD_DATA_CPU_APP_RATE = "cpu_app_rate";

    public static final String KEY_XPREAD_DATA_CPU_SERIVCE_RATE = "cpu_service_rate";

    //
    // public static final String KEY_XPREAD_DATA_PROPERTY_APP_CPU_RATE =
    // "app_cpu_rate";
    //
    // public static final String KEY_XPREAD_DATA_PROPERTY_SERVICE_CPU_RATE =
    // "service_cpu_rate";

    // 蓝牙
    public static final String KEY_XPREAD_DATA_BLUETOOTH_SEND_COUNT = "bluetooth_s_count";

    // 手机信息
    public static final String KEY_XPREAD_DATA_PHONE_ANDROID_VERSION = "android_version";

    public static final String KEY_XPREAD_DATA_PHONE_SDK_INT = "sdk_int";

    public static final String KEY_XPREAD_DATA_PHONE_DEVICE_MODEL = "device_model";

    public static synchronized void put(String key, String value) {
        Long.parseLong(value);
        cache.put(key, value);
    }

    /**
     * @param key
     */
    public static synchronized void addOne(String key) {
        if (cache.containsKey(key)) {
            String s = cache.get(key);
            long l = Long.parseLong(s);
            l++;
            cache.put(key, "" + l);
//            Log.e("addOne --- " + key, cache.toString());
        } else {
            cache.put(key, "1");
//            Log.e("addOne else " + key, cache.toString());
        }
    }

    public static void initAppBeginCPUTime() {
        if (gAppBeginCPUTime != 0L) {
            throw new RuntimeException("gAppBeginTotalCPUTime only can be init one time");
        }
        gAppBeginCPUTime = getTotalCpuTime();
    }

    public static void initAppEndCPUTime() {
        if (gAppEndCPUTime != 0L) {
            throw new RuntimeException("gAppBeginTotalCPUTime only can be init one time");
        }
        gAppEndCPUTime = getTotalCpuTime();
    }

    public static void initTotalAPPBeginCPUTime() {
        if (gTotalAPPBeginCPUTime != 0L) {
            throw new RuntimeException("gAppBeginTotalCPUTime only can be init one time");
        }
        gTotalAPPBeginCPUTime = getTotalCpuTime();
    }

    public static void initTotalAPPEndCPUTime() {
        if (gTotalAPPEndCPUTime != 0L) {
            throw new RuntimeException("gAppBeginTotalCPUTime only can be init one time");
        }
        gTotalAPPEndCPUTime = getTotalCpuTime();
    }

    private static float getAPPCPU() {
        float cpuRate = 100 * (gAppEndCPUTime - gAppBeginCPUTime)
                / (gTotalAPPEndCPUTime - gTotalAPPBeginCPUTime);
        return cpuRate;
    }

    public static int getThisProcessMemeryInfo(Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        int pid = android.os.Process.myPid();
        android.os.Debug.MemoryInfo[] memoryInfoArray = am.getProcessMemoryInfo(new int[] {
            pid
        });
        int result = memoryInfoArray[0].getTotalPrivateDirty();
        return result;
    }

    // /**
    // * @return
    // */
    // public static float getProcessCpuRate() {
    //
    // g
    // try {
    // Thread.sleep(360);
    // } catch (Exception e) {
    // }
    //
    // float totalCpuTime2 = getTotalCpuTime();
    // float processCpuTime2 = getAppCpuTime();
    //
    // float cpuRate = 100 * (processCpuTime2 - processCpuTime1) /
    // (totalCpuTime2 - totalCpuTime1);
    //
    // return cpuRate;
    // }

    private static void caculateCPURate() {
        // Log.e("%%%%%%%%%%%%%%%%%%T%%%%%%%%%%%%%%%%%%%%%%%%",
        // " gTotalAPPBeginCPUTime = "
        // + gTotalAPPBeginCPUTime + " gTotalAPPEndCPUTime = " +
        // gTotalAPPEndCPUTime
        // + " gAppBeginCPUTime = " + gAppBeginCPUTime + " gAppEndCPUTime = " +
        // gAppEndCPUTime);
        if (gTotalAPPBeginCPUTime != 0L && gTotalAPPEndCPUTime != 0L
                && (gTotalAPPEndCPUTime - gTotalAPPBeginCPUTime) != 0L) {
            if (gTotalAPPEndCPUTime > gTotalAPPBeginCPUTime && gAppEndCPUTime > gAppBeginCPUTime) {
                float cpuRate = 100 * (gAppEndCPUTime - gAppBeginCPUTime)
                        / (gTotalAPPEndCPUTime - gTotalAPPBeginCPUTime);
                cache.put(KEY_XPREAD_DATA_CPU_APP_RATE, "" + cpuRate);
            }
        }

        // Log.e("%%%%%%%%%%%%%%%%%%S%%%%%%%%%%%%%%%%%%%%%%%%",
        // " gTotalServiceBeginCPUTime = "
        // + gTotalServiceBeginCPUTime + " gTotalServiceEndCPUTime = "
        // + gTotalServiceEndCPUTime + " gServiceBeginCPUTime = " +
        // gServiceBeginCPUTime
        // + " gServiceEndCPUTime = " + gServiceEndCPUTime);
        if (gTotalServiceBeginCPUTime != 0L && gTotalServiceEndCPUTime != 0L
                && (gTotalServiceEndCPUTime - gTotalServiceBeginCPUTime) != 0L) {
            if (gTotalServiceEndCPUTime > gTotalServiceBeginCPUTime
                    && gServiceEndCPUTime > gServiceBeginCPUTime) {
                float cpuRate = 100 * (gServiceEndCPUTime - gServiceBeginCPUTime)
                        / (gTotalServiceEndCPUTime - gTotalServiceBeginCPUTime);
                cache.put(KEY_XPREAD_DATA_CPU_SERIVCE_RATE, "" + cpuRate);
            }
        }

    }

    /**
     * 获取系统总CPU使用时间
     * 
     * @return
     */
    public static long getTotalCpuTime() {
        String[] cpuInfos = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
                    "/proc/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        long totalCpu = Long.parseLong(cpuInfos[2]) + Long.parseLong(cpuInfos[3])
                + Long.parseLong(cpuInfos[4]) + Long.parseLong(cpuInfos[6])
                + Long.parseLong(cpuInfos[5]) + Long.parseLong(cpuInfos[7])
                + Long.parseLong(cpuInfos[8]);
        return totalCpu;
    }

    /**
     * 获取应用占用的CPU时间
     * 
     * @return
     */
    public static long getAppCpuTime() {
        String[] cpuInfos = null;
        try {
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
                    "/proc/" + pid + "/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        long appCpuTime = Long.parseLong(cpuInfos[13]) + Long.parseLong(cpuInfos[14])
                + Long.parseLong(cpuInfos[15]) + Long.parseLong(cpuInfos[16]);
        return appCpuTime;
    }

    public static long getgSDWriteSize() {
        return gSDWriteSize;
    }

    public static void setgSDWriteSize(long gSDWriteSize) {
        LaboratoryData.gSDWriteSize = gSDWriteSize;
    }

    public static long getgSDReadSize() {
        return gSDReadSize;
    }

    public static void setgSDReadSize(long gSDReadSize) {
        LaboratoryData.gSDReadSize = gSDReadSize;
    }

    public static long getgSDWriteTime() {
        return gSDWriteTime;
    }

    public static void setgSDWriteTime(long gSDWriteTime) {
        LaboratoryData.gSDWriteTime = gSDWriteTime;
    }

    public static long getgSDReadTime() {
        return gSDReadTime;
    }

    public static void setgSDReadTime(long gSDReadTime) {
        LaboratoryData.gSDReadTime = gSDReadTime;
    }

    public static long getgNetWorkWriteTime() {
        return gNetWorkWriteTime;
    }

    public static void setgNetWorkWriteTime(long gNetWorkWriteTime) {
        LaboratoryData.gNetWorkWriteTime = gNetWorkWriteTime;
    }

    public static long getgNetWorkReadTime() {
        return gNetWorkReadTime;
    }

    public static void setgNetWorkReadTime(long gNetWorkReadTime) {
        LaboratoryData.gNetWorkReadTime = gNetWorkReadTime;
    }

    public static long getgNetWorkWriteSize() {
        return gNetWorkWriteSize;
    }

    public static void setgNetWorkWriteSize(long gNetWorkWriteSize) {
        LaboratoryData.gNetWorkWriteSize = gNetWorkWriteSize;
    }

    public static long getgNetWorkReadSize() {
        return gNetWorkReadSize;
    }

    public static void setgNetWorkReadSize(long gNetWorkReadSize) {
        LaboratoryData.gNetWorkReadSize = gNetWorkReadSize;
    }

    public static long getgSendFileTotalSize() {
        return gSendFileTotalSize;
    }

    public static void setgSendFileTotalSize(long gSendFileTotalSize) {
        LaboratoryData.gSendFileTotalSize = gSendFileTotalSize;
    }

    public static long getgReceiveFileTotalSize() {
        return gReceiveFileTotalSize;
    }

    public static void setgReceiveFileTotalSize(long gReceiveFileTotalSize) {
        LaboratoryData.gReceiveFileTotalSize = gReceiveFileTotalSize;
    }

    public static void print() {
        synchronized (LaboratoryData.class) {
            collectTransferSize();
            collectPhoneInfo();
            caculateSpeed();
            caculateFileTransferSuccessRate();
            caculateWifiApEstablishTime();
            caculateWifiEstablishTime();
            caculateCPURate();
//            Log.e("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%", cache.toString());
        }
        if (isWa) {
            flushWa();
        }
    }

    private static void flushWa() {
        WaEntry.statEv(WaKeys.CATEGORY_XPREAD_DATA, WaBodyBuilder.newInstance().build(cache));
    }

    private static void caculateSpeed() {
        if (gSDReadSize != 0L && gSDReadTime != 0L) {
            long sendReadSDSpeed = gSDReadSize / gSDReadTime;
            cache.put(KEY_XPREAD_DATA_TRANSFER_SEND_SPEED_SD_READ, "" + sendReadSDSpeed);
        }
        if (gNetWorkWriteSize != 0L && gNetWorkWriteTime != 0) {
            long sendWriteNetSpeed = gNetWorkWriteSize / gNetWorkWriteTime;
            cache.put(KEY_XPREAD_DATA_TRANSFER_SEND_SPEED_NET_WRITE, "" + sendWriteNetSpeed);
        }
        if (gSendFileTotalSize != 0L && gSendFileTotalTime != 0) {
            long sendTotalSpeed = gSendFileTotalSize / gSendFileTotalTime;
            cache.put(KEY_XPREAD_DATA_TRANSFER_SEND_SPEED_TOTAL, "" + sendTotalSpeed);
        }

        if (gNetWorkReadTime != 0L && gNetWorkReadSize != 0L) {
            long receiveReadNetSpeed = gNetWorkReadSize / gNetWorkReadTime;
            cache.put(KEY_XPREAD_DATA_TRANSFER_RECEIVE_SPEED_NET_READ, "" + receiveReadNetSpeed);
        }

        if (gSDWriteSize != 0L && gSDWriteTime != 0L) {
            long receiveWriteSDSpeed = gSDWriteSize / gSDWriteTime;
            cache.put(KEY_XPREAD_DATA_TRANSFER_RECEIVE_SPEED_SD_WRITE, "" + receiveWriteSDSpeed);
        }

        if (gReceiveFileTotalSize != 0L && gReceiveFileTotalTime != 0L) {
            long receiveTotalSpeed = gReceiveFileTotalSize / gReceiveFileTotalTime;
            cache.put(KEY_XPREAD_DATA_TRANSFER_RECEIVE_SPEED_TOTAL, "" + receiveTotalSpeed);
        }

        gTotalTime += (gSendFileTotalTime + gReceiveFileTotalTime);
        gTotalSize += (gReceiveFileTotalSize + gSendFileTotalSize);

        if (gTotalTime != 0L && gTotalSize != 0L) {
            long totalSpeed = gTotalSize / gTotalTime;
            cache.put(KEY_XPRAD_DATA_TRANSFER_TOTAL_SPEED, "" + totalSpeed);
        }
    }

    private static void caculateFileTransferSuccessRate() {
        String tfs = cache.get(KEY_XPREAD_DATA_TRANSFER_TOTAL_FAIL_FILE);
        String tts = cache.get(KEY_XPREAD_DATA_TRANSFER_TOTAL_FILE);
        if (tts == null) {
            return;
        }

        try {
            long tfl = 0L;
            long ttl = Long.parseLong(tts);
            if (tfs != null) {
                tfl = Long.parseLong(tfs);
            }
            if (ttl != 0L) {
                long totalSuccessRate = (ttl - tfl) * 100 / ttl;
                cache.put(KEY_XPREAD_DATA_TRANSFER_TOTAL_RATE_SUCCESS, "" + totalSuccessRate);
            }
        } catch (NumberFormatException e) {
            // 不对异常处理
            e.printStackTrace();
        }

        String sfs = cache.get(KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_FAIL_FILES);
        String sts = cache.get(KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_TOTAL_FILES);
        if (sts == null) {
            return;
        }
        try {
            long sfl = 0L;
            long stl = Long.parseLong(sts);
            if (sfs != null) {
                sfl = Long.parseLong(sfs);
            }
            if (stl != 0L) {
                long sendSuccessRate = (stl - sfl) * 100 / stl;
                cache.put(KEY_XPREAD_DATA_TRANSFER_SEND_SUCCESS_RATE, "" + sendSuccessRate);
            }
        } catch (NumberFormatException e) {
            // 不对异常处理
            e.printStackTrace();
        }

        String rfs = cache.get(KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_FAIL_FILES);
        String rts = cache.get(KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_FILES);
        if (rts == null) {
            return;
        }
        try {
            long rfl = 0;
            if (rfs != null) {
                rfl = Long.parseLong(rfs);
            }
            long rtl = Long.parseLong(rts);
            if (rtl != 0L) {
                long receiveSuccessRate = (rtl - rfl) * 100 / rtl;
                cache.put(KEY_XPREAD_DATA_TRANSFER_RECEIVE_SUCCESS_RATE, "" + receiveSuccessRate);
            }
        } catch (NumberFormatException e) {
            // 不对异常处理
            e.printStackTrace();
        }
    }

    private static void caculateWifiApEstablishTime() {
        if (gWifiAPEstablishBeginTime != 0L && gWifiAPEstablishEndSuccessTime != 0L) {
            long time = gWifiAPEstablishEndSuccessTime - gWifiAPEstablishBeginTime;
            if (time < 60000) {
                cache.put(KEY_XPREAD_DATA_WIFI_AP_ESTABLISH_SUCCESS_TIME, "" + time);
            }
        }
    }

    private static void caculateWifiEstablishTime() {
        if (gWifiEstablishBeginTime != 0L && gWifiEstablishEndSuccessTime != 0L) {
            long time = gWifiEstablishEndSuccessTime - gWifiEstablishBeginTime;
            if (time < 60000) {
                cache.put(KEY_XPREAD_DATA_WIFI_ESTABLISH_SUCCESS_TIME, "" + time);
            }
        }
    }

    private static void collectPhoneInfo() {
        String deviceModel = android.os.Build.MODEL.trim();
        String phoneAndroidVersion = android.os.Build.VERSION.RELEASE.trim();
        String phoneSDKINT = "" + android.os.Build.VERSION.SDK_INT;
        cache.put(KEY_XPREAD_DATA_PHONE_DEVICE_MODEL, deviceModel);
        cache.put(KEY_XPREAD_DATA_PHONE_ANDROID_VERSION, phoneAndroidVersion);
        cache.put(KEY_XPREAD_DATA_PHONE_SDK_INT, phoneSDKINT);
    }

    private static void collectTransferSize() {
        cache.put(KEY_XPREAD_DATA_TRANSFER_TOTAL_SIZE, "" + gTransferFileTotalSize);
        cache.put(KEY_XPREAD_DATA_TRANSFER_TOTAL_SEND_SIZE, "" + gTransferSendFileTotalSize);
        cache.put(KEY_XPREAD_DATA_TRANSFER_TOTAL_RECEIVE_SIZE, "" + gTransferReceiveFileTotalSize);
    }
}
