package com.dashcamvpro;


import static com.dashcamvpro.DeviceListActivity.items;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;


public class BTReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BT", "Receive");
        String action = intent.getAction();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (settings.getBoolean("use_bt", false)) {
            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String mac_a = device.getAddress();


                    for (DeviceListActivity.Item item : items) {
                        if (item.ItemString.equals(mac_a)) {
                            Intent intent1 = new Intent(context, MainActivity.class);
                            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent1);

                        }
                    }
                    String msg = "started app with " + mac_a;
                    Log.d("BT", msg);


                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    context.stopService(new Intent(context, BackgroundVideoRecorder.class));
                    context.stopService(new Intent(context, WidgetService.class));

            }
        }
    }
}