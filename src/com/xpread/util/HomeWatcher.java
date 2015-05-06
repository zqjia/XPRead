package com.xpread.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class HomeWatcher {
    private static final String TAG = "HomeWatcher";
    
    private Context mContext;
    private IntentFilter mFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
    private OnHomePressedListener mListener;
    private InnerReceiver mReceiver = new InnerReceiver();
    
    public interface OnHomePressedListener {
        public void onHomePressed();
        
        public void onHomeLongPressed();
    }
    
    public HomeWatcher(Context context) {
        this.mContext = context;        
    }
    
    public void setOnHomePressedListener(OnHomePressedListener listener) {
        this.mListener = listener;
    }
    
    public void startWatch() {
        
        if(this.mReceiver == null) {
            this.mReceiver = new InnerReceiver();
        }
        
        if(this.mFilter == null) {
            this.mFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS); 
        }
            
        this.mContext.registerReceiver(mReceiver, mFilter);
    }
    
    public void stopWatch() {
        if (this.mReceiver != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }
    
    private class InnerReceiver extends BroadcastReceiver {
        
        final String SYSTEM_DIALOG_REASON_KEY = "reason"; 
        final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps"; 
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"; 
   
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason != null) {
                    if(LogUtil.isLog) {
                        Log.e(TAG, "action： " + action + ", and reason: " + reason);
                    }
                    if (mListener != null) {
                        if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                            mListener.onHomePressed();
                        } else if (reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
                            mListener.onHomeLongPressed();
                        }
                    }
                }
            }
        }
        
    }
    
}
