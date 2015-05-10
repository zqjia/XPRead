package com.xpread.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

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
}
