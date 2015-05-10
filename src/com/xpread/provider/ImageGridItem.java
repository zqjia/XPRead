package com.xpread.provider;

import android.graphics.Bitmap;

public class ImageGridItem {

    /*
     *  原图路径
     * */
    private String mPath;
    
    /*
     *  原图的缩略图路径
     * */
    private String mThumbPath;
    
    /*
     *  如果原图的缩略图路径不存在，则主动生成一张缩略图
     *  mThumbPath和mThumbBmp只能同时有一个有值，另一个为null
     *  
     * */
    private Bitmap mThumbBmp;
    
    /*
     *  是否被选中
     * */
    private boolean mIsSelected = false;
    
    /*
     *  图片加入手机的时间
     * */
    private String mBucketName;
    
    /*
     *  图片对应的头部id，相同的id将被分在一起
     * */
    private int mHeaderId;
    
    public void setPath(String path) {
        this.mPath = path;
    }
    
    public String getPath() {
        return this.mPath;
    }
    
    public void setThumbPath(String thumbPath) {
        this.mThumbPath = thumbPath;
    }
    
    public String getThumbPath() {
        return this.mThumbPath;
    }
    
    public void setThumbBmp(Bitmap bmp) {
        this.mThumbBmp = bmp;
    }
    
    public Bitmap getThumbBmp() {
        return this.mThumbBmp;
    }
    
    public void setIsSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    }
    
    public boolean getIsSelected() {
        return this.mIsSelected;
    }
    
    public void setBucketName(String name) {
        this.mBucketName = name;
    }
    
    public String getBucketName() {
        return this.mBucketName;
    }
    
    public void setHeaderId(int headerId) {
        this.mHeaderId = headerId;
    } 
    
    public int getHeaderId() {
        return this.mHeaderId;
    }
}
