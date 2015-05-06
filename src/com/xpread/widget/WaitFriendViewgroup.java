package com.xpread.widget;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.xpread.R;
import com.xpread.util.LogUtil;

public class WaitFriendViewgroup extends ViewGroup {

    private static final String TAG = "WaitFriendViewgroup";

    private Context mContext;

    private int mLayoutWidth;

    private int mLayoutHeight;

    private int mBackGroundTop;

    public WaitFriendViewgroup(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public WaitFriendViewgroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    public WaitFriendViewgroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        init();
    }
    
    private void init() {
        mBackGroundTop = mContext.getResources().getDimensionPixelSize(R.dimen.search_friend_background_top);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        this.mLayoutWidth = getWidth();
        this.mLayoutHeight = getHeight();

        if (LogUtil.isLog) {
            Log.e(TAG, "the waitfriend viewgroup width and height is :" + this.mLayoutWidth + " "
                    + this.mLayoutHeight);
        }

        // 底部圆圈
        View circleBackgroud = getChildAt(0);
        circleBackgroud.layout(0, mBackGroundTop, this.mLayoutWidth, this.mLayoutHeight + mBackGroundTop);

        View radarView = getChildAt(1);
        radarView.layout(this.mLayoutWidth / 2 - dip2px(571 / (float)2), dip2px(113)
                - dip2px(154.5f) + mBackGroundTop, this.mLayoutWidth / 2, dip2px(113) + mBackGroundTop);

        /*
         * View radarView = getChildAt(1);
         * radarView.layout(this.mLayoutWidth/2-dip2px(571/(float)2),
         * dip2px(113)-dip2px(571f/2), this.mLayoutWidth/2+dip2px(571f/2),
         * dip2px(113)+dip2px(571f/2));
         */

        // 用户头像
        View userIcon = getChildAt(2);
        userIcon.layout(this.mLayoutWidth / 2 - dip2px(55), dip2px(58) + mBackGroundTop, this.mLayoutWidth / 2
                + dip2px(55), dip2px(168) + mBackGroundTop);

        TextView waitHintBegin = (TextView)getChildAt(3);
        waitHintBegin.layout(0, this.mLayoutHeight - dip2px(236), this.mLayoutWidth,
                this.mLayoutHeight - dip2px(176));

        TextView waitHintReceive = (TextView)getChildAt(4);
        waitHintReceive.layout(this.mLayoutWidth / 2 - dip2px(120), this.mLayoutHeight
                - dip2px(226), this.mLayoutWidth / 2 + dip2px(120), this.mLayoutHeight
                - dip2px(186));

        ImageView waitCancel = (ImageView)getChildAt(5);
        waitCancel.layout(this.mLayoutWidth / 2 - dip2px(20f), this.mLayoutHeight - dip2px(95),
                this.mLayoutWidth / 2 + dip2px(20f), this.mLayoutHeight - dip2px(55));

        ImageView circleBlue1 = (ImageView)getChildAt(6);
        circleBlue1.layout(this.mLayoutWidth / 2 - dip2px(40), dip2px(228) + mBackGroundTop, this.mLayoutWidth / 2
                - dip2px(20), dip2px(248) + mBackGroundTop);

        ImageView circleBlue2 = (ImageView)getChildAt(7);
        circleBlue2.layout(this.mLayoutWidth - dip2px(40), dip2px(133) + mBackGroundTop, this.mLayoutWidth
                - dip2px(20), dip2px(153) + mBackGroundTop);

        ImageView circleBlue3 = (ImageView)getChildAt(8);
        circleBlue3.layout(this.mLayoutWidth / 2 + dip2px(25), dip2px(25) + mBackGroundTop, this.mLayoutWidth / 2
                + dip2px(45), dip2px(45) + mBackGroundTop);
    }

    private int dip2px(float dipValue) {
        final float scale = this.mContext.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int count = getChildCount();

        for (int i = 0; i < count; ++i) {

            MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);
            MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

}
