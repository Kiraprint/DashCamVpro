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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;


public class BTReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BT", "Receive");
        String action = intent.getAction();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        File path = context.getFilesDir();
        if ("EditMac".equals(action)) {
            Log.d("edit MAC", intent.getStringExtra("MAC"));
            String address = intent.getStringExtra("MAC");
            File file = new File(path, "MAC.txt");
            FileOutputStream stream;
            try {
                stream = new FileOutputStream(file);
                stream.write(address.getBytes());
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (settings.getBoolean("use_bt", false)) {
            File file = new File(path, "MAC.txt");
            int length = (int) file.length();

            byte[] bytes = new byte[length];

            FileInputStream in;
            try {
                in = new FileInputStream(file);
                in.read(bytes);
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String con_address = new String(bytes);


            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String mac_a = device.getAddress();
                    String msg = "started app with " + con_address;
                    Log.d("BT", msg);

                    if (con_address.equals(mac_a)) {
                        Intent intent1 = new Intent(context, MainActivity.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent1);

                    }


                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    context.stopService(new Intent(context, BackgroundVideoRecorder.class));
                    context.stopService(new Intent(context, WidgetService.class));

            }

        }
    }


}