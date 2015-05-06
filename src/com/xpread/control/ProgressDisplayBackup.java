
package com.xpread.control;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.xpread.control.Controller.ProgressChangeListener;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;

public class ProgressDisplayBackup implements TotalFileProgress.RefreshInitProgress {

    private static final String TAG = ProgressDisplayBackup.class.getSimpleName();

    private ProgressChangeListener mProgressChangeListener;

    private RefreshProgressThread mRefreshProgressThread;

    private volatile int mSendDisplayProgress = 0;

    private volatile int mReceiveDisplayProgress = 0;

    private static final int PROGRESS_MAX = 100;

    private static final int PROGRESS_MAX_GAP = 10;

    private static final int REFRESH_PROGRESS = 0x0001;

    private static final int REFRESH_TIME_GAP = 50;

    private int mPreSendDisplayProgress = -1;

    private int mPreReceiveDisplayProgress = -1;

    private boolean isFirstSetProgress = true;

    public void setProgressChangeListener(ProgressChangeListener listener) {

        if (LogUtil.isLog) {
            Log.e(TAG, "regist progress change listener");
        }

        this.mProgressChangeListener = listener;
        resetSendProgress();
        resetReceiveProgress();
    }

    public void unRegistProgressChangeListener() {

        if (LogUtil.isLog) {
            Log.e(TAG, "unregist progress change listener");
        }

        resetSendProgress();
        resetReceiveProgress();

        if (this.mProgressChangeListener != null) {
            this.mProgressChangeListener = null;
        }

    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case REFRESH_PROGRESS:
                    int displayProgress = (Integer)msg.obj;
                    int role = msg.arg1;
                    int preDisplayProgress = 0;

                    if (role == Const.SENDER) {
                        preDisplayProgress = mPreSendDisplayProgress;
                    } else {
                        preDisplayProgress = mPreReceiveDisplayProgress;
                    }

                    if (mProgressChangeListener != null && displayProgress > preDisplayProgress) {

                        if (LogUtil.isLog) {
                            Log.e(TAG, "refresh UI the previous progress is " + preDisplayProgress
                                    + " and the display progress" + displayProgress);
                        }
                        mProgressChangeListener.setProgress(displayProgress, role);

                        if (role == Const.SENDER) {
                            mPreSendDisplayProgress = displayProgress;
                            if (displayProgress == PROGRESS_MAX) {
                                resetSendProgress();
                            }
                        } else {
                            mPreReceiveDisplayProgress = displayProgress;
                            if (displayProgress == PROGRESS_MAX) {
                                resetReceiveProgress();
                            }
                        }
                    }

                    break;

                default:
                    break;
            }

        }

    };

    @Override
    public void refreshInitProgress(int initProgress, int role) {

        if (LogUtil.isLog) {
            Log.e(TAG, "init progress is " + initProgress + " and the role is " + role);
        }

        synchronized (this) {
            int preProgress = (role == Const.SENDER ? mPreSendDisplayProgress
                    : mPreReceiveDisplayProgress);

            if (mRefreshProgressThread != null && !mRefreshProgressThread.isInterrupted()) {
                mRefreshProgressThread.interrupt();
            }

            if (initProgress == PROGRESS_MAX) {

                if (PROGRESS_MAX - preProgress > PROGRESS_MAX_GAP) {
                    mRefreshProgressThread = new RefreshProgressThread(PROGRESS_MAX, role, false);
                    mRefreshProgressThread.start();
                } else {
                    Message msg = Message.obtain();
                    msg.what = REFRESH_PROGRESS;
                    msg.obj = PROGRESS_MAX;
                    msg.arg1 = role;
                    mHandler.sendMessage(msg);
                    if (role == Const.SENDER) {
                        mSendDisplayProgress = PROGRESS_MAX;
                    } else {
                        mReceiveDisplayProgress = PROGRESS_MAX;
                    }
                }

            } else if (initProgress - preProgress > PROGRESS_MAX_GAP) {

                mRefreshProgressThread = new RefreshProgressThread(PROGRESS_MAX_GAP, role, true);
                mRefreshProgressThread.start();

            } else {

                Message msg = Message.obtain();
                msg.what = REFRESH_PROGRESS;
                msg.obj = initProgress;
                msg.arg1 = role;
                mHandler.sendMessage(msg);
                if (role == Const.SENDER) {
                    mSendDisplayProgress = initProgress;
                } else {
                    mReceiveDisplayProgress = initProgress;
                }

            }
        }
    }

    private class RefreshProgressThread extends Thread {

        private int limit = 0;

        private int role = -1;

        private boolean isGap = false;

        public RefreshProgressThread(int limit, int role, boolean isGap) {
            this.limit = limit;
            this.role = role;
            this.isGap = isGap;
        }

        @Override
        public void run() {
            super.run();

            int i = 0;

            if (role == Const.SENDER) {

                if (isGap) {
                    limit = limit + mSendDisplayProgress;
                }

                while (!this.isInterrupted() && mSendDisplayProgress < limit) {
                    Message msg = Message.obtain();
                    msg.what = REFRESH_PROGRESS;
                    msg.obj = ++mSendDisplayProgress;
                    msg.arg1 = role;
                    mHandler.sendMessageDelayed(msg, (i++) * REFRESH_TIME_GAP);
                }
            } else {

                if (isGap) {
                    limit = limit + mReceiveDisplayProgress;
                }

                while (!this.isInterrupted() && mReceiveDisplayProgress < limit) {
                    Message msg = Message.obtain();
                    msg.what = REFRESH_PROGRESS;
                    msg.obj = ++mReceiveDisplayProgress;
                    msg.arg1 = role;
                    
                    mHandler.sendMessageDelayed(msg, (i++) * REFRESH_TIME_GAP);
                }
            }

        }

        @Override
        public void interrupt() {
            // mHandler.removeMessages(REFRESH_PROGRESS);
            super.interrupt();
        }

    }

    public void setCurrentProgress(int progress, int role) {
        synchronized (this) {

            if (this.mRefreshProgressThread != null && !this.mRefreshProgressThread.isInterrupted()) {
                this.mRefreshProgressThread.interrupt();
            }

            if (role == Const.SENDER) {
                mSendDisplayProgress = progress;
            } else {
                mReceiveDisplayProgress = progress;
            }
        }
    }

    private void resetSendProgress() {
        this.mSendDisplayProgress = 0;
        this.mPreSendDisplayProgress = -1;
    }

    private void resetReceiveProgress() {
        this.mReceiveDisplayProgress = 0;
        this.mPreReceiveDisplayProgress = -1;
    }

}
