package com.xpread.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.xpread.R;
import com.xpread.widget.RoundImageView;

public class IconAdapter extends BaseAdapter {

    private LayoutInflater mLayoutInflater;

    private List<Drawable> mList;

    public IconAdapter(Context context, List<Drawable> list) {
        mLayoutInflater = LayoutInflater.from(context);
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.icon_item, parent, false);
        }

        RoundImageView icon = (RoundImageView) convertView.findViewById(R.id.icon);
        icon.setImageDrawable(mList.get(position));

        return convertView;
    }

}
