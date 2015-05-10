/**
 * <p>
 * Title: xpread
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 图片资源选择界面
 * <p>
 * Copyright: Copyright (c) 2014
 * </p>
 * 
 * <p>
 * Company: ucweb.com
 * </p>
 * 
 * @author jiazq@ucweb.com
 * @version 1.0
 */

package com.xpread.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
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
import android.widget.GridView;
import android.widget.ProgressBar;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.uc.base.wa.WaEntry;
import com.xpread.R;
import com.xpread.adapter.ImageStickyGridAdapter;
import com.xpread.provider.FileBean;
import com.xpread.provider.ImageGridItem;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;
import com.xpread.util.TimeUtil;
import com.xpread.wa.WaKeys;

public class ImageFragment1 extends BackHandledFragment implements OnItemClickListener {

    private static final String TAG = "ImageFragment";

    private final Uri EXTERNAL_IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private final String IMAGE_ID = MediaStore.Images.Media._ID;
    private final String IMAGE_DATA = MediaStore.Images.Media.DATA;
    private final Uri EXTERNAL_THUMB_IMAGE_URI = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
    private final String THUMB_IMAGE_DATA = MediaStore.Images.Thumbnails.DATA;
    private final String IMAGE_MINE_TYPE = MediaStore.Images.Media.MIME_TYPE;
    private final String IMAGE_BUCKET_NAME = MediaStore.Images.Media.BUCKET_DISPLAY_NAME;

    private ArrayList<ImageGridItem> mImageList = new ArrayList<ImageGridItem>();

    private GridView mImageGridView;
    private ProgressBar mLoadingBar;
    private ImageStickyGridAdapter mImageAdapter;

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

                    mImageAdapter.setData(mImageList);

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
        this.mImageGridView = (GridView) view.findViewById(R.id.image_list);
        mImageAdapter = new ImageStickyGridAdapter(getActivity(), mImageList, mImageGridView);
        mImageGridView.setAdapter(mImageAdapter);
        mImageGridView.setOnItemClickListener(this);
        mImageGridView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(),
                true, true));

        this.mLoadingBar = (ProgressBar) view.findViewById(R.id.image_loading);
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        FileBean file = new FileBean();
        ImageStickyGridAdapter adapter = (ImageStickyGridAdapter) parent.getAdapter();
        ImageGridItem item = (ImageGridItem) adapter.getItem(position);

        file.uri = item.getPath();
        file.type = Const.TYPE_IMAGE;
        if (item.getIsSelected()) {
            item.setIsSelected(false);
            ((FilePickActivity) getActivity()).updateSelectCount(file, false);
        } else {
            item.setIsSelected(true);
            ((FilePickActivity) getActivity()).updateSelectCount(file, true);
            WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SELECT_IMAGE);
        }

        adapter.updateView(position);
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

            // 发送广播，扫描SD卡上的多媒体文件
            /*context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                    + Environment.getExternalStorageDirectory())));*/

            ArrayList<ImageGridItem> noHeaderIdList = new ArrayList<ImageGridItem>();
            
            if (context != null) {
                externalCursor =
                        context.getContentResolver().query(EXTERNAL_IMAGE_URI,
                                new String[] {IMAGE_ID, IMAGE_DATA, IMAGE_BUCKET_NAME},
                                IMAGE_MINE_TYPE + "=? or " + IMAGE_MINE_TYPE + "=?",
                                new String[] {"image/jpeg", "image/png"}, null);
            } else {
                return null;
            }

            if (externalCursor != null && externalCursor.moveToFirst()) {
                int count = externalCursor.getCount();
                if (LogUtil.isLog) {
                    Log.e(TAG, "external cursor count is " + count);
                }

                do {
                    String id = externalCursor.getString(externalCursor.getColumnIndex(IMAGE_ID));
                    String data =
                            externalCursor.getString(externalCursor.getColumnIndex(IMAGE_DATA));
                    
                    // 如果原图存在
                    if (isFileExist(data)) {
                        ImageGridItem item = new ImageGridItem();
                        // 设置图片原图路径
                        item.setPath(data);

                        // 设置图片时间
                        String bucketName =
                                externalCursor.getString(externalCursor
                                        .getColumnIndex(IMAGE_BUCKET_NAME));
                        item.setBucketName(bucketName);

                        // 设置图片对应的缩略图
                        setImageThumb(id, item);

                        noHeaderIdList.add(item);
                    }
                } while (externalCursor.moveToNext());
            }

            if (externalCursor != null) {
                externalCursor.close();
            }

            //生成有header的list数据,已经排序完成
            mImageList = generateHeaderId(noHeaderIdList);
            
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
        
        /**
         * 生成对应原图的缩略图
         * @param pathName 原图路径
         * @param reqWidth 缩略图的宽度
         * @param reqHeight 缩略图的高度
         * */
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

        /**
         * 为图片设置缩略图
         * 
         * @param id 原图在数据库中的id
         * @param item 图片数据的item，对item中的缩略图进行设置
         * */
        private void setImageThumb(String id, ImageGridItem item) {
            Cursor thumbCursor =
                    context.getContentResolver().query(EXTERNAL_THUMB_IMAGE_URI,
                            new String[] {THUMB_IMAGE_DATA},
                            MediaStore.Images.Thumbnails.IMAGE_ID + "=?", new String[] {id}, null);

            if (thumbCursor != null && thumbCursor.moveToFirst()) {
                String thumbPath =
                        thumbCursor.getString(thumbCursor.getColumnIndex(THUMB_IMAGE_DATA));

                if (isFileExist(thumbPath)) {
                    item.setThumbPath(thumbPath);
                }

                if (thumbCursor != null) {
                    thumbCursor.close();
                }
            }

            if (item.getThumbPath() == null) {
                if (context != null) {
                    BitmapFactory.Options mOptions = new BitmapFactory.Options();
                    mOptions.inDither = false;
                    mOptions.inPreferredConfig = Bitmap.Config.RGB_565;

                    Bitmap thumbBmp =
                            MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(),
                                    (long) Integer.parseInt(id), Images.Thumbnails.MINI_KIND,
                                    mOptions);

                    if (thumbBmp == null) {
                        thumbBmp =
                                decodeSampledBitmapFromFd(
                                        item.getPath(),
                                        (int) this.context.getResources().getDimension(
                                                R.dimen.fragment_image_width),
                                        (int) this.context.getResources().getDimension(
                                                R.dimen.fragment_image_height));
                    }
                    item.setThumbBmp(thumbBmp);
                }
            }
        }
        
        /** 
         * 对GridView的Item生成HeaderId, 根据图片的添加时间的年、月、日来生成HeaderId 
         * 年、月、日相等HeaderId就相同 
         * @param nonHeaderIdList 
         * @return 
         */  
        private ArrayList<ImageGridItem> generateHeaderId(ArrayList<ImageGridItem> noHeaderIdList) {  
            //Map中String为图片的时间，Integer为图片的id
            Map<String, Integer> mHeaderIdMap = new HashMap<String, Integer>();  
            int mHeaderId = 1;  
            ArrayList<ImageGridItem> hasHeaderIdList;  
              
            //遍历无headerId的图片list
            for(ListIterator<ImageGridItem> it = noHeaderIdList.listIterator(); it.hasNext();){  
                ImageGridItem item = it.next();  
                String bucketName = item.getBucketName();  
                if(!mHeaderIdMap.containsKey(bucketName)){  
                    item.setHeaderId(mHeaderId);  
                    mHeaderIdMap.put(bucketName, mHeaderId);  
                    mHeaderId ++;  
                }else{  
                    item.setHeaderId(mHeaderIdMap.get(bucketName));  
                }  
            }  
            hasHeaderIdList = noHeaderIdList;  
            Collections.sort(hasHeaderIdList,new YMDComparator());
            return hasHeaderIdList;  
        }  

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
                mImageAdapter.setData(mImageList);
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

    private class YMDComparator implements Comparator<ImageGridItem> {

        @Override
        public int compare(ImageGridItem lhs, ImageGridItem rhs) {
            return lhs.getBucketName().compareTo(rhs.getBucketName());
        }
    }
    
}
