
package com.xpread.control;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.xpread.util.LogUtil;

public class WifiAdmin {

    private static final String TAG = "WifiAdmin";

    public interface ScanResultListener {
        public void handleScanResult(List<ScanResult> list);
    }

    private Context mContext;

    private WifiManager mWifiManager;

    private static final int TYPE_NOPASSWORD = 1;

    private static final int TYPE_WEP = 2;

    private static final int TYPE_WPA = 3;

    private ScanResultListener mScanResultListener;

    private ScanResultReceiver mReceiver;

    public WifiAdmin(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    public boolean openWifi() {
        if (!this.mWifiManager.isWifiEnabled()) {
            this.mWifiManager.setWifiEnabled(true);
            return true;
        }
        return false;
    }

    public boolean isWifiOpen() {
        return mWifiManager.isWifiEnabled();
    }

    public boolean closeWifi() {
        if (this.mWifiManager.isWifiEnabled()) {
            this.mWifiManager.setWifiEnabled(false);
            return true;
        }
        return false;
    }

    public int getWifiState() {
        return this.mWifiManager.getWifiState();
    }

    public void searchFriend() {
        this.mWifiManager.startScan();
    }

    public boolean connectFriend(String ssid, String password, int type) {
        if (ssid == null || password == null || ssid.equals("")) {
            if (LogUtil.isLog) {
                Log.e(TAG, "addNetwork :null point error");
            }
            return false;
        }

        if (!(type == TYPE_NOPASSWORD || type == TYPE_WEP || type == TYPE_WPA)) {
            if (LogUtil.isLog) {
                Log.e(TAG, "addNetwork: type is unknow " + type);
            }
            return false;
        }

        return connectFriend(createWifiInfo(ssid, password, type));
    }

    private boolean connectFriend(WifiConfiguration wcg) {

        int wcgID = this.mWifiManager.addNetwork(wcg);
        boolean result = this.mWifiManager.enableNetwork(wcgID, true);

        new Thread(new Runnable() {

            public void run() {
                if (LogUtil.isLog) {
                    Log.e(TAG, "enable other network");
                }

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mWifiManager.enableNetwork(0, false);
                }
            }

        }).start();

        return result;
    }

    private WifiConfiguration createWifiInfo(String SSID, String password, int type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        if (android.os.Build.VERSION.SDK_INT >= 21)
            config.SSID = "" + SSID.replace("\"", "") + "";
        else
            config.SSID = "\"" + SSID.replace("\"", "") + "\"";
        // config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = null;
        if ((tempConfig = isExist(SSID)) != null) {
            this.mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (type == TYPE_NOPASSWORD) {
            // have no password
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == TYPE_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == TYPE_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        return config;
    }

    private WifiConfiguration isExist(String SSID) {
        List<WifiConfiguration> existConfigs = this.mWifiManager.getConfiguredNetworks();
        if (existConfigs == null) {
            return null;
        }
        for (WifiConfiguration existConfig : existConfigs) {
            if (existConfig.SSID.equals("\"" + SSID + "\"") || existConfig.SSID.equals(SSID)) {
                return existConfig;
            }
        }
        return null;
    }

    public void removeWifiConfiguration(String ssid) {
        WifiConfiguration wcg = isExist(ssid);
        if (wcg != null) {
            this.mWifiManager.removeNetwork(wcg.networkId);
            this.mWifiManager.saveConfiguration();

            reconnect();
        }
    }

    public void disableNetWork(String SSID) {
        WifiConfiguration wc = isExist(SSID);
        if (wc != null) {
            mWifiManager.disableNetwork(wc.networkId);
//            Log.e("@@@@@@@@@@@@@@@@@@@@@@@@@@@", "networkId " + wc.networkId);
        }
    }

    public String getConnectedSsid() {
        return this.mWifiManager.getConnectionInfo().getSSID();
    }

    public boolean connectedHotpot(String ssid) {
        WifiConfiguration wcg = isExist(ssid);

        if (wcg != null) {
            return connectFriend(wcg);
        }

        return false;
    }

    public void disconnectCurrentWifi() {
        this.mWifiManager.disconnect();
    }

    public void reconnect() {
        this.mWifiManager.reconnect();
    }

    public void setScanResultListener(ScanResultListener listener) {
        this.mScanResultListener = listener;
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.mReceiver = new ScanResultReceiver();
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    //add by zqjia
    //for the unable to pause SearchFriendActivity caused by the receiver not regist exception
    public ScanResultReceiver getScanReceiver() {
        return this.mReceiver;
    }
    
    public void unRegisterScanResultListener() {
        this.mScanResultListener = null;
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private class ScanResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<ScanResult> list = (ArrayList<ScanResult>)mWifiManager.getScanResults();
            ArrayList<ScanResult> resultList = new ArrayList<ScanResult>();
            if (list != null && !list.isEmpty()) {

                for (ScanResult item : list) {
                    if (LogUtil.isLog) {
                        Log.d(TAG, "ssid is " + item.SSID);
                    }

                    if (item.SSID.startsWith("xpread_")) {
                        resultList.add(item);
                    }
                }
                mScanResultListener.handleScanResult(resultList);
            }
        }
    }

}
