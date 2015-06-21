
package com.xpread.provider;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.xpread.util.FileUtil;

public class FileBean {
    public String uri;
    public byte[] icon;
    public String fileName;
    public int type;
    public int status;
    public long timeStamp;
    public String target;
    public int role;
    
    private String mThumbImagePath;
    private Bitmap mThumbImageBmp;
    private int mThumbImageId = -1;
    private long mSize;
    
    public void setThumbImage(String thumbImagePath) {
        this.mThumbImagePath = thumbImagePath;
    }
    
    public void setThumbImage(Bitmap thumbImageBmp) {
        this.mThumbImageBmp = thumbImageBmp;
    }
    
    public void setThumbImage(int thumbImageId) {
        this.mThumbImageId = thumbImageId;
    }
    
    /**
     * 返回文件对象的缩略图
     * 返回-1表示未设置缩略图
     * 因为缩略图可能是path，bitmap和resourceId三种类型，所以获取之后需要进行处理
     * */
    
    public Object getThumbImage() {
        if (this.mThumbImagePath != null) {
            return (Object)this.mThumbImagePath;
        } else if (this.mThumbImageBmp != null) {
            return (Object)this.mThumbImageBmp;
        } else if (this.mThumbImageId != -1){
            return (Object)this.mThumbImageId;
        } else {
            return -1;
        }
    }
    
    public void setSize(long size) {
        this.mSize = size;
    }
    
    public long getSize() {
        return this.mSize;
    }

    @Override
    public boolean equals(Object o) {
        FileBean bean = (FileBean)o;
        if (bean == null || uri == null) {
            return false;
        }

        return uri.equals(bean.uri);
    }

    public String getFileName() {
        if (TextUtils.isEmpty(fileName)) {
            fileName = FileUtil.getFileNameWithoutPath(uri);
        }
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
