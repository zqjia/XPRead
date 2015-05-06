


package com.ucweb.xpread.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ucweb.xpread.util.ScreenUtil;

public class SearchFriendViewgroup extends ViewGroup {

    private static final String TAG = "SearchFriendViewgroup";
    
    private Context mContext;
    
    private int mScreenWidth;
    private int mScreenHeight;
    
    public SearchFriendViewgroup(Context context) {
        super(context);
        this.mContext = context;
    }
    
    public SearchFriendViewgroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }
    
    public SearchFriendViewgroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        //底部圆圈
        View circleBackgroud = getChildAt(0);
        circleBackgroud.layout(0, dip2px(-50), this.mScreenWidth, this.mScreenHeight);
        
        //用户头像
        View userIcon = getChildAt(1);
        userIcon.layout(this.mScreenWidth/2-dip2px(75), this.mScreenWidth/2-dip2px(110), 
                this.mScreenWidth/2+dip2px(75), this.mScreenWidth/2+dip2px(40));
        
        //用户名称
        TextView userName =(TextView) getChildAt(2);
        userName.layout(this.mScreenWidth/2-dip2px(75), this.mScreenWidth/2+dip2px(50),
                this.mScreenWidth/2+dip2px(75), this.mScreenWidth/2+dip2px(90));
        
        
        //朋友1
        View friendOneIcon = getChildAt(3);
        friendOneIcon.layout(dip2px(25), this.mScreenWidth/2+dip2px(95),
                dip2px(105), this.mScreenWidth/2+dip2px(175));
        
        TextView friendOneName = (TextView)getChildAt(4);
        friendOneName.layout(dip2px(20), this.mScreenWidth/2+dip2px(180), 
                dip2px(110), this.mScreenWidth/2+dip2px(205));
        
        //朋友2
        View friendTwoIcon = getChildAt(5);
        friendTwoIcon.layout(this.mScreenWidth/2-dip2px(40), this.mScreenWidth/2+dip2px(95), 
                this.mScreenWidth/2+dip2px(40), this.mScreenWidth/2+dip2px(175));
        
        TextView friendTwoName = (TextView)getChildAt(6);
        friendTwoName.layout(this.mScreenWidth/2-dip2px(45), this.mScreenWidth/2+dip2px(180), 
                this.mScreenWidth/2+dip2px(45), this.mScreenWidth/2+dip2px(205));
        
        //朋友3
        View friendThreeIcon = getChildAt(7);
        friendThreeIcon.layout(this.mScreenWidth/2+dip2px(75), this.mScreenWidth/2+dip2px(95),
                this.mScreenWidth/2+dip2px(155), this.mScreenWidth/2+dip2px(175));
        
        TextView friendThreeName = (TextView)getChildAt(8);
        friendThreeName.layout(this.mScreenWidth/2+dip2px(70), this.mScreenWidth/2+dip2px(180),
                this.mScreenWidth/2 + dip2px(160), this.mScreenWidth/2+dip2px(205));
        
        //朋友4 
        View friendFourIcon = getChildAt(9);
        friendFourIcon.layout(dip2px(25), this.mScreenWidth/2+dip2px(220), 
                dip2px(105), this.mScreenWidth/2+dip2px(300));
        
        TextView friendFourName = (TextView)getChildAt(10);
        friendFourName.layout(dip2px(20), this.mScreenWidth/2+dip2px(305), 
                dip2px(110), this.mScreenWidth/2+dip2px(330));
        
        //朋友5 
        View friendfiveIcon = getChildAt(11);
        friendfiveIcon.layout(this.mScreenWidth/2-dip2px(40), this.mScreenWidth/2+dip2px(220), 
                this.mScreenWidth/2 + dip2px(40), this.mScreenWidth/2+dip2px(300));
        
        TextView friendFiveName = (TextView)getChildAt(12);
        friendFiveName.layout(this.mScreenWidth/2-dip2px(45), this.mScreenWidth/2+dip2px(305), 
                this.mScreenWidth/2 + dip2px(45), this.mScreenWidth/2+dip2px(330));
        
        //朋友6
        View friendSixIcon = getChildAt(13);
        friendSixIcon.layout(this.mScreenWidth/2 + dip2px(75), this.mScreenWidth/2 + dip2px(220), 
                this.mScreenWidth/2 + dip2px(155), this.mScreenWidth/2 + dip2px(300));
        
        TextView friendSixName = (TextView)getChildAt(14);
        friendSixName.layout(this.mScreenWidth/2 + dip2px(70), this.mScreenWidth/2 + dip2px(305), 
                this.mScreenWidth/2 + dip2px(160), this.mScreenWidth/2 + dip2px(330));
        
        
        
        
       /* //底部圆圈
        View circleBackgroud = getChildAt(0);
        circleBackgroud.layout(0, 0, this.mScreenWidth, this.mScreenHeight);
        
        //用户头像
        View userIcon = getChildAt(1);
        userIcon.layout(this.mScreenWidth/2-dip2px(75), this.mScreenWidth/2-dip2px(75), 
                this.mScreenWidth/2+dip2px(75), this.mScreenWidth/2+dip2px(75));
        
        //用户名称
        TextView userName =(TextView) getChildAt(2);
        userName.layout(this.mScreenWidth/2-dip2px(75), this.mScreenWidth/2+dip2px(100),
                this.mScreenWidth/2+dip2px(75), this.mScreenWidth/2+dip2px(140));
        userName.setText("Sara");
        
        //朋友1
        View friendOneIcon = getChildAt(3);
        friendOneIcon.layout(this.mScreenWidth/2-dip2px(40), this.mScreenWidth/2+dip2px(160),
                this.mScreenWidth/2+dip2px(40), this.mScreenWidth/2+dip2px(240));
        
        TextView friendOneName = (TextView)getChildAt(4);
        friendOneName.layout(this.mScreenWidth/2-dip2px(40), this.mScreenWidth/2+dip2px(245), 
                this.mScreenWidth/2+dip2px(40), this.mScreenWidth/2+dip2px(275));
        
        //朋友2
        View friendTwoIcon = getChildAt(5);
        friendTwoIcon.layout(dip2px(20), this.mScreenWidth/2+dip2px(60), 
                dip2px(90), this.mScreenWidth/2+dip2px(130));
        
        TextView friendTwoName = (TextView)getChildAt(6);
        friendTwoName.layout(dip2px(20), this.mScreenWidth/2+dip2px(135), 
                dip2px(90), this.mScreenWidth+dip2px(160));
        
        //朋友3
        View friendThreeIcon = getChildAt(7);
        friendThreeIcon.layout(this.mScreenWidth/2 - dip2px(40), dip2px(10),
                this.mScreenWidth/2+dip2px(20), dip2px(70));
        
        TextView friendThreeName = (TextView)getChildAt(8);
        friendThreeName.layout(this.mScreenWidth/2 - dip2px(40), dip2px(75),
                this.mScreenWidth/2 + dip2px(20), this.mScreenWidth/2+dip2px(95));
        
        //朋友4 
        View friendFourIcon = getChildAt(9);
        friendFourIcon.layout(dip2px(40), dip2px(80), dip2px(90), dip2px(130));
        
        TextView friendFourName = (TextView)getChildAt(10);
        friendFourName.layout(dip2px(40), dip2px(135), dip2px(90), dip2px(155));
        
        //朋友5 
        View friendfiveIcon = getChildAt(11);
        friendfiveIcon.layout(this.mScreenWidth/2 + dip2px(105), dip2px(100), this.mScreenWidth/2 + dip2px(165), dip2px(160));
        
        TextView friendFiveName = (TextView)getChildAt(12);
        friendFiveName.layout(this.mScreenWidth/2 + dip2px(105), dip2px(165), 
                this.mScreenWidth/2 + dip2px(165), dip2px(190));
        
        //朋友6
        View friendSixIcon = getChildAt(13);
        friendSixIcon.layout(this.mScreenWidth/2 + dip2px(75), this.mScreenWidth/2 + dip2px(70), 
                this.mScreenWidth/2 + dip2px(145), this.mScreenWidth/2 + dip2px(140));
        
        TextView friendSixName = (TextView)getChildAt(14);
        friendSixName.layout(this.mScreenWidth/2 + dip2px(75), this.mScreenWidth/2 + dip2px(145), 
                this.mScreenWidth/2 + dip2px(145), this.mScreenWidth/2 + dip2px(175));*/
    }
    
    
    
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        int count = getChildCount();
        
        this.mScreenWidth = ScreenUtil.getScreenWidth(getContext());
        this.mScreenHeight = ScreenUtil.getScreenHeight(getContext());
        
        for(int i=0; i<count; ++i) {
           
           MeasureSpec.makeMeasureSpec(this.mScreenWidth, MeasureSpec.EXACTLY);
           MeasureSpec.makeMeasureSpec(this.mScreenHeight, MeasureSpec.EXACTLY);
        }
        
        setMeasuredDimension(widthSize, heightSize);
    }
    
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(pxValue / scale +0.5f); 
    }
    
    public int dip2px(float dipValue) {
        final float scale = this.mContext.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale +0.5f);
    }

}
