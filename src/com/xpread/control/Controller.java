
package com.xpread.control;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.uc.base.wa.WaEntry;
import com.xpread.R;
import com.xpread.provider.FileBean;
import com.xpread.provider.History;
import com.xpread.provider.UserInfo;
import com.xpread.service.FileTransferListener;
import com.xpread.service.ServiceFileTransfer;
import com.xpread.service.ServiceRequest;
import com.xpread.service.UIUpdate;
import com.xpread.util.Const;
import com.xpread.util.FileUtil;
import com.xpread.util.LaboratoryData;
import com.xpread.util.LogUtil;
import com.xpread.util.Utils;
import com.xpread.wa.WaKeys;

public class Controller {

    private static final String TAG = "Controller";

    private volatile static Controller mController;

    private Context mContext;

    private WifiAdmin mWifiAdmin;
    private WifiApAdmin mWifiApAdmin;

    private FileTransferListener mFileTransferListener;
    private NetworkStateChangeListener mNetworkStateChangeListener;

    private List<FileBean> mTempFiles = new ArrayList<FileBean>();

    private int mRole = -1;

    private UserInfo mUserInfo;
    private UserInfo mTargetInfo = new UserInfo();

    private boolean isConnected;
    private boolean isWifiOpenBeforeUse = false;
    private boolean isFirstOpen = true;

    // 发送接收总进度
    private TotalFileProgress mTotalFileProgress;
    private ProgressDisplay mProgressDisplay;
    
    //显示接收文件后控制动画的接口
    private FileReceiveNotice mFileReceiveNotice;

    Handler fileTransferHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            int refreshType = bundle.getInt(UIUpdate.REFRESH_TYPE);
            switch (refreshType) {
                case Const.REFRESH_ESTIBALE: {
                    String name = bundle.getString(UIUpdate.CLIENT_NAME);
                    mTargetInfo.setUserName(name);
                    isConnected = true;
                    if (mNetworkStateChangeListener != null) {
                        mNetworkStateChangeListener.stateChangeListener(Const.REFRESH_ESTIBALE);
                    }

                    if (!mTempFiles.isEmpty() && mRole == Const.SENDER) {
                        handleTransferFiles();
                        sendFiles();
                    }
                }
                    break;
                case Const.REFRESH_USER_INFO: {
                    String name = bundle.getString(UIUpdate.CLIENT_NAME);
                    String picture_id = bundle.getString(UIUpdate.CLIENT_PICTURE_ID);
                    int pID = 0;
                    try {
                        pID = Integer.parseInt(picture_id);
                    } catch (NumberFormatException e) {
                        LogUtil.e(e.getMessage());
                    }
                    String device_id = bundle.getString(UIUpdate.CLIENT_DEVICE_ID);
                    setTargetInfo(name, pID, device_id);
                    if (mNetworkStateChangeListener != null) {
                        mNetworkStateChangeListener.stateChangeListener(Const.REFRESH_USER_INFO);
                    }
                }
                    break;
                case Const.REFRESH_FILES_RECEIVE: {
                    List<String> fileName = bundle.getStringArrayList(UIUpdate.FILES_RECEIVE_NAME);
                    List<Integer> fileSize = bundle
                            .getIntegerArrayList(UIUpdate.FILES_RECEIVE_SIZE);
                    List<Integer> fileState = bundle
                            .getIntegerArrayList(UIUpdate.FILES_RECEIVE_STATE);
                    addReceivedFilesToDB(fileName, fileSize, fileState);
                    if (mFileTransferListener != null) {
                        mFileTransferListener.fileReceiveListener(fileName, fileSize, fileState);
                    }
                }
                    break;
                case Const.REFRESH_FILE_TRANSFER_STATE: {

                    String filePath = bundle.getString(UIUpdate.FILE_PATH);
                    int fileSize = bundle.getInt(UIUpdate.FILE_SIZE);
                    int fileState = bundle.getInt(UIUpdate.FILE_STATE);
                    updateFileState(filePath, fileState, fileSize);
                    LogUtil.e("fileTransferHandler state-- > " + filePath + "----" + fileState
                            + "----" + fileSize);
                    if (mFileTransferListener != null) {
                        mFileTransferListener
                                .fileStateChangeListener(filePath, fileState, fileSize);
                    }

                    // 根据状态更新文件传送总进度
                    mTotalFileProgress.refreshInitProgress(filePath, fileSize, fileState, 0);
                    // FIXME 自测
                    // -----------------------------------------------------
                    processFileAccordToFileState(filePath, fileState);
                    // 错误状态文件
                    // ------------------------------------------------------

                }
                    break;
                case Const.REFRESH_FILE_TRANSFER_SPEED: {

                    String filePath = bundle.getString(UIUpdate.FILE_PATH);
                    int length = bundle.getInt(UIUpdate.REFRESH_LENGTH);
                    int time = bundle.getInt(UIUpdate.REFRESH_TIME);
                    int progress = bundle.getInt(UIUpdate.REFRESH_PROGRESS);
                    int fileSize = bundle.getInt(UIUpdate.FILE_SIZE);
                    if (mFileTransferListener != null) {
                        time /= 1000;
                        if (time != 0) {
                            int speed = length / time;
                            mFileTransferListener.fileTranferingListener(filePath, speed, progress,
                                    fileSize);
                        }
                    }
                    mTotalFileProgress.refreshInitProgress(filePath, fileSize,
                            Const.FILE_TRANSFER_DOING, length);

                }
                    break;
                // FIXME 真正断开连接----这里可能不需要控制层知道
                case Const.REFRESH_ACK_DISCONNECTION: {
                    isConnected = false;
                    resumeRoleToDefault();
                    Toast.makeText(mContext, R.string.connect_disconnect, Toast.LENGTH_SHORT)
                            .show();
                    // Log.e("mNetworkStateChangeListener", "" +
                    // (mNetworkStateChangeListener == null));
                    if (mNetworkStateChangeListener != null) {
                        mNetworkStateChangeListener
                                .stateChangeListener(Const.REFRESH_DISCONNECTION);
                    }
                    break;
                }

            }
            super.handleMessage(msg);
        }

    };

    WifiStateReceiver mWifiStateReceiver = new WifiStateReceiver(
            new WifiStateReceiver.WifiStateListener() {

                @Override
                public void onDisconnected() {

                }

                @Override
                public void onConnected(String ssid) {
                    if (ssid != null && ssid.contains("xpread")) {

                        switch (mRole) {
                            case Const.SENDER:
                                // 实验室数据--------------------------------------------------------------
                                LaboratoryData.gWifiEstablishEndSuccessTime = System
                                        .currentTimeMillis();
                                LaboratoryData
                                        .addOne(LaboratoryData.KEY_XPREAD_DATA_WIFI_ESTABLISH_SUCCESS);
                                // --------------------------------------------------------------------------------
                                establishConnection();
                                break;
                            default:
                                break;
                        }
                    }
                }
            });

    public void establishConnection() {
        ServiceRequest r = new ServiceRequest();
        r.setRequestType(ServiceRequest.ESTABLISH_CONNECTION);
        r.setUserInfo(mUserInfo);
        Intent i = ServiceRequest.toIntent(mContext, r);
        mContext.startService(i);
    }

    public static Controller getInstance(Context context) {

        if (mController == null) {
            synchronized (Controller.class) {
                if (mController == null) {
                    mController = new Controller(context);
                }
            }
        }
        return mController;
    }

    private Controller(Context context) {
        // 实验室数据
        LaboratoryData.put(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_BUFFER, "" + Const.BUFFER_SIZE);
        // --------------------------------------------
        this.mContext = context.getApplicationContext();
        this.mWifiAdmin = new WifiAdmin(context);
        this.mWifiApAdmin = new WifiApAdmin(context);

        ServiceFileTransfer.UPDATE.registHandler(fileTransferHandler);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        this.mContext.registerReceiver(mWifiStateReceiver, filter);

        Utils.saveDeviceId(mContext);
        mUserInfo = this.getUerInfo();

        // 总进度更新
        mTotalFileProgress = new TotalFileProgress(context);
        mProgressDisplay = new ProgressDisplay();
//        mTotalFileProgress.setRefreshInitProgress(mProgressDisplay);
        
        //主界面控制文件接收动画类
        mFileReceiveNotice = new FileReceiveNotice();
        mTotalFileProgress.setRefreshInitProgress(mFileReceiveNotice);
    }

    public final WifiAdmin getWifiAdmin() {
        return this.mWifiAdmin;
    }

    public final WifiApAdmin getWifiApAdmin() {
        return this.mWifiApAdmin;
    }

    public void cancelTransferFile(String file) {
        ServiceRequest r = new ServiceRequest();
        r.setRequestType(ServiceRequest.CANCEL_FILE_SEND);
        r.setFilePath(file);
        Intent i = ServiceRequest.toIntent(mContext, r);
        mContext.startService(i);
        updateCancelTransfer(file);
    }

    // add by zqjia
    // get the total transfer data size
    public long getTotalTransferDataSize() {
        return mTotalFileProgress.getTotalTransmission();
    }

    private void updateCancelTransfer(String file) {
        ContentValues values = new ContentValues();
        values.put(History.RecordsColumns.STATUS, Const.FILE_TRANSFER_CANCEL);
        mContext.getContentResolver()
                .update(History.RecordsColumns.CONTENT_URI,
                        values,
                        History.RecordsColumns.DATA + " = ? AND " + History.RecordsColumns.STATUS
                                + " = ? ", new String[] {
                                file, String.valueOf(Const.FILE_TRANSFER_DOING)
                        });
    }

    public void startService() {
        ServiceRequest r = new ServiceRequest();
        r.setRequestType(ServiceRequest.START_SERVER);
        r.setUserInfo(mUserInfo);
        Intent i = ServiceRequest.toIntent(mContext, r);
        mContext.startService(i);
    }

    public void setUserInfo(String name, String deviceID, int picID) {
        if (mUserInfo != null) {
            if (name != null) {
                mUserInfo.setUserName(name);
            }
            if (deviceID != null) {
                mUserInfo.setDeviceName(deviceID);
            }
            mUserInfo.setPictureID(picID);
        }
    }

    public void registFileTransferListener(FileTransferListener fileTransferListener) {
        if (mFileTransferListener != fileTransferListener) {
            mFileTransferListener = fileTransferListener;
        }
    }

    public void stopService() {
        Intent i = new Intent(mContext, ServiceFileTransfer.class);
        mContext.stopService(i);
    }

    public void unRegistFileTransferListener(FileTransferListener fileTransferListener) {
        if (mFileTransferListener == fileTransferListener)
            mFileTransferListener = null;
    }

    public void preTransferFiles(List<FileBean> files) {
        if (LogUtil.isLog) {
            Log.d(TAG, "preTransferFiles.");
        }

        mTempFiles.clear();
        mTempFiles.addAll(files);
    }

    public void clearTempFiles() {
        if (LogUtil.isLog) {
            Log.d(TAG, "clearTempFiles.");
        }

        mTempFiles.clear();
    }

    public void handleTransferFiles() {
        if (LogUtil.isLog) {
            Log.d(TAG, "handleTransferFiles.");
        }

        if (!mTempFiles.isEmpty()) {
            insertFilesToDB(mTempFiles);
        }

    }

    public void saveUserInformation(UserInfo info) {
        if (mContext != null) {
            Utils.saveUserInfo(mContext, info.getUserName(), info.getPictureID());
        }
    }

    public UserInfo getUerInfo() {
        if (mUserInfo == null) {
            mUserInfo = new UserInfo();
            mUserInfo.setUserName(Utils.getOwnerName(mContext));
            mUserInfo.setPictureID(Utils.getOwerIcon(mContext));
            mUserInfo.setDeviceName(Utils.getDeviceId(mContext));
        }

        return mUserInfo;
    }

    public UserInfo getTargetInfo() {
        return mTargetInfo;
    }

    public void setTargetInfo(String name, int icon, String deviceId) {
        mTargetInfo.setUserName(name);
        mTargetInfo.setPictureID(icon);
        mTargetInfo.setDeviceName(deviceId);

        // update friend table
        ContentValues values = new ContentValues();
        values.put(History.FriendsColumns.USER_NAME, name);
        values.put(History.FriendsColumns.PHOTO, icon);
        values.put(History.FriendsColumns.DEVICE_ID, deviceId);
        Cursor cursor = mContext.getContentResolver().query(History.FriendsColumns.CONTENT_URI,
                null, History.FriendsColumns.DEVICE_ID + " = ? ", new String[] {
                    deviceId
                }, null);
        if (cursor != null && cursor.getCount() == 1) {
            mContext.getContentResolver().delete(History.FriendsColumns.CONTENT_URI,
                    History.FriendsColumns.DEVICE_ID + " = ? ", new String[] {
                        deviceId
                    });
        }

        mContext.getContentResolver().insert(History.FriendsColumns.CONTENT_URI, values);

        if (cursor != null) {
            cursor.close();
        }
    }

    public boolean sendFiles() {
        if (mTempFiles != null && mTempFiles.size() > 0) {
            ArrayList<String> temp = new ArrayList<String>();
            ArrayList<String> name = new ArrayList<String>();
            for (int i = 0; i < mTempFiles.size(); i++) {
                String filePath = mTempFiles.get(i).uri;
                temp.add(filePath);

                name.add(mTempFiles.get(i).getFileName());

                // 实验室数据
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_TOTAL_FILE);
                LaboratoryData
                        .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_TOTAL_FILES);
                long s = FileUtil.getFileSizeByName(filePath);
                LaboratoryData.gTransferFileTotalSize += s;
                LaboratoryData.gTransferSendFileTotalSize += s;
                // ----------------------------------------
            }
            ServiceRequest r = new ServiceRequest();
            r.setRequestType(ServiceRequest.SEND_FILES);
            r.setFileList(temp);
            r.setFileName(name);
            Intent i = ServiceRequest.toIntent(mContext, r);

            mContext.startService(i);
            mTempFiles.clear();
            return true;
        } else {
            return false;
        }
    }

    private void insertFilesToDB(List<FileBean> files) {
        ContentValues values = new ContentValues();
        for (FileBean file : files) {
            values.clear();
            values.put(History.RecordsColumns.DATA, file.uri);
            values.put(History.RecordsColumns.TYPE, file.type);
            values.put(History.RecordsColumns.TIME_STAMP, System.currentTimeMillis());
            values.put(History.RecordsColumns.TARGET,
                    mTargetInfo == null ? "" : mTargetInfo.getDeviceName());
            values.put(History.RecordsColumns.STATUS, Const.FILE_TRANSFER_PREPARED);
            values.put(History.RecordsColumns.ROLE, Const.SENDER);

            File temp = new File(file.uri);
            values.put(History.RecordsColumns.SIZE, temp.length());
            // 更新发送文件数据
            mTotalFileProgress.addFile(file.uri, (int)temp.length(), Const.FILE_TRANSFER_PREPARED,
                    Const.SENDER);

            if (file.type == Const.TYPE_APP) {
                values.put(History.RecordsColumns.DISPLAY_NAME, file.getFileName());
            } else {
                values.put(History.RecordsColumns.DISPLAY_NAME, temp.getName());
            }

            mContext.getContentResolver().insert(History.RecordsColumns.CONTENT_URI, values);
        }

        mTotalFileProgress.addTotalTransmission(mTotalFileProgress.getSendTotalSize());
    }

    private void updateFileState(String path, int status, int size) {
        ContentValues values = new ContentValues();
        values.put(History.RecordsColumns.STATUS, status);

        if (status == Const.FILE_TRANSFER_DOING) {
            mContext.getContentResolver().update(
                    History.RecordsColumns.CONTENT_URI,
                    values,
                    History.RecordsColumns.DATA + " = ? AND " + History.RecordsColumns.STATUS
                            + " = ? ", new String[] {
                            path, String.valueOf(Const.FILE_TRANSFER_PREPARED)
                    });
        } else if (status == Const.FILE_TRANSFER_FAILURE) {
            mContext.getContentResolver().update(
                    History.RecordsColumns.CONTENT_URI,
                    values,
                    History.RecordsColumns.DATA + " = ? AND ( " + History.RecordsColumns.STATUS
                            + " = ? OR " + History.RecordsColumns.STATUS + " = ?) AND  "
                            + History.RecordsColumns.STATUS + " != ? ",
                    new String[] {
                            path, String.valueOf(Const.FILE_TRANSFER_PREPARED),
                            String.valueOf(Const.FILE_TRANSFER_DOING),
                            String.valueOf(Const.FILE_TRANSFER_CANCEL)
                    });
        } else if (status == Const.FILE_TRANSFER_CANCEL) {
            mContext.getContentResolver().update(
                    History.RecordsColumns.CONTENT_URI,
                    values,
                    History.RecordsColumns.DATA + " = ? AND ( " + History.RecordsColumns.STATUS
                            + " = ? OR " + History.RecordsColumns.STATUS + " = ? OR "
                            + History.RecordsColumns.STATUS + " = ? ) ",
                    new String[] {
                            path, String.valueOf(Const.FILE_TRANSFER_PREPARED),
                            String.valueOf(Const.FILE_TRANSFER_DOING),
                            String.valueOf(Const.FILE_TRANSFER_FAILURE)
                    });
        } else {
            mContext.getContentResolver().update(
                    History.RecordsColumns.CONTENT_URI,
                    values,
                    History.RecordsColumns.DATA + " = ? AND ( " + History.RecordsColumns.STATUS
                            + " = ? OR " + History.RecordsColumns.STATUS + " = ? ) ",
                    new String[] {
                            path, String.valueOf(Const.FILE_TRANSFER_PREPARED),
                            String.valueOf(Const.FILE_TRANSFER_DOING),

                    });
        }

    }

    private void insertReceiveFile(String path, int size, int status) {
        ContentValues values = new ContentValues();
        values.put(History.RecordsColumns.DATA, path);
        values.put(History.RecordsColumns.ROLE, Const.RECEIVER);
        File file = new File(path);
        values.put(History.RecordsColumns.DISPLAY_NAME, file.getName());
        values.put(History.RecordsColumns.SIZE, size);
        values.put(History.RecordsColumns.STATUS, status);
        values.put(History.RecordsColumns.TARGET,
                mTargetInfo == null ? "" : mTargetInfo.getDeviceName());
        values.put(History.RecordsColumns.TYPE, Utils.getFileType(path));
        values.put(History.RecordsColumns.TIME_STAMP, System.currentTimeMillis());

        mContext.getContentResolver().insert(History.RecordsColumns.CONTENT_URI, values);

    }

    public int getRole() {
        return mRole;
    }

    public void toBeSender() {
        if (this.mRole == -1) {
            this.mRole = Const.SENDER;
            // Log.e("@@@@@@@@@@@@@@@@@@@@@@@@@", " set role " + mRole);
        }
    }

    public void toBeReceiver() {
        if (this.mRole == -1) {
            this.mRole = Const.RECEIVER;
            // Log.e("@@@@@@@@@@@@@@@@@@@@@@@@@", " set role " + mRole);
        }
    }

    public interface NetworkStateChangeListener {
        public void stateChangeListener(int state);
    }

    public NetworkStateChangeListener getNetworkStateChangeListener() {
        return mNetworkStateChangeListener;
    }

    public void setNetworkStateChangeListener(NetworkStateChangeListener mNetworkStateChangeListener) {
        this.mNetworkStateChangeListener = mNetworkStateChangeListener;
    }

    public void unRegisterNetworkStateChangeListener(
            NetworkStateChangeListener networkStateChangeListener) {
        if (networkStateChangeListener == mNetworkStateChangeListener) {
            this.mNetworkStateChangeListener = null;
        }
    }

    // update progress bar listener
    // add by zqjia
    //----------------------------------------------//
    //主界面不需要显示进度，因此该接口已不使用
    public interface ProgressChangeListener {
        public void setProgress(int progress, int role);
    }

    public void setProgressChangeListener(ProgressChangeListener listener) {
        // mProgressChangeListener = listener;
        this.mProgressDisplay.setProgressChangeListener(listener);
    }

    public void unRegisterProgressChangeListener() {
        this.mProgressDisplay.unRegistProgressChangeListener();
    }
    //-----------------------------------------------//
    
    //-----------------------------------------------//
    //用于控制主界面在有文件接收时候动画显示的接口
    public interface FileReceiveNoticeListener {
        public void animationStart();
    }
    
    public void setFileReceiverNoticeListener(FileReceiveNoticeListener listener) {
        this.mFileReceiveNotice.setFileReceiveNoticeListener(listener);
    }
    
    public void unRegistFileReceiveNoticeListener() {
        this.mFileReceiveNotice.unRegistFileReceiveNoticeListener();
    }
  //-----------------------------------------------//

    public boolean isConnected() {
        return isConnected;
    }

    public void disconnect() {
        if (isConnected) {
            ServiceRequest r = new ServiceRequest();
            r.setRequestType(ServiceRequest.DISCONNECTION);
            Intent disconnectIntent = ServiceRequest.toIntent(mContext, r);
            mContext.startService(disconnectIntent);
            isConnected = false;
        } else {
            stopService();
        }
    }

    public void updateTargetInfo() {

    }

    private void addReceivedFilesToDB(List<String> files, List<Integer> fileSizes,
            List<Integer> fileStatus) {
        if (files == null || fileSizes == null || fileStatus == null) {
            // Log.d(TAG, "receive files info null.");
            return;
        }

        if (files.size() != fileSizes.size() || files.size() != fileStatus.size()) {
            // Log.d(TAG, "get receive files info failed.");
            return;
        }

        int count = files.size();
        for (int i = 0; i < count; i++) {
            String path = files.get(count - 1 - i);
            int size = fileSizes.get(count - 1 - i);
            int status = fileStatus.get(count - 1 - i);
            insertReceiveFile(path, size, status);

            // 实验室数据
            LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_FILES);
            LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_TOTAL_FILE);
            LaboratoryData.gTransferFileTotalSize += size;
            LaboratoryData.gTransferReceiveFileTotalSize += size;
            // --------------------------------
        }

        mTotalFileProgress.addFiles(files, fileSizes, fileStatus, Const.RECEIVER);
        mTotalFileProgress.refreshInitProgress(Const.RECEIVER);

        // 总发送和接受大小
        mTotalFileProgress.addTotalTransmission(mTotalFileProgress.getReceiveTotalSize());
    }

    public void setWifiStateBeforeUse(boolean isWifiOpen) {
        isWifiOpenBeforeUse = isWifiOpen;
        isFirstOpen = false;
    }

    public void setIsFirstOpen(boolean isFirstOpen) {
        this.isFirstOpen = isFirstOpen;
    }

    public boolean isWifiOpenBeforeUse() {
        return isWifiOpenBeforeUse;
    }

    public boolean isFirstOpen() {
        return isFirstOpen;
    }

    /**
     * 根据文件状态对文件进行操作（目前主要是失败或者取消删除文件)
     * 
     * @param filePath
     * @param fileState
     */
    private void processFileAccordToFileState(String filePath, int fileState) {
        if (mTotalFileProgress.isInReciveFileList(filePath)
                || mTotalFileProgress.isInSendFileList(filePath)) {
            waFileTransfer(filePath, fileState);
        }
        if (fileState == Const.FILE_TRANSFER_FAILURE || fileState == Const.FILE_TRANSFER_CANCEL
                || fileState == Const.FILE_TRANSFER_COMPLETE) {
            if (mTotalFileProgress.isInReciveFileList(filePath)) {

                mTotalFileProgress.removeFileInReceiveList(filePath);
                if (fileState == Const.FILE_TRANSFER_FAILURE
                        || fileState == Const.FILE_TRANSFER_CANCEL) {
                    // Log.e("@@@@", "delete " + filePath + " -----------------"
                    // + fileState + " in "
                    // + mTotalFileProgress.toString());
                    FileUtil.deleteFileByPath(filePath);
                }

            } else if (mTotalFileProgress.isInSendFileList(filePath)) {
                mTotalFileProgress.removeFileInSendList(filePath);
            } else {
                // Log.e("@@@@",
                // "not contain " + filePath + " in --- " +
                // mTotalFileProgress.toString());
            }
        }
    }

    /*
     * add by zqjia call when back to MainActivity without connecttion
     */
    public void resumeToDefault() {
        mTempFiles.clear();
        mTotalFileProgress.clear();
        stopService();
        this.mRole = -1;
    }

    private void resumeRoleToDefault() {
        mTotalFileProgress.clear();
        mRole = -1;
    }

    public int getCurrentSendProgress() {
        return mTotalFileProgress.getSendCurrentProgress();
    }

    public int getCurrentReceiverProgress() {
        return mTotalFileProgress.getReceiveCurrentProgress();
    }

    public void setCurrentSendProgress(int progress) {
        this.mProgressDisplay.setCurrentProgress(progress, Const.SENDER);
    }

    public void setCurrentReceiveProgress(int progress) {
        this.mProgressDisplay.setCurrentProgress(progress, Const.RECEIVER);
    }

    private void waFileTransfer(String filePath, int status) {
        switch (status) {
            case Const.FILE_TRANSFER_DOING:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECORD_START);
                break;
            case Const.FILE_TRANSFER_COMPLETE:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECORD_SUCESS);

                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_TOTAL_SUCCESS_FILE);
                if (mTotalFileProgress.isInSendFileList(filePath)) {
                    LaboratoryData
                            .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_SUCCESS_FILES);
                }
                if (mTotalFileProgress.isInReciveFileList(filePath)) {
                    LaboratoryData
                            .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_SUCCESS_FILES);
                }
                break;
            case Const.FILE_TRANSFER_FAILURE: {
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECORD_FAILURE);

                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_TOTAL_FAIL_FILE);
                if (mTotalFileProgress.isInSendFileList(filePath)) {
                    LaboratoryData
                            .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_FAIL_FILES);
                }
                if (mTotalFileProgress.isInReciveFileList(filePath)) {
                    LaboratoryData
                            .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_FAIL_FILES);
                }
            }
                break;
            case Const.FILE_TRANSFER_CANCEL: {
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_TOTAL_CANCEL_FILE);
                if (mTotalFileProgress.isInSendFileList(filePath)) {
                    LaboratoryData
                            .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_SEND_COUNT_CANCEL_FILES);
                }
                if (mTotalFileProgress.isInReciveFileList(filePath)) {
                    LaboratoryData
                            .addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_COUNT_CANCEL_FILES);
                }
            }
                break;

            default:
                break;
        }
    }

}
