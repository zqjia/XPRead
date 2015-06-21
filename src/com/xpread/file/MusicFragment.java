/**
 * <p>Title: xpread</p>
 *
 * <p>Description: </p>
 * 音乐资源选择界面
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
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

import com.uc.base.wa.WaEntry;
import com.xpread.R;
import com.xpread.provider.FileBean;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;
import com.xpread.util.Utils;
import com.xpread.wa.WaKeys;

public class MusicFragment extends BackHandledFragment implements OnItemClickListener {

    private static final String TAG = "MusicFragment";

    private ArrayList<MusicInfo> mMusicList = new ArrayList<MusicInfo>();

    private ListView mMusicListView;

    private ProgressBar mLoadingBar;

    private MusicAdapter mAdapter;

    private class MusicInfo {
        String title;

        String singer;

        long size;

        boolean selected;

        String data;
    }

    private static class ViewHolder {
        ImageView cover;;

        TextView title;

        TextView singer;

        TextView size;

        ImageView check;
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
                    mAdapter.setData(mMusicList);
                    
                    if (mLoadingBar.getVisibility() == View.VISIBLE) {
                        mLoadingBar.setVisibility(View.GONE);
                    }
                    
                    if (mMusicListView.getVisibility() == View.GONE) {
                        mMusicListView.setVisibility(View.VISIBLE);
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
        View view = inflater.inflate(R.layout.music, container, false);

        this.mMusicListView = (ListView)view.findViewById(R.id.music_list);
        this.mMusicListView.setDividerHeight(0);
        mAdapter = new MusicAdapter(mMusicList);
        mMusicListView.setAdapter(mAdapter);
        mMusicListView.setOnItemClickListener(this);
        this.mLoadingBar = (ProgressBar)view.findViewById(R.id.music_loading);

        return view;

    }

    private class MusicAdapter extends BaseAdapter {

        private ArrayList<MusicInfo> musicList;

        private LayoutInflater inflater;

        public MusicAdapter(ArrayList<MusicInfo> list) {
            this.musicList = list;
            this.inflater = LayoutInflater.from(getActivity());
        }

        public void setData(ArrayList<MusicInfo> list) {
            this.musicList = list;
            notifyDataSetChanged();
        }
        
        @Override
        public int getCount() {
            return this.musicList.size();
        }

        @Override
        public Object getItem(int position) {
            return this.musicList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.music_item, parent, false);
                holder.cover = (ImageView)convertView.findViewById(R.id.music_cover);
                holder.title = (TextView)convertView.findViewById(R.id.music_title);
                holder.singer = (TextView)convertView.findViewById(R.id.music_singer);
                holder.size = (TextView)convertView.findViewById(R.id.music_size);
                holder.check = (ImageView)convertView.findViewById(R.id.music_check);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            final MusicInfo info = mMusicList.get(position);
            holder.cover.setImageResource(R.drawable.music);
            holder.title.setText(info.title);
            holder.singer.setText(info.singer);
            holder.size.setText(Utils.getFileSizeForDisplay(info.size));
            holder.check.setImageResource(info.selected ? R.drawable.check : R.drawable.checkbox);
            return convertView;
        }
        
        public void updateView(int itemIndex) {
            int visiblePosition = mMusicListView.getFirstVisiblePosition();
            View view = mMusicListView.getChildAt(itemIndex - visiblePosition);
            final MusicInfo info = mMusicList.get(itemIndex);
            
            ViewHolder viewHolder = (ViewHolder)view.getTag();
            viewHolder.check.setImageResource(info.selected ? R.drawable.check
                    : R.drawable.checkbox);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        FileBean file = new FileBean();
        MusicAdapter adapter = (MusicAdapter)parent.getAdapter();
        MusicInfo info = (MusicInfo)adapter.getItem(position);

        file.uri = info.data;
        file.type = Const.TYPE_MUSIC;
        if (info.selected) {
            info.selected = false;
            ((FilePickActivity)getActivity()).updateSelectCount(file, false);
        } else {
            info.selected = true;
            ((FilePickActivity)getActivity()).updateSelectCount(file, true);

            WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SELECT_MUSIC);
        }

        adapter.updateView(position);
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

            final Uri MUSIC_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] PROJECTION = {
                    MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATA
            };
            String OREDER_BY = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

            Cursor musicCursor = null;
            if (this.context != null) {
                musicCursor = this.context.getContentResolver().query(MUSIC_URI, PROJECTION, null,
                        null, OREDER_BY);
            } else {
                return null;
            }

            if (musicCursor != null && musicCursor.moveToFirst()) {
                int count = musicCursor.getCount();

                if (LogUtil.isLog) {
                    Log.e(TAG, "music count from media provider is " + count);
                }

                do {
                    String path = musicCursor.getString(musicCursor
                            .getColumnIndex(MediaStore.Audio.Media.DATA));

                    if (isFileExist(path)) {
                        MusicInfo info = new MusicInfo();
                        info.title = musicCursor.getString(musicCursor
                                .getColumnIndex(MediaStore.Audio.Media.TITLE));
                        info.singer = musicCursor.getString(musicCursor
                                .getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        info.size = musicCursor.getLong(musicCursor
                                .getColumnIndex(MediaStore.Audio.Media.SIZE));
                        info.data = path;
                        mMusicList.add(info);
                    }
                } while (musicCursor.moveToNext());
            }

            if (musicCursor != null) {
                musicCursor.close();
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
            
            if (LogUtil.isLog) {
                Log.e(TAG, "visiable to user");
            }

           if (this.mIsFirstCreate.get() && this.mIsDataLoadFinished.get()) {
               mAdapter.setData(mMusicList);
               this.mIsFirstCreate.set(false);
           } else {
               if (this.mIsFirstCreate.get()) {
                   if (LogUtil.isLog) {
                       Log.e(TAG, "data load haven't finish, wait the handler to refresh UI");
                   }
               }
               return ;
           }

           if (mLoadingBar.getVisibility() == View.VISIBLE) {
               mLoadingBar.setVisibility(View.GONE);
           }
           
           if (mMusicListView.getVisibility() == View.GONE) {
               mMusicListView.setVisibility(View.VISIBLE);
           }

        } else {
            if (LogUtil.isLog) {
                Log.e(TAG, "not visiable to user");
            }
            this.mIsVisibleToUser.set(false);

        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}
