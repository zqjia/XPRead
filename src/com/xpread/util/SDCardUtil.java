/**
 * <p>Title: xpread</p>
 *
 * <p>Description: </p>
 * 文件资源选择界面
 * <p>Copyright: Copyright (c) 2014</p>
 *
 * <p>Company: ucweb.com</p>
 *
 * @author jiazq@ucweb.com
 * @version 1.0
 */

package com.xpread.util;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;

public class SDCardUtil {  
    
    private SDCardUtil() {   
        throw new UnsupportedOperationException("cannot be instantiated");  
    }  
  
    /** 
     * 判断SDCard是否可用 
     *  
     * @return 
     */  
    public static boolean isSDCardEnable() {  
        return Environment.getExternalStorageState().equals(  
                Environment.MEDIA_MOUNTED);  
        
  
    }  
  
    /** 
     * 获取SD卡路径 
     *  
     * @return 
     */  
    public static String getSDCardPath() {  
        return Environment.getExternalStorageDirectory().getAbsolutePath()  
                + File.separator;  
    }  
  
    /** 
     * 获取SD卡的剩余容量 单位byte 
     *  
     * @return 
     */  
    @SuppressWarnings("deprecation")
    public static long getSDCardAllSize() {  
        if (isSDCardEnable()) {  
            StatFs stat = new StatFs(getSDCardPath());  
             
            long availableBlocks = (long) stat.getAvailableBlocks() - 4;    
            long freeBlocks = stat.getAvailableBlocks();  
            return freeBlocks * availableBlocks;  
        }  
        return 0;  
    }  
  
    /** 
     * 获取指定路径所在空间的剩余可用容量字节数，单位byte 
     *  
     * @param filePath 
     * @return 容量字节 SDCard可用空间，内部存储可用空间 
     */  
    @SuppressWarnings("deprecation")
    public static long getFreeBytes(String filePath) {  
        
        if (filePath.startsWith(getSDCardPath())) {  
            filePath = getSDCardPath();  
        } else {  
            filePath = Environment.getDataDirectory().getAbsolutePath();  
        }  
        StatFs stat = new StatFs(filePath);  
        long availableBlocks = (long) stat.getAvailableBlocks() - 4;  
        return stat.getBlockSize() * availableBlocks;  
    }  
  
    /** 
     * 获取系统存储路径 
     *  
     * @return 
     */  
    public static String getRootDirectoryPath() {  
        return Environment.getRootDirectory().getAbsolutePath();  
    }  
  
  
}  