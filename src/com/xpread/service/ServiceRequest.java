
package com.xpread.service;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;

import com.xpread.provider.UserInfo;

public class ServiceRequest {
    private static final String FILES_LIST = "files_list";

    private static final String FILE_PATH = "file_path";

    private static final String FILE_NAME = "file_name";

    private final static String REQUEST_COMMAND = "request_command";

    private final static String USER_INFO = "user_info";

    public final static int START_SERVER = 0;

    public final static int ESTABLISH_CONNECTION = 100;

    public final static int DISCONNECTION = 101;

    public final static int USER_INFORMATION_EXCHANGE = 200;

    public final static int SEND_FILES = 300;

    public final static int CANCEL_FILE_SEND = 400;

    private ArrayList<String> mFileList;

    private ArrayList<String> mFileName;

    private String mFilePath;

    private int mRequestType;

    private UserInfo mUserInfo;

    public static Intent toIntent(Context context, ServiceRequest s) {
        Intent i = new Intent(context, ServiceFileTransfer.class);
        i.putExtra(REQUEST_COMMAND, s.getRequestType());
        i.putExtra(FILE_PATH, s.getFilePath());
        i.putExtra(FILE_NAME, s.getFileName());
        i.putStringArrayListExtra(FILES_LIST, s.getFileList());
        i.putExtra(USER_INFO, s.getUserInfo());
        return i;
    }

    public static ServiceRequest createRequset(Intent i) {
        ServiceRequest s = new ServiceRequest();
        s.setRequestType(i.getIntExtra(REQUEST_COMMAND, 0));
        s.setFilePath(i.getStringExtra(FILE_PATH));
        s.setFileList(i.getStringArrayListExtra(FILES_LIST));
        s.setFileName(i.getStringArrayListExtra(FILE_NAME));
        s.setUserInfo((UserInfo)i.getSerializableExtra(USER_INFO));
        return s;
    }

    /**
     * @return the mFileList
     */
    public ArrayList<String> getFileList() {
        return mFileList;
    }

    /**
     * @param mFileList the mFileList to set
     */
    public void setFileList(ArrayList<String> mFileList) {
        this.mFileList = mFileList;
    }

    /**
     * @return the filePath
     */
    public String getFilePath() {
        return mFilePath;
    }

    /**
     * @param filePath the filePath to set
     */
    public void setFilePath(String filePath) {
        this.mFilePath = filePath;
    }

    /**
     * @return the requestType
     */
    public int getRequestType() {
        return mRequestType;
    }

    /**
     * @param requestType the requestType to set
     */
    public void setRequestType(int requestType) {
        this.mRequestType = requestType;
    }

    /**
     * @return the mUserInfo
     */
    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    /**
     * @param mUserInfo the mUserInfo to set
     */
    public void setUserInfo(UserInfo mUserInfo) {
        this.mUserInfo = mUserInfo;
    }

    /**
     * @return the mFileName
     */
    public ArrayList<String> getFileName() {
        return mFileName;
    }

    /**
     * @param mFileName the mFileName to set
     */
    public void setFileName(ArrayList<String> mFileName) {
        this.mFileName = mFileName;
    }
}
