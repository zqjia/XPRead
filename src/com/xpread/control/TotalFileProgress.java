/**
 * <p>Title: ucweb</p>
 *
 * <p>Description: </p>
 * 解析中间件协议
 * <p>Copyright: Copyright (c) 2010</p>
 *
 * <p>Company: ucweb.com</p>
 *
 * @author wujm@ucweb.com
 * @version 1.0
 */

package com.xpread.control;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.xpread.util.Const;
import com.xpread.util.Utils;

public class TotalFileProgress {

    private RefreshInitProgress mRefreshInitProgress;

    // 主界面进度更新数据
    // 接收
    private long mReceiveTotalSize = 0;
    private long mHasReceiveSize = 0;
    private Map<String, Integer> mReceiveFileMap = new HashMap<String, Integer>();

    // 发送
    private long mSendTotalSize = 0L;
    private long mHasSendSize = 0L;
    private Map<String, Integer> mSendFileMap = new HashMap<String, Integer>();
    private Map<String, Integer> mFileCurrentProgress = new HashMap<String, Integer>();

    // total transfer size (byte)
    private long mTotalTransmission = -1L;
    private Context mContext;

    public TotalFileProgress(Context context) {
        mContext = context;
    }

    /**
     * 将正在传送的文件加入到接收文件列表 filePaths和fileSizes和fileState必须一一对应
     * 
     * @param filePaths
     * @param fileSizes
     * @param fileState
     * @param role <br>
     *            0 表示添加发送总进度<br>
     *            1 表示添加接收总进度
     */
    public void addFiles(List<String> filePaths, List<Integer> fileSizes, List<Integer> fileState,
            int role) {
        if (filePaths == null || fileSizes == null || fileState == null) {
            throw new NullPointerException("null" + filePaths + fileSizes + fileState);
        }

        if (filePaths.size() != fileSizes.size() || fileSizes.size() != fileState.size()) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < filePaths.size(); i++) {
            if (filePaths == null || fileSizes == null || fileState == null) {
                throw new NullPointerException("null---for" + filePaths + fileSizes + fileState);
            }
            String file = filePaths.get(i);
            int size = fileSizes.get(i);
            int state = fileState.get(i);
            addFile(file, size, state, role);
        }

    }

    /**
     * 主界面总进度更新 只有正在传的文件全部传完，才清空进度
     * 
     * @param filePath
     * @param fileSize
     * @param fileState
     * @param update 更新文件进度量
     */
    public void refreshInitProgress(String filePath, int fileSize, int fileState, int update) {
        int size = 0;
        if (mReceiveFileMap.containsKey(filePath) && mFileCurrentProgress.containsKey(filePath)) {
            size = mReceiveFileMap.get(filePath);
            int progress = mFileCurrentProgress.get(filePath);
            if (fileState == Const.FILE_TRANSFER_CANCEL
                    || fileState == Const.FILE_TRANSFER_COMPLETE
                    || fileState == Const.FILE_TRANSFER_FAILURE) {

                update = size - progress;
            }
            // 每个文件进度
            mFileCurrentProgress.put(filePath, progress + update);
            // 总进度
            mHasReceiveSize += update;

            refreshInitProgress(Const.RECEIVER);

            if (mHasReceiveSize >= mReceiveTotalSize) {
                clearReceiveState();
            }

        } else if (mSendFileMap.containsKey(filePath) && mFileCurrentProgress.containsKey(filePath)) {
            size = mSendFileMap.get(filePath);
            int progress = mFileCurrentProgress.get(filePath);
            if (fileState == Const.FILE_TRANSFER_CANCEL
                    || fileState == Const.FILE_TRANSFER_COMPLETE
                    || fileState == Const.FILE_TRANSFER_FAILURE) {
                update = size - progress;
            }
            // 每个文件进度
            mFileCurrentProgress.put(filePath, progress + update);
            // 总进度
            mHasSendSize += update;

            refreshInitProgress(Const.SENDER);

            if (mHasSendSize >= mSendTotalSize) {
                clearSendState();
            }

        } else {
            return;
        }

    }

    public void addFile(String path, int size, int status, int role) {
        if (size == 0) {
            return;
        }

        if (mSendFileMap.containsKey(path) || mReceiveFileMap.containsKey(path)) {
            return;
        }
        switch (role) {
            case Const.SENDER:
                mSendTotalSize += size;
                mSendFileMap.put(path, size);
                mFileCurrentProgress.put(path, 0);
                break;
            case Const.RECEIVER:
                mReceiveTotalSize += size;
                if (status == Const.FILE_TRANSFER_COMPLETE || status == Const.FILE_TRANSFER_FAILURE
                        || status == Const.FILE_TRANSFER_CANCEL) {
                    mHasReceiveSize += size;
                    mFileCurrentProgress.put(path, size);
                } else {
                    mFileCurrentProgress.put(path, 0);
                }
                mReceiveFileMap.put(path, size);
                break;

            default:
                break;
        }

    }

    /**
     * 更新界面(总进度更新) 必须注册了ProgressChangeListener调用这个方法才有效
     * 
     * @param role 0 表示更新发送总进度 1 表示更新接收总进度
     */
    public void refreshInitProgress(int role) {
        switch (role) {
            case Const.SENDER:
                if (mRefreshInitProgress != null) {
                    if (mSendTotalSize == 0) {
                        return;
                    }
                    int p = (int)((100 * mHasSendSize / mSendTotalSize));
                    mRefreshInitProgress.refreshInitProgress(p, Const.SENDER);
                }

                break;

            case Const.RECEIVER:
                if (mRefreshInitProgress != null) {
                    if (mReceiveTotalSize == 0) {
                        return;
                    }
                    int p = (int)((100 * mHasReceiveSize / mReceiveTotalSize));
                    mRefreshInitProgress.refreshInitProgress(p, Const.RECEIVER);
                }

                break;
            default:

                break;
        }
    }

    public interface RefreshInitProgress {
        /**
         * 更新最原始的进度数据，不对进度做任何处理
         * 
         * @param initProgress 原始进度
         * @param role 角色
         */
        public void refreshInitProgress(int initProgress, int role);
    }

    /**
     * 增加文件总共传送量
     * 
     * @param addValue
     * @return
     */
    public long addTotalTransmission(long addValue) {

        if (addValue < 0) {
            throw new IllegalArgumentException("value can not be negative");
        }
        if (addValue != 0) {
            long temp = Utils.getTotalTransmission();
            if (temp > mTotalTransmission) {
                mTotalTransmission = temp;
            }
            mTotalTransmission += addValue;
            Utils.saveTotalTransmission(mTotalTransmission);
        }
        return mTotalTransmission;
    }

    public long getTotalTransmission() {
        if (mTotalTransmission <= 0) {
            mTotalTransmission = Utils.getTotalTransmission();
        }
        return mTotalTransmission;
    }

    private void clearSendState() {
        for (String key : mSendFileMap.keySet()) {
            mFileCurrentProgress.remove(key);
        }
        // 发送端已经传送完
        // 清空数据
        mHasSendSize = 0L;
        mSendTotalSize = 0L;
    }

    private void clearReceiveState() {
        for (String key : mReceiveFileMap.keySet()) {
            mFileCurrentProgress.remove(key);
        }
        // 接收端已经传送完
        // 清空数据
        mHasReceiveSize = 0L;
        mReceiveTotalSize = 0L;
    }

    public void clear() {
        mSendFileMap.clear();
        mReceiveFileMap.clear();
        mFileCurrentProgress.clear();
        clearSendState();
        clearReceiveState();
    }

    public final long getReceiveTotalSize() {
        return mReceiveTotalSize;
    }

    public final long getSendTotalSize() {
        return mSendTotalSize;
    }

    public boolean isInReciveFileList(String filePath) {
        return mReceiveFileMap.containsKey(filePath);
    }

    public void removeFileInReceiveList(String filePath) {
        mReceiveFileMap.remove(filePath);
    }

    public void removeFileInSendList(String filePath) {
        mSendFileMap.remove(filePath);
    }

    @Override
    public String toString() {
        return mReceiveFileMap.toString();
    }

    public boolean isInSendFileList(String filePath) {
        return mSendFileMap.containsKey(filePath);
    }

    public RefreshInitProgress getRefreshInitProgress() {
        return mRefreshInitProgress;
    }

    public void setRefreshInitProgress(RefreshInitProgress mRefreshInitProgress) {
        this.mRefreshInitProgress = mRefreshInitProgress;
    }

    /**
     * 主动获取当前发送进度
     * 
     * @return -1 表示当前没有进度
     */
    public int getSendCurrentProgress() {
        // Log.e("get send", mHasSendSize +
        // "----------------------------------------"
        // + mSendTotalSize);
        if (mHasSendSize == 0L || mSendTotalSize == 0L) {
            return -1;
        }

        if (mHasSendSize >= mSendTotalSize) {
            return -1;
        }
        return (int)((100 * mHasSendSize) / mSendTotalSize);
    }

    /**
     * 主动获取当前接收进度
     * 
     * @return -1 表示当前没有进度
     */
    public int getReceiveCurrentProgress() {
        // Log.e("get receive", mHasReceiveSize +
        // "----------------------------------------"
        // + mReceiveTotalSize);
        if (mHasReceiveSize == 0L || mReceiveTotalSize == 0L) {
            return -1;
        }

        if (mHasReceiveSize >= mReceiveTotalSize) {
            return -1;
        }
        return (int)((100 * mHasReceiveSize) / mReceiveTotalSize);
    }

}
