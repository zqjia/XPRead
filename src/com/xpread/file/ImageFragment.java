/**
 * <p>Title: xpread</p>
 *
 * <p>Description: </p>
 * 图片资源选择界面
 * <p>Copyright: Copyright (c) 2014</p>
 *
 * <p>Company: ucweb.com</p>
 *
 * @author jiazq@ucweb.com
 * @version 1.0
 */

package com.xpread.file;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.uc.base.wa.WaEntry;
import com.xpread.R;
import com.xpread.provider.FileBean;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;
import com.xpread.wa.WaKeys;

public class ImageFragment extends BackHandledFragment implements OnItemClickListener {

    private static final String TAG = "ImageFragment";

    private final Uri EXTERNAL_IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private final String IMAGE_ID = MediaStore.Images.Media._ID;
    private final String IMAGE_DATA = MediaStore.Images.Media.DATA;
    private final Uri EXTERNAL_THUMB_IMAGE_URI = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
    private final String THUMB_IMAGE_DATA = MediaStore.Images.Thumbnails.DATA;
    private final String IMAGE_MINE_TYPE = MediaStore.Images.Media.MIME_TYPE;

    public class ImageInfo {
        boolean selected;
        String data;
        String thumbData;
        Bitmap thumbBmp;
    }

    private ArrayList<ImageInfo> mImageList = new ArrayList<ImageInfo>();

    private GridView mImageGridView;
    private ProgressBar mLoadingBar;
    private ImageAdapter mAdapter;

    private DisplayImageOptions mOptions = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.classimage).showImageForEmptyUri(R.drawable.classimage)
            .showImageOnFail(R.drawable.classimage).cacheInMemory(true).cacheOnDisk(true)
            .considerExifParams(true).imageScaleType(ImageScaleType.IN_SAMPLE_INT)
            .bitmapConfig(Bitmap.Config.RGB_565).build();

    private AtomicBoolean mIsDataLoadFinished = new AtomicBoolean(false);

    private AtomicBoolean mIsVisibleToUser = new AtomicBoolean(false);

    private AtomicBoolean mIsFirstCreate = new AtomicBoolean(true);

    private static final int DATA_LOAD_FINISH = 0x0100;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DATA_LOAD_FINISH:

                    if (LogUtil.isLog) {
                        Log.e(TAG, "in handler, refresh UI");
                    }

                    mAdapter.setData(mImageList);

                    if (mLoadingBar.getVisibility() == View.VISIBLE) {
                        mLoadingBar.setVisibility(View.GONE);
                    }

                    if (mImageGridView.getVisibility() == View.GONE) {
                        mImageGridView.setVisibility(View.VISIBLE);
                    }

                    break;

                default:
                    break;
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
            new QueryTask(getActivity()).execute();
        } else {
            new QueryTask(getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.images, container, false);
        this.mImageGridView = (GridView)view.findViewById(R.id.image_list);
        mAdapter = new ImageAdapter(mImageList, getActivity());
        mImageGridView.setAdapter(mAdapter);
        mImageGridView.setOnItemClickListener(this);
        mImageGridView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(),
                true, true));

        this.mLoadingBar = (ProgressBar)view.findViewById(R.id.image_loading);
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        FileBean file = new FileBean();
        ImageAdapter adapter = (ImageAdapter)parent.getAdapter();
        ImageInfo info = (ImageInfo)adapter.getItem(position);

        file.uri = info.data;
        file.type = Const.TYPE_IMAGE;
        if (info.selected) {
            info.selected = false;
            ((FilePickActivity)getActivity()).updateSelectCount(file, false);
        } else {
            info.selected = true;
            ((FilePickActivity)getActivity()).updateSelectCount(file, true);
            WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SELECT_IMAGE);
        }

        adapter.updateView(position);
    }

    private class ImageAdapter extends BaseAdapter {

        private ArrayList<ImageInfo> imageList;

        private Context context;

        private LayoutInflater inflater;

        public ImageAdapter(ArrayList<ImageInfo> list, Context ctx) {
            this.imageList = list;
            this.context = ctx;
            this.inflater = LayoutInflater.from(this.context);
        }

        public void setData(ArrayList<ImageInfo> list) {
            this.imageList = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return this.imageList.size();
        }

        @Override
        public Object getItem(int position) {
            return this.imageList.get(position);
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
                convertView = inflater.inflate(R.layout.image_item, parent, false);

                viewHolder.thumbnail = (ImageView)convertView.findViewById(R.id.image_thumbnail);
                viewHolder.imageSelectIcon = (ImageView)convertView
                        .findViewById(R.id.image_select_icon);
                viewHolder.shadowView = (View)convertView.findViewById(R.id.shadow);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            final ImageInfo info = (ImageInfo)getItem(position);
            if (info.thumbData != null) {
                ImageLoader.getInstance().displayImage(Scheme.FILE.wrap(info.thumbData),
                        viewHolder.thumbnail, mOptions, null);
            } else if (info.thumbBmp != null) {
                viewHolder.thumbnail.setImageBitmap(info.thumbBmp);
            } else {
                viewHolder.thumbnail.setImageResource(R.drawable.classimage);
            }

            viewHolder.imageSelectIcon.setVisibility(info.selected ? View.VISIBLE : View.GONE);

            viewHolder.shadowView.setVisibility(info.selected ? View.VISIBLE : View.GONE);

            return convertView;
        }

        public void updateView(int itemIndex) {
            int visiblePosition = mImageGridView.getFirstVisiblePosition();
            View view = mImageGridView.getChildAt(itemIndex - visiblePosition);

            ViewHolder viewHolder = (ViewHolder)view.getTag();

            final ImageInfo info = (ImageInfo)getItem(itemIndex);
            viewHolder.imageSelectIcon.setVisibility(info.selected ? View.VISIBLE : View.GONE);
            viewHolder.shadowView.setVisibility(info.selected ? View.VISIBLE : View.GONE);
        }
    }

    private class QueryTask extends AsyncTask<Void, Void, Void> {

        private Cursor externalCursor;

        private Context context;

        public QueryTask(Context ctx) {
            this.context = ctx;
        }

        private boolean isFileExist(String filePath) {
            File file = new File(filePath);
            return file.exists();
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (context != null) {
                externalCursor = context.getContentResolver().query(EXTERNAL_IMAGE_URI,
                        new String[] {
                                IMAGE_ID, IMAGE_DATA
                        }, IMAGE_MINE_TYPE + "=? or " + IMAGE_MINE_TYPE + "=?", new String[] {
                                "image/jpeg", "image/png"
                        }, null);
            } else {
                return null;
            }

            if (externalCursor != null && externalCursor.moveToFirst()) {
                int count = externalCursor.getCount();
                if (LogUtil.isLog) {
                    Log.e(TAG, "external cursor count is " + count);
                }

                Cursor thumbCursor = null;

                do {
                    String id = externalCursor.getString(externalCursor.getColumnIndex(IMAGE_ID));
                    String data = externalCursor.getString(externalCursor
                            .getColumnIndex(IMAGE_DATA));

                    // 如果原图存在
                    if (isFileExist(data)) {
                        ImageInfo imageInfo = new ImageInfo();
                        imageInfo.data = data;
                        imageInfo.selected = false;

                        thumbCursor = context.getContentResolver().query(EXTERNAL_THUMB_IMAGE_URI,
                                new String[] {
                                    THUMB_IMAGE_DATA
                                }, MediaStore.Images.Thumbnails.IMAGE_ID + "=?", new String[] {
                                    id
                                }, null);

                        if (thumbCursor != null && thumbCursor.moveToFirst()) {
                            String thumbData = thumbCursor.getString(thumbCursor
                                    .getColumnIndex(THUMB_IMAGE_DATA));

                            if (isFileExist(thumbData)) {
                                imageInfo.thumbData = thumbData;
                            }

                            if (thumbCursor != null) {
                                thumbCursor.close();
                            }
                        }

                        if (imageInfo.thumbData == null) {
                            if (context != null) {
                                BitmapFactory.Options mOptions = new BitmapFactory.Options();
                                mOptions.inDither = false;
                                mOptions.inPreferredConfig = Bitmap.Config.RGB_565;

                                imageInfo.thumbBmp = MediaStore.Images.Thumbnails.getThumbnail(
                                        context.getContentResolver(), (long)Integer.parseInt(id),
                                        Images.Thumbnails.MINI_KIND, mOptions);
                                
                                

                                if (imageInfo.thumbBmp == null) {
                                    imageInfo.thumbBmp = decodeSampledBitmapFromFd(
                                            imageInfo.data,
                                            (int)this.context.getResources().getDimension(
                                                    R.dimen.fragment_image_width),
                                            (int)this.context.getResources().getDimension(
                                                    R.dimen.fragment_image_height));
                                }
                            }
                        }

                        mImageList.add(imageInfo);
                    }
                } while (externalCursor.moveToNext());

            }

            if (externalCursor != null) {
                externalCursor.close();
            }

            mIsDataLoadFinished.set(true);
            if (mIsFirstCreate.get() && mIsVisibleToUser.get()) {
                mHandler.sendEmptyMessage(DATA_LOAD_FINISH);
                mIsFirstCreate.set(false);
            }

            if (LogUtil.isLog) {
                Log.e(TAG, "the image list count is " + mImageList.size());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        private Bitmap decodeSampledBitmapFromFd(String pathName, int reqWidth, int reqHeight) {
            final BitmapFactory.Options mOptions = new BitmapFactory.Options();
            mOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(pathName, mOptions);

            mOptions.inSampleSize = calculateInSampleSize(mOptions, reqWidth, reqHeight);
            mOptions.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(pathName, mOptions);

        }

        private int calculateInSampleSize(BitmapFactory.Options mOptions, int reqWidth,
                int reqHeight) {
            final int width = mOptions.outWidth;
            final int height = mOptions.outHeight;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }

            }
            return inSampleSize;
        }

    }

    static class ViewHolder {
        ImageView thumbnail;

        ImageView imageSelectIcon;

        View shadowView;
    }

    @Override
    protected boolean onBackPressed() {
        return false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (LogUtil.isLog) {
                Log.e(TAG, "visiable to user");
            }
            super.setSelectedFragment();
            this.mIsVisibleToUser.set(true);

            if (this.mIsFirstCreate.get() && this.mIsDataLoadFinished.get()) {
                mAdapter.setData(mImageList);
                this.mIsFirstCreate.set(false);
            } else {
                if (this.mIsFirstCreate.get()) {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "data haven't finish load, wait the handler to refresh UI");
                    }
                }
                return;
            }

            if (mLoadingBar.getVisibility() == View.VISIBLE) {
                mLoadingBar.setVisibility(View.GONE);
            }

            if (mImageGridView.getVisibility() == View.GONE) {
                mImageGridView.setVisibility(View.VISIBLE);
            }

        } else {
            if (LogUtil.isLog) {
                Log.e(TAG, "not visiable to user");
            }
            this.mIsVisibleToUser.set(false);

        }
    }

}
