package com.xpread.control;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import com.xpread.XApplication;

public class MobileNetworkManager {
    
    private static final String TAG = MobileNetworkManager.class.getSimpleName();
    
    private static final String SET_MOBILE_DATA_ENABLED = "setMobileDataEnabled";
    
    /**
     * 设置设备移动网络的开关状态
     * @param isEnabled
     *      true 打开设备的移动网络
     *      false 关闭设备的移动网络
     * */
    public static void setMobileNetworkState(boolean isEnabled) {
        Context context = XApplication.getContext();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        //获取需要反射的类对象
        Class<? extends ConnectivityManager> cmClass = cm.getClass();
        
        //为反射调用函数的boolean参数准备
        Class[] argsClass = new Class[1];  
        argsClass[0] = boolean.class;  
        
        //调用反射机制，完成开启和关闭移动网络的功能
        try {
            Method method = cmClass.getMethod(SET_MOBILE_DATA_ENABLED, argsClass);
            method.invoke(cm, isEnabled);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(TAG, "set mobile network fail");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "set mobile network fail");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.e(TAG, "set mobile network fail");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Log.e(TAG, "set mobile network fail");
        }
    } 
    
    public static boolean getMobileNetworkState() {
        Context context = XApplication.getContext();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        //获取需要反射的类对象
        Class<? extends ConnectivityManager> cmClass = cm.getClass();
        Class[] argsClass = null; 
        
        try {
            Method method = cmClass.getMethod("getMobileDataEnabled", argsClass);
            Boolean isOpen = (Boolean) method.invoke(cm, null);
            return isOpen;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(TAG, "get mobile network state error");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "get mobile network state error");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.e(TAG, "get mobile network state error");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Log.e(TAG, "get mobile network state error");
        }
        
        return false;
    }
}
