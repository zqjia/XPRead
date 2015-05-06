package com.xpread.widget;

import com.xpread.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class ShareViewgroup extends ViewGroup {

    private static final String TAG= "ShareViewgroup";
    
    private Context mContext;
    private int mLayoutWidth;
    private int mLayoutHeight;
    
    
    public ShareViewgroup(Context context) {
        super(context);
        this.mContext = context;
    }
    
    public ShareViewgroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }
    
    public ShareViewgroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        
        int topMargin, bottomMargin;
        topMargin = bottomMargin = (int)(this.mLayoutHeight - this.mContext.getResources().getDimension(R.dimen.activity_share_radius) * 4
                                   - this.mContext.getResources().getDimension(R.dimen.activity_share_gap)) / 2;
        
                
                
        RoundImageButton orCodeButton = (RoundImageButton)getChildAt(0);
        orCodeButton.layout((int) (this.mLayoutWidth/2-this.mContext.getResources().getDimension(R.dimen.activity_share_radius)), 
                            topMargin, 
                            (int)(this.mLayoutWidth/2+this.mContext.getResources().getDimension(R.dimen.activity_share_radius)), 
                            (int)(topMargin + this.mContext.getResources().getDimension(R.dimen.activity_share_radius)*2));
        
        
        RoundImageButton bluetoothButton = (RoundImageButton)getChildAt(1);
        bluetoothButton.layout((int) (this.mLayoutWidth/2-this.mContext.getResources().getDimension(R.dimen.activity_share_radius)), 
                                (int)(this.mLayoutHeight-bottomMargin-this.mContext.getResources().getDimension(R.dimen.activity_share_radius)*2), 
                                (int)(this.mLayoutWidth/2+this.mContext.getResources().getDimension(R.dimen.activity_share_radius)), 
                                this.mLayoutHeight-bottomMargin);
                        
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        int count = getChildCount();
        
        this.mLayoutWidth = getWidth();
        this.mLayoutHeight = getHeight();
        
        for(int i=0; i<count; ++i) {
            MeasureSpec.makeMeasureSpec(this.mLayoutWidth, MeasureSpec.EXACTLY);
            MeasureSpec.makeMeasureSpec(this.mLayoutHeight, MeasureSpec.EXACTLY);
        }
        
        setMeasuredDimension(widthSize, heightSize);
        
    }
    
    public int dip2px(float dipValue) {
        final float scale = this.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale +0.5f);
    }
    
}
