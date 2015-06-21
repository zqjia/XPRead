
package com.xpread;

import java.io.File;
import java.util.HashMap;

import android.app.Application;
import android.content.Context;

import com.uc.base.wa.WaBodyBuilder;
import com.uc.base.wa.WaEntry;
import com.uc.base.wa.WaEntry.ConfigIniter;
import com.uc.base.wa.WaEntry.IWaIniter;
import com.uc.base.wa.WaEntry.WaListenerInterface;
import com.uc.base.wa.WaEntry.WaSystemDataInterface;
import com.uc.base.wa.adapter.WaAdapterHelper;
import com.uc.base.wa.cache.WaOperStrategyInterface;
import com.uc.base.wa.config.WaConfig;
import com.uc.base.wa.config.WaDef;
import com.xpread.transfer.exception.CrashHandler;
import com.xpread.util.Utils;
import com.xpread.util.WaUtils;
import com.xpread.wa.DefaultAdapter;
import com.xpread.wa.WaKeys;

public class XApplication extends Application {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());

        Utils.initImageLoader(mContext);
        WaAdapterHelper.registerIniter(new IWaIniter() {

            @Override
            public void onInit() {
                WaEntry.init(new DefaultAdapter(), "0489d183b6b2");

                WaConfig xpreadWaConfig = new WaConfig();
                xpreadWaConfig.init(WaConfig.LEVEL_BUSINESS_CORE);
                WaEntry.initPutCategorieId(WaKeys.CATEGORY_XPREAD, xpreadWaConfig);

                WaConfig xpreadDataWaConfig = new WaConfig();
                xpreadDataWaConfig.init(WaConfig.LEVEL_BUSINESS);
                WaEntry.initPutCategorieId(WaKeys.CATEGORY_XPREAD_DATA, xpreadDataWaConfig);

                String[] heads = {
                        WaDef.KEY_SYSTEM_HEAD_VER, WaDef.KEY_SYSTEM_HEAD_IMSI
                };
                String[] bodyies = {
                    WaDef.KEY_SYSTEM_BODY_TIME
                };
                WaEntry.initSetGlobalAutoCfg(heads, bodyies);

                WaEntry.registerListener(WaDef.CATEGORY_FORCED, new WaListenerInterface() {

                    @Override
                    public void onInvoked(int type, String category, InvokeHelper invokeHelper) {

                        if (type == WaListenerInterface.INVOKED_TYPE_UPLOADING) {
                            long size = caculateDirSize(WaConfig.getWaDir());
                            WaEntry.statEv(WaDef.CATEGORY_FORCED, WaBodyBuilder.newInstance()
                                    .build(WaDef.KEY_FORCED_BODY_FILE_SIZE, String.valueOf(size)));
                        }
                    }

                });

                WaEntry.initSetSystemDataImpl(new WaSystemDataInterface() {

                    @Override
                    public String getSystemData(String key) {
                        String ret = null;

                        if (WaDef.KEY_SYSTEM_HEAD_MODEL.equals(key)) {
                            ret = android.os.Build.MODEL.trim();
                        } else if (WaDef.KEY_SYSTEM_HEAD_ROM.equals(key)) {
                            ret = android.os.Build.VERSION.RELEASE.trim();
                        } else if (WaDef.KEY_SYSTEM_HEAD_CPU_ARCH.equals(key)) {
                            ret = WaUtils.getCpuArch();
                        } else if (WaDef.KEY_SYSTEM_HEAD_IMSI.equals(key)) {
                            ret = WaUtils.getDeviceId();
                        } else if (WaDef.KEY_SYSTEM_HEAD_MAC.equals(key)) {
                            ret = WaUtils.getMacAddress();
                        } else if (WaDef.KEY_SYSTEM_HEAD_WIDTH.equals(key)) {
                            ret = WaUtils.getScreenSize(true);
                        } else if (WaDef.KEY_SYSTEM_HEAD_HEIGHT.equals(key)) {
                            ret = WaUtils.getScreenSize(false);
                        } else if (WaDef.KEY_SYSTEM_HEAD_LANG.equals(key)) {
                            ret = WaUtils.getSystemLanguage();
                        } else if (WaDef.KEY_SYSTEM_HEAD_VER.equals(key)) {
                            ret = WaUtils.getVersionName();
                        } else if (WaDef.KEY_SYSTEM_BODY_FREE_MEM.equals(key)) {
                            ret = WaUtils.getFreeMemory();
                        }

                        return ret;
                    }

                    @Override
                    public void initHeadOperationsStrategy(HashMap<String, Integer> strategyMap) {
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_MODEL,
                                WaOperStrategyInterface.THREAD_SYNC);
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_ROM,
                                WaOperStrategyInterface.THREAD_SYNC);
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_CPU_ARCH,
                                WaOperStrategyInterface.THREAD_SYNC);
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_IMSI,
                                WaOperStrategyInterface.THREAD_SYNC);
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_MAC,
                                WaOperStrategyInterface.THREAD_SYNC);
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_WIDTH,
                                WaOperStrategyInterface.THREAD_SYNC);
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_HEIGHT,
                                WaOperStrategyInterface.THREAD_SYNC);
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_LANG,
                                WaOperStrategyInterface.THREAD_SYNC);
                        strategyMap.put(WaDef.KEY_SYSTEM_HEAD_VER,
                                WaOperStrategyInterface.THREAD_SYNC);

                    }

                    @Override
                    public void initBodyOperationsStrategy(HashMap<String, Integer> strategyMap) {
                        strategyMap.put(WaDef.KEY_SYSTEM_BODY_FREE_MEM,
                                WaOperStrategyInterface.THREAD_SYNC);
                    }
                });

            }
        });

        WaEntry.statEv(WaDef.CATEGORY_SYSTEM, WaDef.KEY_SYSTEM_HEAD_MODEL,
                WaDef.KEY_SYSTEM_HEAD_ROM, WaDef.KEY_SYSTEM_HEAD_CPU_ARCH,
                WaDef.KEY_SYSTEM_HEAD_IMSI, WaDef.KEY_SYSTEM_HEAD_MAC, WaDef.KEY_SYSTEM_HEAD_WIDTH,
                WaDef.KEY_SYSTEM_HEAD_HEIGHT, WaDef.KEY_SYSTEM_HEAD_LANG,
                WaDef.KEY_SYSTEM_HEAD_VER, WaDef.KEY_SYSTEM_BODY_FREE_MEM);
    }

    public static Context getContext() {
        return mContext;
    }

    private long caculateDirSize(File item) {
        if (item == null || !item.exists()) {
            return 0;
        }
        if (item.isDirectory()) {
            long temp = 0;
            File[] fileList = item.listFiles();
            if (null != fileList) {
                for (File subitem : fileList) {
                    temp += caculateDirSize(subitem);
                }
            }
            return temp;
        } else {
            return item.length();
        }
    }
}
