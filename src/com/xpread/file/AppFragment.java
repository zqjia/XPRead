
package com.xpread.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.TextView;

import com.twotoasters.jazzylistview.JazzyGridView;
import com.uc.base.wa.WaEntry;
import com.xpread.R;
import com.xpread.provider.FileBean;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;
import com.xpread.util.Utils;
import com.xpread.wa.WaKeys;

public class AppFragment extends BackHandledFragment implements OnItemClickListener {

    private static final String TAG = "AppFragment";

    File sourceFile;
    private AppAdapter mAppAdapter;
    private JazzyGridView mAppGridView;
    private ProgressBar mLoadingBar;

    class AppInfo {
        String appName;
        String sourceDir;
        Bitmap appIcon;
        long appSize;
        boolean selected;
    }

    ArrayList<AppInfo> mParticalAppList = new ArrayList<AppInfo>();
    ArrayList<AppInfo> mTotalAppList = new ArrayList<AppInfo>();
    private AtomicBoolean mIsPartialDataLoadFinished = new AtomicBoolean(false);
    private AtomicBoolean mIsTotalDataLoadFinished = new AtomicBoolean(false);
    private AtomicBoolean mIsVisibleToUser = new AtomicBoolean(false);
    private AtomicBoolean mIsFirstCreate = new AtomicBoolean(true);
    private AtomicBoolean mIsRefreshUiInHandler = new AtomicBoolean(false);

    private static final int PARTIAL_DATA_LOAD_FINISH = 0X0100;
    private static final int TOTAL_DATA_LOAD_FINISH = 0X0200;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case PARTIAL_DATA_LOAD_FINISH:
                    mAppAdapter.setData(mParticalAppList);

                    if (mLoadingBar.getVisibility() == View.VISIBLE) {
                        mLoadingBar.setVisibility(View.GONE);
                    }

                    if (mAppGridView.getVisibility() == View.GONE) {
                        mAppGridView.setVisibility(View.VISIBLE);
                    }

                    break;

                case TOTAL_DATA_LOAD_FINISH:
                    mAppAdapter.setData(mTotalAppList);

                    if (mLoadingBar.getVisibility() == View.VISIBLE) {
                        mLoadingBar.setVisibility(View.GONE);
                    }

                    if (mAppGridView.getVisibility() == View.GONE) {
                        mAppGridView.setVisibility(View.VISIBLE);
                    }

                    break;

                default:
                    break;
            }
        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.apps, container, false);

        new GetAppDataThread().start();

        mLoadingBar = (ProgressBar)view.findViewById(R.id.image_loading);
        mAppGridView = (JazzyGridView)view.findViewById(R.id.apps_list);
        mAppAdapter = new AppAdapter(this.mParticalAppList);
        mAppGridView.setAdapter(mAppAdapter);
        mAppGridView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileBean file = new FileBean();
        AppAdapter adapter = (AppAdapter)parent.getAdapter();
        final AppInfo info = (AppInfo)adapter.getItem(position);

        file.uri = info.sourceDir;
        file.fileName = info.appName;
        file.type = Const.TYPE_APP;
        file.icon = Utils.createBlobData(info.appIcon);
        
        //为显示选中文件列表准备
        //add by zqjia
        file.setThumbImage(info.appIcon);
        file.setSize(info.appSize);

        if (info.selected) {
            ((FilePickActivity)getActivity()).updateSelectCount(file, false);
            info.selected = false;
        } else {
            ((FilePickActivity)getActivity()).updateSelectCount(file, true);
            info.selected = true;
            WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SELECT_APK);
        }
        mAppAdapter.updateView(position);
    }

    class AppAdapter extends BaseAdapter {

        private ArrayList<AppInfo> appList;

        private LayoutInflater inflater;

        public AppAdapter(ArrayList<AppInfo> list) {
            this.appList = list;
            Context context = getActivity();
            if (context != null) {
                this.inflater = LayoutInflater.from(context);
            }
        }

        public void setData(ArrayList<AppInfo> list) {
            this.appList = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return this.appList.size();
        }

        @Override
        public Object getItem(int position) {
            return this.appList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;

            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = this.inflater.inflate(R.layout.app_item, parent, false);

                viewHolder.appIcon = (ImageView)convertView.findViewById(R.id.app_icon);
                viewHolder.appName = (TextView)convertView.findViewById(R.id.app_name);
                viewHolder.appSize = (TextView)convertView.findViewById(R.id.app_size);
                viewHolder.appSelectIcon = (ImageView)convertView
                        .findViewById(R.id.app_select_icon);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            final AppInfo appInfo = this.appList.get(position);

            if (appInfo.appName.lastIndexOf(".") > 0 ){
                viewHolder.appName.setText(appInfo.appName.substring(0, appInfo.appName.lastIndexOf(".")));
            } else {
                viewHolder.appName.setText(appInfo.appName);
            }
            
            if (appInfo.appIcon != null) {
                viewHolder.appIcon.setImageBitmap(appInfo.appIcon);
            } else {
                viewHolder.appIcon.setImageResource(R.drawable.app);
            }
            viewHolder.appSelectIcon.setVisibility(appInfo.selected ? View.VISIBLE : View.GONE);
            viewHolder.appSize.setText(Utils.getFileSizeForDisplay(appInfo.appSize));
            return convertView;
        }

        public void updateView(int itemIndex) {
            int visiblePosition = mAppGridView.getFirstVisiblePosition();
            View view = mAppGridView.getChildAt(itemIndex - visiblePosition);

            ViewHolder viewHolder = (ViewHolder)view.getTag();

            final AppInfo appInfo = (AppInfo)getItem(itemIndex);
            viewHolder.appSelectIcon.setVisibility(appInfo.selected ? View.VISIBLE : View.GONE);
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
                Log.e(TAG, "visiable tu user");
            }
            super.setSelectedFragment();
            this.mIsVisibleToUser.set(true);
            if (mIsFirstCreate.get()) {

                if (!mIsRefreshUiInHandler.get() && mIsTotalDataLoadFinished.get()) {
                    mAppAdapter.setData(mTotalAppList);
                    mIsFirstCreate.set(false);
                } else if (!mIsRefreshUiInHandler.get() && mIsPartialDataLoadFinished.get()) {
                    mAppAdapter.setData(mParticalAppList);
                } else {
                    if (LogUtil.isLog) {
                        Log.e(TAG, "app data haven't finish loading or refresh in handler already");
                    }
                    return;
                }
            } else {
                if (LogUtil.isLog) {
                    Log.e(TAG, "it's not first create , not create again");
                }
            }

            if (mLoadingBar.getVisibility() == View.VISIBLE) {
                mLoadingBar.setVisibility(View.GONE);
            }

            if (mAppGridView.getVisibility() == View.GONE) {
                mAppGridView.setVisibility(View.VISIBLE);
            }

        } else {
            if (LogUtil.isLog) {
                Log.e(TAG, "not visiable to user");
            }
            this.mIsVisibleToUser.set(false);
        }
    }

    static class ViewHolder {
        TextView appName;;

        ImageView appIcon;

        TextView appSize;

        ImageView appSelectIcon;
    }

    private class GetAppDataThread implements Runnable {

        public void start() {
            Thread thread = new Thread(this);
            if (thread != null) {
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.start();
            }
        }

        @Override
        public void run() {
            final PackageManager pm = getActivity().getPackageManager();
            final List<PackageInfo> packages = pm.getInstalledPackages(0);
            int index = 0;

            for (int i = 0; i < packages.size(); i++) {
                PackageInfo packageInfo = packages.get(i);
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    AppInfo tmp = new AppInfo();
                    tmp.appName = packageInfo.applicationInfo.loadLabel(pm).toString() + ".apk";
                    tmp.sourceDir = packageInfo.applicationInfo.sourceDir;
                    tmp.appIcon = ((BitmapDrawable)(packageInfo.applicationInfo.loadIcon(pm)))
                            .getBitmap();

                    sourceFile = new File(tmp.sourceDir);
                    if (sourceFile != null && sourceFile.exists()) {
                        tmp.appSize = sourceFile.length();
                    }

                    mParticalAppList.add(tmp);

                    if (mParticalAppList.size() == 20 && i < packages.size()) {
                        mIsPartialDataLoadFinished.set(true);
                        if (mIsFirstCreate.get() && mIsVisibleToUser.get()) {
                            mHandler.sendEmptyMessage(PARTIAL_DATA_LOAD_FINISH);
                            mIsRefreshUiInHandler.set(true);
                        }
                        index = i++;
                        break;
                    }
                }
            }

            if (index > 0) {
                mTotalAppList.addAll(mParticalAppList);
                for (int i = index; i < packages.size(); i++) {
                    PackageInfo packageInfo = packages.get(i);
                    if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        AppInfo tmp = new AppInfo();
                        tmp.appName = packageInfo.applicationInfo.loadLabel(pm).toString() + ".apk";
                        tmp.sourceDir = packageInfo.applicationInfo.sourceDir;
                        tmp.appIcon = ((BitmapDrawable)(packageInfo.applicationInfo.loadIcon(pm)))
                                .getBitmap();

                        sourceFile = new File(tmp.sourceDir);
                        if (sourceFile != null && sourceFile.exists()) {
                            tmp.appSize = sourceFile.length();
                        }
                        mTotalAppList.add(tmp);
                    }
                }

                mIsTotalDataLoadFinished.set(true);
                if (mIsFirstCreate.get() && mIsVisibleToUser.get()) {
                    mIsFirstCreate.set(false);
                    mHandler.sendEmptyMessage(TOTAL_DATA_LOAD_FINISH);
                    mIsRefreshUiInHandler.set(true);
                }
            } else {
                mTotalAppList.addAll(mParticalAppList);
                mHandler.sendEmptyMessage(TOTAL_DATA_LOAD_FINISH);
                mIsRefreshUiInHandler.set(true);
            }
        }

    }

}
