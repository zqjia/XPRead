/**
 * <p>Title: xpread</p>
 *
 * <p>Description: </p>
 * 视频资源选择界面
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
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.xpread.R;
import com.xpread.provider.FileBean;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;

public class VideoFragment extends BackHandledFragment implements OnItemClickListener {

    private static final String TAG = "VideoFragment";

    private ArrayList<VideoInfo> mVideoList = new ArrayList<VideoInfo>();

    private ListView mVideoListView;

    private ProgressBar mLoadingBar;

    private VideoAdapter mAdapter;

    private DisplayImageOptions mVideoOptions;

    static class Holder {
        ImageView thumbnail;

        TextView title;

        TextView duration;

        TextView size;

        ImageView check;
    }

    class VideoInfo {
        Bitmap thumbBmp;

        String thumbData;

        String title;

        long duration;

        long size;

        boolean selected;

        String data;
    }
    
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
                    
                    mAdapter.setData(mVideoList);
                    
                    if (mLoadingBar.getVisibility() == View.VISIBLE) {
                        mLoadingBar.setVisibility(View.GONE);
                    }
                    
                    if (mVideoListView.getVisibility() == View.GONE) {
                        mVideoListView.setVisibility(View.VISIBLE);
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

        this.mVideoOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.video)
                .showImageForEmptyUri(R.drawable.video)
                .showImageOnFail(R.drawable.video)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT) 
                .bitmapConfig(Bitmap.Config.RGB_565).build();
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.video, container, false);

        this.mVideoListView = (ListView)view.findViewById(R.id.video_list);
        this.mVideoListView.setDividerHeight(0);
        this.mAdapter = new VideoAdapter(this.mVideoList, getActivity());
        this.mVideoListView.setAdapter(mAdapter);
        this.mVideoListView.setOnItemClickListener(this);
        this.mLoadingBar = (ProgressBar)view.findViewById(R.id.video_loading);
        return view;
    }

    private class VideoAdapter extends BaseAdapter {

        private ArrayList<VideoInfo> videoList;

        private Context context;
        
        private LayoutInflater inflater;

        public VideoAdapter(ArrayList<VideoInfo> list, Context ctx) {
            this.videoList = list;
            this.context = ctx;
            this.inflater = LayoutInflater.from(context);
        }

        public void setData(ArrayList<VideoInfo> list) {
            this.videoList = list;
            notifyDataSetChanged();
        }
        
        @Override
        public int getCount() {
            return this.videoList.size();
        }

        @Override
        public Object getItem(int position) {
            return this.videoList.get(position);
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
                convertView = inflater.inflate(R.layout.video_item, parent, false);
                
                holder.thumbnail = (ImageView)convertView.findViewById(R.id.video_thumbnail);
                holder.title = (TextView)convertView.findViewById(R.id.video_title);
                holder.duration = (TextView)convertView.findViewById(R.id.video_duration);
                holder.size = (TextView)convertView.findViewById(R.id.video_size);
                holder.check = (ImageView)convertView.findViewById(R.id.video_check);
                convertView.setTag(holder);
            } else {
                holder = (Holder)convertView.getTag();
            }

            final VideoInfo info = this.videoList.get(position);
            if (info != null) {

                if(info.thumbData != null) {
                    ImageLoader.getInstance().displayImage(Scheme.FILE.wrap(info.thumbData),
                            holder.thumbnail, mVideoOptions);
                } else if(info.thumbBmp != null) {
                    holder.thumbnail.setImageBitmap(info.thumbBmp);
                } else {
                    holder.thumbnail.setImageResource(R.drawable.video);
                }

                holder.title.setText(info.title);
                holder.duration.setText(DateUtils.formatElapsedTime(info.duration / 1000));

                float size = info.size / Const.KILO;
                if (size > Const.KILO) {
                    size /= Const.KILO;

                    if (size > Const.KILO) {
                        size /= Const.KILO;
                        holder.size.setText(String.format(getResources().getString(R.string.size_GB), size));
                    } else {
                        holder.size.setText(String.format(getResources().getString(R.string.size_MB), size));
                    }
                     
                } else {
                    holder.size.setText(String.format(getResources().getString(R.string.size_KB), (int)size));
                }
                
                holder.check.setImageResource(info.selected ? R.drawable.check : R.drawable.checkbox);
            } else {
                if(LogUtil.isLog) {
                    Log.e(TAG, "video info of this item is null");
                }
            }
            
            return convertView;
        }
        
        public void updateView(int itemIndex) {
            int visiblePosition = mVideoListView.getFirstVisiblePosition();
            View view = mVideoListView.getChildAt(itemIndex - visiblePosition);
            final VideoInfo info = (VideoInfo)getItem(itemIndex);
            
            Holder holder = (Holder)view.getTag();
            holder.check.setImageResource(info.selected ? R.drawable.check : R.drawable.checkbox);
        }
        
    }
    
    private class QueryTask extends AsyncTask<Void, Void, Void> {

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

            Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] PROJECTION = {
                    MediaStore.Video.Media._ID, MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.TITLE, MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DATA
            };
            String ORDER_BY = MediaStore.Video.Media.DEFAULT_SORT_ORDER;

            Cursor videoCursor = null;
            if (this.context != null) {
                videoCursor = this.context.getContentResolver().query(VIDEO_URI, PROJECTION, null,
                        null, ORDER_BY);
            } else {
                return null;
            }

            if (videoCursor != null && videoCursor.moveToFirst()) {
                int count = videoCursor.getCount();
                for (int i = 0; i < count; ++i) {
                    videoCursor.moveToPosition(i);

                    String path = videoCursor.getString(videoCursor
                            .getColumnIndex(MediaStore.Video.Media.DATA));

                    if (isFileExist(path)) {
                        VideoInfo info = new VideoInfo();

                        info.title = videoCursor.getString(videoCursor
                                .getColumnIndex(MediaStore.Video.Media.TITLE));
                        info.duration = videoCursor.getLong(videoCursor
                                .getColumnIndex(MediaStore.Video.Media.DURATION));
                        info.size = videoCursor.getLong(videoCursor
                                .getColumnIndex(MediaStore.Video.Media.SIZE));
                        info.data = videoCursor.getString(videoCursor
                                .getColumnIndex(MediaStore.Video.Media.DATA));

                        int albumId = videoCursor.getInt(videoCursor
                                .getColumnIndex(MediaStore.Video.Media._ID));
                        Uri VIDEO_THUMB_URI = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
                        String[] THUMB_PROJECTION = {
                            MediaStore.Video.Thumbnails.DATA
                        };
                        String THUMB_SELECTION = MediaStore.Video.Thumbnails.VIDEO_ID + "=?";
                        String[] THUMB_SELECTION_ARGS = {
                            String.valueOf(albumId)
                        };
                        String THUMB_ORDER_BY = null;

                        Cursor thumbCursor = null;
                        if (this.context != null) {
                            thumbCursor = this.context.getContentResolver().query(VIDEO_THUMB_URI,
                                    THUMB_PROJECTION, THUMB_SELECTION, THUMB_SELECTION_ARGS,
                                    THUMB_ORDER_BY);
                        }

                        if (thumbCursor != null && thumbCursor.moveToFirst()) {
                            info.thumbData = thumbCursor.getString(thumbCursor
                                    .getColumnIndex(MediaStore.Video.Thumbnails.DATA));
                        } else {

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inDither = false;
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                            if (context != null) {
                                Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                                        context.getContentResolver(), (long)albumId,
                                        Images.Thumbnails.MINI_KIND, options);
                                info.thumbBmp = bitmap;
                            }
                        }

                        mVideoList.add(info);

                        if (thumbCursor != null) {
                            thumbCursor.close();
                        }
                    }
                }
            }

            if (videoCursor != null) {
                videoCursor.close();
            }
            
            mIsDataLoadFinished.set(true);
            if (mIsFirstCreate.get() && mIsVisibleToUser.get()) {
                mHandler.sendEmptyMessage(DATA_LOAD_FINISH);
                mIsFirstCreate.set(false);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

        }

/*        private Bitmap getVideoThumbnail(String videoPath, int width , int height, int kind) {  
            Bitmap bitmap = null;  
            bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);  
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);  
            return bitmap;  
        }  */
        
/*        private Bitmap createVideoThumbnail(String filePath) {
            Bitmap bitmap = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setMode(MediaMetadataRetriever.);
                retriever.setDataSource(filePath);
                bitmap = retriever.captureFrame();
            } catch (IllegalArgumentException ex) {
                // Assume this is a corrupt video file
            } catch (RuntimeException ex) {
                // Assume this is a corrupt video file.
            } finally {
                try {
                    retriever.release();
                } catch (RuntimeException ex) {
                    // Ignore failures while cleaning up.
                }
            }
            return bitmap;
        }*/

    }

    @Override
    protected boolean onBackPressed() {
        return false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            super.setSelectedFragment();
            this.mIsVisibleToUser.set(true);

            if (this.mIsFirstCreate.get() && this.mIsDataLoadFinished.get()) {
                mAdapter.setData(mVideoList);
                this.mIsFirstCreate.set(false);
            } else {
                if(this.mIsFirstCreate.get()) {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "data load haven't finish, wait the handler to refresh UI");
                    }
                }
                return ;
            }
            
            if (this.mLoadingBar.getVisibility() == View.VISIBLE) {
                this.mLoadingBar.setVisibility(View.GONE);
            }
            
            if (this.mVideoListView.getVisibility() == View.GONE) {
                this.mVideoListView.setVisibility(View.VISIBLE);
            }

        } else {
            if (LogUtil.isLog) {
                Log.e(TAG, "not visiable to user");
            }
            this.mIsVisibleToUser.set(false);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        FileBean file = new FileBean();
        VideoAdapter adapter = (VideoAdapter)parent.getAdapter();
        final VideoInfo info = (VideoInfo)adapter.getItem(position);
        
        file.uri = info.data;
        file.type = Const.TYPE_VIDEO;

        if (info.selected) {
            info.selected = false;
            ((FilePickActivity)getActivity()).updateSelectCount(file, false);
        } else {
            info.selected = true;
            ((FilePickActivity)getActivity()).updateSelectCount(file, true);
        }

        adapter.updateView(position);
    }

}
