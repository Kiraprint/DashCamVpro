package com.dashcamvpro;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    public static final int MULTIPLE_PERMISSIONS_RESPONSE_CODE = 10;
    private static final int CODE_REQUEST_PERMISSION_TO_MUTE_SYSTEM_SOUND = 10001;
    private static final int CODE_REQUEST_PERMISSION_DRAW_OVER_APPS = 10002;

    String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MA", "Started app");
        init();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CODE_REQUEST_PERMISSION_TO_MUTE_SYSTEM_SOUND:
                //if user has not allowed this permission close the app, otherwise continue
                if (isPermissionToMuteSystemSoundGranted()) {
                    init();
                } else {
                    finish();
                }
                break;
            case CODE_REQUEST_PERMISSION_DRAW_OVER_APPS:
                //if user has not allowed this permission close the app, otherwise continue
                if (Settings.canDrawOverlays(this)) {
                    init();
                } else {
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void init() {
        // Check permissions to draw over apps
        if (!checkDrawPermission()) return;

        //@dmitriy.chernysh:
        //check permission to mute system audio on Android 7 (AudioManager setStreamVolume)
        //java.lang.SecurityException: Not allowed to change Do Not Disturb state
        if (!checkPermissionToMuteSystemSound()) return;

        if (checkPermissions()) {
            startApp();
        }
    }

    private void startApp() {

        if (!isEnoughStorage()) {
            com.dashcamvpro.Util.showToastLong(this.getApplicationContext(),
                    "Not enough storage to run the app (Need " + String.valueOf(com.dashcamvpro.Util.getQuota())
                            + "MB). Clean up space for recordings.");
        } else {
            // Check if first launch => show tutorial
            // Access shared references file
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                    getString(R.string.db_first_launch_complete_flag),
                    Context.MODE_PRIVATE);

            String firstLaunchFlag = sharedPref.
                    getString(getString(R.string.db_first_launch_complete_flag),
                            "null");

            if (TextUtils.isEmpty(firstLaunchFlag)) {
                Intent intent = new Intent(getApplicationContext(), com.dashcamvpro.WelcomeActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // Otherwise


            // Start recording video
            Intent videoIntent = new Intent(getApplicationContext(), BackgroundVideoRecorder.class);
            startService(videoIntent);

            // Start rootView service (display the widgets)
            Intent i = new Intent(getApplicationContext(), com.dashcamvpro.WidgetService.class);
            startService(i);

            launchOtherApps();
        }

        // Close the activity, we don't have an app window
        finish();
    }

    private boolean checkDrawPermission() {
        // for Marshmallow (SDK 23) and newer versions, get overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_REQUEST_PERMISSION_DRAW_OVER_APPS);

            Toast.makeText(MainActivity.this, "Draw over apps permission needed", Toast.LENGTH_LONG)
                    .show();

            Toast.makeText(MainActivity.this, "Allow and click \"Back\"", Toast.LENGTH_LONG)
                    .show();

            Toast.makeText(MainActivity.this, "Then restart the Open Dash Cam app", Toast.LENGTH_LONG)
                    .show();

            return false;
        }
        return true;
    }


    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ActivityCompat.checkSelfPermission(MainActivity.this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    MULTIPLE_PERMISSIONS_RESPONSE_CODE);
            return false;
        }
        return true;
    }

    /**
     * Check and ask permission to set "Do not Disturb"
     * Note: it uses in BackgroundVideoRecorder : audio.setStreamVolume()
     *
     * @return True - granted
     */
    private boolean checkPermissionToMuteSystemSound() {

        if (!isPermissionToMuteSystemSoundGranted()) {
            Intent intent = new Intent(
                    Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivityForResult(intent, CODE_REQUEST_PERMISSION_TO_MUTE_SYSTEM_SOUND);
            return false;
        }

        return true;
    }

    private boolean isPermissionToMuteSystemSoundGranted() {
        //Android 7+ needs this permission (but Samsung devices may work without it)
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)) return true;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return true;

        return notificationManager.isNotificationPolicyAccessGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == MULTIPLE_PERMISSIONS_RESPONSE_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permissions granted
                startApp();
            } else {
                // permissions not granted
                Toast.makeText(MainActivity.this, "Permissions denied. The app cannot start.", Toast.LENGTH_LONG)
                        .show();

                Toast.makeText(MainActivity.this, "Please re-start Open Dash Cam app and grant the requested permissions.", Toast.LENGTH_LONG)
                        .show();

                finish();
            }
        }
    }


    private boolean isEnoughStorage() {
        File videosFolder = com.dashcamvpro.Util.getVideosDirectoryPath();
        if (videosFolder == null) return false;

        long appVideosFolderSize = com.dashcamvpro.Util.getFolderSize(videosFolder);
        long storageFreeSize = com.dashcamvpro.Util.getFreeSpaceExternalStorage(videosFolder);
        //check enough space
        return storageFreeSize + appVideosFolderSize >= (com.dashcamvpro.Util.getQuota());
    }

    private void launchOtherApps() {
        try {
            String filePath = getApplicationContext().getFilesDir().getPath() + "/apps.txt";
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String string = br.readLine();
            String[] strings = string.split(" ");
            for (String str : strings) {
                Log.d("MA", str);
                Intent intent = getPackageManager().getLaunchIntentForPackage(str);
                startActivity(intent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}