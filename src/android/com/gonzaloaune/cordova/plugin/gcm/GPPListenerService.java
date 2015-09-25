package com.gonzaloaune.cordova.gcm;

import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.graphics.Color;
import android.graphics.Bitmap;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Set;

public class GPPListenerService extends GcmListenerService {

    private final String TAG = "GPPListenerService";
    private final AssetUtil asset = AssetUtil.getInstance(GCMPushPlugin.getContext());

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

        JSONObject jsonNotification = new JSONObject();
        Bundle bundleExtras = new Bundle();

        final Set<String> keys = data.keySet();
        for (String key : keys) {
            try {
                String value = data.getString(key);
                //Log.d(TAG, "key is " + key + " and value is " + value);
                if(key != null) {
                    if (key.startsWith("gcm.notification")) {
                        String keyname = key.substring(key.lastIndexOf(".") + 1, key.length());
                        Log.d(TAG, "adding (" + keyname + "/" + value + ") to notification");
                        jsonNotification.put(keyname, value);
                    } else {
                        if(key.equalsIgnoreCase("collapse_key")) {
                            Log.d(TAG, "not adding collapse_key " + value);
                        }else{
                            Log.d(TAG, "adding (" + key + "/" + value + ") to extras");
                            bundleExtras.putString(key, value);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean notificationInForeground = sharedPreferences.getBoolean(GCMPushPlugin.NOTIFICATION_IN_FOREGROUND_KEY, false);
        boolean dataInBackground = sharedPreferences.getBoolean(GCMPushPlugin.DATA_IN_BACKGROUND_KEY, false);

        if(GCMPushPlugin.isInForeground() || dataInBackground){
            // Notify UI that data was received
            Intent dataReceivedIntend = new Intent(GCMPushPlugin.MSG_RECEIVED_BROADCAST_KEY);
            dataReceivedIntend.putExtra(GCMPushPlugin.getContext().getPackageName() + ".data", bundleExtras);
            LocalBroadcastManager.getInstance(this).sendBroadcast(dataReceivedIntend);
        }else{
            Log.d(TAG, "not sending data because the app is in background, but caching the message");
            GCMPushPlugin.addToMessageCache(bundleExtras);
        }

        if((!GCMPushPlugin.isInForeground() || notificationInForeground) && jsonNotification.length() > 0){
            sendNotification(jsonNotification, bundleExtras);
        }else{
            Log.d(TAG, "not sending a notification because the app is in foreground or there's no notificatin in the sent message");
        }
    }
    // [END receive_message]

    /**
     * Create and show a notification containing the received GCM message.
     *
     * @param jsonNotification GCM notification received as JSONObject.
     * @param bundleExtras The extras (data section) that was extracted from the message.
     */
    private void sendNotification(JSONObject jsonNotification, Bundle bundleExtras) {

        //create an intent that is triggered when the user clicks on the notification
        Intent notificationIntent = new Intent(this, GPPActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra(GCMPushPlugin.getPackageName() + ".extras", bundleExtras);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //create the notification
        Log.d(TAG, "Creating notification:" + jsonNotification.toString());
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(jsonNotification.optString("title"))     //Set the title (first row) of the notification, in a standard notification.
                .setContentText(jsonNotification.optString("body"))       //Set the text (second row) of the notification, in a standard notification.
                //.setExtras(extras)                                        //Not used! Set metadata for this notification.
                //.setGroup()                                               //TODO Set this notification to be part of a group of notifications sharing the same key.
                //.setGroupSummary()                                        //TODO Set this notification to be the group summary for a group of notifications.
                //.setLights()                                              //TODO Set the argb value that you would like the LED on the device to blnk, as well as the rate.
                //.setPriority()                                            //TODO Set the relative priority for this notification.
                //.setTicker()                                              //TODO Set the third line of text in the platform notification template.
                .setAutoCancel(true)                                        //Setting this flag will make it so the notification is automatically canceled when the user clicks it in the panel
                .setContentIntent(contentIntent);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String localIcon = sharedPreferences.getString(GCMPushPlugin.ICON_KEY, null);
        String localIconColor = sharedPreferences.getString(GCMPushPlugin.ICONCOLOR_KEY, null);
        boolean soundOption = sharedPreferences.getBoolean(GCMPushPlugin.SOUND_KEY, true);
        boolean vibrateOption = sharedPreferences.getBoolean(GCMPushPlugin.VIBRATE_KEY, true);
        Log.d(TAG, "stored icon=" + localIcon);
        Log.d(TAG, "stored iconColor=" + localIconColor);
        Log.d(TAG, "stored sound=" + soundOption);
        Log.d(TAG, "stored vibrate=" + vibrateOption);

        try {
            String color = jsonNotification.optString("color");
            int colorValue = getNotificationIconColor(color, localIconColor);
            if(color != null && (colorValue != Integer.MAX_VALUE)){
                notificationBuilder.setColor(colorValue);
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing notification color");
            e.printStackTrace();
        }

        try {
            String sound = jsonNotification.optString("sound");
            if (soundOption) {
                notificationBuilder.setSound(getNotificationSound(sound));
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing notification sound");
            e.printStackTrace();
        }

        try {
            if (vibrateOption) {
                //Set the vibration pattern to use.
                notificationBuilder.setVibrate(new long[] { 1000, 1000});
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing notification vibration");
            e.printStackTrace();
        }

        try {
            String largeIcon = bundleExtras.getString("image", null);
            Bitmap largeIconBitmap = getNotificationLargeIcon(largeIcon);
            if(largeIconBitmap != null){
                //Set the large icon that is shown in the ticker and notification.
                notificationBuilder.setLargeIcon(largeIconBitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing notification image");
            e.printStackTrace();
        }

        try {
            String icon = jsonNotification.optString("icon");
            int smallIcon = getNotificationSmallIcon(icon, localIcon);
            notificationBuilder.setSmallIcon(smallIcon);
        } catch (Exception e) {
            Log.e(TAG, "error parsing notification icon");
            e.printStackTrace();
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    /**
     *  Parse the notification background color either from the color that was sent with the notification
     *  or from the color that was passed from the options
     *  @return The integer value of the color
     */
    private int getNotificationIconColor(String color, String localIconColor) {
        int iconColor = 0;
        if (color != null && !color.isEmpty()) {
            try {
                Log.d(TAG, "going to parse color " + color);
                iconColor = Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "couldn't parse color from android options");
            }
        } else if (localIconColor != null && !localIconColor.isEmpty()) {
            try {
                iconColor = Color.parseColor(localIconColor);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "couldn't parse color from android options");
            }
        }
        if (iconColor != 0) {
            return iconColor;
        }else{
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Get the Uri to a soundfile
     * URIs like file:/// map to absolute paths while file:// point relatively to the www folder
     * within the asset resources. And res:// means a resource from the native
     * res folder. Remote assets are accessible via http:// for example.
     * @param soundPath, the sound from the received notification
     */
    private Uri getNotificationSound(String soundPath) {
        if (soundPath != null&& !soundPath.isEmpty()) {
            Uri soundUri =  asset.parseSound(soundPath);
            Log.d(TAG, "using sound " + soundUri.toString());
            return soundUri;
        } else {
            Log.d(TAG, "using default sound");
            return asset.parseSound("res://platform_default");
        }
    }

    /**
     * Get the large icon (a bitmap) from the given path
     * @param largeIconPath
     * @return The Bitmap that is retrieved from the given path
     */
    private Bitmap getNotificationLargeIcon(String largeIconPath) {
        if (largeIconPath != null && !largeIconPath.isEmpty()) {
            Uri largeIconUri =  asset.parse(largeIconPath);
            Log.d(TAG, "using sound " + largeIconUri.toString());
            try {
                return asset.getIconFromUri(largeIconUri);
            } catch (IOException e) {
                Log.e(TAG, "could not get a bitmap from the path " + largeIconPath);
                return null;
            }
        } else {
            Log.d(TAG, "Not setting large icon");
            return null;
        }
    }

    /**
     * Get the notification icon from the local resources
     * If an icon was sent with the gcm message this one will be used
     * If not then then icon that was passed with the options will be used
     * If neither icon or localIcon were passed then the application icon will be set.
     * @param icon, the icon that was sent with the gcm message
     * @param localIcon, the icon that was passed with the options
     * @return The integer value of the icon
     */
    private int getNotificationSmallIcon(String icon, String localIcon) {
        int iconId = 0;
        if (icon != null && !icon.isEmpty()) {
            iconId = asset.getResIdForDrawable(icon);
            Log.d(TAG, "using icon " + icon);
        }
        else if (localIcon != null && !localIcon.isEmpty()) {
            iconId = asset.getResIdForDrawable(localIcon);
            Log.d(TAG, "using icon from plugin options");
        }
        if (iconId == 0) {
            Log.d(TAG, "no icon resource found - using application icon");
            iconId = getApplicationInfo().icon;
        }
        return iconId;
    }

}
