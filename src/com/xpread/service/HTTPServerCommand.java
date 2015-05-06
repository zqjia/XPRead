
package com.xpread.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Bundle;
import android.os.Message;

import com.xpread.util.Const;
import com.xpread.util.FileUtil;

public class HTTPServerCommand {
    private MyLog myLog = new MyLog(HTTPServerCommand.class.getSimpleName());

    private final ConcurrentHashMap<String, File> mReceiveFiles = new ConcurrentHashMap<String, File>();

    private final ConcurrentLinkedQueue<String> mNeedSendFiles = new ConcurrentLinkedQueue<String>();

    private final ConcurrentHashMap<String, String> mSendingFilesMap = new ConcurrentHashMap<String, String>();

    public byte[] getRecevieFilesTokenResponse(Message msg) {
        Bundle bundle = msg.getData();
        if (bundle == null) {
            return null;
        }
        // -------------------------------------------------------------------------
        // FIXME 测试代码

        ArrayList<String> filePath = bundle.getStringArrayList("path");
        ArrayList<String> fileName = bundle.getStringArrayList("name");
        ArrayList<Integer> fileSize = bundle.getIntegerArrayList("size");

        Map<String, Integer> fileSizeMap = new HashMap<String, Integer>();
        Map<String, Integer> fileStateMap = new HashMap<String, Integer>();

        // ----------------------------------------------------------------------
        // 用于更新文件数据的本地链表
        ArrayList<String> loaclUIPath = new ArrayList<String>();
        ArrayList<Integer> localUIState = new ArrayList<Integer>();
        // --------------------------------------------------------------------

        for (int i = 0; i < filePath.size(); i++) {
            fileSizeMap.put(filePath.get(i), fileSize.get(i));
            if (FileUtil.isFileExist(fileName.get(i), fileSize.get(i))) {
                fileStateMap.put(filePath.get(i), Const.FILE_TRANSFER_COMPLETE);

                // 如果本地文件已经存在
                loaclUIPath.add(FileUtil.getPathByName(fileName.get(i), fileSize.get(i)));
                localUIState.add(Const.FILE_TRANSFER_COMPLETE);
                
                //TODO -------------------
                //文件传送成功
                //-------------------------
            } else {
                mReceiveFiles.put(filePath.get(i), FileUtil.getFileByName(fileName.get(i)));
                fileStateMap.put(filePath.get(i), Const.FILE_TRANSFER_PREPARED);

                // 如果本地文件不存在
                loaclUIPath.add(mReceiveFiles.get(filePath.get(i)).getAbsolutePath());
                localUIState.add(Const.FILE_TRANSFER_PREPARED);
            }
        }
        // ----------------------------------------------------------------------------
        ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getReceiveFilesInformation(loaclUIPath,
                fileSize, localUIState));
        // -----------------------------------------------------------------------------

        return TokenCommand.createFileResponseMessage(fileSizeMap, fileStateMap, false);
    }

    public void addNeedSendFiles(List<String> filePaths, List<String> fileNames) {
        if (filePaths == null || fileNames == null) {
            return;
        }
        if (filePaths.size() == 0 || fileNames.size() != filePaths.size()) {
            return;
        }
        for (int i = 0; i < filePaths.size(); i++) {
            String path = filePaths.get(i);
            String name = fileNames.get(i);
            if (!mNeedSendFiles.contains(path) && !mSendingFilesMap.containsKey(path)) {
                mNeedSendFiles.add(path);
                mSendingFilesMap.put(path, name);
            } else {
                myLog.e("can not send this file because mNeedSendFiles or mSendingFiles contain before send ");
            }
        }
    }

    public int getNeedSendFilesSize() {
        return mNeedSendFiles.size();
    }

    public byte[] getNeedSendFileTokenCommand() {
        if (mNeedSendFiles.size() == 0) {
            return null;
        }
        String[] filesTemp = null;
        synchronized (HTTPServerCommand.class) {
            filesTemp = new String[mNeedSendFiles.size()];
            int i = 0;
            while (mNeedSendFiles.size() > 0) {
                filesTemp[i] = mNeedSendFiles.remove();
                i++;
            }
        }
        int[] sizes = new int[filesTemp.length];
        String[] names = new String[filesTemp.length];
        for (int j = 0; j < filesTemp.length; j++) {
            if (mSendingFilesMap.containsKey(filesTemp[j])) {
                sizes[j] = FileUtil.getFileSizeByName(filesTemp[j]);
                names[j] = mSendingFilesMap.get(filesTemp[j]);
            }
        }
        return TokenCommand.createFileSendMessage(filesTemp, sizes, names, false);
    }

    public boolean cancelFile(String filePath) {
        if (mSendingFilesMap.containsKey(filePath)) {
            mSendingFilesMap.remove(filePath);
            return true;
        } else {
            String cancelKey = null;
            for (String key : mReceiveFiles.keySet()) {
                if (mReceiveFiles.get(key).getAbsolutePath().equals(filePath)) {
                    cancelKey = key;
                    break;
                }
            }
            if (cancelKey != null) {
                mReceiveFiles.remove(cancelKey);
                return true;
            } else {
                myLog.e(filePath + " can not be cancel because not contain in receive file list");
            }
            return false;
        }

    }

    public void dealFileResponse(Message msg) {
        int messageType = (Integer)msg.obj;
        if (messageType != TokenCommand.MESSAGE_TYPE_READY_FOR_RECEIVE_FILE) {
            return;
        }
        Bundle bundle = msg.getData();
        if (bundle == null) {
            return;
        }
        // -----------------------------------------------------------------------------
        // FIXME 测试代码
        List<String> filePath = bundle.getStringArrayList("path");
        List<Integer> fileState = bundle.getIntegerArrayList("state");
        Map<String, Integer> fileStateMap = new HashMap<String, Integer>();
        for (int i = 0; i < filePath.size(); i++) {
            fileStateMap.put(filePath.get(i), fileState.get(i));
        }
        // --------------------------------------------------------------------------------

        for (String file : fileStateMap.keySet()) {
            if (fileStateMap.get(file) != Const.FILE_TRANSFER_PREPARED) {
                // FIXME 如果接受方不需要接收，更新UI
                myLog.e("peer not need receive " + file);
                mSendingFilesMap.remove(file);
                // ----------------------------------------------------------------------------
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(file, 0,
                        fileStateMap.get(file)));
                // ----------------------------------------------------------------------------
            } else if (!mSendingFilesMap.containsKey(file)) {
                myLog.e("why not send this file ????? mSendingFiles not contain  ");
                mSendingFilesMap.remove(file);
                // ----------------------------------------------------------------------------
                ServiceFileTransfer.UPDATE.updateUI(UIUpdate.getStateMessage(file, 0,
                        fileStateMap.get(file)));
                // ----------------------------------------------------------------------------
            }
        }
    }

    public String getChanelFilePath(String filePath) {

        if (mSendingFilesMap.containsKey(filePath)) {
            return filePath;
        }

        File f = new File(filePath);
        if (mReceiveFiles.containsValue(f)) {
            for (String fp : mReceiveFiles.keySet()) {
                if (mReceiveFiles.get(fp).equals(f)) {
                    return fp;
                }
            }
        }
        myLog.e("发送取消请求，但没有发现需要的文件" + filePath);
        return null;
    }

    public String getLocalFilePath(String filePath) {
        if (mSendingFilesMap.containsKey(filePath)) {
            return filePath;
        }

        if (mReceiveFiles.containsKey(filePath)) {
            return mReceiveFiles.get(filePath).getAbsolutePath();
        }
        myLog.e("接收到取消请求后没有发现需要的文件" + filePath);
        return null;

    }

    public final File getReceiveFileByFileName(String filePath) {
        return mReceiveFiles.get(filePath);
    }

    public final boolean isNeedReceiveThisFile(String filePath) {
        return mReceiveFiles.containsKey(filePath);
    }

    public final void removeReceiveFileByFileName(String filePath) {
        mReceiveFiles.remove(filePath);
    }

    public final boolean isNeedSendThisFile(String filePath) {
        return mSendingFilesMap.containsKey(filePath);
    }

    public final void removeSendFileByFilePath(String filePath) {
        mSendingFilesMap.remove(filePath);
    }

    /**
     * 获得遗留没有完成传送的文件
     * 
     * @return
     */
    public ArrayList<String> getReserveFiles() {
        ArrayList<String> list = new ArrayList<String>();
        for (String file : mSendingFilesMap.keySet()) {
            if (file != null) {
                list.add(file);
            }
        }
        for (String file : mReceiveFiles.keySet()) {
            String f = mReceiveFiles.get(file).getAbsolutePath();
            list.add(f);
        }
        return list;
    }

    public void clear() {
        mNeedSendFiles.clear();
        mReceiveFiles.clear();
        mSendingFilesMap.clear();
    }
}
