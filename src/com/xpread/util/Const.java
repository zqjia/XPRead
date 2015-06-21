
package com.xpread.util;

public class Const {
    public static final int PICK_FILE_REQUEST_CODE = 1;

    public static final float KILO = 1024f;

    /**
     * File prepared
     */
    public static final int FILE_TRANSFER_PREPARED = 10;

    /**
     * File is transmitted
     */
    public static final int FILE_TRANSFER_DOING = 11;

    /**
     * File transfer complete
     */
    public static final int FILE_TRANSFER_COMPLETE = 12;

    /**
     * File transfer failure
     */
    public static final int FILE_TRANSFER_FAILURE = 13;

    /**
     * 
     */
    public static final int FILE_TRANSFER_CANCEL = 14;

    public static final int REFRESH_FILE_TRANSFER_STATE = 16;

    /**
     * the port of http server
     */
    public final static int PORT = 9898;

    /**
     * the ip of http server
     */
    public final static String STR_IP = "192.168.43.1";

    public final static String USER_INFO_PREFERENCE = "user_info_preference";

    public final static String REQUEST_URL = Const.STR_IP + ":" + Const.PORT;

    public static final int REFRESH_FILE_TRANSFER_SPEED = 15;

    public static final int REFRESH_USER_INFO = 18;

    public static final int REFRESH_ESTIBALE = 17;

    public static final int REFRESH_DISCONNECTION = 19;

    public static final int REFRESH_ACK_DISCONNECTION = 21;

    public static final int REFRESH_FILES_RECEIVE = 20;

    public static final int REFRESH_FILE_CANCEL = 22;

    public static final int BUFFER_SIZE = 200 * 1024;

    public static final int SENDER = 0;

    public static final int RECEIVER = 1;

    public static final int TYPE_APP = 0;

    public static final int TYPE_IMAGE = 1;

    public static final int TYPE_MUSIC = 2;

    public static final int TYPE_VIDEO = 3;

    public static final int TYPE_TEXT = 4;

    public static final int TYPE_ZIP = 5;

    public static final int TYPE_FILE = 6;

    public static final int TYPE_UNKNOW = 7;

    public static final int WIFI_NETWORK_CONNECT = 1;

    public static final int THREAD_PRIORITY = Thread.NORM_PRIORITY;

    public static final String IMAGE_FILE_NAME = "faceImage.png";

    public static final String PREFERENCES_NAME = "xpread_settings";

    public static final String OWNER_NAME_KEY = "user_name";

    public static final String OWNER_ICON_KEY = "user_icon";

    public static final String OWNER_IMEI_KEY = "device_id";

    public static final String OWNER_TOTAL_TRANSMISSION = "total_transmission";

    public static final String FLAG_UPLOAD_WA_TIME = "wa_upload_time";
    
    public static final String WIFI_AP_PASSWORD = "123456789";
    
    /**
     * 连接好友时wifi加密方式
     * */
    public static final int TYPE_NOPASSWORD = 10001;
    public static final int TYPE_WEP = 10002;
    public static final int TYPE_WPA = 10003;
    
}
