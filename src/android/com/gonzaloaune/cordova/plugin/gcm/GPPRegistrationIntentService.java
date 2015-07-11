package com.gonzaloaune.cordova.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class GPPRegistrationIntentService extends IntentService {

    private static final String TAG = "GPPRegIntentService";

    public GPPRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // In the (unlikely) event that multiple refresh operations occur simultaneously,
            // ensure that they are processed sequentially.
            synchronized (TAG) {
                // [START register_for_gcm]
                // Initially this call goes out to the network to retrieve the token, subsequent calls
                // are local.
                // [START get_token]

                final String projectId = intent.getExtras().getString(GCMPushPlugin.SENDER_ID_KEY);
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(projectId,
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                // [END get_token]
//                Log.i(TAG, "GCM Registration Token: " + token);

                // You should store a boolean that indicates whether the generated token has been
                // sent to your server. If the boolean is false, send the token to your server,
                // otherwise your server should have already received the token.
                sharedPreferences.edit().putBoolean(GCMPushPlugin.SENT_TOKEN_KEY, true).apply();
                sharedPreferences.edit().putString(GCMPushPlugin.GCM_TOKEN_KEY, token).apply();
                // [END register_for_gcm]
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(GCMPushPlugin.SENT_TOKEN_KEY, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(GCMPushPlugin.REG_COMPLETE_BROADCAST_KEY);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

}
