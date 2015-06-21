
package com.xpread;

import android.app.Activity;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.xpread.control.Controller;
import com.xpread.control.Controller.NetworkStateChangeListener;
import com.xpread.control.MobileNetworkManager;
import com.xpread.control.WifiAdmin;
import com.xpread.control.WifiApAdmin;
import com.xpread.provider.UserInfo;
import com.xpread.util.Const;
import com.xpread.util.HomeWatcher;
import com.xpread.util.HomeWatcher.OnHomePressedListener;
import com.xpread.util.LogUtil;

public class BaseActivity extends Activity {

    private static final String TAG = "BaseActivity";

    private HomeWatcher mHomeWatcher;
    private Controller mController;

    public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;

    NetworkStateChangeListener mNscl = new NetworkStateChangeListener() {
        
        @Override
        public void stateChangeListener(int state) {
            switch (state) {
                case Const.REFRESH_USER_INFO:
                    refreshUserInfo();
                    break;
                case Const.REFRESH_ESTIBALE:
                    refreshEstablish();
                    break;
                case Const.REFRESH_DISCONNECTION:
                    //断开后保证移动数据是打开的
                    if (!MobileNetworkManager.getMobileNetworkState()) {
                        MobileNetworkManager.setMobileNetworkState(true);
                    }
                    refreshDisconnected();
                    break;
                default:
                    break;
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // watch the home key
        if (this.mController == null) {
            this.mController = Controller.getInstance(XApplication.getContext());
        }

        mController.setNetworkStateChangeListener(mNscl);
        
        if (mHomeWatcher == null) {
            mHomeWatcher = new HomeWatcher(this);
        }

        mHomeWatcher.setOnHomePressedListener(new OnHomePressedListener() {

            @Override
            public void onHomePressed() {
                /*
                 * resore the wifi state before open the app
                 */
                if (LogUtil.isLog) {
                    Log.e(TAG, "home key listener is capture");
                }

                if (mController.getRole() != -1) {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "transfer file is doing, don't change wifi state now");
                    }
                } else {
                    final UserInfo userInfo = mController.getUerInfo();
                    final WifiAdmin wifiAdmin = mController.getWifiAdmin();
                    int isWifiConnectedBefore = userInfo.getIsWifiConnectedBefore();

                    if (isWifiConnectedBefore == 1) {
                        if (openWifi()) {
                            if (LogUtil.isLog) {
                                Log.e(TAG, "onHomePress, open wifi success");
                            }
                        } else {
                            if (LogUtil.isLog) {
                                Log.e(TAG, "onHomePress, open wifi fail");
                            }
                        }
                        wifiAdmin.reconnect();

                    } else {
                        wifiAdmin.closeWifi();
                    }
                }
            }

            @Override
            public void onHomeLongPressed() {
                onHomePressed();
            }
        });
        mHomeWatcher.startWatch();
    }

    @Override
    protected void onPause() {
        mController.unRegisterNetworkStateChangeListener(mNscl);
        
        if (mHomeWatcher != null) {
            this.mHomeWatcher.setOnHomePressedListener(null);
            this.mHomeWatcher.stopWatch();
        }
        super.onPause();
    }

    private boolean openWifi() {
        WifiAdmin wifiAdmin = mController.getWifiAdmin();
        WifiApAdmin wifiApAdmin = mController.getWifiApAdmin();

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
    

    /**
     * 在user info改变时调用
     * */
    protected void refreshUserInfo() {
        
    }
    
    /**
     * 在连接建立时调用
     * */
    protected void refreshEstablish() {
        
    }
    
    /**
     * 在连接断开时调用
     * */
    protected void refreshDisconnected() {
        
    }
    

}
