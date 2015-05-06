package com.xpread.widget;

import com.xpread.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CircleBackgroudView extends View {

    private static final String TAG = "CircleBackgroudView";
    
    private Paint mPaint;
    private Context mContext;
    
    private final float RADIUS_ONE = 108;
    private final float RADIUS_TWO = 162;
    private final float RADIUS_THREE = 243;
    private final float RADIUS_FOUR = 571/(float)2;
   
    
    public CircleBackgroudView(Context context) {
        super(context);
        this.mContext = context;
        
        initPaint();
    }
    
    
    public CircleBackgroudView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        
        initPaint();
    }
    
    private void initPaint() {
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        //绘制实心圆
        this.mPaint.setStyle(Paint.Style.FILL);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int center = getWidth() / 2;
        
        this.mPaint.setColor(this.mContext.getResources().getColor(R.color.background_circle_four_color));
        canvas.drawCircle(center, dip2px(113), dip2px(RADIUS_FOUR), this.mPaint);
        
        this.mPaint.setColor(this.mContext.getResources().getColor(R.color.background_circle_three_color));
        canvas.drawCircle(center, dip2px(113), dip2px(RADIUS_THREE), this.mPaint);
        
        this.mPaint.setColor(this.mContext.getResources().getColor(R.color.background_circle_two_color));
        canvas.drawCircle(center, dip2px(113), dip2px(RADIUS_TWO), this.mPaint);
        
        this.mPaint.setColor(this.mContext.getResources().getColor(R.color.background_circle_one_color));
        canvas.drawCircle(center, dip2px(113), dip2px(RADIUS_ONE), this.mPaint);
        
    }
    
    public int dip2px(float dpValue) {  
        final float scale = this.mContext.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }  
    
}
