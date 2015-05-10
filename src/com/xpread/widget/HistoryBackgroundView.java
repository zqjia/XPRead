package com.xpread.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.TypeEvaluator;

public class HistoryBackgroundView extends View {

    private String mColor;
    private Paint mPaint = new Paint();;
    private int mCenter = -1;
    
    public static final String START_COLOR = "#FF4040";
    public static final String END_COLOR = "#E066FF";
    public static final int COLOR_ANIMATION_DURATION = 2000;
    
    public HistoryBackgroundView(Context context) {
        this(context, null);
    }
    
    public HistoryBackgroundView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public HistoryBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mPaint.setColor(Color.parseColor(START_COLOR));
        this.mPaint.setStyle(Paint.Style.FILL);
    }
    
    public String getColor() {
        return this.mColor;
    }
    
    public void setColor(String color) {
        this.mColor = color;
        this.mPaint.setColor(Color.parseColor(this.mColor));
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mCenter == -1){
            this.mCenter = getWidth()/2;
        }

        canvas.drawCircle(mCenter, mCenter, mCenter, this.mPaint);
    }
    
}
