package com.dashcamvpro;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class LaunchActivity extends AppCompatActivity {

    public class LaunchItem {
        boolean checked;
        Drawable ItemDrawable;
        String ItemString;

        LaunchItem(Drawable drawable, String t, boolean b) {
            ItemDrawable = drawable;
            ItemString = t;
            checked = b;
        }

        public boolean isChecked() {
            return checked;
        }
    }

    static class ViewHolder {
        CheckBox checkBox;
        ImageView icon;
        TextView text;
    }

    public class LaunchItemsListAdapter extends BaseAdapter {

        private Context context;
        private List<LaunchItem> list;

        LaunchItemsListAdapter(Context c, List<LaunchItem> l) {
            context = c;
            list = l;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public boolean isChecked(int position) {
            return list.get(position).checked;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View rowView = convertView;

            // reuse views
            ViewHolder viewHolder = new ViewHolder();
            if (rowView == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                rowView = inflater.inflate(R.layout.row, null);

                viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.rowCheckBox);
                viewHolder.icon = (ImageView) rowView.findViewById(R.id.rowImageView);
                viewHolder.text = (TextView) rowView.findViewById(R.id.rowTextView);
                rowView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) rowView.getTag();
            }

            viewHolder.icon.setImageDrawable(list.get(position).ItemDrawable);
            viewHolder.checkBox.setChecked(list.get(position).checked);

            final String itemStr = list.get(position).ItemString;
            viewHolder.text.setText(itemStr);

            viewHolder.checkBox.setTag(position);


            viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean newState = !list.get(position).isChecked();
                    list.get(position).checked = newState;

                    LaunchItem some_item = list.get(position);
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(some_item.ItemString, newState);
                    editor.apply();

                    Toast.makeText(getApplicationContext(),
                            itemStr + "setOnClickListener\nchecked: " + newState,
                            Toast.LENGTH_LONG).show();
                }
            });
            viewHolder.checkBox.setChecked(isChecked(position));
            return rowView;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch_activity);

        ListView listViewAppsInstalled = findViewById(R.id.listViewInstalledApps);
        List<ApplicationInfo> listApplicationInfo = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        List<LaunchItem> launchItems = new ArrayList<LaunchItem>();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        for (ApplicationInfo applicationInfo : listApplicationInfo) {
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {

                String label = applicationInfo.loadLabel(getPackageManager()).toString();
                Drawable icon = applicationInfo.loadIcon(getPackageManager());
                boolean savedValue = sharedPreferences.getBoolean(label, false);
                LaunchItem li = new LaunchItem(icon, label, savedValue);
                launchItems.add(li);

            }
        }

        LaunchItemsListAdapter listAdapter = new LaunchItemsListAdapter(LaunchActivity.this, launchItems);
        listViewAppsInstalled.setAdapter(listAdapter);
        listViewAppsInstalled.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(LaunchActivity.this,
                        ((DeviceListActivity.Item) (parent.getItemAtPosition(position))).ItemString,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
