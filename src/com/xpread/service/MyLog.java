
package com.xpread.service;

/*
 Copyright 2009 David Revell

 This file is part of SwiFTP.

 SwiFTP is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 SwiFTP is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

import com.xpread.util.SDCardUtil;

/**
 * 保存service log,其他地方需要用到请复制该类，自己写个保存路径
 */
public class MyLog {
    // private final static StringBuffer MESSAGE = new StringBuffer(10240);

    private static LoggerThread mLogger;

    private final static String SAVE_PATH = SDCardUtil.getSDCardPath() + "xpreadLog";

    private final static BlockingQueue<String> queue = new LinkedBlockingQueue<String>(100);

    private static boolean isShutDown = false;

    private static int mReservations = 0;

    private static File mSaveFile;

    protected String tag;

    private static boolean isLog = false;

    public static void initLog() {
        if (!isLog) {
            Log.e(MyLog.class.getSimpleName(), " init isLog = false");
            return;
        }
        File destDir = new File(SAVE_PATH);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        String dayStr = null;
        String hourStr = null;
        String minuteStr = null;
        String secondStr = null;
        if (day < 10) {
            dayStr = "0" + day;
        } else {
            dayStr = "" + day;
        }
        if (hour < 10) {
            hourStr = "0" + hour;
        } else {
            hourStr = "" + hour;
        }
        if (minute < 10) {
            minuteStr = "0" + minute;
        } else {
            minuteStr = "" + minute;
        }
        if (second < 10) {
            secondStr = "0" + second;
        } else {
            secondStr = "" + second;
        }

        String fileName = "log" + dayStr + "_" + hourStr + "_" + minuteStr + "_" + secondStr
                + ".txt";
        mSaveFile = new File(SAVE_PATH, fileName);
        queue.clear();
        mLogger = new LoggerThread(mSaveFile);
        isShutDown = false;
    }

    public static void start() {
        if (!isLog) {
            Log.e(MyLog.class.getSimpleName(), " start isLog = false");
            return;
        }
        synchronized (MyLog.class) {
            isShutDown = false;
        }
        if (mLogger != null) {
            mLogger.start();
        } else {
            Log.e(MyLog.class.getSimpleName(), " please init MyLog before start");
        }

    }

    public static void stop() {
        Log.e(MyLog.class.getSimpleName(), " shut down");
        synchronized (MyLog.class) {
            isShutDown = true;
        }
        if (mLogger != null) {
            mLogger.interrupt();
        }
    }

    private static boolean log(String message) {
        synchronized (MyLog.class) {
            if (isShutDown) {
                Log.e(MyLog.class.getSimpleName(), "is shut down , can not save message");
                return false;
            }
            ++mReservations;
        }
        return queue.offer(message);
    }

    public MyLog(String tag) {
        this.tag = tag;
    }

    private void l(int level, String str, boolean isLog) {
        if (isLog) {
            switch (level) {
                case Log.ERROR:
                    Log.e(tag, str);
                    log(tag + "----e------->" + str + "\n");
                    break;
                case Log.DEBUG:
                    Log.d(tag, str);
                    log(tag + "----d------->" + str + "\n");
                    break;
                case Log.WARN:
                    Log.w(tag, str);
                    log(tag + "----w------->" + str + "\n");
                    break;
                case Log.INFO:
                    Log.i(tag, str);
                    log(tag + "----i------->" + str + "\n");
                    break;
            }
        }
    }

    public void l(int level, String str) {
        l(level, str, isLog);
    }

    public void e(String s) {
        l(Log.ERROR, s, isLog);
    }

    public void w(String s) {
        l(Log.WARN, s, isLog);
    }

    public void i(String s) {
        l(Log.INFO, s, isLog);
    }

    public void d(String s) {
        l(Log.DEBUG, s, isLog);
    }

    public void logException(Throwable t) {
        if (isLog) {
            String eMsg = null;
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            try {
                t.printStackTrace(pw);
                eMsg = sw.toString();
            } finally {
                pw.close();
                try {
                    sw.close();
                } catch (IOException e) {
                }
            }
            if (eMsg != null) {
                log(tag + "-------------------------expection---------------------\n" + eMsg
                        + "\n----------------------------end------------------------\n");
            }
            t.printStackTrace();
        }
    }

    private static class LoggerThread extends Thread {
        private File mFile;

        public LoggerThread(File file) {
            this.mFile = file;
            this.setPriority(NORM_PRIORITY - 2);
            this.setName("Log Thread");
            this.setDaemon(true);
        }

        @Override
        public void run() {
            BufferedWriter w = null;
            try {
                w = new BufferedWriter(new FileWriter(mFile));
                while (!Thread.interrupted()) {
                    synchronized (MyLog.class) {
                        if (isShutDown && mReservations == 0) {
                            break;
                        }
                    }
                    String str = queue.take();
                    synchronized (MyLog.class) {
                        mReservations--;
                    }
                    w.write(str);
                    w.flush();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (w != null) {
                    try {
                        w.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

}
