
package com.xpread;

import java.io.File;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.uc.base.wa.WaEntry;
import com.xpread.util.LaboratoryData;
import com.xpread.wa.WaKeys;
import com.xpread.widget.RoundImageButton;

public class ShareActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "ShareActivity";

    private ImageView mBackButton;

    private RoundImageButton mQrcodeButton;

    private RoundImageButton mBluetoothButton;

    private Dialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_share);

        initView();
    }

    private void initView() {
        this.mBackButton = (ImageView)findViewById(R.id.back);
        this.mQrcodeButton = (RoundImageButton)findViewById(R.id.qrcode_button);
        this.mBluetoothButton = (RoundImageButton)findViewById(R.id.bluetooth_button);

        this.mBackButton.setOnClickListener(this);
        this.mQrcodeButton.setOnClickListener(this);
        this.mBluetoothButton.setOnClickListener(this);

        // dialog to show the qrcode
        this.mDialog = new AlertDialog.Builder(this).create();
        this.mDialog.setCanceledOnTouchOutside(true);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;

            case R.id.qrcode_button:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SHARE_QR);

                Intent intent = new Intent(ShareActivity.this, QrCodeActivity.class);
                startActivity(intent);
                /*if (!this.mDialog.isShowing()) {
                    LayoutInflater inflater = LayoutInflater.from(this);
                    RelativeLayout relativeLayout = (RelativeLayout)inflater.inflate(
                            R.layout.qrcode_dialog, null);
                    this.mDialog.show();
                    this.mDialog.getWindow().setContentView(relativeLayout);
                }*/

                break;

            case R.id.bluetooth_button:
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_SHARE_BLUETOOTH);
                // 实验室数据
                LaboratoryData.addOne(LaboratoryData.KEY_XPREAD_DATA_BLUETOOTH_SEND_COUNT);
                // -----------------
                final PackageManager pm = ShareActivity.this.getPackageManager();
                final List<PackageInfo> packages = pm.getInstalledPackages(0);

                for (int i = 0; i < packages.size(); i++) {
                    PackageInfo packageInfo = packages.get(i);
                    if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
                        if (packageInfo.applicationInfo.loadLabel(pm).toString().equals("Xpread")) {
                            File file = new File(packageInfo.applicationInfo.sourceDir);
                            Uri appUri = Uri.fromFile(file);
                            if (appUri != null) {

                                Intent newIntent = new Intent(Intent.ACTION_SEND);
                                newIntent.setClassName("com.mediatek.bluetooth",
                                        "com.mediatek.bluetooth.BluetoothShareGatewayActivity");
                                newIntent.putExtra(Intent.EXTRA_STREAM, appUri);
                                newIntent.setType("application/vnd.android.package-archive");

                                Intent oldIntent = new Intent(Intent.ACTION_SEND);
                                oldIntent.setClassName("com.android.bluetooth",
                                        "com.android.bluetooth.opp.BluetoothOppLauncherActivity");
                                oldIntent.putExtra(Intent.EXTRA_STREAM, appUri);
                                oldIntent.setType("application/vnd.android.package-archive");

                                Intent intentHuaWei = new Intent(Intent.ACTION_SEND);
                                intentHuaWei.setClassName("com.android.bluetooth",
                                        "com.broadcom.bt.app.opp.OppLauncherActivity");
                                intentHuaWei.putExtra(Intent.EXTRA_STREAM, appUri);
                                intentHuaWei.setType("application/vnd.android.package-archive");

                                if (isIntentAvailable(oldIntent)) {
                                    startActivity(oldIntent);
                                } else if (isIntentAvailable(newIntent)) {
                                    startActivity(newIntent);
                                } else if (isIntentAvailable(intentHuaWei)) {
                                    startActivity(intentHuaWei);
                                } else {
                                    Toast.makeText(ShareActivity.this,
                                            R.string.exception_bluetooth_share, Toast.LENGTH_SHORT)
                                            .show();
                                }

                                break;
                            }
                        }
                    }
                }

                break;

            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {

        if (this.mDialog.isShowing()) {
            this.mDialog.dismiss();
            return;
        }

        finish();
        super.onBackPressed();
    }

    private boolean isIntentAvailable(Intent intent) {
        final PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
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

}
