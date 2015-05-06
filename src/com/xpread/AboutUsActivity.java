
package com.xpread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.xpread.util.WaUtils;
import com.xpread.widget.RobotoTextView;

public class AboutUsActivity extends BaseActivity {

    private static final String TAG = "AboutUsActivity";

    private ListView mAboutListView;

    private ImageView mBackView;
    
    private RobotoTextView mVersionName;
    
    private static final String GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending";

    private static final String GOOGLE_PLAY_ACTIVITY_NAME = "com.android.vending.AssetBrowserActivity";

    private static final String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=com.xpread";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_aboutus);

        this.mBackView = (ImageView)this.findViewById(R.id.back);
        this.mBackView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        this.mVersionName = (RobotoTextView)this.findViewById(R.id.about_app_version);
        String versionName = "V " + WaUtils.getVersionName();
        this.mVersionName.setText(versionName);
        
        this.mAboutListView = (ListView)this.findViewById(R.id.about_opra);
        this.mAboutListView.setDividerHeight(0);
        String[] from = {
                "icon", "text"
        };
        int[] to = {
                R.id.about_lv_icon, R.id.about_lv_opra
        };

        ArrayList<HashMap<String, Object>> listData = initListViewData();

        SimpleAdapter adapter = new SimpleAdapter(this, listData, R.layout.about_listview_item,
                from, to);
        this.mAboutListView.setAdapter(adapter);
        
        this.mAboutListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || position == 1) {
                    
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    
                    if (isGooglePlayInstall()) {
                        intent.setClassName(GOOGLE_PLAY_PACKAGE_NAME, GOOGLE_PLAY_ACTIVITY_NAME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    intent.setData(Uri.parse(GOOGLE_PLAY_URL));
                    startActivity(intent);
                }
            }
        });

    }

    private ArrayList<HashMap<String, Object>> initListViewData() {
        ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

        HashMap<String, Object> map1 = new HashMap<String, Object>();
        map1.put("icon", R.drawable.rate_us);
        map1.put("text", getResources().getString(R.string.about_rate_us));

        HashMap<String, Object> map2 = new HashMap<String, Object>();
        map2.put("icon", R.drawable.feed_back);
        map2.put("text", getResources().getString(R.string.about_feed_back));

        list.add(map1);
        list.add(map2);
        return list;
    }

    /*
     * add by zqjia call the method in the BaseActivity to start and stop the
     * home key watcher
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private boolean isGooglePlayInstall() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                if (packageInfo.applicationInfo.packageName.contains(GOOGLE_PLAY_PACKAGE_NAME)) {
                    return true;
                }
            }
        }
        
        return false;
    }

}
