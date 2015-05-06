package com.xpread.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xpread.util.LogUtil;

public class FriendViewgroup extends ViewGroup {

    private static final String TAG = "FriendViewroup";
    
    private Context mContext;
    private int mWidth;
    
    public FriendViewgroup(Context context) {
        super(context);
        this.mContext = context;
    }
    
    public FriendViewgroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }
    
    public FriendViewgroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        
        this.mWidth = getWidth();
        int leftMargin, rightMargin;
        
        leftMargin = rightMargin = (this.mWidth - 3*dip2px(80) - dip2px(30)*2)/2;
        
        if(LogUtil.isLog) {
            Log.e(TAG, "left and right margin is " + leftMargin);
            Log.e(TAG, "friend header size is " + dip2px(80));
            Log.e(TAG, "the gap between friend is " + dip2px(30));
        }
        
        //朋友1
        View friendOneIcon = getChildAt(0);
        friendOneIcon.layout(leftMargin, dip2px(30),
                leftMargin + dip2px(80), dip2px(110));
        
        TextView friendOneName = (TextView)getChildAt(1);
        friendOneName.layout(leftMargin - dip2px(3), dip2px(118), 
                leftMargin + dip2px(83), dip2px(143));
        
        //朋友2
        View friendTwoIcon = getChildAt(2);
        friendTwoIcon.layout(this.mWidth/2-dip2px(40), dip2px(30), 
                this.mWidth/2+dip2px(40), dip2px(110));
        
        TextView friendTwoName = (TextView)getChildAt(3);
        friendTwoName.layout(this.mWidth/2-dip2px(43), dip2px(118), 
                this.mWidth/2+dip2px(43), dip2px(143));
        
        //朋友3
        View friendThreeIcon = getChildAt(4);
        friendThreeIcon.layout(this.mWidth-dip2px(80)-rightMargin, dip2px(30),
                this.mWidth-rightMargin, dip2px(110));
        
        TextView friendThreeName = (TextView)getChildAt(5);
        friendThreeName.layout(this.mWidth-dip2px(83)-rightMargin, dip2px(118),
                this.mWidth-rightMargin+dip2px(3), dip2px(143));

        //朋友4 
        View friendFourIcon = getChildAt(6);
        friendFourIcon.layout(leftMargin, dip2px(163),
                leftMargin + dip2px(80), dip2px(243));
        
        TextView friendFourName = (TextView)getChildAt(7);
        friendFourName.layout(leftMargin - dip2px(3), dip2px(251), 
                leftMargin + dip2px(83), dip2px(276));
        
        //朋友5 
        View friendfiveIcon = getChildAt(8);
        friendfiveIcon.layout(this.mWidth/2-dip2px(40), dip2px(163), 
                this.mWidth/2+dip2px(40), dip2px(243));
        
        TextView friendFiveName = (TextView)getChildAt(9);
        friendFiveName.layout(this.mWidth/2-dip2px(43), dip2px(251), 
                this.mWidth/2+dip2px(43), dip2px(276));
        
        //朋友6
        View friendSixIcon = getChildAt(10);
        friendSixIcon.layout(this.mWidth-dip2px(80)-rightMargin, dip2px(163),
                this.mWidth-rightMargin, dip2px(243));
        
        TextView friendSixName = (TextView)getChildAt(11);
        friendSixName.layout(this.mWidth-dip2px(83)-rightMargin, dip2px(251),
                this.mWidth-rightMargin+dip2px(3), dip2px(276));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        int count = getChildCount();
        
        for(int i=0; i<count; ++i) {
            MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);
            MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY);
        }
        
        setMeasuredDimension(widthSize, heightSize);
    }
    
    private int dip2px(float dipValue) {
        final float scale = this.mContext.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale +0.5f);
    }
    
    

}
