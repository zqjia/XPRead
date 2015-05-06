/**
 *****************************************************************************
 * Copyright (C) 2005-2014 UCWEB Corporation. All rights reserved
 * File        : WaDef.java.java
 *
 * Description : WaDef.java
 * 
 * Creation    : 28-10-2014 23:51:00
 * Author      : guozm@ucweb.com
 * History     : 1.0, 28-10-2014 23:51:00, guozhaomin, Create the file
 *****************************************************************************
 **/

package com.xpread.wa;

public class WaKeys {

    public static final String CATEGORY_XPREAD = "xpread";

    public static final String CATEGORY_XPREAD_DATA = "xpread_data";


    public static final int ID_XPREAD = 1000;

    public static final int ID_XPREAD_DATA = 2000;

    // Xpread
    public static final String KEY_XPREAD_USER_SET = "main_1"; // 用户点击头像或设备名
    public static final String KEY_XPREAD_RECORD_ENTRANCE = "main_2"; // 用户点击record入口
    public static final String KEY_XPREAD_SEND_DISCONNECT = "main_3";// 未连接时,用户点击send按钮
    public static final String KEY_XPREAD_RECEIVE = "main_4";// 用户点击Receive按钮
    public static final String KEY_XPREAD_HAMBURGER = "main_5";// 用户点击汉堡按钮
    public static final String KEY_XPREAD_SHARE = "main_6";// 用户点击分享按钮
    public static final String KEY_XPREAD_SEND_CONNECT = "main_7";// 连接时,用户点击send按钮
    public static final String KEY_XPREAD_DISCONNECT = "main_8";// 连接时,用户点击disconnect按钮

    public static final String KEY_XPREAD_MENU_UPDATE = "menu_1";// 用户点击update按钮
    public static final String KEY_XPREAD_MENU_ABOUT = "menu_2";// 用户点击about按钮

    public static final String KEY_XPREAD_SHARE_QR = "share_1";// 用户点击QR Code
    public static final String KEY_XPREAD_SHARE_BLUETOOTH = "share_2";// 用户点击Bluetooth

    public static final String KEY_XPREAD_PROFILE_ICON = "profile_1";// 用户退出profile界面时,头像发生变化
    public static final String KEY_XPREAD_PROFILE_NAME = "profile_2";// 用户退出profile界面时,名称发生变化

    public static final String KEY_XPREAD_RECORD_START = "record_1"; // 某一任务开始传输
    public static final String KEY_XPREAD_RECORD_SUCESS = "record_2"; // 某一任务传输成功
    public static final String KEY_XPREAD_RECORD_FAILURE = "record_3"; // 某一任务传输错误
    public static final String KEY_XPREAD_RECORD_EXPAND_SUCESS = "record_4"; // 点击已完成的任务弹出操作按钮
    public static final String KEY_XPREAD_RECORD_SHARE_SUCESS = "record_5";// 再次分享一个已完成的任务
    public static final String KEY_XPREAD_RECORD_OPEN_SUCESS = "record_6"; // 打开一个已完成的任务
    public static final String KEY_XPREAD_RECORD_DELETE_SUCESS = "record_7"; // 点击删除一个已完成的任务
    public static final String KEY_XPREAD_RECORD_DELETE_COMFIRM_SUCESS = "record_8"; // 确认删除一个已完成的任务
    public static final String KEY_XPREAD_RECORD_EXPAND_OTHERS = "record_9"; // 点击未完成(等待传输,正在传输,传输错误)的任务弹出操作按钮
    public static final String KEY_XPREAD_RECORD_DELETE_OTHER = "record_10"; // 点击删除一个未完成的任务
    public static final String KEY_XPREAD_RECORD_DELETE_COMFIRM_OTHER = "record_11";// 确认删除一个未完成的任务

    public static final String KEY_XPREAD_SELECT_APK = "file_1"; // 用户选择一个App文件
    public static final String KEY_XPREAD_SELECT_IMAGE = "file_2"; // 用户选择一个Image文件
    public static final String KEY_XPREAD_SELECT_MUSIC = "file_3"; // 用户选择一个Music文件
    public static final String KEY_XPREAD_SELECT_VIDEO = "file_4"; // 用户选择一个Video文件
    public static final String KEY_XPREAD_SELECT_FILE = "file_5"; // 用户在File标签下选择一个文件
    public static final String KEY_XPREAD_SEND_ONE = "file_6"; // 用户点击send时，已选文件为1个
    public static final String KEY_XPREAD_SEND_TWO = "file_7"; // 用户点击send时，已选文件为2个
    public static final String KEY_XPREAD_SEND_THREE = "file_8"; // 用户点击send时，已选文件为3个
    public static final String KEY_XPREAD_SEND_FOUR_FIVE = "file_9"; // 用户点击send时，已选文件为4个或5个
    public static final String KEY_XPREAD_SEND_SIX_TEN = "file_10"; // 用户点击send时，已选文件为6个至10个
    public static final String KEY_XPREAD_SEND_TEN_ABOVE = "file_11"; // 用户点击send时，已选文件为10个以上

    public static final String KEY_XPREAD_CONNECT_SCAN_NONE = "connect_1"; // 扫描其他用户1分钟后,可选用户仍为空
    public static final String KEY_XPREAD_CONNECT_SELECT_ONE = "connect_2"; // 扫描其他用户后,选中一个用户
    public static final String KEY_XPREAD_CONNECT_SUCESS = "connect_3"; // 扫描其他用户后,选中一个用户,并成功建立连接
    public static final String KEY_XPREAD_CONNECT_FAILURE = "connect_4"; // 扫描其他用户后,选中一个用户,建立连接失败
    public static final String KEY_XPREAD_CONNECT_SCAN_EXIT = "connect_5"; // 扫描其他用户后,选择退出
    public static final String KEY_XPREAD_WAIT_CREATE_FAILURE = "connect_6"; // 等待用户连接,创建热点失败
    public static final String KEY_XPREAD_WAIT_CREATE_SUCESS = "connect_7"; // 等待用户连接,创建热点成功
    public static final String KEY_XPREAD_WAIT_SELECT_SUCESS = "connect_8"; // 等待用户连接,被选中,并成功建立连接
    public static final String KEY_XPREAD_WAIT_TIMEOUT = "connect_9"; // 等待用户连接,创建热点成功3分钟,仍未成功建立连接
    public static final String KEY_XPREAD_WAIT_EXIT = "connect_10"; // 等待用户连接,选择退出

    public static final String KEY_XPREAD_CRASH = "crash";
}
