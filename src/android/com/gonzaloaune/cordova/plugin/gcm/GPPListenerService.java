package com.gonzaloaune.cordova.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class GPPListenerService extends GcmListenerService {

    private final String TAG = "GPPListenerService";
    private String notificationTitle = "";
    private String notificationBody = "";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        JSONObject jsonObject = new JSONObject();
        final Set<String> keys = data.keySet();
        for (String key : keys) {
            try {
                jsonObject.put(key, data.getString(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "Sending JSON:" + jsonObject.toString());

        sendNotification(jsonObject);
    }
    // [END receive_message]

    /**
     * Create and show a notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(JSONObject message) {
        Intent notificationIntent = new Intent(this, GPPActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("extras",message.optString("extra").toString());
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        if(message.optString("gcm.notification.title").isEmpty())
        {
            notificationTitle=message.optString("title");
        }else{
            notificationTitle=message.optString("gcm.notification.title");
        }
        if(message.optString("gcm.notification.body").isEmpty())
        {
            notificationBody=message.optString("text");
        }else{
            notificationBody=message.optString("gcm.notification.body");
        }
        int resourceSmallIconID = getResources().getIdentifier( "ic_stat_name" , "drawable",getPackageName());
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon((resourceSmallIconID == 0 ? getApplicationInfo().icon : resourceSmallIconID))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), getApplicationInfo().icon))
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(contentIntent);

        if(!message.optString("gcm.notification.color").isEmpty()){
            notificationBuilder.setColor(Integer.parseInt(message.optString("gcm.notification.color")));
        }


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
