package com.xpread.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.twotoasters.jazzylistview.JazzyListView;
import com.xpread.R;
import com.xpread.provider.MusicInfo;
import com.xpread.util.Const;

public class MusicJazzyAdapter extends BaseAdapter {

    private ArrayList<MusicInfo> mMusicList;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private JazzyListView mMusicListView;
    
    public MusicJazzyAdapter(Context context, ArrayList<MusicInfo> list, JazzyListView listView) {
        this.mContext = context;
        this.mMusicList = list;
        this.mMusicListView = listView;
        this.mLayoutInflater = LayoutInflater.from(mContext);
    }
    
    public void setData(ArrayList<MusicInfo> list) {
        this.mMusicList = list;
        notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return this.mMusicList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mMusicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = this.mLayoutInflater.inflate(R.layout.music_item, parent, false);
            viewHolder.cover = (ImageView)convertView.findViewById(R.id.music_cover);
            viewHolder.title = (TextView)convertView.findViewById(R.id.music_title);
            viewHolder.singer = (TextView)convertView.findViewById(R.id.music_singer);
            viewHolder.size = (TextView)convertView.findViewById(R.id.music_size);
            viewHolder.check = (ImageView)convertView.findViewById(R.id.music_check);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        
        final MusicInfo info = mMusicList.get(position);
        viewHolder.cover.setImageResource(R.drawable.music);
        viewHolder.title.setText(info.getTitle());
        viewHolder.singer.setText(info.getSinger());

        float size = info.getSize() / Const.KILO;

        if (size > Const.KILO) {
            size /= Const.KILO;
            viewHolder.size.setText(String.format(mContext.getResources().getString(R.string.size_MB), size));
        } else {
            viewHolder.size.setText(String.format(mContext.getResources().getString(R.string.size_KB), (int)size));
        }

        viewHolder.check.setImageResource(info.getIsSelected() ? R.drawable.check : R.drawable.checkbox);
        return convertView;
    }
    
    public void updateView(int itemIndex) {
        int visiblePosition = mMusicListView.getFirstVisiblePosition();
        View view = mMusicListView.getChildAt(itemIndex - visiblePosition);
        final MusicInfo info = mMusicList.get(itemIndex);
        
        ViewHolder viewHolder = (ViewHolder)view.getTag();
        viewHolder.check.setImageResource(info.getIsSelected() ? R.drawable.check
                : R.drawable.checkbox);
    }

    private static class ViewHolder {
        ImageView cover;;
        TextView title;
        TextView singer;
        TextView size;
        ImageView check;
    }
}
