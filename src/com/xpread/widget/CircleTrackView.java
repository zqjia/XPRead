package com.xpread.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.xpread.R;

public class CircleTrackView extends View{

    private int mCircleTrackColor;
    private Paint mPaint;
    
    public CircleTrackView(Context context) {
       this(context, null);
    }
    
    public CircleTrackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public CircleTrackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleTrack);
        this.mCircleTrackColor = typedArray.getColor(R.styleable.CircleTrack_trackColor, android.R.color.white);
        
        typedArray.recycle();
        
        this.mPaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int center = getWidth()/2;
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setColor(this.mCircleTrackColor);
        canvas.drawCircle(center, center, center, this.mPaint);
        
    }
    
    
    
    
}
