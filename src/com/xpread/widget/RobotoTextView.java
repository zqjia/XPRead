
package com.xpread.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.xpread.R;

/**
 * @Title: RobotoTextView.java
 * @Package com.xpread.widget
 * @Description: 带有Roboto字体的TextView，默认是 Roboto-Light
 * @author zhanhl@ucweb.com
 * @date 2014-12-30 下午3:19:31
 * @version V1.0
 */
public class RobotoTextView extends TextView {

    private static final int LIGHT = 0;

    private static final int REGULAR = 1;

    public RobotoTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
        setCustomAttributes(attrs);
    }

    public RobotoTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        setCustomAttributes(attrs);
    }

    public RobotoTextView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    private void setCustomAttributes(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.RobotoTextView);

        int textType = ta.getInt(R.styleable.RobotoTextView_textfont, LIGHT);

        Typeface mTypeface = null;
        switch (textType) {
            case REGULAR:
                mTypeface = Typeface.createFromAsset(getContext().getAssets(),
                        "fonts/Roboto-Regular.ttf");
                break;
            case LIGHT:
                mTypeface = Typeface.createFromAsset(getContext().getAssets(),
                        "fonts/Roboto-Light.ttf");
                break;
            default:
                break;
        }

        if (mTypeface != null) {
            setTypeface(mTypeface);
        }

        ta.recycle();
    }

}
