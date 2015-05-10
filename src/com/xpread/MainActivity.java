package com.xpread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.uc.base.wa.WaEntry;
import com.xpread.adapter.IconAdapter;
import com.xpread.control.Controller;
import com.xpread.control.Controller.FileReceiveNoticeListener;
import com.xpread.control.Controller.NetworkStateChangeListener;
import com.xpread.control.WifiAdmin;
import com.xpread.control.WifiApAdmin;
import com.xpread.file.FilePickActivity;
import com.xpread.provider.UserInfo;
import com.xpread.util.BitmapUtil;
import com.xpread.util.Const;
import com.xpread.util.LaboratoryData;
import com.xpread.util.LogUtil;
import com.xpread.util.ScreenUtil;
import com.xpread.util.Utils;
import com.xpread.wa.WaKeys;
import com.xpread.widget.ColorEvaluator;
import com.xpread.widget.HistoryBackgroundView;
import com.xpread.widget.RoundImageButton;
import com.xpread.widget.RoundImageView;

public class MainActivity extends BaseActivity implements OnClickListener {

    private static final String TAG = "MainActivity";

    private Controller mController;

    private RoundImageButton mSendButton;
    private RoundImageButton mReceiveButton;
//    private RoundImageButton mDisconnectButton;

    private RelativeLayout mUserInfoLayout;
    private RelativeLayout mConnectedUserInfoLayout;

    RoundImageView mFaceImage;
    TextView mUserName;
    ImageView mUserHistory;
    PopupWindow mPopupWindow;
    List<Drawable> mPhotos;

    int iconId;

    DrawerLayout mDrawerLayout;
    ImageView mMenu;
    RelativeLayout mDrawerContent;
    ListView mDrawerListView;
    TextView mDrawerBottomText;
    ImageView mShare;
    RoundImageView mOwerIcon;
    RoundImageView mGuestIcon;
    TextView mGuestName;

    RelativeLayout mContentLayout;
//    ImageView mHistoryBackground;

    private static final float COLUM_NUMBER = 4.5f;

    private ImageView mWaitConnectCircleView1, mWaitConnectCircleView2, mWaitConnectCircleView3,
            mWaitConnectCircleView4, mWaitConnectCircleView5;

    private Bitmap mWaitConnectCircleBmp1, mWaitConnectCircleBmp2, mWaitConnectCircleBmp3,
            mWaitConnectCircleBmp4, mWaitConnectCircleBmp5;

    private Bitmap[] mWaitConnectCircleBmpArray = {mWaitConnectCircleBmp1, mWaitConnectCircleBmp2,
            mWaitConnectCircleBmp3, mWaitConnectCircleBmp4, mWaitConnectCircleBmp5};

    class WaitConnectIndex {
        int index;
        boolean flag;
    }

    private WaitConnectIndex mWaitConnectIndex1, mWaitConnectIndex2, mWaitConnectIndex3,
            mWaitConnectIndex4, mWaitConnectIndex5;

    private WaitConnectIndex[] mWaitConnectIndexArray = {mWaitConnectIndex1, mWaitConnectIndex2,
            mWaitConnectIndex3, mWaitConnectIndex4, mWaitConnectIndex5};

    private static final String GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending";
    private static final String GOOGLE_PLAY_ACTIVITY_NAME =
            "com.android.vending.AssetBrowserActivity";
    private static final String APK_URL =
            "https://play.google.com/store/apps/details?id=com.xpread";
    private static final int FILE_RECEIVE_REPEAT_TIMES = 2;

    private boolean mIsNameValid = false;

    private Timer mWaitConnectTimer = new Timer();
    private TimerTask mWaitConnectTask;
    
    private HistoryBackgroundView mHistoryBackgroundView;
    
    //Animations
    private static final int ANIMATOR_DURATION = 800;
    private static final int SCALE_REPEAT_COUNT = 2;
    private AnimatorSet mHistoryAnimatorSet;
    private ObjectAnimator mHistoryScaleXAnimator;
    private ObjectAnimator mHistoryScaleYAnimator;
    private ObjectAnimator mHistoryBackgroundColorAnimator;
    
    FileReceiveNoticeListener mFileReceiveNoticeListener = new FileReceiveNoticeListener() {

        @Override
        public void animationStart() {
            mHistoryAnimatorSet.play(mHistoryScaleXAnimator)
                               .with(mHistoryScaleYAnimator)
                               .with(mHistoryBackgroundColorAnimator);
            mHistoryAnimatorSet.start();
        }
    };

    NetworkStateChangeListener mNetworkStateChangeListener = new NetworkStateChangeListener() {

        @Override
        public void stateChangeListener(int state) {
            if (state == Const.REFRESH_USER_INFO) {
                UserInfo targetUserInfo = mController.getTargetInfo();
                if (targetUserInfo != null) {
                    mGuestIcon.setImageDrawable(mPhotos.get(targetUserInfo.getPictureID()));
                    mGuestName.setText(targetUserInfo.getUserName());
                }
                return;
            }
            if (state == Const.REFRESH_DISCONNECTION) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "--------->recieve the disconnect message");
                }
                resetDefaultState();

            } else if (state == Const.REFRESH_ESTIBALE) {

                if (mWaitConnectTask != null) {
                    mWaitConnectTask.cancel();
                }

                // third step, refresh the UI
                boolean isConnected = true;
                changeViewState(isConnected);
                changeWaitCircleState(isConnected);
                
                mOwerIcon.setImageDrawable(mPhotos.get(Utils.getOwerIcon(MainActivity.this)));
                UserInfo targetUserInfo = mController.getTargetInfo();
                if (targetUserInfo != null) {
                    mGuestIcon.setImageDrawable(mPhotos.get(targetUserInfo.getPictureID()));
                    mGuestName.setText(targetUserInfo.getUserName());
                }

                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_CONNECT_SUCESS);
            }
        }
    };
    
    private final int WAIT_CONNECTTED_ANIMATION = 0x0101;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case WAIT_CONNECTTED_ANIMATION:
                    for (int i = 0; i < mWaitConnectIndexArray.length; ++i) {

                        if ((mWaitConnectIndexArray[i].index + 1) >= 5) {
                            mWaitConnectIndexArray[i].flag = false;
                            mWaitConnectIndexArray[i].index = 3;

                            if (i == 0) {
                                mWaitConnectIndexArray[1].flag = true;
                                mWaitConnectIndexArray[2].flag = true;
                                mWaitConnectIndexArray[3].flag = true;
                                mWaitConnectIndexArray[4].flag = true;
                            }

                        } else if ((mWaitConnectIndexArray[i].index - 1) <= -1) {
                            mWaitConnectIndexArray[i].flag = true;
                            mWaitConnectIndexArray[i].index = 1;

                            if (i == 0) {
                                mWaitConnectIndexArray[1].flag = true;
                                mWaitConnectIndexArray[2].flag = true;
                                mWaitConnectIndexArray[3].flag = true;
                                mWaitConnectIndexArray[4].flag = true;
                            }

                        } else {
                            if (mWaitConnectIndexArray[i].flag == true) {
                                mWaitConnectIndexArray[i].index++;
                            } else {
                                mWaitConnectIndexArray[i].index--;
                            }
                        }
                    }

                    mWaitConnectCircleView1
                            .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[4].index]);
                    mWaitConnectCircleView2
                            .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[3].index]);
                    mWaitConnectCircleView3
                            .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[2].index]);
                    mWaitConnectCircleView4
                            .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[1].index]);
                    mWaitConnectCircleView5
                            .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[0].index]);
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
        setContentView(R.layout.activity_main);
        // 机型和Android版本号--------------------------------------------------------------
        // 实验室数据
        LaboratoryData.gAppBeginCPUTime = LaboratoryData.getAppCpuTime();
        LaboratoryData.gTotalAPPBeginCPUTime = LaboratoryData.getTotalCpuTime();
        WaEntry.handleMsg(WaEntry.MSG_UPLOAD_FILE);
        // -------------------------------------------------------------------------------------
        this.mController = Controller.getInstance(getApplicationContext());
        iconId = Utils.getOwerIcon(getApplicationContext());

        initIcons();

        initView();

        initAnimation();

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

    }

    private void initView() {
        mSendButton = (RoundImageButton) findViewById(R.id.send_button);
        mSendButton.setOnClickListener(this);

        mReceiveButton = (RoundImageButton) findViewById(R.id.receive_button);
        mReceiveButton.setOnClickListener(this);

//        mDisconnectButton = (RoundImageButton) findViewById(R.id.disconnect_button);
//        mDisconnectButton.setOnClickListener(this);

        mFaceImage = (RoundImageView) findViewById(R.id.user_icon);
        mFaceImage.setImageDrawable(mPhotos.get(Utils.getOwerIcon(this)));
        mFaceImage.setOnClickListener(this);

        mUserName = (TextView) findViewById(R.id.user_name);
        mUserName.setText(Utils.getOwnerName(this));
        mUserName.setOnClickListener(this);

        mUserHistory = (ImageView) findViewById(R.id.history);
        mUserHistory.setOnClickListener(this);
//        mHistoryBackground = (ImageView) findViewById(R.id.history_background);
        mHistoryBackgroundView = (HistoryBackgroundView)findViewById(R.id.history_background);

        mUserInfoLayout = (RelativeLayout) findViewById(R.id.user_info);
        mConnectedUserInfoLayout = (RelativeLayout) findViewById(R.id.user_info_connected);
        mOwerIcon = (RoundImageView) findViewById(R.id.owner_icon);
        mGuestIcon = (RoundImageView) findViewById(R.id.guest_icon);
        mGuestName = (TextView) findViewById(R.id.guest_name);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {

            @Override
            public void onDrawerStateChanged(int arg0) {

            }

            @Override
            public void onDrawerSlide(View arg0, float arg1) {

            }

            @Override
            public void onDrawerOpened(View arg0) {
                mMenu.setImageResource(R.drawable.back);

                SpannableString ss = generateDrawerBottomText();
                mDrawerBottomText.setText(ss);
            }

            @Override
            public void onDrawerClosed(View arg0) {
                mMenu.setImageResource(R.drawable.menu);
            }
        });

        mMenu = (ImageView) findViewById(R.id.menu);
        mMenu.setOnClickListener(this);
        mDrawerContent = (RelativeLayout) findViewById(R.id.drawer_content);

        mDrawerListView = (ListView) findViewById(R.id.drawer_listvew);
        SimpleAdapter adapter = initDrawerAdapter();
        mDrawerListView.setAdapter(adapter);
        mDrawerListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                handleDrawerClick(position);
            }
        });

        mDrawerBottomText = (TextView) findViewById(R.id.drawer_bottom_text);

        mShare = (ImageView) findViewById(R.id.share);
        mShare.setOnClickListener(this);

        mContentLayout = (RelativeLayout) findViewById(R.id.content);

        initWaitCircle();
        
    }

    private void initAnimation() {
        this.mHistoryAnimatorSet = new AnimatorSet();
        
        this.mHistoryScaleXAnimator = ObjectAnimator.ofFloat(mUserHistory, "scaleX", 1.0f, 1.5f, 1.0f);
        this.mHistoryScaleXAnimator.setDuration(ANIMATOR_DURATION);
        this.mHistoryScaleXAnimator.setRepeatCount(SCALE_REPEAT_COUNT);
        this.mHistoryScaleYAnimator = ObjectAnimator.ofFloat(mUserHistory, "scaleY", 1.0f, 1.5f, 1.0f);
        this.mHistoryScaleYAnimator.setDuration(ANIMATOR_DURATION);
        this.mHistoryScaleYAnimator.setRepeatCount(SCALE_REPEAT_COUNT);
        
        this.mHistoryBackgroundColorAnimator = ObjectAnimator.ofObject(mHistoryBackgroundView, "color", 
            new ColorEvaluator(), HistoryBackgroundView.START_COLOR, HistoryBackgroundView.END_COLOR);
        this.mHistoryBackgroundColorAnimator.setDuration(HistoryBackgroundView.COLOR_ANIMATION_DURATION);
        this.mHistoryBackgroundColorAnimator.setInterpolator(new DecelerateInterpolator());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mController.isConnected()) {
            changeViewState(mController.isConnected());

            mOwerIcon.setImageDrawable(mPhotos.get(Utils.getOwerIcon(this)));
            UserInfo targetUserInfo = mController.getTargetInfo();
            if (targetUserInfo != null) {
                mGuestIcon.setImageDrawable(mPhotos.get(targetUserInfo.getPictureID()));
                mGuestName.setText(targetUserInfo.getUserName());
            }

        } else {
            if (LogUtil.isLog) {
                Log.e(TAG, "MainActivity ----> not Connect");
            }

            changeViewState(mController.isConnected());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus == true) {

            if (mController.isConnected()) {

                // second step, set the wait connected circle view visible
                changeWaitCircleState(true);

                seteBitmapForWaitCircle();
                // third step, start the animation
                if (this.mWaitConnectTimer == null) {
                    mWaitConnectTimer = new Timer();
                }

                if (mWaitConnectTask != null) {
                    mWaitConnectTask.cancel();
                }

                mWaitConnectTask = new WaitConnectTask();
                mWaitConnectTimer.schedule(mWaitConnectTask, 500, 500);

            } else {
                changeWaitCircleState(false);
                if (mWaitConnectTask != null) {
                    mWaitConnectTask.cancel();
                }
            }
        }
    }



    private ArrayList<HashMap<String, Object>> initDrawerListData() {
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map1 = new HashMap<String, Object>();
        map1.put("text", getResources().getString(R.string.drawer_update));
        map1.put("icon", R.drawable.update);
        list.add(map1);

        HashMap<String, Object> map2 = new HashMap<String, Object>();
        map2.put("icon", R.drawable.about_us);
        map2.put("text", getResources().getString(R.string.drawer_about_us));
        list.add(map2);

        return list;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_button:
                if (!mController.isConnected()) {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SEND_DISCONNECT);
                } else {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SEND_CONNECT);
                }
                Intent sendIntent = new Intent(MainActivity.this, FilePickActivity.class);

                startActivityForResult(sendIntent, Const.PICK_FILE_REQUEST_CODE);

                break;

            case R.id.receive_button:
                if (mReceiveButton.getText().equals(R.string.receive_button)) {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECEIVE);
                    Intent receiveIntent = new Intent(this, WaitFriendActivity.class);
                    startActivity(receiveIntent); 
                } else {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_DISCONNECT);
                    mController.disconnect();
                }
                break;

            case R.id.user_icon:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_USER_SET);
                doChangeUserInfo();

                break;

            case R.id.user_name:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_USER_SET);
                doChangeUserInfo();

                break;

            case R.id.history:
                
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECORD_ENTRANCE);
                Intent historyIntent = new Intent(this, RecordsActivity.class);
                startActivity(historyIntent);
                
                mHandler.postDelayed(new Runnable() {
                    
                    @SuppressWarnings("deprecation")
                    @Override
                    public void run() {
                        Log.e(TAG, "the current color is " + mHistoryBackgroundView.getColor());
                        if (mHistoryBackgroundView.getColor().toUpperCase(Locale.getDefault())
                                .equals(HistoryBackgroundView.END_COLOR) ) {
                            Log.e(TAG, "set start color");
                            mHistoryBackgroundView.setColor(HistoryBackgroundView.START_COLOR);
                        }
                    }
                }, 300);
                break;

            case R.id.menu:
                if (mDrawerLayout.isDrawerOpen(mDrawerContent)) {
                    mDrawerLayout.closeDrawer(mDrawerContent);
                } else {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_HAMBURGER);
                    mDrawerLayout.openDrawer(mDrawerContent);
                }
                break;

            case R.id.share:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SHARE);
                Intent shareIntent = new Intent(this, ShareActivity.class);
                startActivity(shareIntent);
                break;

            default:
                break;
        }
    }

    private void doChangeUserInfo() {
        final String userName = Utils.getOwnerName(this);
        final int iconIndex = Utils.getOwerIcon(this);
        if (mPopupWindow == null) {
            View contentView =
                    LayoutInflater.from(this).inflate(R.layout.change_user_info, null, false);
            GridView gridView = (GridView) contentView.findViewById(R.id.user_icon_grid);
            IconAdapter adapter = new IconAdapter(this, mPhotos);
            gridView.setAdapter(adapter);

            int colmunWidth = (int) (ScreenUtil.getScreenWidth(this) / COLUM_NUMBER);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(adapter.getCount() * colmunWidth,
                            LayoutParams.WRAP_CONTENT);
            gridView.setLayoutParams(params);
            gridView.setColumnWidth(colmunWidth);
            gridView.setStretchMode(GridView.NO_STRETCH);
            int count = adapter.getCount();
            gridView.setNumColumns(count);

            final RoundImageView userIcon =
                    (RoundImageView) contentView.findViewById(R.id.user_icon);
            userIcon.setImageDrawable(mPhotos.get(Utils.getOwerIcon(this)));
            final EditText name = (EditText) contentView.findViewById(R.id.user_name);
            name.setText(mUserName.getText());

            name.addTextChangedListener(new TextWatcher() {

                int selectionStart = 0;
                int selectionEnd = 0;
                CharSequence temp = null;
                final int MAX_LEN = 14;

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    this.temp = s;
                }

                @Override
                public void afterTextChanged(Editable s) {
                    mIsNameValid = true;
                    this.selectionStart = name.getSelectionStart();
                    this.selectionEnd = name.getSelectionEnd();

                    if (LogUtil.isLog) {
                        Log.e(TAG, "selection start is " + this.selectionStart);
                    }

                    String reg = "^[a-zA-Z0-9_\\-\\s]*$";
                    Pattern pattern = Pattern.compile(reg);
                    Matcher matcher = null;

                    if (this.temp.length() > MAX_LEN) {
                        mIsNameValid = false;
                        s.delete(this.selectionStart - 1, this.selectionEnd);
                        int tempSelection = this.selectionStart;
                        name.setText(s);
                        name.setSelection(tempSelection);

                        matcher = pattern.matcher(name.getText().toString());
                        if (!matcher.matches()) {
                            Toast.makeText(MainActivity.this,
                                    getResources().getString(R.string.name_illegal_and_length),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this,
                                    getResources().getString(R.string.name_length_limit),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        matcher = pattern.matcher(name.getText().toString());
                        if (!matcher.matches()) {
                            mIsNameValid = false;
                            Toast.makeText(MainActivity.this,
                                    getResources().getString(R.string.name_illegal_hint),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            gridView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    userIcon.setImageDrawable(mPhotos.get(position));
                    iconId = position;
                    mFaceImage.setImageDrawable(mPhotos.get(iconId));
                }
            });

            mPopupWindow =
                    new PopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
            mPopupWindow.setFocusable(true);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            mPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.headedit_bg));
            mPopupWindow.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss() {
                    String user_name = name.getText().toString();

                    if (mIsNameValid) {
                        if (!TextUtils.isEmpty(user_name)) {
                            mUserName.setText(user_name);
                            if (!user_name.equals(userName)) {
                                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD,
                                        WaKeys.KEY_XPREAD_PROFILE_NAME);
                            }
                        } else {
                            user_name = null;
                        }
                    } else {
                        user_name = null;
                    }
                    if (iconIndex != iconId) {
                        WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_PROFILE_ICON);
                    }
                    Utils.saveUserInfo(MainActivity.this, user_name, iconId);
                    mController.setUserInfo(user_name, null, iconId);
                }
            });

            userIcon.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mPopupWindow.isShowing())
                        mPopupWindow.dismiss();
                }
            });
        } else {
            View contentView = mPopupWindow.getContentView();
            final EditText name = (EditText) contentView.findViewById(R.id.user_name);
            name.setText(mUserName.getText());
        }

        mPopupWindow.showAsDropDown(this.mMenu, 0,
                getResources().getDimensionPixelSize(R.dimen.select_icon_top));
    }

    @Override
    public void onBackPressed() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            return;
        }
        mController.disconnect();
        super.onBackPressed();
    }

    public int px2dip(float pxValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    @Override
    protected void onDestroy() {
        // 实验室数据
        LaboratoryData.gAppEndCPUTime = LaboratoryData.getAppCpuTime();
        LaboratoryData.gTotalAPPEndCPUTime = LaboratoryData.getTotalCpuTime();
        LaboratoryData.print();
        // ---------------------------------

        WaEntry.handleMsg(WaEntry.MSG_EXITED);
        WaEntry.handleMsg(WaEntry.MSG_UPLOAD_FILE);

        restoreWifiState();

        if (mWaitConnectTask != null) {
            mWaitConnectTask.cancel();
        }

        mHandler.removeCallbacksAndMessages(null);

        super.onDestroy();

    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        mController.setNetworkStateChangeListener(mNetworkStateChangeListener);
        mController.setFileReceiverNoticeListener(mFileReceiveNoticeListener);

        saveWifiStateBeforeOpen();
        resumeWifiStateToDefault();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        mController.unRegisterNetworkStateChangeListener(mNetworkStateChangeListener);
        mController.unRegistFileReceiveNoticeListener();
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
        super.onPause();
    }

    private boolean openWifi() {
        WifiAdmin wifiAdmin = this.mController.getWifiAdmin();
        WifiApAdmin wifiApAdmin = this.mController.getWifiApAdmin();

        int wifiState = wifiAdmin.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED
                || wifiState == WifiManager.WIFI_STATE_ENABLING) {
            if (LogUtil.isLog) {
                Log.d(TAG, "wifi is enbale or enbaling, it's ok");
            }
            return true;
        } else {
            if (wifiApAdmin.getWifiApState() == WIFI_AP_STATE_ENABLING
                    || wifiApAdmin.getWifiApState() == WifiApAdmin.WIFI_AP_STATE_ENABLED) {
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntent();
    }

    private void processIntent() {
        Intent intent = getIntent();

    }

    private class WaitConnectTask extends TimerTask {

        @Override
        public void run() {
            mHandler.sendEmptyMessage(WAIT_CONNECTTED_ANIMATION);
        }
    }

    private void resetDefaultState() {

        // first step, refresh UI, remove the timeout message and the wait
        // animation timer task
        if (mWaitConnectTask != null) {
            mWaitConnectTask.cancel();
        }

        changeWaitCircleState(false);

        boolean isConnected = false;
        changeViewState(isConnected);

        // second, disconnect the current connection
        mController.getWifiAdmin().disconnectCurrentWifi();

        // second step, remove the configuration
        String connectedFriendSsid = mController.getUerInfo().getConnectedFriendSsid();
        if (android.os.Build.VERSION.SDK_INT != 21) {
            if (connectedFriendSsid != null) {
                mController.getWifiAdmin().removeWifiConfiguration(connectedFriendSsid);
            }
        } else {
            mController.getWifiAdmin().disableNetWork(connectedFriendSsid);
        }

        // third step, resume to the default state
        mController.resumeToDefault();

        // fourth step, open the wifi if wifi not open
        resumeWifiStateToDefault();
    }

    /*
     * change the wifi state in the onResume if role is default , reset the wifi state to
     * default(open) else , the user is connecting or transfer file, so not keep the wifi state
     * under that case
     */
    private void resumeWifiStateToDefault() {
        if (this.mController.getRole() == -1) { // resume to default state
            if (openWifi()) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "onResume---->open wifi success ");
                }
            } else {
                if (LogUtil.isLog) {
                    Log.e(TAG, "onResume---->open wifi fail ");
                }
            }
        } else {
            if (LogUtil.isLog) {
                String role = (this.mController.getRole() == Const.SENDER ? "sender" : "receiver");
                Log.e(TAG, "connect to friend and the role is " + role
                        + "do not change wifi state now");
            }
        }
    }

    /*
     * save the wifi state before open the app it will save the first time, other time will not save
     * again
     */
    private void saveWifiStateBeforeOpen() {

        UserInfo userInfo = this.mController.getUerInfo();

        if (userInfo.getIsWifiConnectedBefore() == -1) { // have not save the
                                                         // state
            WifiAdmin wifiAdmin = this.mController.getWifiAdmin();
            int state = wifiAdmin.getWifiState();

            if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING) {

                if (LogUtil.isLog) {
                    Log.e(TAG, "wifi is open before open xpread");
                }

                userInfo.setIsWifiConnectedBefore(1); // save the state

                // 保存用户的热点设置名称
                WifiApAdmin wifiApAdmin = this.mController.getWifiApAdmin();
                WifiConfiguration wcg = wifiApAdmin.getWifiApConfiguration();
                userInfo.setDefaultSsid(wcg.SSID);

            } else {
                if (LogUtil.isLog) {
                    Log.e(TAG, "wifi is not open before open xpread");
                }
                userInfo.setIsWifiConnectedBefore(0); // save the state
            }
        }
    }

    /*
     * restore the wifi state the method will call when quit the app in MainActivity
     */
    private void restoreWifiState() {

        final UserInfo userInfo = this.mController.getUerInfo();
        WifiAdmin wifiAdmin = this.mController.getWifiAdmin();
        int isWifiConnectedBefore = userInfo.getIsWifiConnectedBefore();
        if (isWifiConnectedBefore == 1) {
            if (openWifi()) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "onDestroy, wifi open success");
                }
            } else {
                if (LogUtil.isLog) {
                    Log.e(TAG, "onDestroy, wifi open fail");
                }
            }
            wifiAdmin.reconnect();

            // 恢复用户的热点设置名称
            WifiApAdmin wifiApAdmin = this.mController.getWifiApAdmin();
            WifiConfiguration wcg = wifiApAdmin.getWifiApConfiguration();
            String defaultSsid = userInfo.getDefaultSsid();
            wcg.SSID = defaultSsid;
        } else if (isWifiConnectedBefore == 0) {
            wifiAdmin.closeWifi();
        }

        userInfo.setIsWifiConnectedBefore(-1);
        userInfo.setDefaultSsid(null);
    }

    private void changeWaitCircleState(boolean isVisible) {
        if (isVisible) {
            this.mWaitConnectCircleView1.setVisibility(View.VISIBLE);
            this.mWaitConnectCircleView2.setVisibility(View.VISIBLE);
            this.mWaitConnectCircleView3.setVisibility(View.VISIBLE);
            this.mWaitConnectCircleView4.setVisibility(View.VISIBLE);
            this.mWaitConnectCircleView5.setVisibility(View.VISIBLE);
        } else {
            this.mWaitConnectCircleView1.setVisibility(View.GONE);
            this.mWaitConnectCircleView2.setVisibility(View.GONE);
            this.mWaitConnectCircleView3.setVisibility(View.GONE);
            this.mWaitConnectCircleView4.setVisibility(View.GONE);
            this.mWaitConnectCircleView5.setVisibility(View.GONE);
        }
    }

    private void handleDrawerClick(int position) {
        switch (position) {
            case 0:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_MENU_UPDATE);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Utils.isGooglePlayInstall(MainActivity.this)) {
                    intent.setClassName(GOOGLE_PLAY_PACKAGE_NAME, GOOGLE_PLAY_ACTIVITY_NAME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                intent.setData(Uri.parse(APK_URL));
                startActivity(intent);
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_MENU_UPDATE);
                break;

            case 1:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_MENU_ABOUT);
                mDrawerLayout.closeDrawer(mDrawerContent);
                Intent intentAboutUs = new Intent();
                intentAboutUs.setClass(MainActivity.this, AboutUsActivity.class);
                startActivity(intentAboutUs);
                break;
            default:
                break;
        }
    }

    private void initWaitCircle() {
        mWaitConnectCircleView1 = (ImageView) findViewById(R.id.wait_connect_circle_view1);
        mWaitConnectCircleView2 = (ImageView) findViewById(R.id.wait_connect_circle_view2);
        mWaitConnectCircleView3 = (ImageView) findViewById(R.id.wait_connect_circle_view3);
        mWaitConnectCircleView4 = (ImageView) findViewById(R.id.wait_connect_circle_view4);
        mWaitConnectCircleView5 = (ImageView) findViewById(R.id.wait_connect_circle_view5);

        Bitmap bitmapSrc = BitmapFactory.decodeResource(getResources(), R.drawable.circle_blue);
        mWaitConnectCircleBmpArray[0] =
                BitmapUtil.toRoundBitmap(BitmapUtil.setAlpha(bitmapSrc, 10));
        mWaitConnectCircleBmpArray[1] =
                BitmapUtil.toRoundBitmap(BitmapUtil.setAlpha(bitmapSrc, 30));
        mWaitConnectCircleBmpArray[2] =
                BitmapUtil.toRoundBitmap(BitmapUtil.setAlpha(bitmapSrc, 50));
        mWaitConnectCircleBmpArray[3] =
                BitmapUtil.toRoundBitmap(BitmapUtil.setAlpha(bitmapSrc, 70));
        mWaitConnectCircleBmpArray[4] =
                BitmapUtil.toRoundBitmap(BitmapUtil.setAlpha(bitmapSrc, 90));

        mWaitConnectIndexArray[0] = new WaitConnectIndex();
        mWaitConnectIndexArray[0].index = 0;
        mWaitConnectIndexArray[0].flag = true;

        mWaitConnectIndexArray[1] = new WaitConnectIndex();
        mWaitConnectIndexArray[1].index = 1;
        mWaitConnectIndexArray[1].flag = true;

        mWaitConnectIndexArray[2] = new WaitConnectIndex();
        mWaitConnectIndexArray[2].index = 2;
        mWaitConnectIndexArray[2].flag = true;

        mWaitConnectIndexArray[3] = new WaitConnectIndex();
        mWaitConnectIndexArray[3].index = 3;
        mWaitConnectIndexArray[3].flag = true;

        mWaitConnectIndexArray[4] = new WaitConnectIndex();
        mWaitConnectIndexArray[4].index = 4;
        mWaitConnectIndexArray[4].flag = true;
    }

    private SimpleAdapter initDrawerAdapter() {
        String[] from = {"icon", "text"};
        int[] to = {R.id.drawer_opra_icon, R.id.drawer_opra_text};
        ArrayList<HashMap<String, Object>> list = initDrawerListData();

        SimpleAdapter adapter =
                new SimpleAdapter(this, list, R.layout.drawer_listview_item, from, to);
        return adapter;
    }

    private SpannableString generateDrawerBottomText() {
        String content = null;
        float size = mController.getTotalTransferDataSize() / Const.KILO;
        if (size > Const.KILO) {
            size /= Const.KILO;

            if (size > Const.KILO) {
                size /= Const.KILO;
                content =
                        String.format(getResources().getString(R.string.drawer_bottom_text_gb),
                                size);
            } else {
                content =
                        String.format(getResources().getString(R.string.drawer_bottom_text_mb),
                                size);
            }
        } else {
            content = String.format(getResources().getString(R.string.drawer_bottom_text_kb), size);
        }

        SpannableString ss = new SpannableString(content);
        int start = content.indexOf(" ") + 1;
        int end = content.lastIndexOf(" ");
        ss.setSpan(new ForegroundColorSpan(getResources()
                .getColor(R.color.drawer_bottom_text_color)), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return ss;
    }

    private void changeViewState(boolean isConnected) {
        if (isConnected) {
            toDisconnectButton();
            mUserInfoLayout.setVisibility(View.GONE);
            mConnectedUserInfoLayout.setVisibility(View.VISIBLE);
        } else {
            toReceiveButton();
            mUserInfoLayout.setVisibility(View.VISIBLE);
            mConnectedUserInfoLayout.setVisibility(View.GONE);
        }
    }

    private void seteBitmapForWaitCircle() {
        mWaitConnectCircleView1
                .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[4].index]);
        mWaitConnectCircleView2
                .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[3].index]);
        mWaitConnectCircleView3
                .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[2].index]);
        mWaitConnectCircleView4
                .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[1].index]);
        mWaitConnectCircleView5
                .setImageBitmap(mWaitConnectCircleBmpArray[mWaitConnectIndexArray[0].index]);
    }
    
    private void initIcons() {
        mPhotos = new ArrayList<Drawable>(8);
        mPhotos.add(getResources().getDrawable(R.drawable.male_01));
        mPhotos.add(getResources().getDrawable(R.drawable.male_02));
        mPhotos.add(getResources().getDrawable(R.drawable.male_03));
        mPhotos.add(getResources().getDrawable(R.drawable.male_04));

        mPhotos.add(getResources().getDrawable(R.drawable.female_01));
        mPhotos.add(getResources().getDrawable(R.drawable.female_02));
        mPhotos.add(getResources().getDrawable(R.drawable.female_03));
        mPhotos.add(getResources().getDrawable(R.drawable.female_04));
    }

    private void toDisconnectButton() {
        Resources res = getResources();
        
        int disconnectBackgroundColor = res.getColor(R.color.disconnect_button_bg_color);
        int disconnectBorderColor = res.getColor(R.color.disconnect_button_border_color);
        int disconnectTextColor = res.getColor(R.color.disconnect_button_text_color);
        Drawable disconnectImageSource = res.getDrawable(R.drawable.disconnet);
        String disconnectText = res.getString(R.string.disconnect_button);
        
        mReceiveButton.refreshButton(disconnectBackgroundColor, disconnectBorderColor, 
            disconnectImageSource, disconnectText, disconnectTextColor);
    }
    
    private void toReceiveButton() {
        Resources res = getResources();
        
        int receiveBackgroundColor = res.getColor(R.color.receive_button_bg_color);
        int receiveBorderColor = res.getColor(R.color.receive_button_border_color);
        int receiveTextColor = res.getColor(R.color.receive_button_text_color);
        Drawable receiveImageSource = res.getDrawable(R.drawable.recieve);
        String receiveText = res.getString(R.string.receive_button);
        
        mReceiveButton.refreshButton(receiveBackgroundColor, receiveBorderColor, 
            receiveImageSource, receiveText, receiveTextColor);
    }
    
}
