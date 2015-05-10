package com.xpread.provider;

public class MusicInfo {
    
    private String mTitle;
    private String mSinger;
    private long mSize;
    private boolean mIsSelected;
    private String mData;
    
    public void setTitle(String title) {
        this.mTitle = title;
    }
    
    public String getTitle() {
        return this.mTitle;
    }
    
    public void setSinger(String singer) {
        this.mSinger = singer;
    }
    
    public String getSinger() {
        return this.mSinger;
    }
    
    public void setSize(long size) {
        this.mSize = size;
    }
    
    public long getSize() {
        return this.mSize;
    }
    
    public void setIsSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    }
    
    public boolean getIsSelected() {
        return this.mIsSelected;
    }
    
    public void setData(String data) {
        this.mData = data;
    } 
    
    public String getData() {
        return this.mData;
    }
}
