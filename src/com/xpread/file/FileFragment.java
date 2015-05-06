/**
 * <p>Title: xpread</p>
 *
 * <p>Description: </p>
 * 文件资源选择界面
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
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.xpread.util.SDCardUtil;
import com.xpread.util.Utils;
import com.xpread.wa.WaKeys;
import com.xpread.widget.RobotoTextView;

public class FileFragment extends BackHandledFragment {

    private static final String TAG = "FileFragment";

    private ArrayList<FileInfo> mFileList = new ArrayList<FileInfo>();;

    private HashSet<String> mSelectSet = new HashSet<String>();

    private FileAdapter mFileAdapter;

    private ListView mFileListView;

    private HorizontalScrollView mCurrentPathView;

    private LinearLayout mLinearlayout;

    private ProgressBar mLoadingBar;

    private DisplayImageOptions mVideoOptions;

    private DisplayImageOptions mImageOptions;

    private AtomicBoolean mIsFirstCreate = new AtomicBoolean(true);

    private AtomicBoolean mIsVisibleToUser = new AtomicBoolean(false);

    private AtomicBoolean mIsDataLoadFinished = new AtomicBoolean(false);

    private static final int DATA_LOAD_FINISH = 0x0100;

    private static final int ENPTY_BUCKET = 0x0010;

    private String mCurrentPath;

    private TextView mEmptyHintTextView;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case DATA_LOAD_FINISH:
                    if (LogUtil.isLog) {
                        Log.e(TAG, "in handler, refresh UI");
                    }

                    mFileAdapter.setData(mFileList);

                    if (mLoadingBar.getVisibility() != View.GONE) {
                        mLoadingBar.setVisibility(View.GONE);
                    }
                    
                    if (mEmptyHintTextView.getVisibility() != View.GONE) {
                        mEmptyHintTextView.setVisibility(View.GONE);
                    }

                    if (mFileListView.getVisibility() != View.VISIBLE) {
                        mFileListView.setVisibility(View.VISIBLE);
                    }
                    break;

                case ENPTY_BUCKET:
                    if (mFileListView.getVisibility() == View.VISIBLE) {
                        mFileListView.setVisibility(View.GONE);
                    }
                    
                    if (mEmptyHintTextView.getVisibility() != View.VISIBLE) {
                        mEmptyHintTextView.setVisibility(View.VISIBLE);
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

        if (SDCardUtil.isSDCardEnable()) {
            boolean isOpen = true;
            boolean isAdd = false;

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                new FileListTask(isOpen, isAdd).execute(SDCardUtil.getSDCardPath());
            } else {
                new FileListTask(isOpen, isAdd).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        SDCardUtil.getSDCardPath());
            }
        } else {
            if (LogUtil.isLog) {
                Log.d(TAG, "sdcard is not access");
            }
        }

        this.mVideoOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.video)
                .showImageForEmptyUri(R.drawable.video).showImageOnFail(R.drawable.video)
                .cacheInMemory(true).cacheOnDisk(true).considerExifParams(true)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT).bitmapConfig(Bitmap.Config.RGB_565)
                .build();

        this.mImageOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.image)
                .showImageForEmptyUri(R.drawable.image).showImageOnFail(R.drawable.image)
                .cacheInMemory(true).cacheOnDisk(true).considerExifParams(true)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT).bitmapConfig(Bitmap.Config.RGB_565)
                .build();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.file, container, false);

        this.mEmptyHintTextView = (TextView)view.findViewById(R.id.file_empty_hint);
        
        this.mFileListView = (ListView)view.findViewById(R.id.file_list);
        this.mFileListView.setDividerHeight(0);
        this.mFileAdapter = new FileAdapter(this.mFileList);
        this.mFileListView.setAdapter(this.mFileAdapter);
        this.mFileListView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(),
                true, true));
        this.mFileListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileAdapter adapter = (FileAdapter)parent.getAdapter();
                final FileInfo info = (FileInfo)adapter.getItem(position);
                File file = new File(info.data);

                if (file.isDirectory()) {

                    boolean isOpen = true;
                    boolean isAdd = false;

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        new FileListTask(isOpen, isAdd).execute(info.data);
                        mCurrentPath = info.data;
                        changeCurrentPath(info.data);
                    } else {
                        new FileListTask(isOpen, isAdd).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR, info.data);
                        mCurrentPath = info.data;
                        changeCurrentPath(info.data);
                    }
                } else {

                    FileBean fileData = new FileBean();
                    fileData.uri = file.getPath();
                    fileData.fileName = info.fileName;
                    fileData.type = Utils.getFileType(fileData.uri);

                    if (info.selected) {
                        info.selected = false;
                        mSelectSet.remove(info.data);
                        ((FilePickActivity)getActivity()).updateSelectCount(fileData, false);
                    } else {
                        info.selected = true;
                        mSelectSet.add(info.data);
                        ((FilePickActivity)getActivity()).updateSelectCount(fileData, true);
                        WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SELECT_FILE);
                    }
                    adapter.updateView(position);
                }
            }

        });
        // 开始显示的进度条
        this.mLoadingBar = (ProgressBar)view.findViewById(R.id.file_loading);

        this.mCurrentPathView = (HorizontalScrollView)view
                .findViewById(R.id.current_path_scroll_view);
        // 装载所有子view的线性布局容器
        this.mLinearlayout = new LinearLayout(getActivity());
        this.mLinearlayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT));
        this.mLinearlayout.setOrientation(LinearLayout.HORIZONTAL);
        this.mCurrentPathView.addView(this.mLinearlayout);

        this.mCurrentPath = SDCardUtil.getSDCardPath();

        TextView textView = getTextView(true);
        textView.setText(R.string.root_dir);

        this.mLinearlayout.addView(textView);

        ImageView imageView = getImageView(true);
        this.mLinearlayout.addView(imageView);

        return view;
    }

    public class FileAdapter extends BaseAdapter {

        private ArrayList<FileInfo> fileInfoList;

        private LayoutInflater inflater;

        public FileAdapter(ArrayList<FileInfo> list) {
            this.fileInfoList = list;
            this.inflater = LayoutInflater.from(getActivity());
        }

        public void updateView(int position) {

            int visiblePosition = mFileListView.getFirstVisiblePosition();
            View view = mFileListView.getChildAt(position - visiblePosition);
            FileInfo info = this.fileInfoList.get(position);

            ViewHolder holder = (ViewHolder)view.getTag();
            holder.fileCheck.setImageResource(info.selected ? R.drawable.check
                    : R.drawable.checkbox);
        }

        public void setData(ArrayList<FileInfo> list) {

            synchronized (FileFragment.this) {
                this.fileInfoList = list;
                notifyDataSetChanged();
                mIsDataLoadFinished.set(false);
            }

        }

        @Override
        public int getCount() {
            if (LogUtil.isLog) {
                Log.d(TAG, "get count :" + this.fileInfoList.size());
            }
            return this.fileInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return this.fileInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            final FileInfo info = (FileInfo)getItem(position);

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.file_item, parent, false);
                holder.icon = (ImageView)convertView.findViewById(R.id.item_image);
                holder.folderName = (TextView)convertView.findViewById(R.id.folder_name);
                holder.fileName = (TextView)convertView.findViewById(R.id.file_name);
                holder.fileSize = (TextView)convertView.findViewById(R.id.file_size);
                holder.fileCheck = (ImageView)convertView.findViewById(R.id.file_check);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            if (info.folderName != null) {
                holder.folderName.setVisibility(View.VISIBLE);
                holder.fileName.setVisibility(View.GONE);
                holder.fileSize.setVisibility(View.GONE);
                holder.icon.setImageResource(R.drawable.file);
                holder.folderName.setText(info.folderName);
                holder.fileCheck.setVisibility(View.GONE);
            } else {

                holder.folderName.setVisibility(View.GONE);
                holder.fileName.setVisibility(View.VISIBLE);
                holder.fileSize.setVisibility(View.VISIBLE);

                holder.fileName.setText(info.fileName);

                float size = Integer.valueOf(info.fileSize) / Const.KILO;
                if (size > Const.KILO) {
                    size /= Const.KILO;

                    if (size > Const.KILO) {
                        size /= Const.KILO;
                        holder.fileSize.setText(String.format(
                                getResources().getString(R.string.size_GB), size));
                    } else {

                        holder.fileSize.setText(String.format(
                                getResources().getString(R.string.size_MB), size));
                    }

                } else {
                    holder.fileSize.setText(String.format(getResources()
                            .getString(R.string.size_KB), (int)size));
                }

                holder.fileCheck.setVisibility(View.VISIBLE);
                int type = Utils.getFileType(info.fileName);
                switch (type) {
                    case Const.TYPE_IMAGE:
                        if (getImageThumb(info.data) != null) {
                            ImageLoader.getInstance().displayImage(Scheme.FILE.wrap(info.data),
                                    holder.icon, mImageOptions, null);
                        } else {
                            holder.icon.setImageResource(R.drawable.image);
                        }
                        break;

                    case Const.TYPE_VIDEO:
                        if (getVideoThumb(info.data) != null) {
                            ImageLoader.getInstance().displayImage(
                                    Scheme.FILE.wrap(getVideoThumb(info.data)), holder.icon,
                                    mVideoOptions, null);
                        } else {
                            holder.icon.setImageResource(R.drawable.video);
                        }
                        break;

                    case Const.TYPE_MUSIC:
                        holder.icon.setImageResource(R.drawable.music);
                        break;

                    case Const.TYPE_ZIP:
                        holder.icon.setImageResource(R.drawable.zip);
                        break;

                    case Const.TYPE_TEXT:
                        holder.icon.setImageResource(R.drawable.txt);
                        break;

                    case Const.TYPE_APP:
                        holder.icon.setImageResource(R.drawable.app);
                        break;

                    case Const.TYPE_UNKNOW:
                        holder.icon.setImageResource(R.drawable.unknown);
                        break;
                }

                if (mSelectSet.contains(info.data)) {
                    info.selected = true;
                }

                holder.fileCheck.setImageResource(info.selected ? R.drawable.check
                        : R.drawable.checkbox);

                // no file bucket selected
                /*
                 * holder.fileCheck.setOnClickListener(new OnClickListener(){
                 * @Override public void onClick(View v) { if(item.selected) {
                 * item.selected = false; } else { item.selected = true; }
                 * boolean isOpen = false; if (Build.VERSION.SDK_INT <=
                 * Build.VERSION_CODES.HONEYCOMB_MR1) { new FileListTask(isOpen,
                 * item.selected).execute(item.data); } else { new
                 * FileListTask(isOpen,
                 * item.selected).executeOnExecutor(AsyncTask
                 * .THREAD_POOL_EXECUTOR, item.data); } } });
                 */

            }

            return convertView;
        }

        private String getImageThumb(String imagePath) {
            ContentResolver cr = getActivity().getContentResolver();

            String[] PROJECTION = {
                    MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID
            };
            String SELECTION = MediaStore.Images.Media.DATA + "=?";
            String[] SELECTION_ARGS = {
                imagePath
            };
            String ORDER_BY = null;

            String thumbData = null;

            Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, PROJECTION,
                    SELECTION, SELECTION_ARGS, ORDER_BY);
            if (cursor != null && cursor.moveToFirst()) {

                String imageId = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media._ID));
                String[] thumbProjection = {
                    MediaStore.Images.Thumbnails.DATA
                };
                String thumbSelection = MediaStore.Images.Thumbnails.IMAGE_ID + "=?";
                String[] thumbSelectionArgs = {
                    imageId
                };
                String thumbSortOrder = null;

                Cursor thumbCursor = cr.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                        thumbProjection, thumbSelection, thumbSelectionArgs, thumbSortOrder);

                if (thumbCursor != null && thumbCursor.moveToFirst()) {
                    thumbCursor.moveToFirst();
                    thumbData = thumbCursor.getString(thumbCursor
                            .getColumnIndex(MediaStore.Video.Thumbnails.DATA));
                }

                if (thumbCursor != null) {
                    thumbCursor.close();
                }
            }

            if (cursor != null) {
                cursor.close();
            }

            return thumbData;
        }
    }

    private String getVideoThumb(String videoPath) {

        ContentResolver cr = FileFragment.this.getActivity().getContentResolver();

        final Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] PROJECTION = {
                MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID
        };
        String SELECTION = MediaStore.Video.Media.DATA + "=?";
        String[] SELECTION_ARGS = {
            videoPath
        };
        String ORDER_BY = null;

        String thumbData = null;

        Cursor cursor = cr.query(VIDEO_URI, PROJECTION, SELECTION, SELECTION_ARGS, ORDER_BY);
        if (cursor != null && cursor.moveToFirst()) {

            String videoId = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
            String[] thumbProjection = {
                MediaStore.Video.Thumbnails.DATA
            };
            String thumbSelection = MediaStore.Video.Thumbnails.VIDEO_ID + "=?";
            String[] thumbSelectionArgs = {
                videoId
            };
            String thumbSortOrder = null;

            Cursor thumbCursor = cr.query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    thumbProjection, thumbSelection, thumbSelectionArgs, thumbSortOrder);

            if (thumbCursor != null && thumbCursor.moveToFirst()) {
                thumbCursor.moveToFirst();
                thumbData = thumbCursor.getString(thumbCursor
                        .getColumnIndex(MediaStore.Video.Thumbnails.DATA));

            }

            if (thumbCursor != null) {
                thumbCursor.close();
            }

        }
        if (cursor != null) {
            cursor.close();
        }

        return thumbData;
    }

    private ArrayList<FileInfo> getFolderAndFile(String path) {

        File[] files = new File(path).listFiles();
        ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();

        if (files != null) {

            for (File f : files) {
                FileInfo fileInfo = new FileInfo();
                if (f.isDirectory()) {
                    fileInfo.folderName = f.getName();
                    fileInfo.data = f.getAbsolutePath();
                } else {
                    fileInfo.fileName = f.getName();
                    fileInfo.fileSize = f.length() + "";
                    fileInfo.data = f.getAbsolutePath();
                }

                fileList.add(fileInfo);
            }

        }

        return fileList;
    }

    private ArrayList<FileInfo> getAllFiles(String path) {
        File[] files = new File(path).listFiles();

        ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();

        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    fileList.addAll(getAllFiles(f.getAbsolutePath()));
                } else {
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.fileName = f.getName();
                    fileInfo.data = f.getAbsolutePath();
                    fileInfo.fileSize = f.length() + "";

                    fileList.add(fileInfo);
                }
            }
        }

        return fileList;
    }

    private class FileListTask extends AsyncTask<String, Void, ArrayList<FileInfo>> {

        private boolean isOpen;

        // controll the file add and remove
        private boolean isAdd;

        public FileListTask(boolean open, boolean add) {
            this.isOpen = open;
            this.isAdd = add;
        }

        @Override
        protected ArrayList<FileInfo> doInBackground(String... params) {

            ArrayList<FileInfo> list = new ArrayList<FileInfo>();

            if (isOpen) {
                // open the dir
                list = getFolderAndFile(params[0]);

            } else {
                // select all files in the dir, not implement in this version
                list = getAllFiles(params[0]);
            }

            if (mIsFirstCreate.get()) {
                mIsFirstCreate.set(false);
            }
            // } else {
            // mFileList.clear();
            // }
            // }
            // mFileList.addAll(list);
            mIsDataLoadFinished.set(true);

            if (mIsVisibleToUser.get()) {
                mHandler.sendEmptyMessage(DATA_LOAD_FINISH);
            }

            return list;

        }

        @Override
        protected void onPostExecute(ArrayList<FileInfo> resultList) {
            mFileList = resultList;
            // 为什么还要再sendMessage？
            if (mIsVisibleToUser.get()) {
                
                if (!mFileList.isEmpty()) {
                    mHandler.sendEmptyMessage(DATA_LOAD_FINISH);
                } else {
                    mHandler.sendEmptyMessage(ENPTY_BUCKET);
                }
            }
            super.onPostExecute(resultList);
        }
    }

    private class FileInfo {
        String folderName;

        String fileName;

        String fileSize;

        String data;

        boolean selected;
    }

    static class ViewHolder {
        ImageView icon;

        TextView folderName;

        TextView fileName;

        TextView fileSize;

        ImageView fileCheck;
    }

    @Override
    protected boolean onBackPressed() {

        if (!this.mCurrentPath.equals(SDCardUtil.getSDCardPath())) {

            String parentPath = null;

            if (this.mCurrentPath.endsWith("/"))
                this.mCurrentPath = this.mCurrentPath.substring(0, mCurrentPath.lastIndexOf("/"));

            parentPath = mCurrentPath.substring(0, mCurrentPath.lastIndexOf("/") + 1);

            boolean isOpen = true;
            boolean isAdd = false;

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                new FileListTask(isOpen, isAdd).execute(parentPath);
            } else {
                new FileListTask(isOpen, isAdd).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        parentPath);
            }

            changeCurrentPath(parentPath);
            this.mCurrentPath = parentPath;

            return true;
        } else {
            return false;
        }

    }

    private void changeCurrentPath(String path) {

        this.mLinearlayout.removeAllViews();

        TextView textViewRoot = getTextView(true);
        textViewRoot.setText(R.string.root_dir);
        textViewRoot.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                changeCurrentPath(SDCardUtil.getSDCardPath());
                boolean isOpen = true;
                boolean isAdd = false;

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    new FileListTask(isOpen, isAdd).execute(SDCardUtil.getSDCardPath());
                } else {
                    new FileListTask(isOpen, isAdd).executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR, SDCardUtil.getSDCardPath());
                }
            }
        });
        this.mLinearlayout.addView(textViewRoot);
        ImageView imageViewRoot = getImageView(true);
        this.mLinearlayout.addView(imageViewRoot);

        if (!path.equals(SDCardUtil.getSDCardPath())) {

            String childPath = path.substring(SDCardUtil.getSDCardPath().length());
            String[] pathItem = childPath.split("/");
            int count = pathItem.length;

            for (int i = 0; i < count; ++i) {
                TextView textView = getTextView(false);
                textView.setText(pathItem[i]);
                ImageView imageView = getImageView(false);
                this.mLinearlayout.addView(textView);
                this.mLinearlayout.addView(imageView);

                // 构造监听
                String tempPath = SDCardUtil.getSDCardPath();
                for (int j = 0; j <= i; ++j) {
                    tempPath += pathItem[j] + "/";
                }

                final String textPath = tempPath;
                textView.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        changeCurrentPath(textPath);
                        boolean isOpen = true;
                        boolean isAdd = false;

                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                            new FileListTask(isOpen, isAdd).execute(textPath);
                        } else {
                            new FileListTask(isOpen, isAdd).executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR, textPath);
                        }
                    }
                });
            }
        }
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

            if (this.mIsDataLoadFinished.get()) {
                this.mFileAdapter.setData(mFileList);
            } else {
                if (LogUtil.isLog) {
                    Log.e(TAG, "not visiable to user");
                }
            }

            if (mLoadingBar.getVisibility() == View.VISIBLE) {
                mLoadingBar.setVisibility(View.GONE);
            }

            if (mFileListView.getVisibility() == View.GONE) {
                mFileListView.setVisibility(View.VISIBLE);
            }

        }
    }

    private RobotoTextView getTextView(boolean isRootDirection) {
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);

        if (isRootDirection) {
            param.setMargins(dip2px(10), 0, 0, 0);
        }

        RobotoTextView textView = new RobotoTextView(getActivity());
        textView.setLayoutParams(param);
        textView.setPadding(dip2px(10), 0, dip2px(6), 0);

        textView.setTextSize((float)14);
        textView.setTextColor(Color.parseColor("#4d4d4d"));
        textView.setGravity(Gravity.CENTER_VERTICAL);
        return textView;

    }

    private ImageView getImageView(boolean isRootPath) {
        LayoutParams param = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        ImageView imageView = new ImageView(getActivity());
        imageView.setLayoutParams(param);
        if (isRootPath) {
            imageView.setImageResource(R.drawable.crumbs);
        } else {
            imageView.setImageResource(R.drawable.crumbsmini);
        }

        return imageView;
    }

    public int dip2px(float dpValue) {
        final float scale = this.getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5f);
    }

}
