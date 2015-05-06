
package com.xpread.control;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.xpread.control.Controller.ProgressChangeListener;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;

public class ProgressDisplay implements TotalFileProgress.RefreshInitProgress {

    private static final String TAG = ProgressDisplay.class.getSimpleName();

    private ProgressChangeListener mProgressChangeListener;

    private volatile int mSendDisplayProgress = 0;

    private volatile int mReceiveDisplayProgress = 0;

    private static final int PROGRESS_MAX = 100;

    private static final int PROGRESS_MAX_GAP = 10;

    private static final int REFRESH_PROGRESS = 0x0001;

    private static final int REFRESH_TIME_GAP = 50;

    private int mPreSendDisplayProgress = -1;

    private int mPreReceiveDisplayProgress = -1;
    
    

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
                    int preDisplayProgress;

                    if (role == Const.SENDER) {
                        preDisplayProgress = mPreSendDisplayProgress;
                    } else {
                        preDisplayProgress = mPreReceiveDisplayProgress;
                    }

                    if (displayProgress < 0 || displayProgress > 100) {
                        if (LogUtil.isLog ) {
                            Log.e(TAG, "invalid display progress: " + displayProgress);
                        }
                        
                        if (displayProgress <0) {
                            displayProgress = 0;
                        } else if (displayProgress > 100) {
                            displayProgress = 100;
                        }
                    }
                    
                    if (mProgressChangeListener != null && displayProgress > preDisplayProgress) {

                        if (LogUtil.isLog) {
                            Log.e(TAG, "refresh UI the previous progress is " + preDisplayProgress
                                    + " and the display progress" + displayProgress);
                        }
                        mProgressChangeListener.setProgress(displayProgress, role);

                        if (role == Const.SENDER) {
                            mPreSendDisplayProgress = displayProgress;
                            if (displayProgress >= PROGRESS_MAX) {
                                resetSendProgress();
                            }
                        } else {
                            mPreReceiveDisplayProgress = displayProgress;
                            if (displayProgress >= PROGRESS_MAX) {
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
        int i = 0;

        switch (role) {
            case Const.SENDER:

                if (initProgress == PROGRESS_MAX) {
                    while (mSendDisplayProgress < PROGRESS_MAX) {
                        Message msg = Message.obtain();
                        msg.what = REFRESH_PROGRESS;
                        msg.obj = ++mSendDisplayProgress;
                        msg.arg1 = Const.SENDER;
                        mHandler.sendMessageDelayed(msg, (i++) * REFRESH_TIME_GAP);
                    }
                } else if (initProgress - mSendDisplayProgress > PROGRESS_MAX_GAP) {
                    while (i < PROGRESS_MAX_GAP) {
                        Message msg = Message.obtain();
                        msg.what = REFRESH_PROGRESS;
                        msg.obj = ++mSendDisplayProgress;
                        msg.arg1 = Const.SENDER;
                        mHandler.sendMessageDelayed(msg, i * REFRESH_TIME_GAP);
                        i++;
                    }
                } else {
                    Message msg = Message.obtain();
                    msg.what = REFRESH_PROGRESS;
                    msg.obj = initProgress;
                    msg.arg1 = Const.SENDER;
                    mHandler.sendMessage(msg);
                    mSendDisplayProgress = initProgress;
                }
                break;

            case Const.RECEIVER:
                if (initProgress == PROGRESS_MAX) {

                    if (LogUtil.isLog) {
                        Log.e(TAG, "receiver----------display progress is "
                                + mReceiveDisplayProgress);
                    }

                    while (mReceiveDisplayProgress < PROGRESS_MAX) {
                        Message msg = Message.obtain();
                        msg.what = REFRESH_PROGRESS;
                        msg.obj = ++mReceiveDisplayProgress;
                        msg.arg1 = Const.RECEIVER;
                        mHandler.sendMessageDelayed(msg, (i++) * REFRESH_TIME_GAP);
                    }
                } else if (initProgress - mReceiveDisplayProgress > PROGRESS_MAX_GAP) {
                    while (i < PROGRESS_MAX_GAP) {
                        Message msg = Message.obtain();
                        msg.what = REFRESH_PROGRESS;
                        msg.obj = ++mReceiveDisplayProgress;
                        msg.arg1 = Const.RECEIVER;
                        mHandler.sendMessageDelayed(msg, i * REFRESH_TIME_GAP);
                        i++;
                    }
                } else {
                    Message msg = Message.obtain();
                    msg.what = REFRESH_PROGRESS;
                    msg.obj = initProgress;
                    msg.arg1 = Const.RECEIVER;
                    mHandler.sendMessage(msg);
                    mReceiveDisplayProgress = initProgress;
                }
                break;
            default:
                break;
        }

    }

    public void setCurrentProgress(int progress, int role) {

        if (role == Const.SENDER) {
            mSendDisplayProgress = progress;

            Message msg = Message.obtain();
            msg.what = REFRESH_PROGRESS;
            msg.obj = mSendDisplayProgress;
            msg.arg1 = Const.SENDER;
            mHandler.sendMessage(msg);
        } else {
            mReceiveDisplayProgress = progress;

            Message msg = Message.obtain();
            msg.what = REFRESH_PROGRESS;
            msg.obj = mReceiveDisplayProgress;
            msg.arg1 = Const.RECEIVER;
            mHandler.sendMessage(msg);
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

    public void removeAllMessage() {
        mHandler.removeMessages(REFRESH_PROGRESS);
    }

}
