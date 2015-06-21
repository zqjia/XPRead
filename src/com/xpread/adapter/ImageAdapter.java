package com.xpread.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.xpread.R;
import com.xpread.provider.ImageGridItem;
import com.xpread.util.Utils;

public class ImageAdapter extends CustomAdapter<ImageGridItem> {

    private static final String TAG = ImageAdapter.class.getSimpleName();
    private ArrayList<ImageGridItem> mImageList;
    private DisplayImageOptions mOptions = Utils.getDisplayOptions(R.drawable.classimage);

    public ImageAdapter(Context context, List<ImageGridItem> dataList, int itemLayoutId) {
        super(context, dataList, itemLayoutId);
        this.mImageList = (ArrayList<ImageGridItem>) dataList;
    }

    @Override
    public void convert(CustomViewHolder viewHolder, ImageGridItem item) {
        ImageView thumbnail = (ImageView) viewHolder.getView(R.id.image_thumbnail);
        ImageView selectedIcon = (ImageView) viewHolder.getView(R.id.image_select_icon);
        View shadow = (View) viewHolder.getView(R.id.image_shadow);
        
        if (item.getThumbPath() != null) {
            ImageLoader.getInstance().displayImage(Scheme.FILE.wrap(item.getThumbPath()),
                thumbnail, mOptions, null);
        } else {
            thumbnail.setImageBitmap(item.getThumbBmp());
        }
        
        selectedIcon.setVisibility(item.getIsSelected() ? View.VISIBLE : View.GONE);
        shadow.setVisibility(item.getIsSelected() ? View.VISIBLE : View.GONE);
    }

    public void setData(ArrayList<ImageGridItem> dataList) {
        this.mImageList = dataList;
        notifyDataSetChanged();
    }

    public void updateView(int position, GridView imageGridView) {
        int visiblePosition = imageGridView.getFirstVisiblePosition();
        View view = imageGridView.getChildAt(position - visiblePosition);
        CustomViewHolder viewHolder = (CustomViewHolder) view.getTag();

        final ImageGridItem item = (ImageGridItem) getItem(position);
        viewHolder.getView(R.id.image_select_icon).setVisibility(item.getIsSelected() ? View.VISIBLE : View.GONE);
        viewHolder.getView(R.id.image_shadow).setVisibility(item.getIsSelected() ? View.VISIBLE : View.GONE);
    }

}
