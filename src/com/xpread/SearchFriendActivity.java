/**
 * <p>Title: xpread</p>
 *
 * <p>Description: </p>
 * 好友搜索界面
 * <p>Copyright: Copyright (c) 2014</p>
 *
 * <p>Company: ucweb.com</p>
 *
 * @author jiazq@ucweb.com
 * @version 1.0
 */

package com.xpread;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
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
import com.xpread.control.WifiAdmin.ScanResultListener;
import com.xpread.control.WifiApAdmin;
import com.xpread.provider.History;
import com.xpread.util.BitmapUtil;
import com.xpread.util.Const;
import com.xpread.util.LaboratoryData;
import com.xpread.util.LogUtil;
import com.xpread.util.Utils;
import com.xpread.wa.WaKeys;
import com.xpread.widget.RobotoTextView;
import com.xpread.widget.RoundImageView;

public class SearchFriendActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "WifiSearchFriendActivity";

    public static final int WIFI_AP_STATE_DISABLING = 10;

    public static final int WIFI_AP_STATE_DISABLED = 11;

    public static final int WIFI_AP_STATE_ENABLING = 12;

    public static final int WIFI_AP_STATE_ENABLED = 13;

    public static final int WIFI_AP_STATE_FAILED = 14;

    private static final int ANIMATION_START = 2;

    private static final int SCAN_TIMEOUT = 3;

    private ImageView mFriendOneIcon;

    private ImageView mFriendTwoIcon;

    private ImageView mFriendThreeIcon;

    private ImageView mFriendFourIcon;

    private ImageView mFriendFiveIcon;

    private ImageView mFriendSixIcon;

    private RobotoTextView mFriendOneName;

    private RobotoTextView mFriendTwoName;

    private RobotoTextView mFriendThreeName;

    private RobotoTextView mFriendFourName;

    private RobotoTextView mFriendFiveName;

    private RobotoTextView mFriendSixName;

    private ImageView mBackView;

    private Controller mController;

    private WifiAdmin mWifiAdmin;

    private WifiApAdmin mWifiApAdmin;

    private ArrayList<ScanResult> mFriendsList;

    private final int FRIENDLIST_MSG = 1;

    private final String FRIENDLIST_KEY = "friend_list";

    private Timer mTimer;

    private ScanFriendTask mTimerTask;

    private static final int TYPE_WPA = 3;

    private static final String PASSWORD = "123456789";

    private HashMap<ImageView, FriendEntity> mFriendMap;

    private AnimationSet mCircleAnimationSet;

    private ImageView mCircleBlue1;

    private ImageView mCircleBlue2;

    private ImageView mCircleBlue3;

    private RoundImageView mUserIcon;

    private AnimationSet mFriendAnimationSet;

    private ImageView mSearchCancel;

    private ImageView mRadar;

    private ObjectAnimator mRadarAnimator;

    private static final int FRIEND_BUTTON_STATE_HAVE_CLICK = 1;

    private static final int FRIEND_BUTTON_STATE_HAVENOT_CLICK = 0;

    private int mFriendButtonClickCount = 0;

    private final String INTENT_TYPE = "type";

    private final int WAIT_CONNECTTED = 0x0010;

    private final int SEARCH_TIME_OUT = 0x0011;

    private NetworkStateChangeListener mNetworkStateChangeListener = new NetworkStateChangeListener() {

        @Override
        public void stateChangeListener(int state) {
            if (state == Const.REFRESH_ESTIBALE) {

                Intent intent = new Intent(SearchFriendActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }

    };

    private ImageView[] mFriendIconArray = {
            mFriendOneIcon, mFriendTwoIcon, mFriendThreeIcon, mFriendFourIcon, mFriendFiveIcon,
            mFriendSixIcon
    };

    private RobotoTextView[] mFriendNameArray = {
            mFriendOneName, mFriendTwoName, mFriendThreeName, mFriendFourName, mFriendFiveName,
            mFriendSixName
    };

    ScanResultListener mScanResultListener = new ScanResultListener() {

        @Override
        public void handleScanResult(List<ScanResult> list) {
            if (list != null && !list.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putSerializable(FRIENDLIST_KEY, (Serializable)list);
                Message msg = mHandler.obtainMessage();
                msg.what = FRIENDLIST_MSG;
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
        }
    };

    // use weakReference to avoid leak occur
    private Handler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.search_friend);

        initView();
        Controller.getInstance(getApplicationContext()).toBeSender();

        this.mFriendMap = new HashMap<ImageView, FriendEntity>();
        this.mController = Controller.getInstance(this);
        this.mWifiAdmin = this.mController.getWifiAdmin();

        this.mWifiApAdmin = this.mController.getWifiApAdmin();
        Controller.getInstance(getApplicationContext()).setNetworkStateChangeListener(
                mNetworkStateChangeListener);

        // 超时机制,扫描其他用户1分钟后,可选用户仍为空即为超时
        mHandler.sendEmptyMessageDelayed(SCAN_TIMEOUT, 1 * 60 * 1000);
    }

    private void initView() {
        mFriendIconArray[0] = (ImageView)this.findViewById(R.id.friend_one_icon);
        mFriendNameArray[0] = (RobotoTextView)this.findViewById(R.id.friend_one_name);
        mFriendIconArray[1] = (ImageView)this.findViewById(R.id.friend_two_icon);
        mFriendNameArray[1] = (RobotoTextView)this.findViewById(R.id.friend_two_name);
        mFriendIconArray[2] = (ImageView)this.findViewById(R.id.friend_three_icon);
        mFriendNameArray[2] = (RobotoTextView)this.findViewById(R.id.friend_three_name);
        mFriendIconArray[3] = (ImageView)this.findViewById(R.id.friend_four_icon);
        mFriendNameArray[3] = (RobotoTextView)this.findViewById(R.id.friend_four_name);
        mFriendIconArray[4] = (ImageView)this.findViewById(R.id.friend_five_icon);
        mFriendNameArray[4] = (RobotoTextView)this.findViewById(R.id.friend_five_name);
        mFriendIconArray[5] = (ImageView)this.findViewById(R.id.friend_six_icon);
        mFriendNameArray[5] = (RobotoTextView)this.findViewById(R.id.friend_six_name);

        mFriendIconArray[0].setOnClickListener(this);
        mFriendIconArray[1].setOnClickListener(this);
        mFriendIconArray[2].setOnClickListener(this);
        mFriendIconArray[3].setOnClickListener(this);
        mFriendIconArray[4].setOnClickListener(this);
        mFriendIconArray[5].setOnClickListener(this);

        mUserIcon = (RoundImageView)findViewById(R.id.user_icon);
        mUserIcon.setImageResource(Utils.photos[Utils.getOwerIcon(this)]);

        this.mBackView = (ImageView)this.findViewById(R.id.back);
        this.mBackView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mController.resumeToDefault();
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_CONNECT_SCAN_EXIT);
                finish();
            }
        });

        this.mCircleBlue1 = (ImageView)this.findViewById(R.id.circle_bule1);
        this.mCircleBlue2 = (ImageView)this.findViewById(R.id.circle_blue2);
        this.mCircleBlue3 = (ImageView)this.findViewById(R.id.circle_blue3);
        this.mRadar = (ImageView)this.findViewById(R.id.radar);
        initAnimation();

        this.mSearchCancel = (ImageView)this.findViewById(R.id.search_cancel);
        this.mSearchCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mController.resumeToDefault();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (openWifi()) {
            if (mTimerTask == null) {
                mTimerTask = new ScanFriendTask();
                if (this.mTimer == null) {
                    this.mTimer = new Timer();
                    this.mTimer.schedule(mTimerTask, 0, 1500);
                }
            }
        } else {
            if (LogUtil.isLog) {
                Log.e(TAG, "open wifi error");
            }
        }

        this.mWifiAdmin.setScanResultListener(this.mScanResultListener);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (!this.mRadarAnimator.isStarted()) {
                this.mRadarAnimator.start();
            }
        } else {
            if (this.mRadarAnimator != null && this.mRadarAnimator.isRunning()) {
                this.mRadarAnimator.cancel();
            }
        }
    }

    private void initAnimation() {

        Animation alphaAnimation = new AlphaAnimation(1f, 0f);
        alphaAnimation.setDuration(400);
        alphaAnimation.setRepeatCount(0);

        Animation scaleAnimation = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(400);
        scaleAnimation.setRepeatCount(0);

        Animation friendTranslateAnimation = new TranslateAnimation(0, 0, 0, -dip2px(40));
        friendTranslateAnimation.setDuration(500);
        friendTranslateAnimation.setRepeatCount(0);
        friendTranslateAnimation.setFillAfter(false);

        Animation friendAlphaAnimation = new AlphaAnimation(0f, 1f);
        friendAlphaAnimation.setDuration(500);
        friendAlphaAnimation.setRepeatCount(0);

        this.mCircleAnimationSet = new AnimationSet(true);
        this.mCircleAnimationSet.addAnimation(alphaAnimation);
        this.mCircleAnimationSet.addAnimation(scaleAnimation);

        this.mFriendAnimationSet = new AnimationSet(true);
        this.mFriendAnimationSet.addAnimation(friendAlphaAnimation);
        this.mFriendAnimationSet.addAnimation(friendTranslateAnimation);

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
            public void onAnimationUpdate(ValueAnimator animation) {

                int count = (int)Math.floor((-mRadar.getRotation()) / 90);
                switch (count) {
                    case 0:
                        if (!isBegin) {
                            // Log.e(TAG, "begin : rotation is -->" +
                            // mRadar.getRotation());
                            isBegin = true;
                            isCircle1Start = false;
                            isCircle2Start = false;
                            isCircle3Start = false;
                        }
                        break;

                    case 1:
                        if (!isCircle1Start) {
                            // Log.e(TAG, "ciecle 1 start : rotation is -->" +
                            // mRadar.getRotation());
                            mCircleBlue1.startAnimation(mCircleAnimationSet);
                            isCircle1Start = true;
                        }

                        break;
                    case 2:
                        if (!isCircle2Start) {
                            // Log.e(TAG, "ciecle 2 start : rotation is -->" +
                            // mRadar.getRotation());
                            mCircleBlue2.startAnimation(mCircleAnimationSet);
                            isCircle2Start = true;
                        }

                        break;
                    case 3:
                        if (!isCircle3Start) {
                            // Log.e(TAG, "ciecle 3 start : rotation is -->" +
                            // mRadar.getRotation());
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
    public void onClick(View v) {
        FriendEntity friendEntity = mFriendMap.get((ImageView)v);

        if (v.getTag() == null || (Integer)v.getTag() == FRIEND_BUTTON_STATE_HAVENOT_CLICK) {

            v.setTag(FRIEND_BUTTON_STATE_HAVE_CLICK);
            mFriendButtonClickCount = 1;

            String ssid = friendEntity.getSsid();
            String connectedSsid = this.mWifiAdmin.getConnectedSsid();
            WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_CONNECT_SELECT_ONE);

            if (connectedSsid == null || (connectedSsid != null && !connectedSsid.equals(ssid))) {
                // TODO
                // 开始wifi打开计时--------------------------------------------------------------
                LaboratoryData.gWifiEstablishBeginTime = System.currentTimeMillis();
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_WIFI_ESTABLISH_TOTAL_COUNT);
                // --------------------------------------------------------------------------------
                if (connectedSsid != null) {
                    mWifiAdmin.disconnectCurrentWifi();
                }
                // TODO
                // wifi 连接次数 +
                // 1--------------------------------------------------------------
                // --------------------------------------------------------------------------------
                if (mWifiAdmin.connectFriend(ssid, PASSWORD, TYPE_WPA)) {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "connect to friend success");
                    }
                } else {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "connect to friend fail");
                    }
                }
                Toast.makeText(this, getResources().getString(R.string.connect_is_connecting),
                        Toast.LENGTH_LONG).show();

                mController.setTargetInfo(friendEntity.name, friendEntity.getIcon(),
                        friendEntity.deviceId);
                mController.getUerInfo().setConnectedFriendSsid(ssid);

                // FIXME
                /*
                 * add by zqjia jump to MainActivity
                 */
                Intent intent = new Intent();
                intent.putExtra(INTENT_TYPE, WAIT_CONNECTTED);
                intent.setClass(SearchFriendActivity.this, MainActivity.class);
                startActivity(intent);

            } else if (connectedSsid != null && connectedSsid.equals(ssid)) {
                Toast.makeText(SearchFriendActivity.this,
                        getResources().getString(R.string.connect_friend_already),
                        Toast.LENGTH_SHORT).show();
                mController.establishConnection();
            }
        } else {
            if (mFriendButtonClickCount < 5) {
                Toast.makeText(SearchFriendActivity.this,
                        getResources().getString(R.string.click_again_hint), Toast.LENGTH_SHORT)
                        .show();
                mFriendButtonClickCount++;
            }
        }
    }

    private boolean openWifi() {

        int wifiState = this.mWifiAdmin.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED
                || wifiState == WifiManager.WIFI_STATE_ENABLING) {
            if (LogUtil.isLog) {
                Log.d(TAG, "wifi is enbale or enbaling, it's ok");
            }
            return true;
        } else {
            if (this.mWifiApAdmin.getWifiApState() == WIFI_AP_STATE_ENABLING
                    || this.mWifiApAdmin.getWifiApState() == WIFI_AP_STATE_ENABLED) {
                if (LogUtil.isLog) {
                    Log.d(TAG, "wifi ap is enbale , close it");
                }

                WifiConfiguration wcg = mWifiApAdmin.getWifiApConfiguration();
                if (!this.mWifiApAdmin.setWifiApEnabled(wcg, false)) {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "close wifi ap fail");
                        return false;
                    }
                }
            }

            if (!this.mWifiAdmin.openWifi()) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "open wifi fail");
                }
                return false;
            }
            return true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (this.mTimerTask != null) {
            this.mTimerTask.cancel();
            this.mTimerTask = null;
        }

        if (this.mTimer != null) {
            this.mTimer.cancel();
            this.mTimer = null;
        }

        if (this.mRadarAnimator.isRunning()) {
            this.mRadarAnimator.cancel();
        }

        if (this.mWifiAdmin != null && this.mWifiAdmin.getScanReceiver() != null) {
            this.mWifiAdmin.unRegisterScanResultListener();
        }

        for (int i = 0; i < mFriendIconArray.length; ++i) {
            if (mFriendIconArray[i].getTag() != null
                    && (Integer)mFriendIconArray[i].getTag() == FRIEND_BUTTON_STATE_HAVE_CLICK) {
                mFriendIconArray[i].setTag(FRIEND_BUTTON_STATE_HAVENOT_CLICK);
            }
        }

    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        mController.resumeToDefault();
        WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_CONNECT_SCAN_EXIT);
        super.onBackPressed();
    }

    private class ScanFriendTask extends TimerTask {

        @Override
        public void run() {
            /*
             * final ArrayList<ScanResult> list = mWifiAdmin.searchFriend(); if
             * (list != null && list.size() > 0) { Bundle bundle = new Bundle();
             * bundle.putSerializable(FRIENDLIST_KEY, list); Message msg =
             * mHandler.obtainMessage(); msg.what = FRIENDLIST_MSG;
             * msg.setData(bundle); mHandler.sendMessage(msg); }
             */
            mWifiAdmin.searchFriend();
        }
    }

    class FriendEntity {

        private String name = "";

        private String deviceId = "";

        private String ssid = "";

        private int mIcon;

        private int index = -1;

        public FriendEntity(String name, String deviceId, String ssid) {
            this.name = name;
            this.deviceId = deviceId;
            this.ssid = ssid;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getDeviceId() {
            return this.deviceId;
        }

        public void setSsid(String ssid) {
            this.ssid = ssid;
        }

        public String getSsid() {
            return this.ssid;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return this.index;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }

            if (object == null) {
                return false;
            }

            if (!(object instanceof FriendEntity)) {
                return false;
            }

            final FriendEntity friendEntity = (FriendEntity)object;

            if (!getName().equals(friendEntity.getName())) {
                return false;
            }

            if (!getDeviceId().equals(friendEntity.getDeviceId()))
                return false;

            if (!getSsid().equals(friendEntity.getSsid())) {
                return false;
            }

            return true;
        }

        public int getIcon() {
            return mIcon;
        }

        public void setIcon(int mIcon) {
            this.mIcon = mIcon;
        }

    }

    private class MyHandler extends Handler {

        private WeakReference<SearchFriendActivity> activity;

        private int circleCount;

        public MyHandler(SearchFriendActivity act) {
            activity = new WeakReference<SearchFriendActivity>(act);
            circleCount = 0;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            SearchFriendActivity sfa = activity.get();
            if (sfa != null) {
                switch (msg.what) {
                    case SCAN_TIMEOUT:
                        WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_CONNECT_SCAN_NONE);
                        Intent intent = new Intent(SearchFriendActivity.this, MainActivity.class);
                        intent.putExtra(INTENT_TYPE, SEARCH_TIME_OUT);
                        startActivity(intent);
                        break;

                    case FRIENDLIST_MSG:

                        if (mTimerTask == null)
                            break;

                        Bundle bundle = msg.getData();
                        mFriendsList = (ArrayList<ScanResult>)bundle
                                .getSerializable(FRIENDLIST_KEY);

                        // change the scanresult info to friend entity
                        ArrayList<FriendEntity> friends = new ArrayList<FriendEntity>();
                        for (ScanResult result : mFriendsList) {
                            String ssid = result.SSID;
                            String[] info = ssid.split("_");
                            String deviceId = info[info.length - 1];
                            String name = info[1];
                            if (info.length > 3) {
                                name = info[1] + "_" + info[2];
                            }
                            FriendEntity entity = new FriendEntity(name, deviceId, ssid);
                            friends.add(entity);
                        }

                        if (friends != null && !friends.isEmpty()) {
                            mHandler.removeMessages(SCAN_TIMEOUT);
                        }

                        // go over the friend map, when the entity is not null
                        // and is not in the friends list
                        // remove it and set it view gone
                        for (ImageView image : mFriendMap.keySet()) {
                            FriendEntity entity = mFriendMap.get(image);

                            if (entity.getIndex() != -1 && !friends.contains(entity)) {
                                final int index = entity.getIndex();

                                YoYo.with(Techniques.FadeOutDown).duration(1000)
                                        .withListener(new Animator.AnimatorListener() {

                                            @Override
                                            public void onAnimationStart(Animator arg0) {

                                            }

                                            @Override
                                            public void onAnimationRepeat(Animator arg0) {

                                            }

                                            @Override
                                            public void onAnimationEnd(Animator arg0) {
                                                mFriendIconArray[index].setVisibility(View.GONE);
                                            }

                                            @Override
                                            public void onAnimationCancel(Animator arg0) {

                                            }
                                        }).playOn(mFriendIconArray[index]);

                                YoYo.with(Techniques.FadeOutDown).duration(1000)
                                        .withListener(new Animator.AnimatorListener() {

                                            @Override
                                            public void onAnimationStart(Animator arg0) {

                                            }

                                            @Override
                                            public void onAnimationRepeat(Animator arg0) {

                                            }

                                            @Override
                                            public void onAnimationEnd(Animator arg0) {
                                                mFriendNameArray[index].setVisibility(View.GONE);
                                            }

                                            @Override
                                            public void onAnimationCancel(Animator arg0) {

                                            }
                                        }).playOn(mFriendNameArray[index]);
                            }

                            if (entity.getIndex() == -1) { // entity is null
                                continue;
                            } else if (friends.contains(entity)) { // entity is
                                continue;
                            } else { // entity is not null and is not in the
                                     // friends list
                                int index = entity.getIndex();
                                mFriendIconArray[index].setVisibility(View.GONE);
                                mFriendNameArray[index].setVisibility(View.GONE);

                                entity.setName("");
                                entity.setDeviceId("");
                                entity.setSsid("");
                                entity.setIndex(-1);
                            }

                        }

                        for (FriendEntity entity : friends) {
                            boolean isFind = false;

                            if (mFriendMap.containsValue(entity)) {
                                continue;
                            } else {
                                int displayIndex = (int)(Math.random() * 5);
                                int j = 0;
                                for (; j < mFriendIconArray.length; ++j) {
                                    if (mFriendIconArray[displayIndex].getVisibility() == View.GONE) {
                                        // associate the entity with the view
                                        entity.setIndex(displayIndex);
                                        mFriendMap.put(mFriendIconArray[displayIndex], entity);

                                        // change the visibility of the view
                                        mFriendIconArray[displayIndex].setVisibility(View.VISIBLE);
                                        mFriendNameArray[displayIndex].setVisibility(View.VISIBLE);

                                        // set data to the view to display
                                        mFriendNameArray[displayIndex].setText(entity.getName());
                                        int userIcon = 0;
                                        Cursor cursor = getContentResolver().query(
                                                History.FriendsColumns.CONTENT_URI, null,
                                                History.FriendsColumns.DEVICE_ID + " = ? ",
                                                new String[] {
                                                    entity.getDeviceId()
                                                }, null);

                                        if (cursor != null && cursor.moveToFirst()) {
                                            userIcon = cursor.getInt(cursor
                                                    .getColumnIndex(History.FriendsColumns.PHOTO));
                                        }

                                        entity.setIcon(userIcon);

                                        mFriendIconArray[displayIndex].setImageBitmap(BitmapUtil
                                                .toRoundBitmap(SearchFriendActivity.this,
                                                        Utils.photos[userIcon]));

                                        if (cursor != null) {
                                            cursor.close();
                                        }

                                        final int index = displayIndex;

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
                                                        YoYo.with(Techniques.Wobble).duration(1000)
                                                                .playOn(mFriendIconArray[index]);
                                                    }

                                                    @Override
                                                    public void onAnimationCancel(Animator arg0) {

                                                    }
                                                }).playOn(mFriendIconArray[displayIndex]);

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
                                                        YoYo.with(Techniques.Wobble).duration(1000)
                                                                .playOn(mFriendNameArray[index]);
                                                    }

                                                    @Override
                                                    public void onAnimationCancel(Animator arg0) {

                                                    }
                                                }).playOn(mFriendNameArray[displayIndex]);

                                        isFind = true;
                                        break;
                                    }
                                    displayIndex = (displayIndex + 1) % 6;
                                }

                                if (isFind && j < mFriendIconArray.length - 1) {
                                    break;
                                } else if (j == mFriendIconArray.length - 1) {
                                    // Toast.makeText(getParent(),
                                    // R.string.connect_search_cross_the_border,
                                    // Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        break;

                    case ANIMATION_START:
                        switch (circleCount % 4) {
                            case 1:
                                mCircleBlue1.startAnimation(mCircleAnimationSet);
                                circleCount = 1;
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
                        circleCount++;
                        break;
                    default:
                        break;
                }
            } else {
                if (mTimerTask != null) {
                    mTimerTask.cancel();
                    mTimerTask = null;
                }

                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
            }
        }
    }

    private int dip2px(float dipValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

}
