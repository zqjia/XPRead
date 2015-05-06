
package com.xpread.service;

import java.util.HashMap;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;

public class TokenRequest implements Parcelable {

    public static final String TOKEN_REQUEST = "token_requset";

    private HashMap<String, String> params;

    private HashMap<String, Integer> fileSizeMaps;

    private HashMap<String, Integer> fileStateMaps;

    private int mTokenRequestType;

    public TokenRequest(HashMap<String, String> params, HashMap<String, Integer> fileSizeMaps,
            HashMap<String, Integer> fileStateMaps, int mTokenRequestType) {
        super();
        this.params = params;
        this.fileSizeMaps = fileSizeMaps;
        this.fileStateMaps = fileStateMaps;
        this.mTokenRequestType = mTokenRequestType;
    }

    /**
     * @return the mTokenRequestType
     */
    public int getTokenRequestType() {
        return mTokenRequestType;
    }

    /**
     * @param mTokenRequestType the mTokenRequestType to set
     */
    public void setTokenRequestType(int mTokenRequestType) {
        this.mTokenRequestType = mTokenRequestType;
    }

    /**
     * @return the params
     */
    public HashMap<String, String> getParams() {
        return params;
    }

    /**
     * @param params the params to set
     */
    public void setParams(HashMap<String, String> params) {
        this.params = params;
    }

    /**
     * @return the fileSizeMaps
     */
    public HashMap<String, Integer> getFileSizeMaps() {
        return fileSizeMaps;
    }

    /**
     * @param fileSizeMaps the fileSizeMaps to set
     */
    public void setFileSizeMaps(HashMap<String, Integer> fileSizeMaps) {
        this.fileSizeMaps = fileSizeMaps;
    }

    /**
     * @return the fileStateMaps
     */
    public Map<String, Integer> getFileStateMaps() {
        return fileStateMaps;
    }

    /**
     * @param fileStateMaps the fileStateMaps to set
     */
    public void setFileStateMaps(HashMap<String, Integer> fileStateMaps) {
        this.fileStateMaps = fileStateMaps;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // 序列化的方法需要重写
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(params);
        dest.writeSerializable(fileSizeMaps);
        dest.writeSerializable(fileStateMaps);
        dest.writeInt(mTokenRequestType);
    }

    // public static final Parcelable.Creator<TokenRequest> CREATOR = new
    // Parcelable.Creator<TokenRequest>() {
    //
    // @Override
    // public TokenRequest createFromParcel(Parcel source) {
    // HashMap<String, String> p =
    // source.readHashMap(HashMap.class.getClassLoader());
    // HashMap<String, Integer> fi =
    // source.readHashMap(HashMap.class.getClassLoader());
    // HashMap<String, Integer> fa =
    // source.readHashMap(HashMap.class.getClassLoader());
    // int type = source.readInt();
    // TokenRequest r = new TokenRequest(p, fi, fa, type);
    // return r;
    // }
    //
    // @Override
    // public TokenRequest[] newArray(int size) {
    // return null;
    // }
    // };

}
