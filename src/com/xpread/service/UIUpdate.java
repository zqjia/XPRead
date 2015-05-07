
package com.xpread.service;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.xpread.util.Const;

public class UIUpdate {

    public static final String REFRESH_TYPE = "refresh_type";
    public static final String FILE_PATH = "file_path";
    public static final String FILE_SIZE = "file_size";
    public static final String FILE_STATE = "file_state";
    public static final String REFRESH_LENGTH = "refresh_length";
    public static final String REFRESH_TIME = "refresh_time";
    public static final String REFRESH_PROGRESS = "refresh_progress";
    public static final String CLIENT_NAME = "client_name";
    public static final String CLIENT_PICTURE_ID = "client_picture_id";
    public static final String CLIENT_DEVICE_ID = "client_device_id";
    public static final String FILES_RECEIVE_NAME = "files_receive_name";
    public static final String FILES_RECEIVE_SIZE = "files_receive_size";
    public static final String FILES_RECEIVE_STATE = "files_receive_state";

    private ConcurrentLinkedQueue<Handler> mList = new ConcurrentLinkedQueue<Handler>();

    public void registHandler(Handler h) {
        // FIXME Message复用导致anr，此处根据需求先确保list里面只有一个Handler
        mList.clear();
        if (h == null) {
            return;
        }

        if (mList.contains(h)) {
            return;
        }

        mList.add(h);

    }

    public void unRegistHandler(Handler h) {
        if (h == null) {
            return;
        }
        mList.remove(h);
    }

    public void updateUI(Message msg) {
        for (Handler h : mList) {
            h.sendMessage(msg);
        }
    }

    public static Message getStateMessage(String filePath, int fileSize, int state) {
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt(REFRESH_TYPE, Const.REFRESH_FILE_TRANSFER_STATE);
        bundle.putString(FILE_PATH, filePath);
        bundle.putInt(FILE_SIZE, fileSize);
        bundle.putInt(FILE_STATE, state);
        msg.setData(bundle);
        return msg;
    }

    public static Message getTransferingMessage(String filePath, int refreshLength,
            int refreshTime, int progress, int fileSize) {
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt(REFRESH_TYPE, Const.REFRESH_FILE_TRANSFER_SPEED);
        bundle.putString(FILE_PATH, filePath);
        bundle.putInt(REFRESH_LENGTH, refreshLength);
        bundle.putInt(REFRESH_TIME, refreshTime);
        bundle.putInt(FILE_SIZE, fileSize);
        bundle.putInt(REFRESH_PROGRESS, progress);
        msg.setData(bundle);
        return msg;
    }

    public static Message getEstableConnect(String name) {
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt(REFRESH_TYPE, Const.REFRESH_ESTIBALE);
        bundle.putString(CLIENT_NAME, name);
        msg.setData(bundle);
        return msg;
    }

    public static Message getUserInformation(Message msg) {
        Bundle bundle = msg.getData();
        Message m = Message.obtain();
        Bundle b = new Bundle();
        b.putInt(REFRESH_TYPE, Const.REFRESH_USER_INFO);
        b.putString(CLIENT_NAME, bundle.getString("user-name"));
        b.putString(CLIENT_PICTURE_ID, bundle.getString("user-picture"));
        b.putString(CLIENT_DEVICE_ID, bundle.getString("user-device"));
        m.setData(b);
        return m;
    }

    public static Message getDisconnectInformation() {
        Message m = Message.obtain();
        Bundle b = new Bundle();
        b.putInt(REFRESH_TYPE, Const.REFRESH_DISCONNECTION);
        m.setData(b);
        return m;
    }

    public static Message getACKDisconnectInformation() {
        Message m = Message.obtain();
        Bundle b = new Bundle();
        b.putInt(REFRESH_TYPE, Const.REFRESH_ACK_DISCONNECTION);
        m.setData(b);
        return m;
    }

    /**
     * fileName 和 fileState 必须一一对应，且不能为空<br>
     * fileSize 可以为null
     * 
     * @param fileName
     * @param fileSize
     * @param fileState
     * @return
     */
    public static Message getReceiveFilesInformation(ArrayList<String> fileName,
            ArrayList<Integer> fileSize, ArrayList<Integer> fileState) {
        if (fileName == null || fileState == null) {
            return null;
        }
        if (fileSize == null) {
            fileSize = new ArrayList<Integer>();
            for (int i = 0; i < fileName.size(); i++) {
                fileSize.add(0);
            }
        }
        Message m = Message.obtain();
        Bundle b = new Bundle();
        b.putInt(REFRESH_TYPE, Const.REFRESH_FILES_RECEIVE);
        b.putStringArrayList(FILES_RECEIVE_NAME, fileName);
        b.putIntegerArrayList(FILES_RECEIVE_SIZE, fileSize);
        b.putIntegerArrayList(FILES_RECEIVE_STATE, fileState);
        m.setData(b);
        return m;
    }

}
