
package com.xpread.service;

import java.net.ServerSocket;

public class ThreadServerSocket extends Thread {
    private ServerSocket mServerSocket;

    private MyLog myLog = new MyLog(ThreadServerSocket.class.getSimpleName());

    private final int THREAD_PRIORITY = Thread.NORM_PRIORITY - 1;

    public ThreadServerSocket(ServerSocket ss) {
        this.mServerSocket = ss;
        this.setPriority(THREAD_PRIORITY);
        this.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                // FIXME 保存log
                myLog.logException(ex);
            }
        });
    }

    @Override
    public void interrupt() {
        try {
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        } catch (Exception ignored) {
            // do nothing
        } finally {
            super.interrupt();
        }
    }

}
