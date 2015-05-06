
package com.xpread;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.uc.base.wa.WaEntry;
import com.xpread.adapter.FileRecordAdapter;
import com.xpread.adapter.FileRecordAdapter.DismissListener;
import com.xpread.control.Controller;
import com.xpread.provider.History;
import com.xpread.provider.RecordItem;
import com.xpread.service.FileTransferListener;
import com.xpread.swipelistview.BaseSwipeListViewListener;
import com.xpread.swipelistview.SwipeListView;
import com.xpread.util.Const;
import com.xpread.wa.WaKeys;

public class RecordsActivity extends BaseActivity {

    FileRecordAdapter mAdapter;

    List<RecordItem> mList = new ArrayList<RecordItem>();

    List<String> mPathList = new ArrayList<String>();

    SwipeListView mListView;

    private int mSreenWidth;

    private int mBackViewMargin;

    private ProgressBar mLoadingBar;

    private FileTransferListener mFileTransferListener = new FileTransferListener() {

        @Override
        public void fileStateChangeListener(String filePath, int state, int fileSize) {
            int pos = mPathList.indexOf(filePath);
            if (pos != -1 && mAdapter != null) {
                mAdapter.updateViewState(pos, state);
            }

        }

        @Override
        public void fileTranferingListener(String filePath, int speed, int progress, int fileSize) {
            int pos = mPathList.indexOf(filePath);

            if (pos != -1 && mAdapter != null) {
                int current = (int)(progress / (float)fileSize * 100);
                mAdapter.updateViewTransferInfo(pos, current, speed);
            }
        }

        @Override
        public void fileReceiveListener(List<String> files, List<Integer> fileSizes,
                List<Integer> fileStatus) {
            if (files == null || files.isEmpty()) {
                return;
            }
            new QueryTask().execute();

            int count = files.size();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_records);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        mSreenWidth = dm.widthPixels;

        mLoadingBar = (ProgressBar)findViewById(R.id.image_loading);
        mLoadingBar.setVisibility(View.VISIBLE);
        mBackViewMargin = getResources().getDimensionPixelSize(R.dimen.record_backview_margin);

        mListView = (SwipeListView)findViewById(R.id.record_list);
        mListView.setSwipeListViewListener(new BaseSwipeListViewListener() {

            @Override
            public int onChangeSwipeMode(int position) {

                RecordItem item = mList.get(position);
                switch (item.role) {
                    case Const.SENDER:
                        mListView.setOffsetLeft(mBackViewMargin);
                        mListView.setOffsetRight(mSreenWidth);
                        return SwipeListView.SWIPE_MODE_LEFT;
                    case Const.RECEIVER:
                        mListView.setOffsetLeft(mSreenWidth);
                        mListView.setOffsetRight(mBackViewMargin);
                        return SwipeListView.SWIPE_MODE_RIGHT;
                    default:
                        return super.onChangeSwipeMode(position);

                }
            }

            @Override
            public void onOpened(int position, boolean toRight) {

                RecordItem item = mList.get(position);
                switch (item.status) {
                    case Const.FILE_TRANSFER_COMPLETE:
                        WaEntry.statEpv(WaKeys.CATEGORY_XPREAD,
                                WaKeys.KEY_XPREAD_RECORD_EXPAND_SUCESS);
                        break;

                    case Const.FILE_TRANSFER_FAILURE:
                    case Const.FILE_TRANSFER_DOING:
                    case Const.FILE_TRANSFER_PREPARED:
                        WaEntry.statEpv(WaKeys.CATEGORY_XPREAD,
                                WaKeys.KEY_XPREAD_RECORD_EXPAND_OTHERS);
                        break;

                    default:
                        break;
                }

                super.onOpened(position, toRight);
            }

            @Override
            public void onClosed(int position, boolean fromRight) {

                if (mAdapter != null) {
                    mAdapter.resetView(position);
                }

            }

            @Override
            public void onStartOpen(int position, int action, boolean right) {

                mListView.closeOpenedItems();
            }

            @Override
            public void onClickFrontView(int position) {
                mListView.closeOpenedItems();
                mListView.toggleAnimate(position);
            }

        });

        new QueryTask().execute();

        mAdapter = new FileRecordAdapter(RecordsActivity.this, mListView, mList, null);
        mAdapter.setDismissListener(new DismissListener() {

            @Override
            public void onItemDismiss(int postion) {
                if (postion >= 0 && postion < mPathList.size()) {
                    mPathList.remove(postion);
                }
            }

            @Override
            public void onItemAdded(int postion, String fileName) {
                if (postion >= 0 && postion < mPathList.size()) {
                    mPathList.add(postion, fileName);
                }
            }
        });

        mListView.setAdapter(mAdapter);

        Controller.getInstance(getApplicationContext()).registFileTransferListener(
                mFileTransferListener);

        ImageView back = (ImageView)findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Controller.getInstance(getApplicationContext()).unRegistFileTransferListener(
                mFileTransferListener);
        super.onDestroy();
    }

    private class QueryTask extends AsyncTask<Void, Void, List<Object>> {

        @Override
        protected List<Object> doInBackground(Void... params) {
            List<RecordItem> list = new ArrayList<RecordItem>();
            List<String> path = new ArrayList<String>();

            Cursor cursor = getContentResolver().query(History.RecordsColumns.CONTENT_URI, null,
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);

                    RecordItem item = new RecordItem();
                    item.filePath = cursor.getString(cursor
                            .getColumnIndex(History.RecordsColumns.DATA));
                    item.fileName = cursor.getString(cursor
                            .getColumnIndex(History.RecordsColumns.DISPLAY_NAME));
                    item.role = cursor.getInt(cursor.getColumnIndex(History.RecordsColumns.ROLE));
                    item.status = cursor.getInt(cursor
                            .getColumnIndex(History.RecordsColumns.STATUS));
                    item.size = cursor.getInt(cursor.getColumnIndex(History.RecordsColumns.SIZE));
                    item.type = cursor.getInt(cursor.getColumnIndex(History.RecordsColumns.TYPE));
                    item.time = cursor.getLong(cursor
                            .getColumnIndex(History.RecordsColumns.TIME_STAMP));

                    String deviceId = cursor.getString(cursor
                            .getColumnIndex(History.RecordsColumns.TARGET));
                    if (deviceId == null) {
                        deviceId = "";
                    }

                    Cursor userCursor = getContentResolver().query(
                            History.FriendsColumns.CONTENT_URI, null,
                            History.FriendsColumns.DEVICE_ID + " = ? ", new String[] {
                                deviceId
                            }, null);

                    if (userCursor != null && userCursor.moveToFirst()) {
                        item.targetIcon = userCursor.getInt(userCursor
                                .getColumnIndex(History.FriendsColumns.PHOTO));
                        item.targetName = userCursor.getString(userCursor
                                .getColumnIndex(History.FriendsColumns.USER_NAME));
                    }

                    list.add(item);
                    path.add(item.filePath);

                    if (userCursor != null) {
                        userCursor.close();
                    }

                }

            }

            if (cursor != null) {
                cursor.close();
            }

            List<Object> lists = new ArrayList<Object>();
            lists.add(0, list);
            lists.add(1, path);

            return lists;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(List<Object> result) {
            super.onPostExecute(result);

            mLoadingBar.setVisibility(View.GONE);

            mList = (List<RecordItem>)result.get(0);
            mPathList = (List<String>)result.get(1);
            mAdapter.setData(mList);
        }

    }

    /*
     * add by zqjia call the BaseActivity method to start and stop watching the
     * home key
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
