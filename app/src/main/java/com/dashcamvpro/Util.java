package com.dashcamvpro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.dashcamvpro.models.Recording;

import java.io.File;
import java.util.ArrayList;

/**
 * Global utility methods
 */

public class Util {
    public static final String ACTION_UPDATE_RECORDINGS_LIST = "update.recordings.list";
    public static final int FOREGROUND_NOTIFICATION_ID = 51288;

    private static Util util = null;

    private static final String NOTIFICATIONS_CHANNEL_ID_MAIN_NOTIFICATIONS = "1001";
    private static final String NOTIFICATIONS_CHANNEL_NAME_MAIN_NOTIFICATIONS = "Main notifications";

    public static int QUOTA = 200; // megabytes
    public static int QUOTA_WARNING_THRESHOLD = 100; // megabytes
    public static int MAX_DURATION = 45000; // 45 seconds

    public static synchronized Util getInstance(){
        if (util == null) util = new Util();
        return util;
    }

    public static File getVideosDirectoryPath() {
        //remove an old directory if exists
        File oldDirectory = new File(Environment.getExternalStorageDirectory() + "/OpenDashCam/");
        removeNonEmptyDirectory(oldDirectory);

        //New directory
        File appVideosFolder = getAppPrivateVideosFolder(DashAppV.getAppContext());

        if (appVideosFolder != null) {
            //create app-private folder if not exists
            if (!appVideosFolder.exists()) appVideosFolder.mkdir();
            return appVideosFolder;
        }

        return null;
    }

    public static int getQuota() {
        return QUOTA;
    }

    public static int getQuotaWarningThreshold() {
        return QUOTA_WARNING_THRESHOLD;
    }

    public static int getMaxDuration() {
        return MAX_DURATION;
    }

    /**
     * Displays toast message of LONG length
     *
     * @param context Application context
     * @param msg     Message to display
     */
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Display a 9-seconds-long toast.
     * Inspired by https://stackoverflow.com/a/7173248
     *
     * @param context Application context
     * @param msg     Message to display
     */
    public static void showToastLong(Context context, String msg) {
        final Toast tag = Toast.makeText(context, msg, Toast.LENGTH_SHORT);

        tag.show();

        new CountDownTimer(9000, 1000) {

            public void onTick(long millisUntilFinished) {
                tag.show();
            }

            public void onFinish() {
                tag.show();
            }

        }.start();
    }

    /**
     * Starts new activity to open speicified file
     *
     * @param file     File to open
     * @param mimeType Mime type of the file to open
     */
    public static void openFile(Context context, Uri file, String mimeType) {
        Intent openFile = new Intent(Intent.ACTION_VIEW);
        openFile.setDataAndType(file, mimeType);
        openFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openFile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(openFile);
        } catch (ActivityNotFoundException e) {
            Log.i("OpenDashCam", "Cannot open file.");
        }
    }

    /**
     * Calculates the size of a directory in megabytes
     *
     * @param file The directory to calculate the size of
     * @return size of a directory in megabytes
     */
    public static long getFolderSize(File file) {
        long size = 0;
        if (file.isDirectory()) {
            for (File fileInDirectory : file.listFiles()) {
                size += getFolderSize(fileInDirectory);
            }
        } else {
            size = file.length();
        }
        return size / 1024;
    }

    /**
     * Get available space on the device
     *
     * @return
     */
    public static long getFreeSpaceExternalStorage(File storagePath) {
        if (storagePath == null || !storagePath.isDirectory()) return 0;
        return storagePath.getFreeSpace() / 1024 / 1024;
    }

    /**
     * Delete all recordings from storage and sqlite
     * <p>
     * NOTE: called from UI settings (here uses asynctask for background operation)
     */
    public static void deleteRecordings() {
        AsyncTaskCompat.executeParallel(new DeleteRecordingsTask());
    }

    /**
     * Star/unstar recording
     * <p>
     * NOTE: called from UI (uses asynctasks)
     *
     * @param recording
     */
    public static void updateStar(Recording recording) {
        AsyncTaskCompat.executeParallel(new UpdateStarTask(recording));
    }

    /**
     * Delete single recording from storage and SQLite
     * <p>
     * NOTE: called from background thread (BackgroundVideoRecorder)
     *
     * @param recording Recording
     */
    public static void deleteSingleRecording(Recording recording) {
        if (recording == null) return;
        //delete from storage
        new File(recording.getFilePath()).delete();

        //delete from db
        DBHelper.getInstance(DashAppV.getAppContext()).deleteRecording(
                new Recording(recording.getFilePath())
        );

        //broadcast for updating videos list in UI
        LocalBroadcastManager.getInstance(DashAppV.getAppContext()).sendBroadcast(
                new Intent(ACTION_UPDATE_RECORDINGS_LIST)
        );
    }

    /**
     * Insert new recording to SQLite
     * <p>
     * NOTE: called from background thread (BackgroundVideoRecorder)
     *
     * @param recording Recording
     */
    public static void insertNewRecording(Recording recording) {
        if (recording == null) return;
        DBHelper.getInstance(DashAppV.getAppContext()).insertNewRecording(recording);

        //broadcast for updating videos list in UI
        LocalBroadcastManager.getInstance(DashAppV.getAppContext()).sendBroadcast(
                new Intent(ACTION_UPDATE_RECORDINGS_LIST)
        );
    }


    /**
     * Create notification for status bar
     *
     * @param context Context
     * @return Notification
     */
    public static Notification createStatusBarNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                context,
                NOTIFICATIONS_CHANNEL_ID_MAIN_NOTIFICATIONS)
                .setContentTitle(context.getResources().getString(R.string.notification_title))
                .setContentText(context.getResources().getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_videocam_red_128dp)
                .setAutoCancel(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATIONS_CHANNEL_ID_MAIN_NOTIFICATIONS,
                    NOTIFICATIONS_CHANNEL_NAME_MAIN_NOTIFICATIONS,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.enableVibration(false);
            channel.setVibrationPattern(null);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }

        return notificationBuilder.build();
    }

    /**
     * Get path to app-private folder (Android/data/[app name]/files)
     *
     * @param context Context
     * @return Folder
     */
    private static File getAppPrivateVideosFolder(Context context) {
        try {
            File[] extAppFolders = ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_MOVIES + ".provider");

            for (File file : extAppFolders) {
                if (file != null) {
                    //find external app-private folder (emulated - it's internal storage)
                    if (!file.getAbsolutePath().toLowerCase().contains("emulated") && isStorageMounted(file)) {
                        return file;
                    }
                }
            }

            //if external storage is not found
            if (extAppFolders.length > 0) {
                File appFolder;
                //get available app-private folder form the list
                for (int i = extAppFolders.length - 1, j = 0; i >= j; i--) {
                    appFolder = extAppFolders[i];
                    if (appFolder != null && isStorageMounted(appFolder)) {
                        return appFolder;
                    }
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.e(Util.class.getSimpleName(), "getAppPrivateVideosFolder: Exception - " + e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Check if storage mounted and has read/write access.
     *
     * @param storagePath Storage path
     * @return True - can write data
     */
    private static boolean isStorageMounted(File storagePath) {
        String storageState = EnvironmentCompat.getStorageState(storagePath);
        return storageState.equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Remove non-empty directory
     *
     * @param path Directory path
     * @return True - Removed
     */
    private static boolean removeNonEmptyDirectory(File path) {
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (file.isDirectory()) {
                    removeNonEmptyDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return path.delete();
    }


    /**
     * AsyncTask for delete recordings from storage and SQLite
     */
    private static class DeleteRecordingsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            DBHelper dbHelper = DBHelper.getInstance(DashAppV.getAppContext());

            //select all saved recordings for removing files from storage
            ArrayList<Recording> recordingsList = dbHelper.selectAllRecordingsList();

            //remove items from SQLite database
            boolean result = dbHelper.deleteAllRecordings();

            if (result) {
                File videoFile;
                //remove files from storage
                for (Recording recording : recordingsList) {
                    videoFile = !TextUtils.isEmpty(recording.getFilePath()) ? new File(recording.getFilePath()) : null;
                    if (videoFile != null) {
                        videoFile.delete();
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Context context = DashAppV.getAppContext();
            Resources res = context.getResources();
            Util.showToastLong(
                    context,
                    aBoolean
                            ? res.getString(R.string.pref_delete_recordings_confirmation)
                            : res.getString(R.string.recordings_list_empty_message_title)
            );
        }
    }

    /**
     * AsyncTask for star/unstar
     */
    private static class UpdateStarTask extends AsyncTask<Void, Void, Void> {
        private Recording mRecording;

        UpdateStarTask(Recording recording) {
            mRecording = recording;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            DBHelper dbHelper = DBHelper.getInstance(DashAppV.getAppContext());
            //insert or delete star
            dbHelper.updateStar(mRecording);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //broadcast for updating videos list in UI
            LocalBroadcastManager.getInstance(DashAppV.getAppContext()).sendBroadcast(
                    new Intent(Util.ACTION_UPDATE_RECORDINGS_LIST)
            );
        }
    }


}
