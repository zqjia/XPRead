package com.xpread.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.xpread.R;
import com.xpread.provider.ImageGridItem;
import com.xpread.stickygridheaders.StickyGridHeadersSimpleAdapter;

public class ImageStickyGridAdapter extends BaseAdapter 
        implements StickyGridHeadersSimpleAdapter {

    private ArrayList<ImageGridItem> mImageList;
    private LayoutInflater mLayoutInflater;
    private GridView mImageGridView;
    private Context mContext;
    
    private DisplayImageOptions mOptions = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.classimage).showImageForEmptyUri(R.drawable.classimage)
            .showImageOnFail(R.drawable.classimage).cacheInMemory(true).cacheOnDisk(true)
            .considerExifParams(true).imageScaleType(ImageScaleType.IN_SAMPLE_INT)
            .bitmapConfig(Bitmap.Config.RGB_565).build();

    public ImageStickyGridAdapter(Context context, ArrayList<ImageGridItem> imageList, GridView imageGridView) {
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mImageList = imageList;
        this.mImageGridView = imageGridView;
        this.mContext = context;
    }

    public void setData(ArrayList<ImageGridItem> list) {
        this.mImageList = list;
        notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return this.mImageList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mImageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.image_item, parent, false);

            viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.image_thumbnail);
            viewHolder.imageSelectIcon =
                    (ImageView) convertView.findViewById(R.id.image_select_icon);
            viewHolder.shadowView = (View) convertView.findViewById(R.id.shadow);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final ImageGridItem item = (ImageGridItem) getItem(position);
        if (item.getThumbPath() != null) {
            ImageLoader.getInstance().displayImage(Scheme.FILE.wrap(item.getThumbPath()),
                    viewHolder.thumbnail, mOptions, null);
        } else if (item.getThumbBmp() != null) {
            viewHolder.thumbnail.setImageBitmap(item.getThumbBmp());
        } else {
            viewHolder.thumbnail.setImageResource(R.drawable.classimage);
        }

        viewHolder.imageSelectIcon.setVisibility(item.getIsSelected() ? View.VISIBLE : View.GONE);
        viewHolder.shadowView.setVisibility(item.getIsSelected() ? View.VISIBLE : View.GONE);

        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        return mImageList.get(position).getHeaderId();
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder headerViewHolder;
        
        if (convertView == null) {
            headerViewHolder = new HeaderViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.image_header, parent, false);
            headerViewHolder.headerText = (TextView)convertView.findViewById(R.id.image_header);
            convertView.setTag(headerViewHolder);
        } else {
            headerViewHolder = (HeaderViewHolder)convertView.getTag();
        }
        
        headerViewHolder.headerText.setText(mContext.getResources().getString(R.string.image_header_prefix) 
            + mImageList.get(position).getBucketName());
        
        return convertView;
    }
    
    public void updateView(int itemIndex) {
        int visiblePosition = mImageGridView.getFirstVisiblePosition();
        View view = mImageGridView.getChildAt(itemIndex - visiblePosition);
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        final ImageGridItem item = (ImageGridItem) getItem(itemIndex);
        viewHolder.imageSelectIcon.setVisibility(item.getIsSelected() ? View.VISIBLE : View.GONE);
        viewHolder.shadowView.setVisibility(item.getIsSelected() ? View.VISIBLE : View.GONE);
    }

    static class ViewHolder {
        ImageView thumbnail;
        ImageView imageSelectIcon;
        View shadowView;
    }

    static class HeaderViewHolder {
        TextView headerText;
    }
}
