package com.gonzaloaune.cordova.gcm;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class GPPActivity extends Activity {
    private static String TAG = "GPPActivity";

    /*
     * this activity will be started if the user touches a notification that we own. 
     * We send it's data off to the push plugin for processing.
     * If needed, we boot up the main activity to kickstart the application. 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getBundleExtra(getApplicationContext().getPackageName() + ".extras");

        if (extras != null) {

            //What is this good for?
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            sharedPreferences.edit().putString(GCMPushPlugin.LAST_PUSH_KEY, extras.toString()).commit();

            Intent intent = new Intent(GCMPushPlugin.NOTIFICATION_CLICKED_BROADCAST_KEY);
            intent.putExtra(getApplicationContext().getPackageName() + ".data", extras);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }else{
            Log.d(TAG, "Booting GPPActivity received no extras");
        }

        finish();

        if(!GCMPushPlugin.isActive()){
            Log.d(TAG, "Booting GPPActivity!");
            forceMainActivityReload();
        }
    }

    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        startActivity(launchIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}
