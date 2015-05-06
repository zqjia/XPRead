
package com.xpread.service;

import java.net.Socket;
import java.net.SocketException;

public class ThreadSocket extends Thread {

    private MyLog myLog = new MyLog(ThreadSocket.class.getSimpleName());

    private Socket mSocket;

    private final int THREAD_PRIORITY = Thread.NORM_PRIORITY - 1;

    public ThreadSocket(Socket s) {
        this.mSocket = s;
        try {
            this.mSocket.setSoTimeout(ServiceFileTransfer.READ_TIME_OUT);
        } catch (SocketException e) {
            // 不处理设置超时导致的异常
            myLog.logException(e);
        }
        this.setPriority(THREAD_PRIORITY);
        this.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                // FIXME 保存log
                myLog.logException(ex);
            }
        });
    }

    protected Socket getSocket() {
        return this.mSocket;
    }

    @Override
    public void interrupt() {
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (Exception ignored) {
            // do nothing
        } finally {
            super.interrupt();
        }
    }

}
