
package com.xpread.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
    
    private static final String TAG = "NetworkUtil";
    
    private NetworkUtil() {
        throw new UnsupportedOperationException("can't be instance");
    }
    
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            
            if(info != null && info.isConnected()) {
                if(info.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }
    
}
