package com.xpread.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;

public class CircleAnimation extends View {
  
    private Context mContext;
    private Paint mPaint;
    private float mInnerRadius = 25f;
    private float mOuterRadius = 67.5f;
    
    public CircleAnimation(Context context) {
        super(context);
        this.mContext = context;
        initPaint();
    }
    
    public CircleAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initPaint();
    }
    
    
    public CircleAnimation(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initPaint();
    }
    
    private void initPaint() {
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        //绘制实心圆
        this.mPaint.setStyle(Paint.Style.FILL);
    }
    
    public void setRadius(int inner, int outer) {
        this.mInnerRadius = inner;
        this.mOuterRadius = outer;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        this.mPaint.setColor(Color.parseColor("#7f0399"));
        canvas.drawCircle(getWidth()/2, getHeight()/2, dip2px(this.mOuterRadius), this.mPaint);
        
        this.mPaint.setColor(Color.WHITE);
        canvas.drawCircle(getWidth()/2, getHeight()/2, dip2px(this.mInnerRadius), this.mPaint);
        
    }
    
    private int dip2px(float dipValue) {
        final float scale = this.mContext.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale +0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        setMeasuredDimension(dip2px(this.mOuterRadius*2), dip2px(this.mOuterRadius*2));
    }
    
}
