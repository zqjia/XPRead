
package com.xpread.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xpread.R;
import com.xpread.util.ScreenUtil;

public class SearchFriendViewgroup extends ViewGroup {

    private static final String TAG = "SearchFriendViewgroup";

    private Context mContext;

    private int mLayoutWidth;

    private int mLayoutHeight;

    private int mFriendViewTop;
    private int mBackGroundTop;

    public SearchFriendViewgroup(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public SearchFriendViewgroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    public SearchFriendViewgroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        init();
    }

    private void init() {
        mFriendViewTop = mContext.getResources().getDimensionPixelSize(R.dimen.search_friend_position_top);
        mBackGroundTop = mContext.getResources().getDimensionPixelSize(R.dimen.search_friend_background_top);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        this.mLayoutWidth = getWidth();
        this.mLayoutHeight = getHeight();

        // 底部圆圈
        View circleBackgroud = getChildAt(0);
        circleBackgroud.layout(0, mBackGroundTop, this.mLayoutWidth, this.mLayoutHeight + mBackGroundTop);

        View radarView = getChildAt(1);
        radarView.layout(this.mLayoutWidth / 2 - dip2px(571 / (float)2), dip2px(113)
                - dip2px(154.5f) + mBackGroundTop, this.mLayoutWidth / 2, dip2px(113) + mBackGroundTop);

        // 用户头像
        View userIcon = getChildAt(2);
        userIcon.layout(this.mLayoutWidth / 2 - dip2px(55), dip2px(58) + mBackGroundTop, this.mLayoutWidth / 2
                + dip2px(55), dip2px(168) + mBackGroundTop);

        FriendViewgroup friendViewgroup = (FriendViewgroup)getChildAt(3);
        friendViewgroup.layout(0, mFriendViewTop, this.mLayoutWidth, this.mLayoutHeight);

        // 这里的控件位置可能需要改动
        ImageView searchCancel = (ImageView)getChildAt(4);
        searchCancel.layout(this.mLayoutWidth / 2 - dip2px(15f), this.mLayoutHeight - dip2px(45),
                this.mLayoutWidth / 2 + dip2px(15f), this.mLayoutHeight - dip2px(15));

        ImageView circleBlue1 = (ImageView)getChildAt(5);
        circleBlue1.layout(this.mLayoutWidth / 2 - dip2px(40), dip2px(228) + mBackGroundTop, this.mLayoutWidth / 2
                - dip2px(20), dip2px(248) + mBackGroundTop);

        ImageView circleBlue2 = (ImageView)getChildAt(6);
        circleBlue2.layout(this.mLayoutWidth - dip2px(40), dip2px(133) + mBackGroundTop, this.mLayoutWidth
                - dip2px(20), dip2px(153) + mBackGroundTop);

        ImageView circleBlue3 = (ImageView)getChildAt(7);
        circleBlue3.layout(this.mLayoutWidth / 2 + dip2px(25), dip2px(25) + mBackGroundTop, this.mLayoutWidth / 2
                + dip2px(45), dip2px(45) + mBackGroundTop);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int count = getChildCount();

        int screenWidth = ScreenUtil.getScreenWidth(this.mContext);
        int height = ScreenUtil.getScreenHeight(this.mContext);

        for (int i = 0; i < count; ++i) {

            MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY);
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    private int dip2px(float dipValue) {
        final float scale = this.mContext.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

}
