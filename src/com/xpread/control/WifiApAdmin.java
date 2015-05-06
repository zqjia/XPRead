
package com.xpread.control;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.xpread.util.LogUtil;
import com.xpread.util.Utils;

public class WifiApAdmin {

    private static final String TAG = "WifiApAdmin";

    public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;
    
    private static final String METHOD_GET_WIFI_AP_STATE = "getWifiApState";
    private static final String METHOD_SET_WIFI_AP_ENABLED = "setWifiApEnabled";
    private static final String METHOD_GET_WIFI_AP_CONFIG = "getWifiApConfiguration";
    private static final String METHOD_IS_WIFI_AP_ENABLED = "isWifiApEnabled";

    private static final Map<String, Method> mMethodMap = new HashMap<String, Method>();

    private static Boolean mIsSupport = null;

    private static boolean mIsHtc;

    // hardcode 密码
    private static final String PASSWORD = "123456789";

    private WifiManager mWifiManager;

    // private String mSsid = null;

    private Context mContext;

    public WifiApAdmin(Context context) {

        if (!isSupport()) {
            throw new RuntimeException("Unsupport Ap!");
        }

        this.mContext = context;
        this.mWifiManager = (WifiManager)this.mContext.getSystemService(Context.WIFI_SERVICE);
    }

    public synchronized static final boolean isSupport() {
        if (mIsSupport != null) {
            return mIsSupport;
        }

        boolean result = Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO;

        if (result) {
            try {
                Field field = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
                mIsHtc = field != null;
            } catch (Exception e) {
                mIsHtc = false;
                if (LogUtil.isLog) {
                    Log.d(TAG, "get declared field mWifiApProfile error, not htc");

                }

            }
        }

        if (result) {
            try {
                String name = METHOD_GET_WIFI_AP_STATE;
                Method method = WifiManager.class.getMethod(name);
                mMethodMap.put(name, method);
                result = method != null;
            } catch (SecurityException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method getWifiApState error: SecurityException");
                }
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method getWifiApState error: NoSuchMethodException");
                }
                return false;
            }
        }

        if (result) {
            try {
                String name = METHOD_SET_WIFI_AP_ENABLED;
                Method method = WifiManager.class.getMethod(name, WifiConfiguration.class,
                        boolean.class);
                mMethodMap.put(name, method);
                result = method != null;
            } catch (SecurityException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method setWifiApState error: SecurityException");
                }
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method setWifiApState error: NoSuchMethodException");
                }
                return false;
            }
        }

        if (result) {
            try {
                String name = METHOD_GET_WIFI_AP_CONFIG;
                Method method = WifiManager.class.getMethod(name);
                mMethodMap.put(name, method);
                result = method != null;
            } catch (SecurityException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method getWifiApConfig error: SecurityException");
                }
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method getWifiApConfig error: NoSuchMethodException");
                }
                return false;
            }
        }

        if (result) {
            try {
                String name = getSetWifiApConfigName();
                Method method = WifiManager.class.getMethod(name, WifiConfiguration.class);
                mMethodMap.put(name, method);
                result = method != null;
            } catch (SecurityException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method getSetWifiApConfig error: SecurityException");
                }
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method getSetWifiApConfig error: NoSuchMethodException");
                }
                return false;
            }
        }

        if (result) {
            try {
                String name = METHOD_IS_WIFI_AP_ENABLED;
                Method method = WifiManager.class.getMethod(name);
                mMethodMap.put(name, method);
                result = method != null;
            } catch (SecurityException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method isWifiApEnabled error: SecurityException");
                }
                return false;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                if (LogUtil.isLog) {
                    Log.d(TAG, "get method isWifiApEnabled error: NoSuchMethodException");
                }
                return false;
            }
        }

        mIsSupport = result;
        return mIsSupport;
    }

    public int getWifiApState() {
        try {
            Method method = mMethodMap.get(METHOD_GET_WIFI_AP_STATE);
            return (Integer)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
            if (LogUtil.isLog) {
                Log.e(TAG, "get wifi state exception");
            }
        }
        return -1; // wifi state unknown
    }

    private WifiConfiguration getHtcWifiApConfiguration(WifiConfiguration config) {
        WifiConfiguration htcWifiConfig = config;
        try {
            Object mWifiApProfileValue = getFieldValue(config, "mWifiApProfile");

            if (mWifiApProfileValue != null) {
                htcWifiConfig.SSID = (String)getFieldValue(mWifiApProfileValue, "SSID");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (LogUtil.isLog) {
                Log.e(TAG, "getHtcWifiApConfiguration: get field value error");
            }
        }
        return htcWifiConfig;
    }

    public WifiConfiguration getWifiApConfiguration() {
        WifiConfiguration configuration = null;
        try {
            Method method = mMethodMap.get(METHOD_GET_WIFI_AP_CONFIG);
            configuration = (WifiConfiguration)method.invoke(mWifiManager);
            if (isHtc()) {
                configuration = getHtcWifiApConfiguration(configuration);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (LogUtil.isLog) {
                Log.e(TAG, "getWifiApConfiguration : get method error");
            }
        }
        return configuration;
    }

    public boolean setWifiApConfiguration(WifiConfiguration netConfig) {
        boolean result = false;
        try {
            if (isHtc()) {
                setupHtcWifiConfiguration(netConfig);
            }

            Method method = mMethodMap.get(getSetWifiApConfigName());
            Class<?>[] params = method.getParameterTypes();
            for (Class<?> clazz : params) {
                Log.i(TAG, "param -> " + clazz.getSimpleName());
            }

            if (isHtc()) {
                int rValue = (Integer)method.invoke(mWifiManager, netConfig);
                result = rValue > 0;
            } else {
                result = (Boolean)method.invoke(mWifiManager, netConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (LogUtil.isLog) {
                Log.e(TAG, "setWifiApConfiguration: exception");
            }
        }
        return result;
    }

    private void setupHtcWifiConfiguration(WifiConfiguration config) {
        try {

            Field fieldApProfile = WifiConfiguration.class.getDeclaredField("mWifiApProfile");

            fieldApProfile.setAccessible(true);

            Object configObject = fieldApProfile.get(config);

            if (configObject != null) {
                Field ssidField = configObject.getClass().getDeclaredField("SSID");
                ssidField.setAccessible(true);
                ssidField.set(configObject, config.SSID);
                ssidField.setAccessible(false);

            }
        } catch (Exception e) {
            e.printStackTrace();
            if (LogUtil.isLog) {
                Log.e(TAG, "setupHtcWifiConfiguration: exception");
            }
        }

    }

    public boolean setWifiApEnabled(WifiConfiguration configuration, boolean enabled) {
        boolean result = false;
        try {
            Method method = mMethodMap.get(METHOD_SET_WIFI_AP_ENABLED);
            result = (Boolean)method.invoke(mWifiManager, configuration, enabled);
        } catch (Exception e) {
            e.printStackTrace();
            if (LogUtil.isLog) {
                Log.e(TAG, "setWifiApEnabled: exception");
            }
        }
        return result;
    }

    public boolean isWifiApEnabled() {
        boolean result = false;
        try {
            Method method = mMethodMap.get(METHOD_IS_WIFI_AP_ENABLED);
            result = (Boolean)method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
            if (LogUtil.isLog) {
                Log.e(TAG, "isWifiApEnabled: exception");
            }
        }
        return result;
    }

    private Object getFieldValue(Object object, String propertyName) throws IllegalAccessException,
            NoSuchFieldException {
        Assert.assertNotNull(object);

        Field field = object.getClass().getDeclaredField(propertyName);
        field.setAccessible(true);

        return field.get(object);
    }

    public WifiConfiguration buildConfiguration(String userName, String deviceId) {
        WifiConfiguration config = new WifiConfiguration();

        if (userName == null) {
            userName = Utils.getOwnerName(mContext);
        }

        String ssid = "xpread_" + userName + "_" + deviceId;

        config.SSID = ssid;
        config.preSharedKey = PASSWORD;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        // 设置WPA2/IEEE 802.11i
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        // 设置WPA/IEEE 802.11i/D3.0
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        // WPA pre-shared key (requires preSharedKey to be specified).
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        // AES in Counter mode with CBC-MAC [RFC 3610, IEEE 802.11i/D7.0]
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        // Temporal Key Integrity Protocol [IEEE 802.11i/D7.0]
        config.allowedGroupCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        // AES in Counter mode with CBC-MAC [RFC 3610, IEEE 802.11i/D7.0]
        config.allowedPairwiseCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        // AES in Counter mode with CBC-MAC [RFC 3610, IEEE 802.11i/D7.0]
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        return config;
    }

    private static String getSetWifiApConfigName() {
        return mIsHtc ? "setWifiApConfig" : "setWifiApConfiguration";
    }

    private static boolean isHtc() {
        return mIsHtc;
    }

}
