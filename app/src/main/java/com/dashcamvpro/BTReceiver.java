package com.dashcamvpro;


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;


public class BTReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if ("com.dashcamvpro.Some_general_constant".equals(action)) {
            Log.d("edit MAC", intent.getStringExtra("MAC"));
            String address = intent.getStringExtra("MAC");
            String filePath = context.getFilesDir().getPath() + "MAC.txt";
            File file = new File(filePath);
            if (!file.exists()) {

            }

            try {
                FileOutputStream fOut = new FileOutputStream(filePath);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);
                osw.write(address);
                osw.flush();
                osw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else if (settings.getBoolean("use_bt", false)) {
            try {
                String filePath = context.getFilesDir().getPath() + "MAC.txt";
                BufferedReader br = new BufferedReader(new FileReader(filePath));
                String con_address = br.readLine();


                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        String mac_a = device.getAddress();
                        String msg = "started app with " + con_address;
                        Log.d("BT", msg);
                        if (con_address.equals(mac_a)) {
                            Intent i = new Intent(context, com.dashcamvpro.MainActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(i);


                        }


                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        context.stopService(new Intent(context, com.dashcamvpro.BackgroundVideoRecorder.class));
                        context.stopService(new Intent(context, com.dashcamvpro.WidgetService.class));

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}