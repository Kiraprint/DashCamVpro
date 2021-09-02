package com.dashcamvpro;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch_activity);

        ListView listViewAppsInstalled = findViewById(R.id.listViewInstalledApps);
        List<ApplicationInfo> listApplicationInfo = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        String[] stringsList = new String[listApplicationInfo.size()];

        int i = 0;
        for (ApplicationInfo applicationInfo : listApplicationInfo) {
            stringsList[i] = applicationInfo.packageName;
            i++;
        }

        listViewAppsInstalled.setAdapter(new ArrayAdapter<String>(LaunchActivity.this, android.R.layout.simple_list_item_1, stringsList));
        listViewAppsInstalled.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String str = adapterView.getItemAtPosition(i).toString().trim();
                String filePath = getApplicationContext().getFilesDir().getPath() + "/apps.txt";
                File file = new File(filePath);

                if (!file.exists()) {
                    Log.d("LA", "wtf");
                }

                try (FileWriter fw = new FileWriter(filePath);
                     BufferedWriter bw = new BufferedWriter(fw);
                     PrintWriter out = new PrintWriter(bw)) {

                    out.println(str);

                } catch (IOException e) {
                    Log.d("LA", "smth wrong with writing");
                }

            }
        });
    }
}
