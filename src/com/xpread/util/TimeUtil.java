package com.xpread.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Formatter;

public class TimeUtil {

    /** 
     * 将毫秒数装换成pattern这个格式，这里是转换成年月日 
     * @param time 
     * @param pattern 
     * @return 
     */  
    public static String paserTimeToYMD(long time, String pattern ) {  
        /*SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());  
        return format.format(new Date(time * 1000));*/  
        
        Date dat=new Date(time * 1000);  
        GregorianCalendar gc = new GregorianCalendar();   
        gc.setTime(dat);  
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());  
        String sb=format.format(gc.getTime());  
        return sb;
    } 
    
    public static String stringForTime(int timeMs) {
        StringBuilder formatBuilder = new StringBuilder();
        Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
        
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        formatBuilder.setLength(0);
        String result = null;
        if (hours > 0) {
            result = formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            result = formatter.format("%02d:%02d", minutes, seconds).toString();
        }
        
        formatter.close();
        return result;
        
    }
}
