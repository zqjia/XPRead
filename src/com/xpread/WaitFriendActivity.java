
package com.xpread;

import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;
import com.uc.base.wa.WaEntry;
import com.xpread.control.Controller;
import com.xpread.control.Controller.NetworkStateChangeListener;
import com.xpread.control.WifiAdmin;
import com.xpread.control.WifiApAdmin;
import com.xpread.util.Const;
import com.xpread.util.LaboratoryData;
import com.xpread.util.LogUtil;
import com.xpread.util.Utils;
import com.xpread.wa.WaKeys;
import com.xpread.widget.RoundImageView;

public class WaitFriendActivity extends BaseActivity {

    private static final String TAG = WaitFriendActivity.class.getSimpleName();

    private Controller mController;

    private WifiAdmin mWifiAdmin;
    private WifiApAdmin mWifiApAdmin;

    private ImageView mBackView;

    private RoundImageView mUserIcon;

    public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;
    
    private static final int AP_ENABLED_MSG = 1;
    private static final int AP_ENABLED_FAIL_MSG = 3;

    private static final int TIME_OUT = 10;

    private TextView mWaitHintBegin;
    private TextView mWaitHintReceive;

    private ImageView mCircleBlue1;
    private ImageView mCircleBlue2;
    private ImageView mCircleBlue3;

    private ImageView mCancelWait;

    private AnimationSet mCircleAnimationSet;

    private static final int ANIMATION_START = 2;

    private ImageView mRadar;
    private ObjectAnimator mRadarAnimator;

    private WaitWifiApThread mWaitWifiApThread;

    private NetworkStateChangeListener mNetworkStateChangeListener = new NetworkStateChangeListener() {

        @Override
        public void stateChangeListener(int state) {
            Log.e("mController", "" + mController.hashCode());
            if (state == Const.REFRESH_ESTIBALE) {

                Intent intent = new Intent(WaitFriendActivity.this, RecordsActivity.class);
                startActivity(intent);
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_WAIT_SELECT_SUCESS);
                finish();
            } else if (state == Const.REFRESH_DISCONNECTION) {
                Intent intent = new Intent(WaitFriendActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    };

    private Handler mHandler = new Handler() {

        int count = 0;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case AP_ENABLED_MSG:
                    if (LogUtil.isLog) {
                        Log.d(TAG, "wifi ap is enabled");
                    }
                    // TODO 开启wifi热点结束计时
                    // ----------------------------------------------
                    LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_WIFI_AP_ESTABLISH_SUCCESS);
                    LaboratoryData.gWifiAPEstablishEndSuccessTime = System.currentTimeMillis();
                    // ------------------------------------------------------------------
                    YoYo.with(Techniques.TakingOff).duration(1000).playOn(mWaitHintBegin);
                    mWaitHintReceive.setVisibility(View.VISIBLE);
                    YoYo.with(Techniques.DropOut).duration(1000)
                            .withListener(new Animator.AnimatorListener() {

                                @Override
                                public void onAnimationStart(Animator arg0) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator arg0) {

                                }

                                @Override
                                public void onAnimationEnd(Animator arg0) {
                                    // YoYo.with(Techniques.Swing).duration(1000).playOn(mWaitHintReceive);
                                }

                                @Override
                                public void onAnimationCancel(Animator arg0) {

                                }
                            }).playOn(mWaitHintReceive);
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_WAIT_CREATE_SUCESS);
                    break;

                case AP_ENABLED_FAIL_MSG: {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "set up ap fail");
                    }

                    Toast.makeText(WaitFriendActivity.this,
                            R.string.exception_connect_wifi_ap_establish, Toast.LENGTH_SHORT)
                            .show();
                    // 实验室数据
                    LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_WIFI_AP_ESTABLISH_FAILURE);
                    LaboratoryData.gWifiAPEstablishBeginTime = 0L;
                    // --------------------------------------
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_WAIT_CREATE_FAILURE);
                    break;
                }
                case TIME_OUT:
                    // 实验室数据
                    LaboratoryData
                            .addOne(LaboratoryData.KEY_XPREAD_DATA_WIFI_AP_WAIT_WIFI_ESTABLISH_TIME_OUT);
                    LaboratoryData.gWifiAPEstablishBeginTime = 0L;
                    // --------------------------------------
                    mWifiApAdmin.setWifiApEnabled(mWifiApAdmin.getWifiApConfiguration(), false);
                    mController.resumeToDefault();
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_WAIT_TIMEOUT);
                    finish();
                    break;

                case ANIMATION_START:
                    switch (count % 4) {
                        case 1:
                            mCircleBlue1.startAnimation(mCircleAnimationSet);
                            count = 1;
                            break;
                        case 2:
                            mCircleBlue2.startAnimation(mCircleAnimationSet);
                            break;
                        case 3:
                            mCircleBlue3.startAnimation(mCircleAnimationSet);
                            break;
                        default:
                            break;

                    }
                    count++;
                    break;

                default:
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.wait_friend);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        this.mBackView = (ImageView)this.findViewById(R.id.back);
        this.mBackView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                mController.resumeToDefault();
                finish();
            }
        });

        mCancelWait = (ImageView)findViewById(R.id.wait_cancel);
        mCancelWait.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                mController.resumeToDefault();
                finish();
            }
        });

        mUserIcon = (RoundImageView)findViewById(R.id.user_icon);
        mUserIcon.setImageResource(Utils.photos[Utils.getOwerIcon(this)]);

        // 开启服务器服务
        mController = Controller.getInstance(this);
        mController.startService();

        /*
         * add by zqjia to be receiver
         */
        /*-----------------begin-----------------------*/
        mController.toBeReceiver();
        /*-----------------end-----------------------*/

        mWifiAdmin = this.mController.getWifiAdmin();
        mWifiApAdmin = this.mController.getWifiApAdmin();

        this.mRadar = (ImageView)this.findViewById(R.id.radar);

        this.mCircleBlue1 = (ImageView)this.findViewById(R.id.circle_bule1);
        this.mCircleBlue2 = (ImageView)this.findViewById(R.id.circle_blue2);
        this.mCircleBlue3 = (ImageView)this.findViewById(R.id.circle_blue3);

        this.mWaitHintBegin = (TextView)this.findViewById(R.id.wait_hint_begin);
        this.mWaitHintReceive = (TextView)this.findViewById(R.id.wait_hint_receive);

        initAnimation();

    }

    private void initAnimation() {

        Animation alphaAnimation = new AlphaAnimation(1f, 0f);
        alphaAnimation.setDuration(400);
        alphaAnimation.setRepeatCount(0);

        Animation scaleAnimation = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(400);
        scaleAnimation.setRepeatCount(0);

        this.mCircleAnimationSet = new AnimationSet(true);
        this.mCircleAnimationSet.addAnimation(alphaAnimation);
        this.mCircleAnimationSet.addAnimation(scaleAnimation);

        // radar animator
        this.mRadarAnimator = ObjectAnimator.ofFloat(this.mRadar, "rotation", 0f, -360f);
        this.mRadar.setPivotX(dip2px(571 / (float)2));
        this.mRadar.setPivotY(dip2px(154.5f));
        this.mRadarAnimator.setDuration(4000);
        this.mRadarAnimator.setInterpolator(new LinearInterpolator());
        this.mRadarAnimator.setRepeatCount(-1);
        this.mRadarAnimator.addUpdateListener(new AnimatorUpdateListener() {

            boolean isBegin = false, isCircle1Start = false, isCircle2Start = false,
                    isCircle3Start = false;

            @Override
            public void onAnimationUpdate(ValueAnimator arg0) {
                int count = (int)Math.floor((-mRadar.getRotation()) / 90);
                switch (count) {
                    case 0:
                        if (!isBegin) {
                            isBegin = true;
                            isCircle1Start = false;
                            isCircle2Start = false;
                            isCircle3Start = false;
                        }
                        break;

                    case 1:
                        if (!isCircle1Start) {
                            mCircleBlue1.startAnimation(mCircleAnimationSet);
                            isCircle1Start = true;
                        }

                        break;
                    case 2:
                        if (!isCircle2Start) {
                            mCircleBlue2.startAnimation(mCircleAnimationSet);
                            isCircle2Start = true;
                        }

                        break;
                    case 3:
                        if (!isCircle3Start) {
                            mCircleBlue3.startAnimation(mCircleAnimationSet);
                            isCircle3Start = true;
                            isBegin = false;
                        }
                        break;
                    default:
                        break;
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mController.setNetworkStateChangeListener(mNetworkStateChangeListener);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus == true) {

            if (!this.mRadarAnimator.isStarted()) {
                this.mRadarAnimator.start();
            }

            if (this.mWifiApAdmin.getWifiApState() != WIFI_AP_STATE_ENABLED) {

                if (this.mWaitHintBegin.getVisibility() == View.GONE) {
                    this.mWaitHintBegin.setVisibility(View.VISIBLE);
                }

                if (this.mWaitHintReceive.getVisibility() == View.VISIBLE) {
                    this.mWaitHintReceive.setVisibility(View.GONE);
                }

                YoYo.with(Techniques.BounceInLeft).duration(1000).playOn(this.mWaitHintBegin);
            } else {
                if (this.mWaitHintBegin.getVisibility() == View.VISIBLE) {
                    this.mWaitHintBegin.setVisibility(View.GONE);
                }

                if (this.mWaitHintReceive.getVisibility() == View.GONE) {
                    this.mWaitHintReceive.setVisibility(View.VISIBLE);
                }

                YoYo.with(Techniques.DropOut).duration(1000).playOn(this.mWaitHintReceive);
            }
        } else {
            if (this.mRadarAnimator != null && this.mRadarAnimator.isRunning()) {
                this.mRadarAnimator.cancel();
            }
            mHandler.removeMessages(TIME_OUT);
            
            if (this.mCircleAnimationSet != null && this.mCircleAnimationSet.hasStarted()) {
                this.mCircleAnimationSet.cancel();
            }
            
        }
    }

    private void setupWifiAp(String userName) {
        if (this.mWifiApAdmin.getWifiApState() == WIFI_AP_STATE_ENABLING
                || this.mWifiApAdmin.getWifiApState() == WIFI_AP_STATE_ENABLED) {
            WifiConfiguration wcg = this.mWifiApAdmin.getWifiApConfiguration();
            if (!wcg.SSID.startsWith("xpread_")) {
                this.mWifiApAdmin.setWifiApEnabled(wcg, false);
            }
        } else {
            // speed up the ap build
            this.mWifiAdmin.closeWifi();
        }

        String deviceId = Utils.getDeviceId(this);
        WifiConfiguration xpreadWcg = this.mWifiApAdmin.buildConfiguration(userName, deviceId);
        this.mWifiApAdmin.setWifiApConfiguration(xpreadWcg);
        this.mWifiApAdmin.setWifiApEnabled(xpreadWcg, true);

        mHandler.removeMessages(TIME_OUT);
        mHandler.sendEmptyMessageDelayed(TIME_OUT, 3 * 60 * 1000);
    }

    @Override
    protected void onStop() {
//        Log.e("***********WaitActivity************", "onResume ----- " + mController.isConnected());

        super.onStop();
    }

    public int dip2px(float dipValue) {
        final float scale = this.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    @Override
    public void onBackPressed() {
        mController.resumeToDefault();
        WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_WAIT_EXIT);
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
//        Log.e("***********WaitActivity************", "onResume ----- " + mController.isConnected());
        super.onResume();

        if (this.mWifiApAdmin.getWifiApState() != WIFI_AP_STATE_ENABLED) {

            final String userName = Utils.getOwnerName(this);
            // 实验室数据
            LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_WIFI_AP_ESTABLISH_TOTAL_COUNT);
            LaboratoryData.gWifiAPEstablishBeginTime = System.currentTimeMillis();
            // ------------------------------------
            setupWifiAp(userName);

            if (this.mWaitWifiApThread == null) {
                this.mWaitWifiApThread = new WaitWifiApThread();
                this.mWaitWifiApThread.start();
            }
        }
    }

    @Override
    protected void onPause() {
        if (this.mWaitWifiApThread != null) {
            this.mWaitWifiApThread.interrupt();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        Controller.getInstance(WaitFriendActivity.this).unRegisterNetworkStateChangeListener(
                mNetworkStateChangeListener);

        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private class WaitWifiApThread extends Thread {

        @Override
        public void run() {
            super.run();
            int count = 0;
            int tryCount = 0;

            while (!this.isInterrupted() && mWifiApAdmin.getWifiApState() != WIFI_AP_STATE_ENABLED) {
                if (LogUtil.isLog) {
                    Log.d(TAG, "wait for ap setup");
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    interrupt();
                }
                count++;

                if (count >= 20) {
                    tryCount += 1;
                    if (tryCount > 3) {
                        mHandler.sendEmptyMessage(AP_ENABLED_FAIL_MSG);
                        return;
                    }

                    if (mWifiApAdmin.getWifiApState() != WIFI_AP_STATE_ENABLED) {
                        if (LogUtil.isLog) {
                            Log.e(TAG, "setup ap fail, try again");
                        }
                    }
                    setupWifiAp(Utils.getOwnerName(WaitFriendActivity.this));
                    count = 0;
                }
            }
            mHandler.sendEmptyMessage(AP_ENABLED_MSG);
        }

    }

}
