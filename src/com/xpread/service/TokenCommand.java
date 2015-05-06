
package com.xpread.service;

import java.util.Map;

import com.xpread.provider.UserInfo;
import com.xpread.util.Const;
import com.xpread.util.FileUtil;

public class TokenCommand {
    /**
     * 发送文件
     */
    public static final int MESSAGE_TYPE_SEND_FILES = 300;

    /**
     * 准备好接收文件
     */
    public static final int MESSAGE_TYPE_READY_FOR_RECEIVE_FILE = 301;

    /**
     * 取消文件
     */
    public static final int MESSAGE_TYPE_CANCEL_FILE = 302;

    /**
     * 用户信息
     */
    public static final int MESSAGE_TYPE_USER_INFORMAITION = 200;

    /**
     * 完成连接建立
     */
    public static final int MESSAGE_TYPE_CONNECT_ESTABLE = 100;

    /**
     * 断开连接
     */
    public static final int MESSAGE_TYPE_DISCONNECT = 101;

    /**
     * 确认断开连接
     */
    public static final int MESSAGE_TYPE_ACK_DISCONNECT = 102;

    // 不允许初始化
    private TokenCommand() {
    }

    public static byte[] createFileSendMessage(String[] filePaths, int[] size, String[] fileNames,
            boolean withoutPath) {
        if (filePaths == null) {
            return null;
        }
        String[] tempFiles = new String[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            if (withoutPath) {
                tempFiles[i] = FileUtil.getFileNameWithoutPath(filePaths[i]);
            } else {
                tempFiles[i] = filePaths[i];
            }
        }
        String end = "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("message-type:" + MESSAGE_TYPE_SEND_FILES);
        sb.append(end);
        StringBuilder content = new StringBuilder();
        if (tempFiles != null) {
            for (int i = 0; i < tempFiles.length; i++) {
                content.append(tempFiles[i]);
                content.append(":");
                content.append(size[i]);
                content.append(":");
                content.append(fileNames[i]);
                content.append(end);
            }
        }
        int contentLength = content.toString().getBytes().length;
        sb.append("content-length:");
        sb.append(contentLength);
        sb.append(end);
        sb.append(end);

        sb.append(content);
        return sb.toString().getBytes();
    }

    public static byte[] createConnecetEstableMessage() {
        String end = "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("message-type:" + MESSAGE_TYPE_CONNECT_ESTABLE);
        sb.append(end);
        sb.append("content-length:0");
        sb.append(end);
        sb.append(end);
        return sb.toString().getBytes();
    }

    public static byte[] createDisConnecetMessage() {
        String end = "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("message-type:" + MESSAGE_TYPE_DISCONNECT);
        sb.append(end);
        sb.append("content-length:0");
        sb.append(end);
        sb.append(end);
        return sb.toString().getBytes();
    }

    public static byte[] createACKDisConnecetMessage() {
        String end = "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("message-type:" + MESSAGE_TYPE_ACK_DISCONNECT);
        sb.append(end);
        sb.append("content-length:0");
        sb.append(end);
        sb.append(end);
        return sb.toString().getBytes();
    }

    public static byte[] httpGet(String mClientName) {
        String end = "\r\n";
        StringBuilder httpGet = new StringBuilder();
        httpGet.append("GET  ?socket=main&name=");
        httpGet.append(mClientName);
        httpGet.append(" HTTP/1.1");
        httpGet.append(end);
        httpGet.append("host:");
        httpGet.append(Const.REQUEST_URL);
        httpGet.append(end);
        httpGet.append(end);
        return httpGet.toString().getBytes();
    }

    public static byte[] createUserInfoMessage(UserInfo userInfo) {
        if (userInfo == null)
            return null;

        String end = "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("message-type:" + MESSAGE_TYPE_USER_INFORMAITION);
        sb.append(end);

        StringBuilder content = new StringBuilder();
        content.append("user-name:");
        content.append(userInfo.getUserName());
        content.append(end);
        content.append("user-picture:");
        content.append(userInfo.getPictureID());
        content.append(end);
        content.append("user-device:");
        content.append(userInfo.getDeviceName());
        content.append(end);

        int contentLength = content.toString().getBytes().length;
        sb.append("content-length:");
        sb.append(contentLength);
        sb.append(end);
        sb.append(end);
        sb.append(content);
        return sb.toString().getBytes();
    }

    public static byte[] createFileResponseMessage(Map<String, Integer> fileSizeMap,
            Map<String, Integer> fileStateMap, boolean withoutPath) {
        if (fileSizeMap == null || fileStateMap == null) {
            return null;
        }

        if (fileSizeMap.size() != fileStateMap.size()) {
            return null;
        }

        String end = "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("message-type:" + MESSAGE_TYPE_READY_FOR_RECEIVE_FILE);
        sb.append(end);
        StringBuilder content = new StringBuilder();
        for (String file : fileSizeMap.keySet()) {
            content.append(file);
            content.append(":");
            content.append(fileSizeMap.get(file));
            content.append(":");
            content.append(fileStateMap.get(file));
            content.append(end);
        }
        int contentLength = content.toString().getBytes().length;
        sb.append("content-length:");
        sb.append(contentLength);
        sb.append(end);
        sb.append(end);

        sb.append(content);
        return sb.toString().getBytes();
    }

    public static byte[] createFileCancelMessage(String file, int size, boolean withoutPath) {
        if (file == null) {
            return null;
        }
        if (withoutPath) {
            file = FileUtil.getFileNameWithoutPath(file);
        }
        String end = "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append("message-type:" + MESSAGE_TYPE_CANCEL_FILE);
        sb.append(end);
        StringBuilder content = new StringBuilder();
        content.append(file);
        content.append(":");
        content.append(size);
        content.append(":");
        content.append(Const.FILE_TRANSFER_CANCEL);
        content.append(end);
        int contentLength = content.toString().getBytes().length;
        sb.append("content-length:");
        sb.append(contentLength);
        sb.append(end);
        sb.append(end);
        sb.append(content);
        return sb.toString().getBytes();
    }
}
