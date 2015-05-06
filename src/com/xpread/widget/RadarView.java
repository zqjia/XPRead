package com.xpread.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.xpread.R;
import com.xpread.util.LogUtil;

public class RadarView extends View {

    private static final String TAG = "RadarView";

    private Context mContext;

    private Bitmap mBitmap;

    private float mAngle = 0;

    private long mBeginTime = 0;

    private boolean mIsBegin = false;

    public RadarView(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    public RadarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init();
    }

    private void init() {
        this.mBitmap = BitmapFactory.decodeResource(this.mContext.getResources(),
                R.drawable.radar_blue_2);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long drawStartTime = System.currentTimeMillis();
        if (!mIsBegin) {
            mIsBegin = true;
            this.mBeginTime = drawStartTime;
        }

        canvas.save();

        this.mAngle = -(float)((drawStartTime - this.mBeginTime) / 4000.0 * 360);
        this.mAngle %= -360;
        canvas.rotate(this.mAngle, getWidth() / 2, getHeight() / 2);
        drawImage(canvas);
        canvas.restore();

        postInvalidate();
    }

    private void drawImage(Canvas canvas) {

        if (mBitmap == null) {
            if (LogUtil.isLog) {
                Log.e(TAG, "bitmap is null");
            }
        } else {
            canvas.drawBitmap(mBitmap, 0, dip2px(571f / 2 - 154.5f), null);
        }
    }

    private int dip2px(float dipValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

}
