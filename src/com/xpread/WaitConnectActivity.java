package com.xpread;

import com.uc.base.wa.WaEntry;
import com.xpread.control.Controller;
import com.xpread.control.Controller.NetworkStateChangeListener;
import com.xpread.util.Const;
import com.xpread.wa.WaKeys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class WaitConnectActivity extends Activity {
    
    private Controller mController = Controller.getInstance(this);
    
    private NetworkStateChangeListener mNetworkStateChangeListener 
        = new NetworkStateChangeListener() {

        @Override
        public void stateChangeListener(int state) {
            if (state == Const.REFRESH_ESTIBALE) {
                Intent intent = new Intent(WaitConnectActivity.this, RecordsActivity.class);
                startActivity(intent);
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_WAIT_SELECT_SUCESS);
                finish();
            } else if (state == Const.REFRESH_DISCONNECTION) {
                Intent intent = new Intent(WaitConnectActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_waitconnect);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mController.setNetworkStateChangeListener(mNetworkStateChangeListener);
    }

    @Override
    protected void onStop() {
        this.mController.unRegisterNetworkStateChangeListener(mNetworkStateChangeListener);
        super.onStop();
    }


    
    
}
