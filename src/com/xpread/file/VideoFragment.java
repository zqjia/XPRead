/**
 * <p>
 * Title: xpread
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 视频资源选择界面
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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.VideoView;

import com.xpread.R;
import com.xpread.adapter.VideoAdapter;
import com.xpread.provider.FileBean;
import com.xpread.provider.VideoInfo;
import com.xpread.util.Const;
import com.xpread.util.FileUtil;
import com.xpread.util.LogUtil;

public class VideoFragment extends BackHandledFragment implements 
    OnItemClickListener, OnItemLongClickListener, View.OnClickListener {

    private static final String TAG = "VideoFragment";

    private ArrayList<VideoInfo> mVideoList = new ArrayList<VideoInfo>();
    private RelativeLayout mVideoLayout;
    private ListView mVideoListView;
    private ProgressBar mLoadingBar;
    private VideoAdapter mVideoAdapter;

    private AtomicBoolean mIsDataLoadFinished = new AtomicBoolean(false);
    private AtomicBoolean mIsVisibleToUser = new AtomicBoolean(false);
    private AtomicBoolean mIsFirstCreate = new AtomicBoolean(true);
    private static final int DATA_LOAD_FINISH = 0x0100;
    
    //视频播放器相关
    private boolean mIsVideoPlayerLayoutCreaterd = false;
    private View mVideoPlayerLayout;
    private VideoView mVideoView;
    private ProgressBar mVideoProgressBar;
    private ImageButton mVideoStateImageButton;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case DATA_LOAD_FINISH:
                    if (LogUtil.isLog) {
                        Log.e(TAG, "in handler, refresh UI");
                    }

                    mVideoAdapter.setData(mVideoList);

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.video, container, false);

        this.mVideoLayout = (RelativeLayout)view.findViewById(R.id.video_layout);
        this.mVideoListView = (ListView) view.findViewById(R.id.video_list);
        this.mVideoListView.setDividerHeight(0);
        this.mVideoAdapter = new VideoAdapter(this.mVideoList, getActivity(), this.mVideoListView);
        this.mVideoListView.setAdapter(mVideoAdapter);
        this.mVideoListView.setOnItemClickListener(this);
        this.mVideoListView.setOnItemLongClickListener(this);
        this.mLoadingBar = (ProgressBar) view.findViewById(R.id.video_loading);
        
        
        {
            Dialog dialog = new AlertDialog.Builder(getActivity()).create();
            dialog.setCanceledOnTouchOutside(true);
            
        }
        return view;
    }

    private class QueryTask extends AsyncTask<Void, Void, Void> {

        private Context context;

        public QueryTask(Context ctx) {
            this.context = ctx;
        }

        @Override
        protected Void doInBackground(Void... params) {

            boolean result = queryVideoData();
            if (!result) {
                return null;
            }

            mIsDataLoadFinished.set(true);
            if (mIsFirstCreate.get() && mIsVisibleToUser.get()) {
                mHandler.sendEmptyMessage(DATA_LOAD_FINISH);
                mIsFirstCreate.set(false);
            }
            return null;
        }
        
        private boolean queryVideoData() {
            Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] PROJECTION =
                    {MediaStore.Video.Media._ID, MediaStore.Video.Media.DURATION,
                            MediaStore.Video.Media.TITLE, MediaStore.Video.Media.SIZE,
                            MediaStore.Video.Media.DATA};
            String ORDER_BY = MediaStore.Video.Media.DEFAULT_SORT_ORDER;

            Cursor videoCursor = null;
            if (this.context != null) {
                videoCursor =
                        this.context.getContentResolver().query(VIDEO_URI, PROJECTION, null, null,
                                ORDER_BY);
            } else {
                return false;
            }

            if (videoCursor != null && videoCursor.moveToFirst()) {
                int count = videoCursor.getCount();
                for (int i = 0; i < count; ++i) {
                    videoCursor.moveToPosition(i);

                    String path =
                            videoCursor.getString(videoCursor
                                    .getColumnIndex(MediaStore.Video.Media.DATA));

                    if (FileUtil.isFileExist(path)) {
                        VideoInfo info = new VideoInfo();

                        info.setVideoTitle(videoCursor.getString(videoCursor
                                .getColumnIndex(MediaStore.Video.Media.TITLE)));
                        info.setVideoDuration(videoCursor.getLong(videoCursor
                                .getColumnIndex(MediaStore.Video.Media.DURATION)));
                        info.setVideoSize(videoCursor.getLong(videoCursor
                                .getColumnIndex(MediaStore.Video.Media.SIZE)));
                        info.setVideoData(videoCursor.getString(videoCursor
                                .getColumnIndex(MediaStore.Video.Media.DATA)));

                        int albumId =
                                videoCursor.getInt(videoCursor
                                        .getColumnIndex(MediaStore.Video.Media._ID));
                        Uri VIDEO_THUMB_URI = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
                        String[] THUMB_PROJECTION = {MediaStore.Video.Thumbnails.DATA};
                        String THUMB_SELECTION = MediaStore.Video.Thumbnails.VIDEO_ID + "=?";
                        String[] THUMB_SELECTION_ARGS = {String.valueOf(albumId)};
                        String THUMB_ORDER_BY = null;

                        Cursor thumbCursor = null;
                        if (this.context != null) {
                            thumbCursor =
                                    this.context.getContentResolver().query(VIDEO_THUMB_URI,
                                            THUMB_PROJECTION, THUMB_SELECTION,
                                            THUMB_SELECTION_ARGS, THUMB_ORDER_BY);
                        }

                        if (thumbCursor != null && thumbCursor.moveToFirst()) {
                            info.setVideoThumbdata(thumbCursor.getString(thumbCursor
                                    .getColumnIndex(MediaStore.Video.Thumbnails.DATA)));
                        } else {

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inDither = false;
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                            if (context != null) {
                                Bitmap bitmap =
                                        MediaStore.Video.Thumbnails.getThumbnail(
                                                context.getContentResolver(), (long) albumId,
                                                Images.Thumbnails.MINI_KIND, options);
                                info.setVideoThumbBmp(bitmap);
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
            return true;
        }
    }

    @Override
    protected boolean onBackPressed() {
        if (mVideoLayout.findViewById(R.id.video_player_layout) != null) {
            mVideoView.stopPlayback();
            mVideoLayout.removeView(mVideoPlayerLayout);
            mHandler.removeCallbacks(mUpdateThread);
            return true;
        } 
        
        return false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            super.setSelectedFragment();
            this.mIsVisibleToUser.set(true);

            if (this.mIsFirstCreate.get() && this.mIsDataLoadFinished.get()) {
                mVideoAdapter.setData(mVideoList);
                this.mIsFirstCreate.set(false);
            } else {
                if (this.mIsFirstCreate.get()) {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "data load haven't finish, wait the handler to refresh UI");
                    }
                }
                return;
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
        VideoAdapter adapter = (VideoAdapter) parent.getAdapter();
        final VideoInfo info = (VideoInfo) adapter.getItem(position);

        file.uri = info.getVideoData();
        file.type = Const.TYPE_VIDEO;
        
        //为显示选中文件列表准备
        //add by zqjia
        if(info.getVideoThumbData() != null) {
            file.setThumbImage(info.getVideoData());
        } else if (info.getVideoThumbBmp() != null) {
            file.setThumbImage(info.getVideoThumbBmp());
        } else {
            file.setThumbImage(R.drawable.video);
        }
        file.setSize(FileUtil.getFileSizeByName(info.getVideoData()));
        
        if (info.getIsSelected()) {
            info.setIsSelected(false);
            ((FilePickActivity) getActivity()).updateSelectCount(file, false);
        } else {
            info.setIsSelected(true);
            ((FilePickActivity) getActivity()).updateSelectCount(file, true);
        }

        adapter.updateView(position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!mIsVideoPlayerLayoutCreaterd) {
            mIsVideoPlayerLayoutCreaterd  = true;
            initVideoPlayerLayout();
        }
        
        if (mVideoLayout.findViewById(R.id.video_player_layout) == null) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mVideoLayout.addView(mVideoPlayerLayout, lp);
        }
        
        VideoAdapter adapter = (VideoAdapter) parent.getAdapter();
        VideoInfo videoInfo = (VideoInfo) adapter.getItem(position);
        playVideo(videoInfo);
        
        return true;
    }
    
    private void initVideoPlayerLayout() {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        mVideoPlayerLayout = layoutInflater.inflate(R.layout.video_player_dialog, null, false);
        mVideoView = (VideoView)mVideoPlayerLayout.findViewById(R.id.video_view);
        mVideoProgressBar = (SeekBar)mVideoPlayerLayout.findViewById(R.id.video_progressbar);
        mVideoStateImageButton = (ImageButton)mVideoPlayerLayout.findViewById(R.id.video_state);
        
        mVideoPlayerLayout.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "video player layout is click");
                }
                
                mVideoView.stopPlayback();
                mVideoLayout.removeView(mVideoPlayerLayout);
                mHandler.removeCallbacks(mUpdateThread);
            }
        });
        
        
        mVideoView.setOnTouchListener(new OnTouchListener() {
            
            @SuppressLint("ClickableViewAccessibility") 
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (LogUtil.isLog) {
                    Log.e(TAG, "video view is touch");
                }
                return true;
            }
        });
        mVideoProgressBar.setOnClickListener(this);
        mVideoStateImageButton.setOnClickListener(this);
        
        mVideoView.setOnCompletionListener(new OnCompletionListener() {
            
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.stopPlayback();
                mVideoLayout.removeView(mVideoPlayerLayout);
                mHandler.removeCallbacks(mUpdateThread);
            }
        });
        
        if (mVideoProgressBar instanceof SeekBar) {
            ((SeekBar)mVideoProgressBar).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    
                }
                
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                   
                    if (fromUser) {
                        mVideoView.pause();
                        mHandler.removeCallbacks(mUpdateThread);
                        
                        long duration = mVideoView.getDuration();
                        long newPosition = duration * progress / 1000L;
                        mVideoView.seekTo((int)newPosition);
                        
                        mVideoView.start();
                        mHandler.post(mUpdateThread);
                    }
                }
            });
        }
    }
    
    private void playVideo(VideoInfo videoInfo) {
        String videoPath = videoInfo.getVideoData();
        mVideoView.setVideoPath(videoPath);
        mVideoView.start();
        
        mHandler.post(mUpdateThread);
    }
    
    Runnable mUpdateThread = new Runnable(){  
        
        public void run() {  
            //获得歌曲现在播放位置并设置成播放进度条的值  
            int current = mVideoView.getCurrentPosition();
            int duration = mVideoView.getDuration();
//            Log.e(TAG, "current and duration is " + current + " ---" + duration);
            if (current == 0 || duration == 1) {        //have not get Ready
                mHandler.postDelayed(mUpdateThread, 100);
            }
            
            long newPosition = mVideoView.getCurrentPosition() * 1000L /mVideoView.getDuration();
//            Log.e(TAG, "new position is " + newPosition);
            mVideoProgressBar.setProgress((int)newPosition);  
            //每次延迟100毫秒再启动线程  
            mHandler.postDelayed(mUpdateThread, 100);  
        }  
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.layout.video_player_dialog:
                /*mVideoView.stopPlayback();
                mVideoLayout.removeView(mVideoPlayerLayout);
                mHandler.removeCallbacks(mUpdateThread);*/
                break;
            case R.id.video_view:
            case R.id.video_progressbar:
                break;
            case R.id.video_state:
                if (mVideoView != null) {
                    if (mVideoView.isPlaying()) {
                        mVideoView.pause();
                        mVideoStateImageButton.setImageResource(android.R.drawable.ic_media_play);
                        mHandler.removeCallbacks(mUpdateThread);
                    } else {
                        mVideoView.start();
                        mVideoStateImageButton.setImageResource(android.R.drawable.ic_media_pause);
                        mHandler.post(mUpdateThread);
                    }
                }
                break;
            default:
                break;
        }
    } 

}
