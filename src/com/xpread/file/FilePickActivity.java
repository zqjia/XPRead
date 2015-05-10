
package com.xpread.file;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.uc.base.wa.WaEntry;
import com.xpread.MainActivity;
import com.xpread.R;
import com.xpread.RecordsActivity;
import com.xpread.SearchFriendActivity;
import com.xpread.adapter.TabPagerAdapter;
import com.xpread.control.Controller;
import com.xpread.control.WifiAdmin;
import com.xpread.control.WifiApAdmin;
import com.xpread.provider.FileBean;
import com.xpread.provider.UserInfo;
import com.xpread.util.HomeWatcher;
import com.xpread.util.HomeWatcher.OnHomePressedListener;
import com.xpread.util.LogUtil;
import com.xpread.wa.WaKeys;
import com.xpread.widget.CircleAnimation;

public class FilePickActivity extends FragmentActivity implements BackHandledInterface {

    private static final String TAG = "FilePickActivity";

    private ViewPager mPager;
    private Button mSelectedButton;
    private Button mSendButton;
    private HorizontalScrollView mHorizontalScrollView;
    
    TextView mAppLabel;
    TextView mImageLabel;
    TextView mMusicLabel;
    TextView mVideoLabel;
    TextView mFileLabel;

    View mViewDivider;

    int minWidth;

    int mCurrentIndex = 0;

    private ArrayList<Fragment> mFragmentsList;

    private ArrayList<FileBean> mSelectFile = new ArrayList<FileBean>();
    private ArrayList<TextView> mLabelLists;

    private BackHandledFragment mBackHandedFragment;

    // FIXME test animation
    private CircleAnimation mCircleAnimation;

    private RelativeLayout mRootView;

    private HomeWatcher mHomeWatcher;
    private Controller mController;

    public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;

    BroadcastReceiver mAppReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                String packageName = intent.getDataString();
                for (int i = 0; i < mSelectFile.size(); i++) {
                    if (mSelectFile.get(i).uri.contains(packageName)) {
                        updateSelectCount(mSelectFile.get(i), false);
                        break;
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.file_picker);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        minWidth = (int)(dm.widthPixels / 5);
        initView();

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        registerReceiver(mAppReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mAppReceiver);
        mSelectFile.clear();
        super.onDestroy();
    }

    private void initView() {
        mHorizontalScrollView = (HorizontalScrollView)findViewById(R.id.tab_scroll);

        mSelectedButton = (Button)findViewById(R.id.select_files);
        mSelectedButton.setText(String.format(getResources().getString(R.string.file_selected), 0));
        mSelectedButton.setEnabled(false);

        mSendButton = (Button)findViewById(R.id.send_files);
        mSendButton.setEnabled(false);
        mSendButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Controller controller = Controller.getInstance(getApplicationContext());
                controller.preTransferFiles(mSelectFile);
                int fileCount = mSelectFile.size();
                if (fileCount <= 1) {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SEND_ONE);
                } else if (fileCount <= 2) {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SEND_TWO);
                } else if (fileCount <= 3) {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SEND_THREE);
                } else if (fileCount <= 5) {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SEND_FOUR_FIVE);
                } else if (fileCount <= 10) {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SEND_SIX_TEN);
                } else {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SEND_TEN_ABOVE);
                }

                if (controller.isConnected()) {
                    controller.handleTransferFiles();
                    controller.sendFiles();

//                    Intent intent = new Intent(FilePickActivity.this, MainActivity.class);
//                    intent.putExtra(INTENT_TYPE, SEND_FILE);
                    /**
                     * 交互改变，如果已经连接则跳转到传输介面
                     * */
                    Intent intent = new Intent(FilePickActivity.this, RecordsActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(FilePickActivity.this, SearchFriendActivity.class);
                    startActivity(intent);
                }
            }

        });

        mAppLabel = (TextView)findViewById(R.id.app_label);
        mImageLabel = (TextView)findViewById(R.id.image_label);
        mMusicLabel = (TextView)findViewById(R.id.music_label);
        mVideoLabel = (TextView)findViewById(R.id.video_label);
        mFileLabel = (TextView)findViewById(R.id.file_label);

        mLabelLists = new ArrayList<TextView>();
        mLabelLists.clear();
        mLabelLists.add(mAppLabel);
        mLabelLists.add(mImageLabel);
        mLabelLists.add(mMusicLabel);
        mLabelLists.add(mVideoLabel);
        mLabelLists.add(mFileLabel);

        for (int i = 0; i < mLabelLists.size(); i++) {
            mLabelLists.get(i).setMinimumWidth(minWidth);
            mLabelLists.get(i).setOnClickListener(new LableOnClick(i));
            mLabelLists.get(i).setTextColor(
                    i == mCurrentIndex ? getResources().getColor(R.color.title_color_checked)
                            : getResources().getColor(R.color.title_color_unchecked));
        }

        mViewDivider = findViewById(R.id.divider);
        LayoutParams params = (LayoutParams)mViewDivider.getLayoutParams();
        params.leftMargin = minWidth * mCurrentIndex + minWidth / 2 - params.width / 2;
        mViewDivider.setLayoutParams(params);

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(4);
        mFragmentsList = new ArrayList<Fragment>();
        mFragmentsList.clear();
        Fragment appFragment = new AppFragment();
        mFragmentsList.add(appFragment);
        Fragment imageFragment = new ImageFragment1();
        mFragmentsList.add(imageFragment);
        Fragment musicFragment = new MusicFragment1();
        mFragmentsList.add(musicFragment);
        Fragment videoFragment = new VideoFragment();
        mFragmentsList.add(videoFragment);
        Fragment fileFragment = new FileFragment();
        mFragmentsList.add(fileFragment);

        mPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager(), mFragmentsList));
        mPager.setCurrentItem(mCurrentIndex);
        mPager.setOnPageChangeListener(new UOnPageChangeListener());
        
        //设置ViewPager滑动时的动画效果
        mPager.setPageTransformer(true, new AlphaPageTransfer());

        ImageView back = (ImageView)findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mSelectFile.clear();
                finish();
            }
        });
    }

    public void updateSelectCount(FileBean file, boolean add) {
        if (add) {
            mSelectFile.add(file);
        } else {
            mSelectFile.remove(file);
        }
        String content = String.format(getResources().getString(R.string.file_selected),
                mSelectFile.size());
        if (mSelectFile.size() > 0) {
            SpannableString ss = new SpannableString(content);
            int start = content.indexOf("(");
            int end = content.indexOf(")") + 1;
            ss.setSpan(new ForegroundColorSpan(Color.parseColor("#2b86eb")), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSelectedButton.setText(ss);
        } else {
            mSelectedButton.setText(content);
        }

        mSendButton.setEnabled(mSelectFile.size() > 0);
    }

    class LableOnClick implements OnClickListener {
        private int index;

        LableOnClick(int i) {
            index = i;
        }

        @Override
        public void onClick(View v) {
            if (mPager.getCurrentItem() != index) {
                mPager.setCurrentItem(index);
            }

        }
    }

    public class UOnPageChangeListener implements OnPageChangeListener {

        public UOnPageChangeListener() {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

            Animation animation = null;
            switch (position) {
                case 0:
                    mHorizontalScrollView.smoothScrollTo(0, 0);
                    switch (mCurrentIndex) {
                        case 0:
                            break;
                        case 1:
                            animation = new TranslateAnimation(minWidth, 0, 0, 0);
                            break;
                        case 2:
                            animation = new TranslateAnimation(minWidth * 2, 0, 0, 0);
                            break;
                        case 3:
                            animation = new TranslateAnimation(minWidth * 3, 0, 0, 0);
                            break;
                        case 4:
                            animation = new TranslateAnimation(minWidth * 4, 0, 0, 0);
                            break;
                        default:
                            break;
                    }
                    break;
                case 1:
                    mHorizontalScrollView.smoothScrollTo(0, 0);
                    switch (mCurrentIndex) {
                        case 0:
                            animation = new TranslateAnimation(0, minWidth, 0, 0);
                            break;
                        case 1:
                            break;
                        case 2:
                            animation = new TranslateAnimation(minWidth * 2, minWidth, 0, 0);
                            break;
                        case 3:
                            animation = new TranslateAnimation(minWidth * 3, minWidth, 0, 0);
                            break;
                        case 4:
                            animation = new TranslateAnimation(minWidth * 4, minWidth, 0, 0);
                            break;
                        default:
                            break;
                    }

                    break;
                case 2:
                    mHorizontalScrollView.smoothScrollTo(0, 0);
                    switch (mCurrentIndex) {
                        case 0:
                            animation = new TranslateAnimation(0, minWidth * 2, 0, 0);
                            break;
                        case 1:
                            animation = new TranslateAnimation(minWidth, minWidth * 2, 0, 0);
                            break;
                        case 2:
                            break;
                        case 3:
                            animation = new TranslateAnimation(minWidth * 3, minWidth * 2, 0, 0);
                            break;
                        case 4:
                            animation = new TranslateAnimation(minWidth * 4, minWidth * 2, 0, 0);
                            break;
                        default:
                            break;
                    }

                    break;
                case 3:
                    mHorizontalScrollView.smoothScrollTo(minWidth * 4, 0);
                    switch (mCurrentIndex) {
                        case 0:
                            animation = new TranslateAnimation(0, minWidth * 3, 0, 0);
                            break;
                        case 1:
                            animation = new TranslateAnimation(minWidth, minWidth * 3, 0, 0);
                            break;
                        case 2:
                            animation = new TranslateAnimation(minWidth * 2, minWidth * 3, 0, 0);
                            break;
                        case 3:
                            break;
                        case 4:
                            animation = new TranslateAnimation(minWidth * 4, minWidth * 3, 0, 0);
                            break;
                        default:
                            break;
                    }

                    break;
                case 4:
                    mHorizontalScrollView.smoothScrollTo(minWidth * 4, 0);
                    switch (mCurrentIndex) {
                        case 0:
                            animation = new TranslateAnimation(0, minWidth * 4, 0, 0);
                            break;
                        case 1:
                            animation = new TranslateAnimation(minWidth, minWidth * 4, 0, 0);
                            break;
                        case 2:
                            animation = new TranslateAnimation(minWidth * 2, minWidth * 4, 0, 0);
                            break;
                        case 3:
                            animation = new TranslateAnimation(minWidth * 3, minWidth * 4, 0, 0);
                            break;
                        case 4:
                            break;
                        default:
                            break;
                    }

                    break;
                default:
                    break;
            }

            mCurrentIndex = position;

            if (animation != null) {
                animation.setDuration(200);
                animation.setFillAfter(true);
                animation.setInterpolator(new DecelerateInterpolator());
                mViewDivider.startAnimation(animation);
            }

            for (int i = 0; i < mLabelLists.size(); i++) {
                mLabelLists.get(i).setTextColor(
                        i == position ? getResources().getColor(R.color.title_color_checked)
                                : getResources().getColor(R.color.title_color_unchecked));
            }
        }

    }

    /*
     * 继承 BackHandledInterface 实现fragment的back事件自定义
     */

    @Override
    public void setSelectedFragment(BackHandledFragment selectedFragment) {
        this.mBackHandedFragment = selectedFragment;
    }

    @Override
    public void onBackPressed() {
        if (mBackHandedFragment == null || !mBackHandedFragment.onBackPressed()) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                super.onBackPressed();
            } else {
                getSupportFragmentManager().popBackStack();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // watch the home key

        if (this.mController == null) {
            this.mController = Controller.getInstance(getApplicationContext());
        }

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
        super.onPause();
        if (mHomeWatcher != null) {
            this.mHomeWatcher.setOnHomePressedListener(null);
            this.mHomeWatcher.stopWatch();
        }
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

}
