package com.xpread;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.uc.base.wa.WaEntry;
import com.xpread.util.BitmapUtil;
import com.xpread.wa.WaKeys;
import com.xpread.widget.RobotoTextView;

public class WaitConnectActivity extends BaseActivity {
    
    private static final String TAG = WaitConnectActivity.class.getSimpleName();
    
    private ImageView mRocketBody;
    private ImageView mRocketFire;
    private ImageView mRocketSmoke;
    private ImageView mRocketTrack;
    private RobotoTextView mConnectFailTextView;
    
    private Animation mRocketFireScaleAnimation;
    private Animation mRocketFireTranslateAnimation;
    private Animation mRocketBodyTranslateAnimation;
    private Animation mRocketSmokeAppearAnimation;
    private Animation mRocketTrackAppearAnimation;
    private Animation mRocketSmokeAndTrackDisappearAnimation;
    
    private static final int TIMEOUT_MSG = 1;
    
    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TIMEOUT_MSG:
                    mRocketFireScaleAnimation.cancel();
                    mRocketFire.clearAnimation();
                    mRocketBodyTranslateAnimation.cancel();
                    mRocketBody.clearAnimation();
                    mRocketBody.setVisibility(View.GONE);
                    mRocketFire.setVisibility(View.GONE);
                    
                    mConnectFailTextView.setVisibility(View.VISIBLE);
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
        setContentView(R.layout.activity_waitconnect);
        
        this.mRocketBody = (ImageView) this.findViewById(R.id.rocket_body);
        this.mRocketFire = (ImageView) this.findViewById(R.id.rocket_fire);
        this.mRocketSmoke = (ImageView) this.findViewById(R.id.rocket_smoke);
        this.mRocketTrack = (ImageView) this.findViewById(R.id.rocket_track);
        this.mConnectFailTextView = (RobotoTextView) this.findViewById(R.id.connect_fail_hint);
        
        initAnimation();
        
        mHandler.sendEmptyMessageDelayed(TIMEOUT_MSG, 32 * 1000);
    }
    
    @Override
    protected void refreshEstablish() {
        super.refreshEstablish();
        mRocketFireScaleAnimation.cancel();
    }

    private void initAnimation() {
        this.mRocketFireScaleAnimation = new ScaleAnimation(1.0f, 1.0f, 1.0f, 2.0f, 
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.0f);
        this.mRocketFireScaleAnimation.setDuration(200);
        this.mRocketFireScaleAnimation.setRepeatCount(Animation.INFINITE);
        this.mRocketFireScaleAnimation.setFillAfter(true);
        this.mRocketFireScaleAnimation.setAnimationListener(new AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
                
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                mRocketFire.setImageBitmap(BitmapUtil.scaleBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.rocket_fire), 1.0f, 2.0f));
                initTranslateAnimation();
                mRocketFire.startAnimation(mRocketFireTranslateAnimation);
                mRocketBody.startAnimation(mRocketBodyTranslateAnimation);
                
                mRocketSmoke.setVisibility(View.VISIBLE);
                mRocketTrack.setVisibility(View.VISIBLE);
                mRocketSmoke.startAnimation(mRocketSmokeAppearAnimation);
                mRocketTrack.startAnimation(mRocketTrackAppearAnimation);
            }
        });
        
        this.mRocketSmokeAppearAnimation = new AlphaAnimation(0.1f, 1.0f);
        this.mRocketSmokeAppearAnimation.setDuration(500);
        this.mRocketSmokeAppearAnimation.setRepeatCount(0);
        
        this.mRocketTrackAppearAnimation = new AlphaAnimation(0.1f, 1.0f);
        this.mRocketTrackAppearAnimation.setDuration(500);
        this.mRocketTrackAppearAnimation.setRepeatCount(0);
        this.mRocketTrackAppearAnimation.setStartOffset(50);
        
        this.mRocketSmokeAndTrackDisappearAnimation = new AlphaAnimation(1.0f, 0.0f);
        this.mRocketSmokeAndTrackDisappearAnimation.setInterpolator(new AccelerateInterpolator());
        this.mRocketSmokeAndTrackDisappearAnimation.setDuration(500);
        this.mRocketSmokeAndTrackDisappearAnimation.setRepeatCount(0);
        this.mRocketSmokeAndTrackDisappearAnimation.setAnimationListener(new AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
                
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                mRocketBody.setVisibility(View.GONE);
                mRocketFire.setVisibility(View.GONE);
                mRocketSmoke.setVisibility(View.GONE);
                mRocketTrack.setVisibility(View.GONE);
                
                Intent intent = new Intent(WaitConnectActivity.this, RecordsActivity1.class);
                startActivity(intent);
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_WAIT_SELECT_SUCESS);
                finish();
            }
        });
        
    }
    
    private void initTranslateAnimation() {
        Log.e(TAG, "translate distance is " + (mRocketFire.getY() - mRocketBody.getHeight()));
        this.mRocketFireTranslateAnimation = new TranslateAnimation(0f, 0f, 0f, -(mRocketFire.getY() + mRocketFire.getHeight()));
        this.mRocketFireTranslateAnimation.setFillAfter(true);
        this.mRocketFireTranslateAnimation.setInterpolator(new AccelerateInterpolator());
        this.mRocketFireTranslateAnimation.setDuration(1500);
        this.mRocketFireTranslateAnimation.setRepeatCount(0);
        
        this.mRocketBodyTranslateAnimation = new TranslateAnimation(0f, 0f, 0f, -(mRocketFire.getY() + mRocketFire.getHeight()));
        this.mRocketBodyTranslateAnimation.setFillAfter(true);
        this.mRocketBodyTranslateAnimation.setInterpolator(new AccelerateInterpolator());
        this.mRocketBodyTranslateAnimation.setDuration(1500);
        this.mRocketBodyTranslateAnimation.setRepeatCount(0);
        this.mRocketBodyTranslateAnimation.setAnimationListener(new AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
                
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                mRocketSmoke.startAnimation(mRocketSmokeAndTrackDisappearAnimation);
                mRocketTrack.startAnimation(mRocketSmokeAndTrackDisappearAnimation);
            }
        });
        
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            this.mRocketFire.startAnimation(this.mRocketFireScaleAnimation);
        }
    }

    @Override
    public void onBackPressed() {
        if (this.mConnectFailTextView.getVisibility() == View.VISIBLE) {
            Intent intent = new Intent(WaitConnectActivity.this, MainActivity.class);
            startActivity(intent);
        }
        super.onBackPressed();
    }
}
