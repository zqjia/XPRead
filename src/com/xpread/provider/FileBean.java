
package com.xpread.provider;

import android.text.TextUtils;

import com.xpread.util.FileUtil;

public class FileBean {
    public String uri;

    public byte[] icon;

    public String fileName;

    public int type;

    public int status;

    public int size;

    public long timeStamp;

    public String target;

    public int role;

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
