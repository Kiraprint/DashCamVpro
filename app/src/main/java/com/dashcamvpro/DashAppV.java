package com.dashcamvpro;

import android.app.Application;
import android.content.Context;

/**
 * Created by ashish on 8/23/17.
 */

public class DashAppV extends Application {

    private static DashAppV sApp;

    @Override
    public void onCreate() {
        super.onCreate();

        if (sApp == null) {
            sApp = this;
        }
    }

    /**
     * Get app context
     *
     * @return Context
     */
    public static Context getAppContext() {
        return sApp.getApplicationContext();
    }
}
