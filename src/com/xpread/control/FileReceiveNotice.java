package com.xpread.control;

import android.util.Log;

import com.xpread.control.Controller.FileReceiveNoticeListener;
import com.xpread.util.Const;
import com.xpread.util.LogUtil;

public class FileReceiveNotice implements TotalFileProgress.RefreshInitProgress{

    private static final String TAG = FileReceiveNotice.class.getSimpleName();
    
    private static final int MAX_PROGRESS = 100;
    
    private boolean mIsAnimationDisplay = true;
    
    private FileReceiveNoticeListener mFileReceiveNoticeListener;
    
    public void setFileReceiveNoticeListener(FileReceiveNoticeListener listener) {
        this.mFileReceiveNoticeListener = listener;
    }
    
    public void unRegistFileReceiveNoticeListener() {
        this.mFileReceiveNoticeListener = null;
    }
    
    @Override
    public void refreshInitProgress(int initProgress, int role) {
        
        if (LogUtil.isLog) {
            Log.e(TAG, "init progress is " + initProgress + " and the role is " + role);
        }
        
       if (this.mFileReceiveNoticeListener != null) {
           if (role == Const.RECEIVER) {
               if (mIsAnimationDisplay) {
                   this.mFileReceiveNoticeListener.animationStart();
                   this.mIsAnimationDisplay = false;
               }
               
               if (initProgress >= MAX_PROGRESS) {
                   this.mIsAnimationDisplay = true;
               }
           }
       }
    }

}
