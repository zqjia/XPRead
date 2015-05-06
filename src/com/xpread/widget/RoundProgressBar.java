
package com.xpread.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.xpread.R;

public class RoundProgressBar extends View {

    private static final String TAG = "RoundProgressBar";

    private static final int MAX = 100;

    private Paint mPaint;

    private int mRoundColor;

    private int mProgressColor;

    private float mProgressWidth;

    private int mProgress = 0;

    public RoundProgressBar(Context context) {
        this(context, null);
    }

    public RoundProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundProgressbar);
        this.mRoundColor = typedArray.getColor(R.styleable.RoundProgressbar_roundColor,
                getResources().getColor(R.color.round_progressbar_background));
        this.mProgressColor = typedArray.getColor(R.styleable.RoundProgressbar_roundProgressColor,
                getResources().getColor(R.color.round_progressbar_owner));
        this.mProgressWidth = typedArray.getDimension(R.styleable.RoundProgressbar_roundWidth,
                getResources().getDimension(R.dimen.progressbar_width));

        typedArray.recycle();

        this.mPaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //draw the background
        int center = getWidth() / 2;
        int radius = (int)(center - this.mProgressWidth / 2);
        this.mPaint.setColor(this.mRoundColor);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(this.mProgressWidth);
        this.mPaint.setAntiAlias(true);
        canvas.drawCircle(center, center, radius, this.mPaint);
        
        //draw the arc of the progress
        this.mPaint.setColor(this.mProgressColor);
        this.mPaint.setStrokeWidth(this.mProgressWidth);
        RectF oval = new RectF(center - radius, center - radius, center + radius, center + radius);
        this.mPaint.setStyle(Paint.Style.STROKE);
        if (this.mProgress != 0) {
            canvas.drawArc(oval, -90, -(360 * this.mProgress / MAX), false, this.mPaint);
        }
    }

    public synchronized int getProgress() {
        return this.mProgress;
    }

    public synchronized void setProgress(int progress) {

        if (progress < 0) {
            progress = 0;
        }

        if (progress > MAX) {
            progress = MAX;
        }

        this.mProgress = progress;
        postInvalidate();
    }

}
