package com.xpread.adapter;

import java.util.List;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


public abstract class CustomAdapter<T> extends BaseAdapter {

    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private List<T> mDataList;
    protected int mItemLayoutId;
    
    public abstract void convert(CustomViewHolder viewHolder, T item);
    
    public CustomAdapter(Context context, List<T> dataList, int itemLayoutId) {
        mLayoutInflater = LayoutInflater.from(context);
        this.mContext = context;
        this.mDataList = dataList;
        this.mItemLayoutId = itemLayoutId;
    }

    @Override
    public int getCount() {
        return mDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CustomViewHolder viewHolder = CustomViewHolder
                .getViewHolder(mContext, convertView, parent, mItemLayoutId, position);
        convert(viewHolder, (T)getItem(position));
        return viewHolder.getConvertView();
    }
    


}
