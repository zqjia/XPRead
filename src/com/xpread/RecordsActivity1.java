package com.xpread;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
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

public class RecordsActivity1 extends BaseActivity implements View.OnClickListener{

    FileRecordAdapter mFileRecordsAdapter;
    List<RecordItem> mList = new ArrayList<RecordItem>();
    List<String> mPathList = new ArrayList<String>();
    SwipeListView mListView;

    private int mSreenWidth;
    private int mBackViewMargin;
    private ProgressBar mLoadingBar;
    private Button mOrderByTimeButton;
    private Button mOrderBySizeButton;
    private Button mDeleteAllButton;

    private static final int DEFAULT = -1;
    private static final int SORT_BY_TIME = 1000;
    private static final int SORT_BY_SIZE = 1001;
    private static final int DELETE_ALL = 1003;

    private FileTransferListener mFileTransferListener = new FileTransferListener() {

        @Override
        public void fileStateChangeListener(String filePath, int state, int fileSize) {
            int pos = mPathList.indexOf(filePath);
            if (pos != -1 && mFileRecordsAdapter != null) {
                mFileRecordsAdapter.updateViewState(pos, state);
            }
        }

        @Override
        public void fileTranferingListener(String filePath, int speed, int progress, int fileSize) {
            int pos = mPathList.indexOf(filePath);

            if (pos != -1 && mFileRecordsAdapter != null) {
                int current = (int) (progress / (float) fileSize * 100);
                mFileRecordsAdapter.updateViewTransferInfo(pos, current, speed);
            }
        }

        @Override
        public void fileReceiveListener(List<String> files, List<Integer> fileSizes,
                List<Integer> fileStatus) {
            if (files == null || files.isEmpty()) {
                return;
            }
            new QueryTask(DEFAULT).execute();
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

        mLoadingBar = (ProgressBar) findViewById(R.id.image_loading);
        mLoadingBar.setVisibility(View.VISIBLE);
        mBackViewMargin = getResources().getDimensionPixelSize(R.dimen.record_backview_margin);

        mOrderBySizeButton = (Button) findViewById(R.id.order_by_size);
        mOrderByTimeButton = (Button) findViewById(R.id.order_by_time);
        mDeleteAllButton = (Button) findViewById(R.id.delete_all);
        mOrderBySizeButton.setOnClickListener(this);
        mOrderByTimeButton.setOnClickListener(this);
        mDeleteAllButton.setOnClickListener(this);
        
        mListView = (SwipeListView) findViewById(R.id.record_list);
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

                if (mFileRecordsAdapter != null) {
                    mFileRecordsAdapter.resetView(position);
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

        new QueryTask(DEFAULT).execute();

        mFileRecordsAdapter = new FileRecordAdapter(RecordsActivity1.this, mListView, mList, null);
        mFileRecordsAdapter.setDismissListener(new DismissListener() {

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

        mListView.setAdapter(mFileRecordsAdapter);

        ImageView back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    protected void onDestroy() {
        super.onDestroy();
    }

    private class QueryTask extends AsyncTask<Void, Void, List<Object>> {

        /**
         * 增加三种处理类型：按时间排序，按大小排序和一键删除
         * */
        private int handleType;
        private List<RecordItem> recordItemList = new ArrayList<RecordItem>();
        private List<String> recordItemPathList = new ArrayList<String>();

        public QueryTask(int handleType) {
            this.handleType = handleType;
            recordItemList.clear();
            recordItemPathList.clear();
        }

        @Override
        protected List<Object> doInBackground(Void... params) {
            List<Object> resultList = new ArrayList<Object>();
            
            switch (handleType) {
                case DEFAULT:
                    resultList = getRecords(null);
                    break;
                case SORT_BY_TIME:
                    resultList = getRecords(History.RecordsColumns.TIME_STAMP);
                    break;
                case SORT_BY_SIZE:
                    resultList = getRecords(History.RecordsColumns.SIZE);
                    break;
                case DELETE_ALL:
                    RecordsActivity1.this.getContentResolver().delete(History.RecordsColumns.CONTENT_URI, null, null);
                    break;
                default:
                    break;
            }
            return resultList;
            
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(List<Object> result) {
            super.onPostExecute(result);

            if (result != null && !result.isEmpty() && result.size() == 2) {
                mLoadingBar.setVisibility(View.GONE);
                
                mList = (List<RecordItem>) result.get(0);
                mPathList = (List<String>) result.get(1);
                mFileRecordsAdapter.setData(mList);
            } else {
                mList.clear();
                mFileRecordsAdapter.setData(mList);
            }
        }

        /**
         * 根据传入参数获取查询结果
         * @param order 查询顺序，为null表示没有查询顺序，其他则表示根据相应条件降序查询
         * @return List<Object> 查询结果，包括recordItem的列表和recordItemPath的列表
         * */
        private List<Object> getRecords(String orderBy) {
            Cursor cursor = null;
            if (orderBy != null) {
                cursor = getContentResolver().query(History.RecordsColumns.CONTENT_URI, 
                    null, null, null, orderBy + " desc");
            } else {
                cursor = getContentResolver().query(History.RecordsColumns.CONTENT_URI, 
                    null, null, null, null);
            }

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    RecordItem item = new RecordItem();
                    item.filePath =
                            cursor.getString(cursor.getColumnIndex(History.RecordsColumns.DATA));
                    item.fileName =
                            cursor.getString(cursor
                                    .getColumnIndex(History.RecordsColumns.DISPLAY_NAME));
                    item.role = cursor.getInt(cursor.getColumnIndex(History.RecordsColumns.ROLE));
                    item.status =
                            cursor.getInt(cursor.getColumnIndex(History.RecordsColumns.STATUS));
                    item.size = cursor.getInt(cursor.getColumnIndex(History.RecordsColumns.SIZE));
                    item.type = cursor.getInt(cursor.getColumnIndex(History.RecordsColumns.TYPE));
                    item.time =
                            cursor.getLong(cursor.getColumnIndex(History.RecordsColumns.TIME_STAMP));

                    String deviceId =
                            cursor.getString(cursor.getColumnIndex(History.RecordsColumns.TARGET));
                    if (deviceId == null) {
                        deviceId = "";
                    }

                    Cursor userCursor =
                            getContentResolver().query(History.FriendsColumns.CONTENT_URI, null,
                                    History.FriendsColumns.DEVICE_ID + " = ? ",
                                    new String[] {deviceId}, null);

                    if (userCursor != null && userCursor.moveToFirst()) {
                        item.targetIcon =
                                userCursor.getInt(userCursor
                                        .getColumnIndex(History.FriendsColumns.PHOTO));
                        item.targetName =
                                userCursor.getString(userCursor
                                        .getColumnIndex(History.FriendsColumns.USER_NAME));
                    }

                    recordItemList.add(item);
                    recordItemPathList.add(item.filePath);

                    if (userCursor != null) {
                        userCursor.close();
                    }
                } while (cursor.moveToNext());
            }

            //关闭cursor
            if (cursor != null) {
                cursor.close();
            }
            
            List<Object> lists = new ArrayList<Object>();
            lists.add(0, recordItemList);
            lists.add(1, recordItemPathList);
            return lists;
        }
    }

    /*
     * add by zqjia call the BaseActivity method to start and stop watching the home key
     */
    @Override
    protected void onResume() {
        super.onResume();
        Controller.getInstance(getApplicationContext()).registFileTransferListener(
                mFileTransferListener);
    }

    @Override
    protected void onPause() {
        Controller.getInstance(getApplicationContext()).unRegistFileTransferListener(
                mFileTransferListener);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(RecordsActivity1.this, MainActivity.class);
        startActivity(intent);
        super.onBackPressed();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.order_by_time:
                new QueryTask(SORT_BY_TIME).execute();
                break;
            case R.id.order_by_size:
                new QueryTask(SORT_BY_SIZE).execute();
                break;
            case R.id.delete_all:
                new QueryTask(DELETE_ALL).execute();
                break;
            default:
                break;
        }
    }
}
