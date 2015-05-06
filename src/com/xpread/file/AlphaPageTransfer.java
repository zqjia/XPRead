package com.xpread.file;

import android.support.v4.view.ViewPager;
import android.view.View;

public class AlphaPageTransfer implements ViewPager.PageTransformer{

    private static final float MIN_ALPHA = 0.3f;
    
    /*
     * page Apply the transformation to this page
     * position     Position of page relative to the current front-and-center 
     *              position of the pager. 
     *              0 is front and center. 
     *              1 is one full page position to the right, 
     *              and -1 is one page position to the left.
     * */
    @Override
    public void transformPage(View view, float position) {
        
        if (position < -1) {
            view.setAlpha(0f);
        } else if (position <= 1) {
            float scaleFactor = Math.max(MIN_ALPHA, 1 - Math.abs(position));
            view.setAlpha(MIN_ALPHA + 
                (scaleFactor - MIN_ALPHA)/(1-MIN_ALPHA)*(1-MIN_ALPHA));
        } else {
            view.setAlpha(0f);
        }
    }

}
