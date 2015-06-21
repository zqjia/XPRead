package com.xpread.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.xpread.R;
import com.xpread.provider.VideoInfo;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;
import com.xpread.util.Utils;

public class VideoAdapter extends BaseAdapter {

    private static final String TAG = "VideoAdapter";

    private ArrayList<VideoInfo> mVideoList;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private ListView mVideoListView;
    private DisplayImageOptions mVideoOptions = Utils.getDisplayOptions(R.drawable.video);

    public VideoAdapter(ArrayList<VideoInfo> list, Context ctx, ListView listView) {
        this.mVideoList = list;
        this.mContext = ctx;
        this.mLayoutInflater = LayoutInflater.from(mContext);
        this.mVideoListView = listView;
    }

    public void setData(ArrayList<VideoInfo> list) {
        this.mVideoList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return this.mVideoList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mVideoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder = null;

        if (convertView == null) {
            holder = new Holder();
            convertView = mLayoutInflater.inflate(R.layout.video_item, parent, false);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
            holder.title = (TextView) convertView.findViewById(R.id.video_title);
            holder.duration = (TextView) convertView.findViewById(R.id.video_duration);
            holder.size = (TextView) convertView.findViewById(R.id.video_size);
            holder.check = (ImageView) convertView.findViewById(R.id.video_check);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        final VideoInfo info = this.mVideoList.get(position);
        if (info != null) {
            if (info.getVideoThumbData() != null) {
                ImageLoader.getInstance().displayImage(Scheme.FILE.wrap(info.getVideoThumbData()),
                        holder.thumbnail, mVideoOptions);
            } else if (info.getVideoThumbBmp() != null) {
                holder.thumbnail.setImageBitmap(info.getVideoThumbBmp());
            } else {
                holder.thumbnail.setImageResource(R.drawable.video);
            }

            holder.title.setText(info.getVideoTitle());
            holder.duration.setText(DateUtils.formatElapsedTime(info.getVideoDuration() / 1000));

            String videoSize = Utils.getFileSizeForDisplay(info.getVideoSize());
            holder.size.setText(videoSize);
            
            holder.check.setImageResource(info.getIsSelected() ? R.drawable.check : R.drawable.checkbox);
        } else {
            if (LogUtil.isLog) {
                Log.e(TAG, "video info of this item is null");
            }
        }

        return convertView;
    }

    public void updateView(int itemIndex) {
        int visiblePosition = mVideoListView.getFirstVisiblePosition();
        View view = mVideoListView.getChildAt(itemIndex - visiblePosition);
        final VideoInfo info = (VideoInfo) getItem(itemIndex);
        Holder holder = (Holder) view.getTag();
        holder.check.setImageResource(info.getIsSelected() ? R.drawable.check : R.drawable.checkbox);
    }

    public class Holder {
        ImageView thumbnail;
        TextView title;
        TextView duration;
        TextView size;
        ImageView check;
    }

}
