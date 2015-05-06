
package com.xpread.wa;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.os.Environment;
import android.util.Log;

import com.uc.base.encryption.M8Helper;
import com.uc.base.wa.WaEntry.WaErrorCode;
import com.uc.base.wa.adapter.WaAdapterInterface;
import com.xpread.util.Const;
import com.xpread.util.FileUtil;
import com.xpread.util.LogUtil;
import com.xpread.util.WaUtils;

public class DefaultAdapter implements WaAdapterInterface {

    public static final boolean ENABLE_TEST = false;

    public static final boolean ENABLE_SAVE_M8 = true;

    public static final int WA_LOCAL_FILE_HEAD_VER0_SIZE = 16;

    public static final int WA_LOCAL_FILE_HEAD_VER1_SIZE = 17;

    public static final byte LOCAL_FILE_VERSION_0 = 0; // 旧版本，size16

    public static final byte LOCAL_FILE_VERSION_1 = 1; // 新版本，size17

    public static final byte LOCAL_FILE_VERSION = LOCAL_FILE_VERSION_1;

    public static final int LOCAL_ENCRYPTION_TYPE_M8 = 1;

    public static final int LOCAL_ENCRYPTION_TYPE = LOCAL_ENCRYPTION_TYPE_M8;

    public static final int LOCAL_ENCRYPTION_KEY_MAX = 2;

    public static final int LOCAL_ENCRYPTION_KEY_WA = 1;

    public static final int LOCAL_ENCRYPTION_KEY = LOCAL_ENCRYPTION_KEY_WA;

    public static String gAppName = null;

    private static final int[] COMMON_M8_KEY_WA = {
            125, 21, 37, 79, 211, 41, 4, 168
    };

    public static final ArrayList<int[]> LOCAL_KEY_CONTENT;
    static {
        // 这么做，应该起一点点混淆效果，避免直接拿到key
        LOCAL_KEY_CONTENT = new ArrayList<int[]>(LOCAL_ENCRYPTION_KEY_MAX);
        LOCAL_KEY_CONTENT.add(0, null);
        LOCAL_KEY_CONTENT.add(LOCAL_ENCRYPTION_KEY_WA, COMMON_M8_KEY_WA);
    }

    // ==== adapter interface begin ====

    @Override
    public boolean encodeData2File(byte[] data, File file) {

        if (data == null) {
            return false;
        }

        if (ENABLE_SAVE_M8) {
            byte[] fileData = M8Helper.m8Encode(data, LOCAL_KEY_CONTENT.get(LOCAL_ENCRYPTION_KEY));

            if (fileData == null) {
                return false;
            }

            byte[] ver = new byte[WA_LOCAL_FILE_HEAD_VER1_SIZE];
            ver[0] = LOCAL_FILE_VERSION;
            ver[1] = LOCAL_ENCRYPTION_TYPE;
            ver[2] = LOCAL_ENCRYPTION_KEY;
            ver[WA_LOCAL_FILE_HEAD_VER1_SIZE - 1] = 0x71;

            WaUtils.writeBytes(file, ver, fileData, 0, fileData.length);
        } else {
            byte[] ver = new byte[WA_LOCAL_FILE_HEAD_VER1_SIZE];
            ver[0] = LOCAL_FILE_VERSION;

            WaUtils.writeBytes(file, ver, data, 0, data.length);
        }
        return true;
    }

    @Override
    public byte[] decodeFile2Data(File file) {
        byte[] data = FileUtil.readBytes(file);

        if (data == null) {
            Log.e("gzm_wa_WaCache", "encodedData is null", new Throwable());
            return null;
        }

        if (data.length <= 0) {
            Log.e("gzm_wa_WaCache", "encodedData len is 0", new Throwable());
            return null;
        }

        if (ENABLE_SAVE_M8) {
            if (data[0] == LOCAL_FILE_VERSION_0) {
                data = M8Helper.m8Decode(data, WA_LOCAL_FILE_HEAD_VER0_SIZE,
                        LOCAL_KEY_CONTENT.get(LOCAL_ENCRYPTION_KEY));
            } else {
                data = M8Helper.m8Decode(data, WA_LOCAL_FILE_HEAD_VER1_SIZE,
                        LOCAL_KEY_CONTENT.get(LOCAL_ENCRYPTION_KEY));
            }
        } else {
            int length = data.length - WA_LOCAL_FILE_HEAD_VER1_SIZE;
            byte[] newData = new byte[length];
            System.arraycopy(data, WA_LOCAL_FILE_HEAD_VER1_SIZE, newData, 0, length);
        }

        return data;
    }

    @Override
    public byte[] encodeForUploading(byte[] data) {

        return data;
    }

    @Override
    public String getEncodedTypeForUploading() {
        return null;
    }

    @Override
    public long getWaUploadedTime() {
        return WaUtils.getLongValue(Const.FLAG_UPLOAD_WA_TIME);
    }

    @Override
    public void setWaUploadedTime(long time) {
        WaUtils.setLongValue(Const.FLAG_UPLOAD_WA_TIME, time);
    }

    @Override
    public void assertFail(String msg) {
        // UCAssert.fail(msg);
    }

    @Override
    public boolean isWifiNetwork() {
        return WaUtils.isWifiNetwork();
    }

    @Override
    public boolean isMobileNetwork() {
        return WaUtils.isMobileNetwork();
    }

    @Override
    public String getUUID() {
        String dn = WaUtils.getDeviceId();
        return dn;
    }

    @Override
    public String fillUcParamInUrl(String url) {
        return url;
    }

    @Override
    public String getWaServerUrl() {
        return "http://gj.applog.uc.cn:9081/collect?uc_param_str=frcpve";
    }

    @Override
    public WaUploadRetStruct upload(String url, byte[] data) {
        LogUtil.d("uploading wa file.");

        WaUploadRetStruct ret = new WaUploadRetStruct();

        ByteArrayOutputStream bo = null;
        InputStream in = null;

        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5 * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, 5 * 1000);

        HttpClient client = new DefaultHttpClient(httpParams);

        HttpEntity entity = new ByteArrayEntity(data);

        HttpPost httpRequest = new HttpPost(url);
        httpRequest.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencode");

        httpRequest.setEntity(entity);

        HttpResponse response = null;
        try {
            response = client.execute(httpRequest);
            if (response == null) {
                return null;
            }

            ret.uploadedSize = data.length;
            StatusLine statusLine = response.getStatusLine();
            ret.statusCode = statusLine.getStatusCode();

            if (ret.statusCode == 200) {
                // 获取返回的数据
                bo = new ByteArrayOutputStream();
                in = response.getEntity().getContent();
                byte[] buff = new byte[1024];
                int length = 0;
                while ((length = in.read(buff)) >= 0) {
                    bo.write(buff, 0, length);
                }

                ret.retByteArray = bo.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
            ret.exception = e;
        } finally {
            if (bo != null) {
                try {
                    bo.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

        return ret;
    }

    /**
     * 测试用，正式版不要开启
     */
    @Override
    public void onUpload(int errorCode, IWaLogHelper waLogHelper) {
        if (gIsTestModel) {
            byte[] logBuf = null;
            String logName = null;
            if (errorCode == WaErrorCode.E_SUCCESS) {
                logName = "wa_upload_" + System.currentTimeMillis() + ".log";
                logBuf = waLogHelper.getLogBuf();
            } else {
                logName = "wa_upload_fail_" + System.currentTimeMillis() + ".log";
                logBuf = new String("errorCode = " + errorCode).getBytes();
            }

            Log.d("gzm_wa_WaNet", "write uploaded log: " + "/sdcard/wa/" + logName);

            String testPath = "/sdcard/wa";
            new File(testPath).mkdirs();

            File file = new File(testPath + "/" + logName);

            FileOutputStream fileOs = null;
            try {
                fileOs = new FileOutputStream(file);
                fileOs.write(logBuf);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileOs != null) {
                    try {
                        fileOs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fileOs = null;
                }
            }
        }
    }

    private static final boolean gIsTestModel;
    static {
        File file = new File("/sdcard/data/ServerAddr.ini");

        if (!file.exists() || !file.canRead()) {
            gIsTestModel = false;
        } else {
            gIsTestModel = true;
        }
    }

    @Override
    public String getPublicHead() {

        return null;
    }

    @Override
    public String getSavedDir() {

        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/.xpread";
    }
}
