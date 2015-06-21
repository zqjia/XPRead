package com.xpread.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CustomViewHolder {

    private SparseArray<View> mViews;
    private View mConvertView;

    private CustomViewHolder(Context context, ViewGroup parent, int layoutId, int position) {
        this.mViews = new SparseArray<View>();
        this.mConvertView = LayoutInflater.from(context).inflate(layoutId, parent, false);
        this.mConvertView.setTag(this);
    }

    public static CustomViewHolder getViewHolder(Context context, View convertView,
            ViewGroup parent, int layoutId, int position) {
        if (convertView == null) {
            return new CustomViewHolder(context, parent, layoutId, position);
        } 
        
        return (CustomViewHolder) convertView.getTag();
    }
    
    @SuppressWarnings("unchecked")
    public <T extends View> T getView(int viewId) {
        
        View view = mViews.get(viewId);
        if (view == null) {
            view = mConvertView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        
        return (T)view;
    }
    
    public View getConvertView() {
        return mConvertView;
    }
}
