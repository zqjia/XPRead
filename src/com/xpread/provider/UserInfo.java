
package com.xpread.provider;

import java.io.Serializable;

/**
 * @author Administrator
 */
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static String USER_INFO = "user_info";
    public final static String USER_NAME = "user_name";
    public final static String USER_PICTURE_ID = "user_picture_id";
    public final static String USER_DEVICES_ID = "user_devices_id";
    public final static String DEFAULT_NAME = "User";

    public final static int DEFAULT_PICTURE_ID = 1;

    public final static String DEFAULT_DEVICE_NAME = "phone";

    private String mUserName = DEFAULT_NAME;
    private String mDeviceName = DEFAULT_DEVICE_NAME;
    private int mPictureID = DEFAULT_PICTURE_ID;

    // FIXME
    // add by zqjia
    // save the wifi state before open the app
    /*---------begin-------------*/
    
    //the default ssid 
    private String mDefaultSsid;

    //the connected friend ssid
    private String mConnectedFriendSsid;
    
    /*
     *  default        -1
     *  not connected   0 
     *  connected       1
     * 
     * */
    private int mIsWifiConnectedBefore = -1;

    public void setIsWifiConnectedBefore(int isConnected) {
        this.mIsWifiConnectedBefore = isConnected;
    }

    public int getIsWifiConnectedBefore() {
        return this.mIsWifiConnectedBefore;
    }
    
    public void setDefaultSsid(String defaultSsid) {
        this.mDefaultSsid = defaultSsid;
    }
    
    public String getDefaultSsid() {
        return this.mDefaultSsid;
    }
    
    public void setConnectedFriendSsid(String friendSsid) {
        this.mConnectedFriendSsid = friendSsid;
    }
    
    public String getConnectedFriendSsid() {
        return this.mConnectedFriendSsid;
    }

    /*---------end-------------*/

    public UserInfo() {
        super();
    }

    public UserInfo(String mUserName, String mDevieceName, int mPictureID) {
        super();
        this.mUserName = mUserName;
        this.mDeviceName = mDevieceName;
        this.mPictureID = mPictureID;
    }

    public UserInfo(String mUserName, String mDevieceName, String mPictureID) {
        super();
        this.mUserName = mUserName;
        this.mDeviceName = mDevieceName;
        this.mPictureID = Integer.parseInt(mPictureID);
    }

    public final String getUserName() {
        return mUserName;
    }

    public final void setUserName(String mUserName) {
        this.mUserName = mUserName;
    }

    public final String getDeviceName() {
        return mDeviceName;
    }

    public final void setDeviceName(String mDevieceName) {
        this.mDeviceName = mDevieceName;
    }

    public final int getPictureID() {
        return mPictureID;
    }

    public final void setPictureID(int mPictureID) {
        this.mPictureID = mPictureID;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "UserInfo [mUserName=" + mUserName + ", mDeviceName=" + mDeviceName
                + ", mPictureID=" + mPictureID + "]";
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDeviceName == null) ? 0 : mDeviceName.hashCode());
        result = prime * result + mPictureID;
        result = prime * result + ((mUserName == null) ? 0 : mUserName.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserInfo other = (UserInfo)obj;
        if (mDeviceName == null) {
            if (other.mDeviceName != null)
                return false;
        } else if (!mDeviceName.equals(other.mDeviceName))
            return false;
        if (mPictureID != other.mPictureID)
            return false;
        if (mUserName == null) {
            if (other.mUserName != null)
                return false;
        } else if (!mUserName.equals(other.mUserName))
            return false;
        return true;
    }

}
