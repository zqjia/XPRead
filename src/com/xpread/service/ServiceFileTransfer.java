
package com.xpread.service;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

import com.xpread.R;
import com.xpread.provider.UserInfo;
import com.xpread.service.HTTPClientCommand.ExcuteHttpConnection;
import com.xpread.service.HTTPSession.ReceviceTokenRequest;
import com.xpread.service.TokenSession.ReceiveTokenServe;
import com.xpread.transfer.exception.FileTransferCancelException;
import com.xpread.util.Const;
import com.xpread.util.LaboratoryData;
import com.xpread.util.Utils;
//FIXME 下次重构注意点
//1.消息对称性（不能太依赖上层实现）
//2.WIFI管理功能
//3.断开连接和取消文件的新逻辑，和超时ACK的新实现.
//4.画出服务的状态转换图，重构每个状态能完成的任务

public class ServiceFileTransfer extends Service implements ReceviceTokenRequest,
        ReceiveTokenServe, ExcuteHttpConnection {
    public final static UIUpdate UPDATE = new UIUpdate();

    // 已经连接超时
    private final static int SERVICE_TIME_OUT = 300000;
    // 检查连接
    private final static int SERVICE_CHECK_CONNECT_TIME = 10000;
    // 连接超时s
    private final static int CONNECT_TIME_OUT = 30000;
    // 读取超时
    public final static int READ_TIME_OUT = 30000;
    // 断开连接超时
    private final static int DISCONNECT_TIME_OUT = 2000;
    // 重连接时间间隔
    private final static int RECONNECT_TIME = 5000;
    // 重连次数
    private final static int RECONNECT_TIMES = 15;
    private final static int SERVICE_CLIENT_TYPE = -1;
    private final static int SERVICE_SERVER_TYPE = 1;
    private final static String STR_IP = Const.STR_IP;
    // 服务器or客户端
    private int mServiceType = 0;
    
    // 服务器
    private ServerSocket mServerSocket;
    private AtomicBoolean isServerOpen;
    private Thread mServerSocketThread;
    private HTTPServerCommand mHttpServerCommand;
    private AtomicBoolean isClientConnect;
    
    // 客户端
    private Thread mClientConnectThread;
    private AtomicBoolean isConnectionEstablish;
    private HTTPClientCommand mHttpCLientCommand;
    private ConcurrentLinkedQueue<HttpURLConnection> mHttpURLConnections;

    // 共有
    private UserInfo mUserInfo = new UserInfo("", "", 1);
    private ThreadTokenSocket mTokenOutputThread;
    private ThreadTokenSocket mTokenInputThread;
    private TokenSession mTokenSession;
    private Socket mTokenSocket;
    private HashSet<Socket> mSockets;
    private AtomicBoolean isDisConnection;
    private final int mPort = 9898;
    private final String requestURL = "http://" + Const.REQUEST_URL;
    private ThreadPoolExecutor mExecutor;
    private Context mContext;
    private MyLog myLog = new MyLog(ServiceFileTransfer.class.getSimpleName());
    private Handler mStopHandler;

    // 超时自停止
    private AtomicInteger mConnectCount;
    private AtomicBoolean isTimeOutDisconnect;

    // 自停止
    private final Runnable mStopSelfRunnable = new Runnable() {
        @Override
        public void run() {
            if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
                throw new IllegalStateException(
                        "This runnable can only be called in the Main thread!");
            }

            if (isDisConnection.get()) {
                // 实验室数据 超时没有收到ACK服务停止
                // -----------------------------------------------
                LaboratoryData
                        .addOne(LaboratoryData.KEY_XPREAD_DATA_CONNECT_DISCONNECT_ACK_TIME_OUT);
                // ---------------------------------------------------------------------
                stopSelf();
                return;
            }

            if ((isClientConnect.get() || isConnectionEstablish.get()) && mConnectCount.get() == 0) {
                if (isTimeOutDisconnect.get()) {
                    // 实验室数据
                    LaboratoryData
                            .addOne(LaboratoryData.KEY_XPREAD_DATA_CONNECT_DISCONNECT_NO_FILE_SEND_TIME_OUT);
                    // ----------------
                    Toast.makeText(mContext, R.string.service_no_file_send_time_out,
                            Toast.LENGTH_SHORT).show();
                    disConnection();
                    return;
                }
            }
        }
    };

    // 检查连接状态
    private final Runnable mCheckConnectStateRunnable = new Runnable() {

        @Override
        public void run() {
            if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
                throw new IllegalStateException(
                        "This runnable can only be called in the Main thread!");
            }

            if (isDisConnection.get()) {
                return;
            }
            // 判断是否需要准备超时断开连接
            if ((isClientConnect.get() || isConnectionEstablish.get()) && mConnectCount.get() == 0) {
                // 重置超時
                if (!isTimeOutDisconnect.get()) {
                    isTimeOutDisconnect.set(true);
                    mStopHandler.removeCallbacks(mStopSelfRunnable);
                    mStopHandler.postDelayed(mStopSelfRunnable, SERVICE_TIME_OUT);
                }
            } else if ((isClientConnect.get() || isConnectionEstablish.get())
                    && mConnectCount.get() > 0) {
                isTimeOutDisconnect.set(false);
                mStopHandler.removeCallbacks(mStopSelfRunnable);
            }
            // 每10s检查一下连接状态
            mStopHandler.postDelayed(mCheckConnectStateRunnable, SERVICE_CHECK_CONNECT_TIME);
        }
    };

    @SuppressLint("HandlerLeak")
    Handler tokenHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int messageType = (Integer)msg.obj;
            switch (mServiceType) {
                case SERVICE_SERVER_TYPE: {
                    switch (messageType) {
                        case TokenCommand.MESSAGE_TYPE_USER_INFORMAITION: {
                            UPDATE.updateUI(UIUpdate.getUserInformation(msg));
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_SEND_FILES: {
                            try {
                                mTokenSession.sendMessageFromToken(mHttpServerCommand
                                        .getRecevieFilesTokenResponse(msg));
                            } catch (Exception e) {
                                myLog.logException(e);
                            }
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_READY_FOR_RECEIVE_FILE: {
                            // 只是更新UI
                            mHttpServerCommand.dealFileResponse(msg);
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_CANCEL_FILE: {
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_DISCONNECT: {
                            isDisConnection.set(true);
                            try {
                                mTokenSession.clearAllMessageAndSendThisMessage(TokenCommand
                                        .createACKDisConnecetMessage());
                            } catch (Exception e) {
                                myLog.logException(e);
                            }
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_ACK_DISCONNECT: {
                            // 收到连接断开确认后立即停止服务
                            // 实验室数据
                            // 用户收到ACK断开连接计时--------------------------------------------------------------
                            LaboratoryData
                                    .addOne(LaboratoryData.KEY_XPREAD_DATA_CONNECT_DISCONNECT_ACK_SUCCESS);
                            // --------------------------------------------------------------------------------
                            mStopHandler.removeCallbacks(mStopSelfRunnable);
                            // 告诉上层整个连接已经断开
                            stopSelf();
                            break;
                        }
                    }
                    break;
                }
                case SERVICE_CLIENT_TYPE: {
                    switch (messageType) {
                        case TokenCommand.MESSAGE_TYPE_CONNECT_ESTABLE: {
                            try {
                                UPDATE.updateUI(UIUpdate.getEstableConnect(""));
                                isConnectionEstablish.set(true);
                                // 建立连接第一时刻同时发送要发送的文件信息和用户信息
                                mTokenSession.sendMessageFromToken(TokenCommand
                                        .createUserInfoMessage(mUserInfo));
                                // 如果有文件需要发送，那么就发送文件信息
                                if (mHttpCLientCommand.isFileNeedSend()) {
                                    mTokenSession.sendMessageFromToken(mHttpCLientCommand
                                            .getNeedSendFileTokenCommand());
                                }
                            } catch (Exception e) {
                                myLog.logException(e);
                            }
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_USER_INFORMAITION: {
                            UPDATE.updateUI(UIUpdate.getUserInformation(msg));
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_SEND_FILES: {
                            try {
                                mTokenSession.sendMessageFromToken(mHttpCLientCommand
                                        .getRecevieFilesTokenResponse(msg));
                                mHttpCLientCommand.dealFilesResponse(msg);
                            } catch (Exception e) {
                                myLog.logException(e);
                            }
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_READY_FOR_RECEIVE_FILE: {
                            // 唯一实际发送文件接口
                            mHttpCLientCommand.dealFilesResponse(msg);
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_CANCEL_FILE: {
                            // FIXME 更新取消文件信息,现在以异常处理
                            // String filePath =
                            // msg.getData().getStringArrayList("path").get(0);
                            // filePath =
                            // mHttpCLientCommand.getLocalFilePath(filePath);
                            // UPDATE.updateUI(UIUpdate.getStateMessage(filePath,
                            // 0,
                            // Const.FILE_TRANSFER_CANCEL));
                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_DISCONNECT: {
                            isDisConnection.set(true);
                            try {
                                mTokenSession.sendMessageFromToken(TokenCommand
                                        .createACKDisConnecetMessage());
                            } catch (Exception e) {
                                myLog.logException(e);
                                // Toast.makeText(mContext, "Token消息为null",
                                // Toast.LENGTH_SHORT).show();
                            }

                            break;
                        }
                        case TokenCommand.MESSAGE_TYPE_ACK_DISCONNECT: {
                            // 实验室数据
                            // 用户收到ACK断开连接计时--------------------------------------
                            LaboratoryData
                                    .addOne(LaboratoryData.KEY_XPREAD_DATA_CONNECT_DISCONNECT_ACK_SUCCESS);
                            // --------------------------------------------------------------------------------
                            mStopHandler.removeCallbacks(mStopSelfRunnable);
                            // 收到连接断开确认后立即停止服务
                            stopSelf();
                            break;
                        }

                    }
                    break;
                }
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate() {
        myLog.e("service onCreate");
        // 实验室数据
        LaboratoryData.gTotalServiceBeginCPUTime = LaboratoryData.getTotalCpuTime();
        LaboratoryData.gServiceBeginCPUTime = LaboratoryData.getAppCpuTime();
        // --------------------------------------------------
        mExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(3);
        mContext = getApplicationContext();
        isDisConnection = new AtomicBoolean(false);
        // 服务器
        isServerOpen = new AtomicBoolean(false);
        isClientConnect = new AtomicBoolean(false);
        mSockets = new HashSet<Socket>();
        mHttpServerCommand = new HTTPServerCommand();
        // 客户端
        isConnectionEstablish = new AtomicBoolean(false);
        mHttpCLientCommand = new HTTPClientCommand();
        mHttpCLientCommand.setExcuteHttpConnection(ServiceFileTransfer.this);
        mHttpURLConnections = new ConcurrentLinkedQueue<HttpURLConnection>();
        // 开启自关闭
        mStopHandler = new Handler();
        mStopHandler.postDelayed(mCheckConnectStateRunnable, SERVICE_CHECK_CONNECT_TIME);
        // 开启记录线程，但是记录线程不归service管

        // 自关闭
        mConnectCount = new AtomicInteger(0);
        isTimeOutDisconnect = new AtomicBoolean(false);

        MyLog.initLog();
        MyLog.start();

        super.onCreate();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 每次有命令来重新计算自关闭
        // mStopHandler.removeCallbacks(mStopSelfRunnable);

        handleIntent(intent);
        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void handleIntent(Intent i) {
        if (i == null) {
            // Toast.makeText(mContext, "intent is null",
            // Toast.LENGTH_SHORT).show();
            return;
        }
        ServiceRequest s = ServiceRequest.createRequset(i);
        if (s == null) {
            return;
        }
        
        int requestCommand = s.getRequestType();
        if (s.getUserInfo() != null) {
            this.mUserInfo = s.getUserInfo();
        }
        // 处于正在断开连接状态
        if (isDisConnection.get()) {
            Toast.makeText(mContext, R.string.service_is_disconnected, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCommand) {
            case ServiceRequest.START_SERVER: {
                startServer();
                break;
            }
            case ServiceRequest.ESTABLISH_CONNECTION: {
                establishConnection();
                break;
            }
            case ServiceRequest.USER_INFORMATION_EXCHANGE: {
                exchangeUserInformation();
                break;
            }
            case ServiceRequest.SEND_FILES: {
                // FIXME用datadroid的request
                /**
                 * 在传送文件过程中，保持CPU唤醒
                 * add by zqjia
                 * */
                WakeLock wakeLock = Utils.getPartialWakeLock();
                wakeLock.acquire();
                sendFiles(s);
                
                /**
                 * 传送完成之后释放WakeLock
                 * add by zqjia
                 * */
                wakeLock.release();
                break;
            }
            case ServiceRequest.CANCEL_FILE_SEND: {
                cancelFile(s);
                break;
            }
            case ServiceRequest.DISCONNECTION: {
                disConnection();
                break;
            }
            default: {
                // Toast.makeText(mContext, "bad service request",
                // Toast.LENGTH_SHORT).show();
                // mStopHandler.postDelayed(mStopSelfRunnable, STOP_SELF_DELAY);
            }
                break;

        }
    }

    // 服务器方法
    private void startServer() {
        // 如果已经作为客户端，不能调用此方法
        if (mServiceType != 0 && mServiceType != SERVICE_SERVER_TYPE) {
            Toast.makeText(mContext, R.string.service_is_disconnected, Toast.LENGTH_SHORT).show();
            return;
        } else if (mServiceType == 0) {
            mServiceType = SERVICE_SERVER_TYPE;
        }

        myLog.e("service startServer");
        if (isClientConnect.get()) {
            Toast.makeText(mContext, R.string.service_is_disconnected, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isServerOpen.get()) {
            // Toast.makeText(mContext, "server is open before",
            // Toast.LENGTH_SHORT).show();
            return;
        }

        if (!setUpServerSocketListener()) {
            return;
        }

        if (mServerSocketThread != null && mServerSocketThread.isAlive()) {
            myLog.e("已经开启服务器线程");
            return;
        }

        mServerSocketThread = new ThreadServerSocket(mServerSocket) {
            @Override
            public void run() {
                try {
                    do {
                        try {
                            // 等待wifi热点开启
                            // wifiHostOpenThread.join();
                            myLog.e("Server socket accept ..............................");
                            // FIXME 通知作为服务器，已经打开
                            isServerOpen.set(true);
                            final Socket finalAccept = mServerSocket.accept();
                            myLog.e("Server Socket accept------->get a socket request...");
                            if (mConnectCount.get() == 3) {
                                myLog.e("Server Socket accept-------> current connect is 3 close this request");
                            }

                            executeHttpSocket(finalAccept);
                        } catch (IOException e) {
                            myLog.logException(e);
                            safeClose(mServerSocket);
                        }
                    } while (!mServerSocket.isClosed() && !Thread.interrupted());
                } finally {
                    // FIXME 通知作为服务器，已经关闭
                    isServerOpen.set(false);

                    safeClose(mServerSocket);

                }

            }
        };
        mServerSocketThread.start();
    }

    private boolean setUpServerSocketListener() {
        try {
            mServerSocket = new ServerSocket();
            mServerSocket.setReuseAddress(true);
            mServerSocket.bind(new InetSocketAddress(mPort));
            mServerSocket.setReceiveBufferSize(Const.BUFFER_SIZE);
            return true;
        } catch (IOException e) {
            // Toast.makeText(mContext, "port " + mPort + " is used",
            // Toast.LENGTH_SHORT).show();
            myLog.logException(e);
            return false;
        }
    }

    // 客户端方法
    private void establishConnection() {

        myLog.e("service establishConnection");

        // 如果已经作为服务器，不能调用此方法
        if (mServiceType != 0 && mServiceType != SERVICE_CLIENT_TYPE) {
            // Toast.makeText(mContext, "open client error",
            // Toast.LENGTH_SHORT).show();
            return;
        } else if (mServiceType == 0) {
            mServiceType = SERVICE_CLIENT_TYPE;
        }

        if (isConnectionEstablish.get()) {
            Toast.makeText(mContext, R.string.service_is_disconnected, Toast.LENGTH_SHORT).show();
            return;
        }
        /*
         * 如果已经连接一个服务器，不再进行连接
         */
        // if(isConnectToOneServer){
        // return ;
        // }

        if (mTokenSocket != null && !mTokenSocket.isClosed() && mTokenSocket.isConnected()) {
            // Toast.makeText(mContext, "连接失败，请确认之前服务已经关闭",
            // Toast.LENGTH_SHORT).show();
            return;
        }
        if ((mTokenOutputThread != null && mTokenOutputThread.isAlive())
                || (mTokenInputThread != null && mTokenInputThread.isAlive())) {
            myLog.e("已经建立连接");
            return;
        }

        if (mClientConnectThread != null && mClientConnectThread.isAlive()) {
            myLog.e("已经开启建立连接线程");
            return;
        }

        // TODO 总共连接次数 ---------------------------------------------------
        // -------------------------------------------------------------------

        // 只对连接失败的socket进行处理，不对连接成功的socket进行处理
        mClientConnectThread = new Thread() {
            @Override
            public void run() {
                boolean connectRefused = true;
                int reConnectTimes = RECONNECT_TIMES;
                boolean flag = false;
                while (!flag && connectRefused && reConnectTimes > 0) {
                    reConnectTimes--;
                    try {
                        // 建立连接前需要的状态
                        mTokenSocket = new Socket();

                        TokenSocket tokenSocket = new TokenSocket(mTokenSocket,
                                mUserInfo.getUserName());
                        InetSocketAddress address = new InetSocketAddress(STR_IP, mPort);
                        myLog.e(tokenSocket.getSocketName() + " 正在连接服务器....");

                        // TODO
                        // 开始socket连接打开计时--------------------------------------------------------------
                        // --------------------------------------------------------------------------------
                        mTokenSocket.connect(address, CONNECT_TIME_OUT);
                        // TODO
                        // 結束socket连接打开计时--------------------------------------------------------------
                        // --------------------------------------------------------------------------------
                        connectRefused = false;
                        isConnectionEstablish.set(true);

                        mTokenSession = new TokenSession(tokenSocket);
                        mTokenSession.setReceiveTokenServe(ServiceFileTransfer.this);
                        establishTokenChanel(tokenSocket);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        myLog.logException(e);
                        connectRefused = true;
                        try {
                            Thread.sleep(RECONNECT_TIME);
                        } catch (InterruptedException e1) {
                            Thread.currentThread().interrupt();
                            myLog.logException(e);
                        }
                        safeClose(mTokenSocket);
                        flag = Thread.interrupted();
                    }

                }
                // 尝试完重新连接后，还是没有连接上，停止服务
                if (reConnectTimes == 0 && connectRefused) {
                    stopSelf();
                }
            }

        };

        mClientConnectThread.start();

    }

    private void establishTokenChanel(final TokenSocket tokenSocket) {
        mTokenOutputThread = new ThreadTokenSocket(tokenSocket) {
            @Override
            public void run() {
                try {
                    mTokenSession.sendMessageFromToken(TokenCommand.httpGet(tokenSocket
                            .getSocketName()));
                    mTokenSession.dealTokenOutputSteam();
                } catch (IOException e) {
                    myLog.logException(e);
                } catch (InterruptedException e) {
                    myLog.logException(e);
                } catch (Exception e) {
                    myLog.logException(e);
                } finally {
                    // 连接断开
                    UPDATE.updateUI(UIUpdate.getDisconnectInformation());
                    UPDATE.updateUI(UIUpdate.getACKDisconnectInformation());
                    // --------------------------------------------------------------------
                    isConnectionEstablish.set(false);
                    myLog.e(" token连接断开 " + "token output stream close");
                    mTokenInputThread.interrupt();
                    tokenSocket.close();
                    unRegisterConnection(tokenSocket.getSocket());
                    // 连接中断之后关闭服务，立即关闭服务
                    if (!isDisConnection.get()) {
                        stopSelf();
                    }
                }
            }
        };

        mTokenInputThread = new ThreadTokenSocket(tokenSocket) {
            @Override
            public void run() {
                try {
                    mTokenSession.dealTokenInputStream();
                } catch (IOException e) {
                    myLog.logException(e);
                } finally {
                    isConnectionEstablish.set(false);
                    myLog.e("token input stream close");
                    mTokenOutputThread.interrupt();
                    tokenSocket.close();
                    unRegisterConnection(tokenSocket.getSocket());
                }
            }

        };
        mTokenOutputThread.start();
        mTokenInputThread.start();
    }

    // token是否刚刚发送了消息的回调接口
    @Override
    public void justSendOrReceiveMessage() {
        isTimeOutDisconnect.set(false);
    }

    private void sendFiles(ServiceRequest s) {
        myLog.e("service send file");
        List<String> filePaths = s.getFileList();

        if (filePaths == null || filePaths.size() == 0) {
            myLog.e("need filePaths is null or empty");
            return;
        }

        // FIXME 对一次性传送文件太多怎么处理？
        if (filePaths.size() > 20) {
            Toast.makeText(mContext, R.string.service_too_many_file_send_once, Toast.LENGTH_SHORT);
            return;
        }
        // 实验室数据---------------------------------------------------------------------------
        if (mConnectCount.get() == 0) {
            LaboratoryData.put(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_HEAP_IDEA, ""
                    + LaboratoryData.getThisProcessMemeryInfo(getApplicationContext()));
        }

        if (mConnectCount.get() == 1) {
            LaboratoryData.put(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_HEAP_ONE_FILE, ""
                    + LaboratoryData.getThisProcessMemeryInfo(getApplicationContext()));
        }

        if (mConnectCount.get() == 2) {
            LaboratoryData.put(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_HEAP_TWO_FILE, ""
                    + LaboratoryData.getThisProcessMemeryInfo(getApplicationContext()));
        }

        if (mConnectCount.get() == 3) {
            LaboratoryData.put(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_HEAP_THREE_FILE, ""
                    + LaboratoryData.getThisProcessMemeryInfo(getApplicationContext()));
        }
        // ----------------------------------------------------------------------------------
        List<String> fileNames = s.getFileName();
        if (fileNames == null || fileNames.size() == 0 || fileNames.size() != filePaths.size()) {
            // Toast.makeText(mContext, "发送文件和名称匹配异常", Toast.LENGTH_SHORT);
            myLog.e("文件名错误");
            return;
        }
        switch (mServiceType) {
            case SERVICE_CLIENT_TYPE: {
                myLog.e(filePaths.toString() + "\n" + fileNames.toString() + "--> need send");
                mHttpCLientCommand.addNeedSendFiles(filePaths, fileNames);
                // 连接建立才开始发送文件信息
                if (isConnectionEstablish.get()) {
                    try {
                        mTokenSession.sendMessageFromToken(mHttpCLientCommand
                                .getNeedSendFileTokenCommand());
                    } catch (Exception e) {
                        myLog.logException(e);
                        // Toast.makeText(mContext, "Token消息为null",
                        // Toast.LENGTH_SHORT).show();
                        // TODO ---------------
                        // 文件发送失败 （102）
                        // ----------------------------------------------------------------
                    }
                } else {
                    // Toast.makeText(mContext, "no connection",
                    // Toast.LENGTH_SHORT).show();
                }
            }
                break;
            case SERVICE_SERVER_TYPE: {
                mHttpServerCommand.addNeedSendFiles(filePaths, fileNames);
                if (isClientConnect.get()) {
                    try {
                        mTokenSession.sendMessageFromToken(mHttpServerCommand
                                .getNeedSendFileTokenCommand());
                    } catch (Exception e) {
                        myLog.logException(e);
                        // Toast.makeText(mContext, "Token消息为null",
                        // Toast.LENGTH_SHORT).show();
                        // TODO ---------------
                        // 文件发送失败 （102）
                        // ----------------------------------------------------------------
                    }
                } else {
                    // Toast.makeText(mContext, "no connection",
                    // Toast.LENGTH_SHORT).show();
                }
            }
                break;
            default: {
                // Toast.makeText(mContext, "请先开启服务器或者连接服务器",
                // Toast.LENGTH_SHORT).show();
            }
                break;
        }
    }

    private void exchangeUserInformation() {
        if (!isClientConnect.get() && !isConnectionEstablish.get()) {
            // Toast.makeText(mContext, "没有连接，你想发个人信息给谁",
            // Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            mTokenSession.sendMessageFromToken(TokenCommand.createUserInfoMessage(mUserInfo));
        } catch (Exception e) {
            // Toast.makeText(mContext, "Token消息为null",
            // Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelFile(ServiceRequest r) {
        if (!isConnectionEstablish.get() && !isClientConnect.get()) {
            // Toast.makeText(mContext, "请先开启服务器或者连接服务器",
            // Toast.LENGTH_SHORT).show();
            return;
        }

        String filePath = r.getFilePath();
        if (filePath == null) {
            // Toast.makeText(mContext, "需要取消的文件为空", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (mServiceType) {
            case SERVICE_CLIENT_TYPE: {
                // FIXME getChanelFilePath和cancelFile方法有重叠，可以整合
                String chanelFilePath = mHttpCLientCommand.getChanelFilePath(filePath);
                if (chanelFilePath == null) {
                    return;
                }
                if (mHttpCLientCommand.cancelFile(filePath)) {
                    
                }

                break;
            }
            case SERVICE_SERVER_TYPE: {
                // FIXME getChanelFilePath和cancelFile方法有重叠，可以整合
                String chanelFilePath = mHttpServerCommand.getChanelFilePath(filePath);
                if (chanelFilePath == null) {
                    return;
                }
                if (mHttpServerCommand.cancelFile(filePath)) {
                    
                }
                break;
            }
        }
    }

    private void disConnection() {
        if (!isConnectionEstablish.get() && !isClientConnect.get()) {
            myLog.e("尚未建立连接，无法停止");
            stopSelf();
            return;
        }

        if (isDisConnection.get()) {
            return;
        }
        if (mTokenSession == null) {
            myLog.e("连接已经断开");
            return;
        }

        // 处于正在断开连接状态
        isDisConnection.set(true);
        // 用户点击断开连接次数--------------------------------------------------------------
        LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_CONNECT_DISCONNECT_COUNT);
        // --------------------------------------------------------------------------------
        mTokenSession.clearAllMessageAndSendThisMessage(TokenCommand.createDisConnecetMessage());

    }

    // 服务器httpsocket处理
    private void executeHttpSocket(Socket socket) {
        ThreadSocket t = new ThreadSocket(socket) {
            @Override
            public void run() {
                // 连接数+1
                mConnectCount.addAndGet(1);

                HTTPFileSession session = null;
                try {
                    //构造传送消息的session头信息
                    session = new HTTPFileSession(getSocket(), mHttpServerCommand);
                    session.setReceviceTokenRequest(ServiceFileTransfer.this);
                    session.execute();
                } catch (IOException e) {
                    myLog.logException(e);
                } catch (Exception e) {
                    if (e instanceof FileTransferCancelException) {
                        // 主动取消文件传送
                        myLog.logException(e);
                    }
                } finally {
                    // 普通http处理完成之后关闭socket 连接
                    if (session != null) {
                        if (session.isCanClose()) {
                            safeClose(getSocket());
                            unRegisterConnection(getSocket());
                        }
                    }
                    // 连接数-1
                    mConnectCount.decrementAndGet();
                }
            }
        };
        registerConnection(socket);
        mExecutor.execute(t);
    }

    @Override
    public boolean dealTokenSocket(final TokenSocket tokenSocket) {
        if ((mTokenInputThread != null && mTokenInputThread.isAlive())
                && (mTokenOutputThread != null && mTokenOutputThread.isAlive())) {
            myLog.e("已经有用户连接，不处理当前用户连接");
            return true;
        }
        // 服务器连接建立
        isClientConnect.set(true);

        myLog.e("有用户连接----->" + tokenSocket.getSocketName() + " token连接 ");
        UPDATE.updateUI(UIUpdate.getEstableConnect(tokenSocket.getSocketName()));
        // 提示有用户连入
        unRegisterConnection(tokenSocket.getSocket());
        mTokenSocket = tokenSocket.getSocket();
        mTokenSession = new TokenSession(tokenSocket);
        mTokenSession.setReceiveTokenServe(ServiceFileTransfer.this);

        mTokenInputThread = new ThreadTokenSocket(tokenSocket) {
            @Override
            public void run() {
                try {
                    mTokenSession.dealTokenInputStream();
                } catch (IOException e) {
                    myLog.logException(e);
                } finally {
                    UPDATE.updateUI(UIUpdate.getDisconnectInformation());
                    UPDATE.updateUI(UIUpdate.getACKDisconnectInformation());
                    // -----------------------------------------------------------------------------
                    isClientConnect.set(false);
                    myLog.e(tokenSocket.getSocketName() + " token input stream close");
                    mTokenOutputThread.interrupt();
                    tokenSocket.close();
                    unRegisterConnection(tokenSocket.getSocket());

                    // 连接已断开 立即停掉服务
                    if (!isDisConnection.get()) {
                        stopSelf();
                    }
                }
            }
        };

        mTokenOutputThread = new ThreadTokenSocket(tokenSocket) {
            @Override
            public void run() {
                try {
                    // 发送连接已经建立和用户信息
                    mTokenSession.sendMessageFromToken(TokenCommand.createConnecetEstableMessage());
                    mTokenSession.sendMessageFromToken(TokenCommand
                            .createUserInfoMessage(mUserInfo));
                    mTokenSession.dealTokenOutputSteam();
                } catch (IOException e) {
                    myLog.logException(e);
                } catch (InterruptedException e) {
                    myLog.logException(e);
                } catch (Exception e) {
                    myLog.logException(e);
                } finally {
                    isClientConnect.set(false);
                    // stopALL();
                    myLog.e(tokenSocket.getSocketName() + " token连接断开"
                            + "token output stream close");
                    mTokenInputThread.interrupt();
                    tokenSocket.close();
                    unRegisterConnection(tokenSocket.getSocket());
                }
            }
        };

        mTokenInputThread.start();
        mTokenOutputThread.start();
        return false;
    }

    // token信息
    @Override
    public void serveToken(HashMap<String, String> headers, HashMap<String, String> params,
            HashMap<String, Integer> fileSizeMap, HashMap<String, Integer> fileStateMap,
            HashMap<String, String> fileNameMap) {
        int messageType = Integer.parseInt(headers.get("message-type"));

        Message msg = tokenHandler.obtainMessage();
        msg.obj = messageType;
        Bundle bundle = new Bundle();
        // ----------------------------------------------------------------------------
        // FIXME 测试代码 之后改成对象序列化
        ArrayList<String> filePath = new ArrayList<String>();
        ArrayList<Integer> fileSize = new ArrayList<Integer>();
        ArrayList<Integer> fileState = new ArrayList<Integer>();
        ArrayList<String> fileName = new ArrayList<String>();

        for (String file : fileSizeMap.keySet()) {
            filePath.add(file);
            fileSize.add(fileSizeMap.get(file));
            fileState.add(fileStateMap.get(file));
            fileName.add(fileNameMap.get(file));
        }
        for (String p : params.keySet()) {
            bundle.putString(p, params.get(p));
        }

        bundle.putStringArrayList("path", filePath);
        bundle.putIntegerArrayList("size", fileSize);
        bundle.putIntegerArrayList("state", fileState);
        bundle.putStringArrayList("name", fileName);
        // ----------------------------------------------------------------------------
        // TokenRequest r = new TokenRequest(params, fileSizeMap, fileStateMap,
        // messageType);
        // bundle.putParcelable(TokenRequest.TOKEN_REQUEST, r);
        msg.setData(bundle);
        tokenHandler.sendMessage(msg);
    }

    // 发送完连接断开确认后立即停止服务
    @Override
    public void finishWriteDisConnectionMessage(int tokenCommand) {
        switch (tokenCommand) {
            case TokenCommand.MESSAGE_TYPE_ACK_DISCONNECT: {
                stopSelf();
                isDisConnection.set(false);
                break;
            }
            case TokenCommand.MESSAGE_TYPE_DISCONNECT: {
                mStopHandler.removeCallbacks(mStopSelfRunnable);
                // FIXME 超时没有收到ACK，自动断开连接
                mStopHandler.postDelayed(mStopSelfRunnable, DISCONNECT_TIME_OUT);
                break;
            }
        }

    }

    // 文件上传请求
    @Override
    public void excuteHttpPostConnection(final String filePath) {
        if (mExecutor.isShutdown()) {
            myLog.e("pool is shutdown " + mExecutor.hashCode());
        }

        // FIXME 需要改成线程
        mExecutor.execute(new Runnable() {

            @Override
            public void run() {
                // 连接数+1
                mConnectCount.addAndGet(1);
                // TODO
                // 文件上传数量（包含对方取消文件）--------------------------------------------------------------
                // -------------------------------------------------------------------------------------
                HttpURLConnection con = null;
                try {
                    URL url = new URL(requestURL);
                    con = (HttpURLConnection)url.openConnection();
                    con.setConnectTimeout(CONNECT_TIME_OUT);
                    // FIXME 这里为什么先不用超时？
                    // 自测后发现明显
                    // 1.写比读更加快的时候 哪怕是10s的超时也是常发生的
                    // 2.服务器涉及到线程调度问题，假如在那段时间，服务器线程没有给运行，那么就会增加超时的机会
                    con.setReadTimeout(READ_TIME_OUT);
                    myLog.e("HttpURLConnection post - > " + con.hashCode()
                            + " *************** create " + " current task " + mConnectCount.get());
                    mHttpURLConnections.add(con);
                    mHttpCLientCommand.postUseUrlConnection(con, filePath);
                } catch (MalformedURLException e) {
                    myLog.logException(e);
                } catch (IOException e) {
                    myLog.logException(e);
                } catch (Exception e) {
                    myLog.logException(e);
                } finally {
                    safeClose(con);
                    myLog.e("HttpURLConnection post - > " + con.hashCode()
                            + " *************** close");
                    mHttpURLConnections.remove(con);
                    // 连接数-1
                    mConnectCount.decrementAndGet();
                }
            }
        });
    }

    @Override
    public void excuteHttpGetConnection(final String requestFilePath, final File localSaveFile) {

        // 文件下载请求
        mExecutor.execute(new Runnable() {

            @Override
            public void run() {
                // 连接数+1
                mConnectCount.addAndGet(1);
                // TODO
                // 文件下载数量--------------------------------------------------------------
                // -------------------------------------------------------------------------------------
                HttpURLConnection con = null;
                try {
                    URL url = new URL("http://" + Const.REQUEST_URL + "?file="
                            + URLEncoder.encode(requestFilePath, "utf-8"));
                    con = (HttpURLConnection)url.openConnection();
                    // 设置连接超时
                    con.setConnectTimeout(CONNECT_TIME_OUT);
                    con.setReadTimeout(READ_TIME_OUT);
                    mHttpURLConnections.add(con);
                    myLog.e("HttpURLConnection get - > " + con.hashCode()
                            + " *************** create");
                    mHttpCLientCommand.downloadFile(con, requestFilePath, localSaveFile);
                } catch (MalformedURLException e) {
                    myLog.logException(e);
                } catch (IOException e) {
                    myLog.logException(e);
                } catch (Exception e) {
                    myLog.logException(e);
                } finally {
                    // FIXME 当取消文件传送的时候
                    // 由于上面有handler的消息发送，并且有exception异常抛出，所以运行到con.close的速度比较慢
                    safeClose(con);
                    myLog.e("HttpURLConnection get - > " + con.hashCode()
                            + " *************** close");
                    mHttpURLConnections.remove(con);
                    // 连接数-1
                    mConnectCount.decrementAndGet();
                }
            }
        });
    }

    /**
     * Registers that a new connection has been set up.
     * 
     * @param socket the {@link Socket} for the connection.
     */
    private synchronized void registerConnection(Socket socket) {
        mSockets.add(socket);
    }

    /**
     * Registers that a connection has been closed
     * 
     * @param socket the {@link Socket} for the connection.
     */
    private synchronized void unRegisterConnection(Socket socket) {
        mSockets.remove(socket);
    }

    private synchronized void closeAllSockets() {
        for (Socket s : mSockets) {
            safeClose(s);
        }
        mSockets.clear();

    }

    public void closeAllHttpUrlConnection() {
        for (HttpURLConnection con : mHttpURLConnections) {
            safeClose(con);
        }
        mHttpURLConnections.clear();
    }

    private final void safeClose(Socket closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    private final void safeClose(HttpURLConnection con) {
        if (con != null) {
            con.disconnect();
        }
    }

    private final void safeClose(ServerSocket closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 如果已經建立连接，那么，最后尽力尝试断开连接
     */
    private void makeTheBestToDisconnectBeforeEvent() {
        if (!isClientConnect.get() && !isConnectionEstablish.get()) {
            return;
        }

        if (isDisConnection.get()) {
            return;
        }
        if (mTokenSession != null) {
            mTokenSession
                    .clearAllMessageAndSendThisMessage(TokenCommand.createDisConnecetMessage());
        }

    }

    private void stopALL() {

        makeTheBestToDisconnectBeforeEvent();
        // 关闭tokenSocket
        safeClose(mTokenSocket);
        if (mTokenInputThread != null) {
            mTokenInputThread.interrupt();
        }
        if (mTokenOutputThread != null) {
            mTokenOutputThread.interrupt();
        }
        // 关闭所有文件socket
        closeAllSockets();
        closeAllHttpUrlConnection();
        mExecutor.shutdown();

        // 关闭serversocket
        safeClose(mServerSocket);
        if (mServerSocketThread != null) {
            mServerSocketThread.interrupt();
        }
        // 关闭connectThread
        if (mClientConnectThread != null) {
            mClientConnectThread.interrupt();
        }
        clear();
    }

    private void notifyReserveFiles() {

        ArrayList<String> rFiles = null;
        switch (mServiceType) {
            case SERVICE_CLIENT_TYPE:
                rFiles = mHttpCLientCommand.getReserveFiles();
                break;
            case SERVICE_SERVER_TYPE:
                rFiles = mHttpServerCommand.getReserveFiles();
                break;
        }
        if (rFiles == null || rFiles.size() == 0) {
            myLog.e("没有遗留文件");
            return;
        }

        for (int i = 0; i < rFiles.size(); i++) {
            myLog.e(rFiles.toString() + " ------------ 服务停止 ， 全部设置成 error");
            UPDATE.updateUI(UIUpdate.getStateMessage(rFiles.get(i), 0, Const.FILE_TRANSFER_FAILURE));

            // 实验室数据
            LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_TRANSFER_RECEIVE_ERROR_201);
            // ---------------------------------------------
        }

    }

    // 还原服务状态，只能在stopAll之后使用
    private void clear() {

        // 向上提示没有传送的文件
        notifyReserveFiles();
        // 还原服务类型
        mServiceType = 0;

        // 清除状态
        isDisConnection.set(false);
        isTimeOutDisconnect.set(false);
        if (mTokenSession != null) {
            mTokenSession.clear();
        }
        // 服务器
        isServerOpen = new AtomicBoolean(false);
        isClientConnect = new AtomicBoolean(false);
        mSockets.clear();
        mHttpServerCommand.clear();
        // 客户端
        isConnectionEstablish = new AtomicBoolean(false);
        mHttpCLientCommand.clear();
        mHttpURLConnections.clear();

        // 停止检查命令
        myLog.e("停止检查命令");
        mStopHandler.removeCallbacks(mCheckConnectStateRunnable);
    }

    @Override
    public void onDestroy() {
        myLog.e("service on destroy");
        // 实验室数据
        LaboratoryData.gTotalServiceEndCPUTime = LaboratoryData.getTotalCpuTime();
        LaboratoryData.gServiceEndCPUTime = LaboratoryData.getAppCpuTime();
        // ---------------------------------------------
        stopALL();

        // 停止记录线程
        MyLog.stop();

        myLog.e("service on destroy release resource");
        super.onDestroy();
    }
}
