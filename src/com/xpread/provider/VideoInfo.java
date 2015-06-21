package com.xpread.provider;

import android.graphics.Bitmap;

public class VideoInfo {
    private Bitmap mVideoThumbBmp;
    private String mVideoThumbData;
    private String mVideoTitle;
    private long mVideoDuration;
    private long mVideoSize;
    private boolean mIsSelected;
    private String mVideoData;
    
    public void setVideoThumbBmp(Bitmap bitmap) {
        this.mVideoThumbBmp = bitmap;
    }
    
    public Bitmap getVideoThumbBmp() {
        return this.mVideoThumbBmp;
    }
    
    public void setVideoThumbdata(String data) {
        this.mVideoThumbData = data;
    }
    
    public String getVideoThumbData() {
        return this.mVideoThumbData;
    }
    
    public void setVideoTitle(String title) {
        this.mVideoTitle = title;
    }
    
    public String getVideoTitle() {
        return this.mVideoTitle;
    } 
    
    public void setVideoDuration(long duration) {
        this.mVideoDuration = duration;
    }
    
    public long getVideoDuration() {
        return this.mVideoDuration;
    }
    
    public void setVideoSize(long size) {
        this.mVideoSize = size;
    }
    
    public long getVideoSize() {
        return this.mVideoSize;
    }
    
    public void setIsSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    } 
    
    public boolean getIsSelected() {
        return this.mIsSelected;
    }
    
    public void setVideoData(String data) {
        this.mVideoData = data;
    }
    
    public String getVideoData() {
        return this.mVideoData;
    }
    
}
