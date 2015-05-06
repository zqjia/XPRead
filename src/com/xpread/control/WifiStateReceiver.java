
package com.xpread.control;

import com.xpread.util.LogUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

public class WifiStateReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiStateReceiver";
    
    public interface WifiStateListener {        
        void onConnected(String ssid);        
        void onDisconnected();
    }
    
    private WifiStateListener mListener;
    
    public WifiStateReceiver(WifiStateListener listener) {
        this.mListener = listener;
    }
    
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 
                                WifiManager.WIFI_STATE_UNKNOWN);
            
            if(wifiState == WifiManager.WIFI_STATE_ENABLED) {
                if(LogUtil.isLog) {
                    Log.d(TAG, "wifi is enabled");
                }
            } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                if(LogUtil.isLog) {
                    Log.d(TAG, "wifi is disabled");
                }
            }
        } else if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

            String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID); 

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(networkInfo != null) {
                
                if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI && 
                        networkInfo.getDetailedState().equals(NetworkInfo.DetailedState.CONNECTED)) {
                    
                    String ssid = null;

                    if (Build.VERSION.SDK_INT >= 14) {

                        WifiInfo wifiInfo = intent.getParcelableExtra("wifiInfo");
                        if (wifiInfo != null) {
                            ssid = wifiInfo.getSSID();
                        }
                    }
   
                    if(LogUtil.isLog) {
                        Log.d(TAG, "Connected to wifi: (" + bssid + ")" + ssid);
                    }
                    
                    if (this.mListener != null) {
                        this.mListener.onConnected(ssid);
                    }
                    
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && 
                        networkInfo.getDetailedState().equals(NetworkInfo.DetailedState.DISCONNECTED)) {
                    if(LogUtil.isLog) {
                        
                        Log.d(TAG, "Disconnected from wifi: (" + bssid + ")");
                    }
                        
                    if (this.mListener != null) {
                        this.mListener.onDisconnected();        
                    }
                }  
                
            }
            
              
        }
          
    }
}

