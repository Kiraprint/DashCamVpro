package com.dashcamvpro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dashcamvpro.models.Recording;

import java.util.ArrayList;

/**
 * Created by Max on 8/5/2017.
 */

public final class DBHelper extends SQLiteOpenHelper implements IDBHelper {
    // Make it a singleton
    private static DBHelper sHelper = null;
    private static final String DATABASE_NAME = "DashCamVpro.db";
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 2;

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DBHelper getInstance(Context context) {
        if (sHelper == null) {
            // Use the application context, which will ensure that you
            // don't accidentally leak an Activity's context.
            // See this article for more information: http://bit.ly/6LRzfx
            sHelper = new DBHelper(context.getApplicationContext());
        }
        return sHelper;
    }

    public void onCreate(SQLiteDatabase db) {
        com.dashcamvpro.DBRecordingsContract.onCreate(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Since we have only one version, upgrade policy is to simply to discard the data
        // and start over
        com.dashcamvpro.DBRecordingsContract.onUpgrade(db, oldVersion, newVersion);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    /**
     * Select all recordings for videos list
     *
     * @return List of recordings
     */
    @Override
    public ArrayList<Recording> selectAllRecordingsList() {
        ArrayList<Recording> recordingsList = new ArrayList<>();
        Recording recording;

        Cursor cursor = com.dashcamvpro.DBRecordingsContract.queryAllRecordings(
                getReadableDatabase()
        );

        try {
            if (cursor.moveToFirst()) {
                do {
                    recording = com.dashcamvpro.DBRecordingsContract.getRecordingFromCursor(cursor);
                    recordingsList.add(recording);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(DBHelper.class.getSimpleName(), "selectAllRecordingsList: EXCEPTION - " + e.getLocalizedMessage(), e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return recordingsList;
    }

    /**
     * Insert new recording
     *
     * @param recording Recording
     * @return True - inserted successfully
     */
    @Override
    public boolean insertNewRecording(Recording recording) {
        if (com.dashcamvpro.DBRecordingsContract.isRecordingExists(getReadableDatabase(), recording))
            return false;
        return com.dashcamvpro.DBRecordingsContract.insertRecording(getWritableDatabase(), recording);
    }

    /**
     * Delete single recording
     *
     * @param recording Recording
     * @return True - deleted successfully
     */
    @Override
    public boolean deleteRecording(Recording recording) {
        return com.dashcamvpro.DBRecordingsContract.deleteRecording(getWritableDatabase(), recording);
    }

    /**
     * Delete all recordings
     * <p>
     * Note: Uses for "Delete all recordings" from settings
     *
     * @return True - deleted successfully
     */
    @Override
    public boolean deleteAllRecordings() {
        for (Recording rec : selectAllRecordingsList()) {
            if (!rec.isStarred()) deleteRecording(rec);
        }
        return true;
    }

    /**
     * Check recording starred or not
     *
     * @param recording Recording
     * @return True - starred
     */
    @Override
    public boolean isRecordingStarred(Recording recording) {
        return com.dashcamvpro.DBRecordingsContract.isRecordingStarred(getReadableDatabase(), recording);
    }

    /**
     * Insert or delete star for recording
     *
     * @param recording Recording
     * @return True - star updated successfully
     */
    @Override
    public boolean updateStar(Recording recording) {
        boolean isStarred = recording.isStarred();

        if (!isStarred) {
            //insert start for recording
            return com.dashcamvpro.DBRecordingsContract.insertStar(getWritableDatabase(), recording);
        } else {
            //remove star
            return com.dashcamvpro.DBRecordingsContract.deleteStar(getWritableDatabase(), recording);
        }
    }
}
