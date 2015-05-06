
package com.xpread.service;

public class ThreadTokenSocket extends Thread {

    private MyLog myLog = new MyLog(ThreadTokenSocket.class.getSimpleName());

    private TokenSocket mSocket;

    private final int THREAD_PRIORITY = Thread.NORM_PRIORITY - 1;

    public ThreadTokenSocket(TokenSocket s) {
        this.mSocket = s;
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
