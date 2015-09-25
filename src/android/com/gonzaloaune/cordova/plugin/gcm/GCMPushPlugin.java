package com.gonzaloaune.cordova.gcm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.iid.InstanceID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;

/**
 * Author: Gonzalo Aune gonzaloaune@gmail.com
 */
public class GCMPushPlugin extends CordovaPlugin {

    private final String TAG = "GCMPushPlugin";

    //plugin actions
    private static final String REGISTER_GCM = "register";
    private static final String UNREGISTER_GCM = "unregister";
    private static final String SUBSCRIBE_TOPIC = "subscribeTopics";
    private static final String UNSUBSCRIBE_TOPIC = "unsubscribeTopics";
    private static final String GET_CACHED_DATA = "getCachedData";

    //options
    public static final String SENDER_ID_KEY = "SENDER_ID";
    public static final String ICON_KEY = "ICON";
    public static final String ICONCOLOR_KEY = "ICONCOLOR";
    public static final String VIBRATE_KEY = "VIBRATE";
    public static final String SOUND_KEY = "SOUND";
    public static final String CLEARNOTIFICATIONS_KEY = "CLEARNOTIFICATIONS";
    public static final String NOTIFICATION_IN_FOREGROUND_KEY = "NOTIFICATION_IN_FOREGROUND";
    public static final String DATA_IN_BACKGROUND_KEY = "DATA_IN_BACKGROUND";


    public static final String LAST_PUSH_KEY = "LAST_PUSH";

    public static final String SENT_TOKEN_KEY = "SENT_TOKEN_TO_SERVER";
    public static final String REFRESH_TOKEN_KEY = "REFRESH_TOKEN";
    public static final String GCM_TOKEN_KEY = "GCM_TOKEN";

    //BroadcastReceiver Intent filters
    public static final String REG_COMPLETE_BROADCAST_KEY = "REGISTRATION_COMPLETE";
    public static final String MSG_RECEIVED_BROADCAST_KEY = "MESSAGE_RECEIVED";
    public static final String NOTIFICATION_CLICKED_BROADCAST_KEY = "NOTIFICATION_CLICKED";

    private static CallbackContext callback = null;
    private static Context context = null;
    private static String packageName = null;

    private JSONObject options;
    private static CordovaWebView cordovaWebView;
    private static boolean isAppInForeground = false;
    private static ArrayDeque<Bundle> messageCache = new ArrayDeque<Bundle>();
    private String registrationId = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.d(TAG, "plugin initialized");
        super.initialize(cordova, webView);

        context = cordova.getActivity().getApplicationContext();
        packageName = context.getPackageName();
        cordovaWebView = this.webView;
        isAppInForeground = true;

        if (mRegistrationBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(REG_COMPLETE_BROADCAST_KEY));
        }
        if (mMessageReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mMessageReceiver,
                    new IntentFilter(MSG_RECEIVED_BROADCAST_KEY));
        }
        if (mNotificationReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mNotificationReceiver,
                    new IntentFilter(NOTIFICATION_CLICKED_BROADCAST_KEY));
        }
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        try {
            callback = callbackContext;
            options = args.optJSONObject(0);

            if (REGISTER_GCM.equals(action)) {

                if (options.optString("senderId", null) == null) {
                    callbackContext.error("You need to provide a Sender ID, please check: https://developers.google.com/cloud-messaging/android/client?configured=true for more information.");
                    return false;
                }

                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
                    final SharedPreferences.Editor editor = sharedPreferences.edit();

                    editor.putString(ICON_KEY, options.optString("icon", null))
                        .putString(ICONCOLOR_KEY, options.optString("iconColor", null))
                        .putBoolean(SOUND_KEY, options.optBoolean("sound", true))
                        .putBoolean(VIBRATE_KEY, options.optBoolean("vibrate", true))
                        .putBoolean(CLEARNOTIFICATIONS_KEY, options.optBoolean("clearNotifications", true))
                        .putString(SENDER_ID_KEY, options.optString("senderId", null))
                        .putBoolean(NOTIFICATION_IN_FOREGROUND_KEY, options.optBoolean("notificationInForeground", false))
                        .putBoolean(DATA_IN_BACKGROUND_KEY, options.optBoolean("dataInBackground", false))
                        .apply();

                    if (checkPlayServices()) {
                        // Start IntentService to register this application with GCM.
                        Intent intent = new Intent(cordova.getActivity(), GPPRegistrationIntentService.class);
                        intent.putExtra(SENDER_ID_KEY, options.optString("senderId", null));
                        cordova.getActivity().startService(intent);
                    }
                  }
                });
                return true;
            } else if (UNREGISTER_GCM.equals(action)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        deleteSharedPreferences();
                        unregisterGCM();
                    }
                });
                return true;
            } else if (SUBSCRIBE_TOPIC.equals(action)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        subcribeTopics(options.optJSONArray("topics"));
                    }
                });
                return true;
            }  else if (UNSUBSCRIBE_TOPIC.equals(action)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        unsubcribeTopics(options.optJSONArray("topics"));
                    }
                });
                return true;
            }  else if (GET_CACHED_DATA.equals(action)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        getChachedData();
                    }
                });
                return true;
            }  else {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.INVALID_ACTION);
                pluginResult.setKeepCallback(true);
                callback.sendPluginResult(pluginResult);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(e.getMessage());
            return false;
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        Log.d(TAG, "Plugin goes to background");
        super.onPause(multitasking);
        isAppInForeground = false;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
        if (sharedPreferences.getBoolean("clearNotifications", true)) {
            final NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "Plugin started");
        super.onStart();
    };

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "Plugin comes to foreground");
        super.onResume(multitasking);
        isAppInForeground = true;
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @throws IOException if unable to reach the GCM PubSub service
     */
    private void subcribeTopics(JSONArray topics) {
        try {
            if(registrationId == null){
                sendError("you need to call 'init()' before you can subscribe to topics");
            }else {
                GcmPubSub pubSub = GcmPubSub.getInstance(context);
                Boolean error = false;
                for (int i = 0; i < topics.length(); i++) {
                    try {
                        String topic = topics.getString(i);
                        pubSub.subscribe(registrationId, "/topics/" + topic, null);
                        Log.i(TAG, "successfully subscribed to topic " + topic);
                    } catch (Exception e) {
                        sendError("error subscribing to topic: " + e.getLocalizedMessage());
                    }
                }

                if (!error) {
                    sendNoResult();
                }
            }
        } catch (IllegalArgumentException e) {
            sendError("error subscribing to topic: " + e.getLocalizedMessage());
        }
    }

    private void unsubcribeTopics(JSONArray topics) {
         try {
             if(registrationId == null){
                sendError("you need to call 'init()' before you can unsubsribe from topics");
             }else {
                 GcmPubSub pubSub = GcmPubSub.getInstance(context);
                 Boolean error = false;
                 for (int i = 0; i < topics.length(); i++) {
                     try {
                         String topic = topics.getString(i);
                         pubSub.unsubscribe(registrationId, "/topics/" + topic);
                         Log.i(TAG, "successfully unsubscribed from topic " + topic);
                     } catch (Exception e) {
                         sendError("error unsubscribing from topic: " + e.getLocalizedMessage());
                         error = true;
                         break;
                     }
                 }

                 if (!error) {
                     sendNoResult();
                 }
             }
        } catch (IllegalArgumentException e) {
            sendError("error unsubscribing from topic: " + e.getLocalizedMessage());
        }
    }

    private void unregisterGCM() {
        InstanceID instanceID = InstanceID.getInstance(cordova.getActivity());
        try {
            instanceID.deleteInstanceID();
            Log.i(TAG, "Successfully unregistered from GCM");
            sendNoResult();
        } catch (IOException e) {
            sendError("Unable to unregister from GCM: " + e.getLocalizedMessage());
        }
    }

    private void getChachedData() {
        try {
            if(!messageCache.isEmpty() && isActive()){
                Log.i(TAG, "There are " + messageCache.size() + " cached messages to deliver");
                JSONArray jsonArray = new JSONArray();

                while (!messageCache.isEmpty()) {
                    Bundle bundleExtras = messageCache.poll();
                    //Bundle bundleExtras = iterator.next();
                    try {
                        JSONObject jsonObject = bundleToJSON(bundleExtras);
                        jsonArray.put(jsonObject);
                    } catch (Exception e) {
                        sendError("Error while sending cached data to application " + e.getLocalizedMessage());
                    }
                }

                try {
                    JSONObject result = new JSONObject();
                    result.put("messages", jsonArray);
                    result.put("eventName", "dataReceived");

                    Log.d(TAG, "Sending cached messages " + result.toString() + "to application");
                    sendEvent(result);
                } catch (JSONException e) {
                    sendError("Error while sending cached data to application " + e.getLocalizedMessage());
                }
            }else{
                Log.i(TAG, "The data cache is empty");
            };
        } catch (Exception e) {
            sendError("Could not send the cached Data: " + e.getLocalizedMessage());
        }
    }


    private void deleteSharedPreferences() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove(SENDER_ID_KEY)
            .remove(LAST_PUSH_KEY)
            .remove(SENT_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(GCM_TOKEN_KEY).apply();
    }

    @Override
    public Object onMessage(String id, Object data) {
        if (id.equals("onPageFinished")) {
            //This here is to catch throw the notification once the app has been down.
            //TODO: Maybe there is a better place to do this ? or another way to do this ?
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

            String lastPush = sharedPreferences.getString(LAST_PUSH_KEY, null);
            if (lastPush != null) {
                //sendPushToJavascript(lastPush);
            }
        }
        return super.onMessage(id, data);
    }

    private void unregisterBroadcastReceivers() {
        if (mRegistrationBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).unregisterReceiver(mRegistrationBroadcastReceiver);
        }
        if (mMessageReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).unregisterReceiver(mMessageReceiver);
        }
        if (mNotificationReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).unregisterReceiver(mNotificationReceiver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceivers();
        isAppInForeground = false;
        cordovaWebView = null;
    }

    /**
     * This BroadcastReceiver receives intends that are send by
     * GPPListenerService (after a gcm message was received)
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getBundleExtra(context.getPackageName() + ".data");

            if (data != null) {
                //We remove the last saved push since we're sending one.
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
                sharedPreferences.edit().remove(LAST_PUSH_KEY).apply();

                try {
                    JSONObject jsonObject = bundleToJSON(data);

                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(jsonObject);

                    JSONObject result = new JSONObject();
                    result.put("messages", jsonArray);
                    result.put("eventName", "dataReceived");

                    Log.d(TAG, "Sending messages " + result.toString() + "to application");
                    sendEvent(result);
                } catch (JSONException e) {
                    sendError("Error while sending data to application " + e.getLocalizedMessage());
                }
            }
        }
    };

    /**
     * This BroadcastReceiver receives intends that are send by
     * GPPActivity (after a notification was clicked on)
     */
    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getBundleExtra(context.getPackageName() + ".data");

            if (data != null) {
                //We remove the last saved push since we're sending one.
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
                sharedPreferences.edit().remove(LAST_PUSH_KEY).apply();

                try {
                    JSONObject jsonObject = bundleToJSON(data);
                    jsonObject.put("eventName", "notificationClicked");

                    Log.d(TAG, "notification was clicked. Sending " + jsonObject.toString() + "to application");
                    sendEvent(jsonObject);
                } catch (JSONException e) {
                     sendError("Error while sending notification data to application" + e.getLocalizedMessage());
                }
            }
        }
    };

    /**
     * This BroadcastReceiver receives intends that are send by
     * - GPPRegistrationIntentService (after registration completed)
     */
    private BroadcastReceiver mRegistrationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean sentToken = sharedPreferences.getBoolean(SENT_TOKEN_KEY, false);
            boolean shouldRefreshToken = sharedPreferences.getBoolean(REFRESH_TOKEN_KEY, false);
            registrationId = sharedPreferences.getString(GCM_TOKEN_KEY, null);
            if (sentToken || shouldRefreshToken) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("registrationId", registrationId);
                    jsonObject.put("eventName", "registrationCompleted");

                    Log.d(TAG, "sending registration result " + jsonObject.toString() + " to application");
                    sendEvent(jsonObject);
                } catch (JSONException e) {
                    sendError("Error while sending registration result to application" + e.getLocalizedMessage());
                }
            } else {
                sendError("Error while getting token from gcm");
            }
        }
    };

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(cordova.getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GooglePlayServicesUtil.getErrorDialog(resultCode, cordova.getActivity(), 9000).show();
                    }
                });
            } else {
                String message = "This device is not supported.";
                Log.i(TAG, message);
                sendError(message);
                cordova.getActivity().finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Use this method to inform the UI via triggering an event
     * @param jsonObject
     */
    private void sendEvent(JSONObject jsonObject) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
    };

    /**
     * Use this method to return without triggering any event
     */
    private void sendNoResult() {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
    };

    /**
     * Use this method to inform the UI by triggering an error event
     * @param message
     */
    private void sendError(String message) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
    };

    private JSONObject bundleToJSON(Bundle bundle){
        JSONObject jsonObject = new JSONObject();
        final Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                String value = bundle.getString(key);
                Log.d(TAG, "adding to data: (" + key + "/" + value + ")");
                jsonObject.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    /**
     * Is there a better way to access the app context from other classes?
     * @return the cordova context
     */
    public static Context getContext(){
        return context;
    };

    public static boolean isInForeground(){
        return isAppInForeground;
    }

    public static boolean isActive(){
        return cordovaWebView != null;
    }

    public static void addToMessageCache(Bundle message){
        messageCache.add(message);
    }

    public static String getPackageName(){
        return packageName;
    }

}
