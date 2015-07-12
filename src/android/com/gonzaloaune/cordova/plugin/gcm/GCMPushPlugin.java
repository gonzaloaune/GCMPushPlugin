package com.gonzaloaune.cordova.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.iid.InstanceID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Author: Gonzalo Aune gonzaloaune@gmail.com
 */
public class GCMPushPlugin extends CordovaPlugin {

    private final String TAG = "GCMPushPlugin";

    private static final String REGISTER_GCM = "register";
    private static final String UNREGISTER_GCM = "unregister";

    public static final String JS_CALLBACK_KEY = "JS_CALLBACK";
    public static final String SENDER_ID_KEY = "SENDER_ID";
    public static final String LAST_PUSH_KEY = "LAST_PUSH";

    public static final String SENT_TOKEN_KEY = "SENT_TOKEN_TO_SERVER";
    public static final String REFRESH_TOKEN_KEY = "REFRESH_TOKEN";
    public static final String GCM_TOKEN_KEY = "GCM_TOKEN";

    public static final String REG_COMPLETE_BROADCAST_KEY = "REGISTRATION_COMPLETE";
    public static final String MSG_RECEIVED_BROADCAST_KEY = "MESSAGE_RECEIVED";

    private CallbackContext callback = null;

    private String senderId;
    private String jsCallback;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this.cordova.getActivity());
        jsCallback = sharedPreferences.getString(JS_CALLBACK_KEY, null);

        if (mRegistrationBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(REG_COMPLETE_BROADCAST_KEY));
        }
        if (mMessageReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mMessageReceiver,
                    new IntentFilter(MSG_RECEIVED_BROADCAST_KEY));
        }
    }

    @Override
    public boolean execute(String action, final JSONArray args,
                           final CallbackContext callbackContext) throws JSONException {
        try {
            callback = callbackContext;
            if (REGISTER_GCM.equals(action)) {
                senderId = args.optJSONObject(0).optString("senderId", null);
                if (senderId == null) {
                    callbackContext.error("You need to provide a Sender ID, please check: https://developers.google.com/cloud-messaging/android/client?configured=true for more information.");
                    return false;
                }
                jsCallback = args.optJSONObject(0).optString("jsCallback", null);
                if (jsCallback == null) {
                    callbackContext.error("Please provide a jsCallback to fully support notifications");
                    return false;
                }
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences sharedPreferences =
                                PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
                        final SharedPreferences.Editor edit = sharedPreferences.edit();
                        edit.putString(SENDER_ID_KEY, senderId)
                            .putString(JS_CALLBACK_KEY, jsCallback).apply();

                        if (checkPlayServices()) {
                            // Start IntentService to register this application with GCM.
                            Intent intent = new Intent(cordova.getActivity(), GPPRegistrationIntentService.class);
                            intent.putExtra(SENDER_ID_KEY, senderId);
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
            } else {
                callbackContext.error("Action not Recognized.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
            return false;
        }
    }

    private void unregisterGCM() {
        InstanceID instanceID = InstanceID.getInstance(cordova.getActivity());
        try {
            instanceID.deleteInstanceID();
            callback.success("Successfully unregistered from GCM");
        } catch (IOException e) {
            e.printStackTrace();
            callback.error("Unable to unregister from GCM: " + e.getLocalizedMessage());
        }
    }

    private void deleteSharedPreferences() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

        final SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove(SENDER_ID_KEY)
            .remove(JS_CALLBACK_KEY)
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
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

            String lastPush = sharedPreferences.getString(LAST_PUSH_KEY, null);
            if (lastPush != null) {
                sendPushToJavascript(lastPush);
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
    }

    @Override
    public void onDestroy() {
        unregisterBroadcastReceivers();
        super.onDestroy();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendPushToJavascript(intent.getStringExtra("data"));
        }
    };

    private void sendPushToJavascript(String data) {
        Log.d(TAG, "sendPushToJavascript: " + data);

        if (data != null) {
            //We remove the last saved push since we're sending one.
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
            sharedPreferences.edit().remove(LAST_PUSH_KEY).apply();

            final String js = "javascript:"+jsCallback+"(" + JSONObject.quote(data).toString() + ")";
            webView.getEngine().loadUrl(js, false);
        }
    }

    private BroadcastReceiver mRegistrationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
            boolean sentToken = sharedPreferences
                    .getBoolean(SENT_TOKEN_KEY, false);
            boolean shouldRefreshToken = sharedPreferences
                    .getBoolean(REFRESH_TOKEN_KEY, false);
            if (sentToken || shouldRefreshToken) {
                try {
                    final JSONObject jsonObject = new JSONObject();
                    jsonObject.put("gcm", sharedPreferences.getString(GCM_TOKEN_KEY, null));
                    callback.success(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.error("Error while sending token");
                }
            } else {
                callback.error("Error while getting token");
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
                Log.i(TAG, "This device is not supported.");
                cordova.getActivity().finish();
            }
            return false;
        }
        return true;
    }

}
