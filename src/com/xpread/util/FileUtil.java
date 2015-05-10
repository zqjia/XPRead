
package com.xpread.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <p>
 * Title: ucweb
 * </p>
 * <p>
 * Description:当前仅仅支持2G以内的文件
 * </p>
 * file util
 * <p>
 * Copyright: Copyright (c) 2010
 * </p>
 * <p>
 * Company: ucweb.com
 * </p>
 * 
 * @author wujm@ucweb.com
 * @version 1.0
 */
public class FileUtil {
    private final static String FILE_SAVE_PATH = SDCardUtil.getSDCardPath() + "Xpread/";

    /**
     * Don't need to be instantiated
     */
    private FileUtil() {

    }

    static {
        File dir = new File(FILE_SAVE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * get the file by the file name </br> if the fileName is exit , add
     * (number) before type </br> a.mp3 if exist ,change to a(1).mp3
     * 
     * @param aFileName
     * @return
     */
    public static File getFileByName(String aFileName) {

        if (aFileName == null) {
            throw new NullPointerException(" file name can not be null");
        }
        if (aFileName.length() == 0) {
            throw new IllegalArgumentException("the length of file name is error");
        }

        File dir = new File(FILE_SAVE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (aFileName.lastIndexOf("/") > 0) {
            aFileName = aFileName.substring(aFileName.lastIndexOf("/") + 1, aFileName.length());
        }

        return changeFileName(new StringBuilder(aFileName), 0);
    }

    private static File changeFileName(StringBuilder aFileName, int i) {
        if (aFileName == null || aFileName.length() == 0)
            return null;

        File file = new File(FILE_SAVE_PATH, aFileName.toString());

        if (file.exists()) {
            if (i != 0) {
                if (aFileName.indexOf("(") != -1 && aFileName.indexOf(")") != -1) {
                    aFileName.replace(aFileName.lastIndexOf("(") + 1, aFileName.lastIndexOf(")"),
                            "" + i);
                } else {
                    return null;
                }
            } else {
                if (aFileName.lastIndexOf(".") > 0) {
                    aFileName.insert(aFileName.lastIndexOf("."), "(1)");
                } else {
                    return null;
                }
            }

        } else {
            return file;
        }
        return changeFileName(aFileName, i + 1);
    }

    public static String getFileNameWithoutPath(String filePath) {
        if (filePath == null || filePath.length() == 0) {
            return null;
        }

        if (filePath.lastIndexOf("/") > 0) {
            return filePath.substring(filePath.lastIndexOf("/") + 1);
        }
        return filePath;
    }

    public static int getFileSizeByName(String filePath) {
        if (filePath == null || filePath.length() == 0) {
            return 0;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }

        int fileSize = (int)file.length();

        return fileSize;
    }

    public static boolean isFileExist(String aFileName, int fileSize) {

        if (aFileName == null || aFileName.length() == 0) {
            return false;
        }

        if (aFileName.lastIndexOf("/") > 0) {
            aFileName = aFileName.substring(aFileName.lastIndexOf("/") + 1, aFileName.length());
        }
        File file = new File(FILE_SAVE_PATH, aFileName);
        if (!file.exists()) {
            return false;
        }

        if ((int)file.length() != fileSize) {
            return false;
        }
        return true;
    }
    
    public static boolean isFileExist(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    public static String getPathByName(String aFileName, int fileSize) {
        if (aFileName == null || aFileName.length() == 0) {
            return null;
        }
        if (aFileName.lastIndexOf("/") > 0) {
            aFileName = aFileName.substring(aFileName.lastIndexOf("/") + 1, aFileName.length());
        }
        File file = new File(FILE_SAVE_PATH, aFileName);
        if (!file.exists()) {
            return null;
        }
        if ((int)file.length() != fileSize) {
            return null;
        }
        return file.getAbsolutePath();
    }

    public static void deleteFileByPath(String path) {
        if (path == null || path.length() == 0) {
            return;
        }
        File file = new File(path);
        // FIXME 解决了EBUSY的问题了吗？
        // 提测点：是否出现取消了的文件仍然存在
        if (file.exists()) {
            final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
            file.renameTo(to);
            to.delete();
        }
    }

    public static byte[] readBytes(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        byte[] buffer = null;
        try {
            int cacheSize = 1024;

            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(cacheSize);
            byte[] b = new byte[cacheSize];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }

            buffer = bos.toByteArray();

            fis.close();
            bos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO: handle exception
        }

        return buffer;
    }

    public static void writeBytes(String dirPath, String fileName, byte[] data) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(dirPath);
            if (!dir.exists() && dir.isDirectory()) {// 判断文件目录是否存在
                dir.mkdirs();
            }

            file = new File(dirPath, fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
