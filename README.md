# Push Notifications for Cordova

## Description

Implementation of Push Notifications using the new GCM that Google featured in their 2015 Google I/O conference: https://developers.google.com/cloud-messaging/

This plugin was extended to support native iOS push notifications since the only plugin available was outdated / deprecated.

### Attention!

- This plugin only works with the latest Cordova 5 release, it uses Gradle for Android.

### Contents
- [Android Installation](#android-install)
- [Android Usage](#usage)
- [iOS Installation - GCM](#ios-install-gcm)
- [iOS GCM Usage](#ios-usage-gcm)
- [iOS Installation - Native](#ios-install-native)
- [iOS Native Usage](#ios-usage-native)
- [Changelog](#changelog)
- [Upcomings](#upcomings)
- [LICENSE](#license)

##<a name="android-install"></a> Android installation

Assuming you have your Cordova application up and running:

1) Go to https://developers.google.com/cloud-messaging/android/start and generate the configuration file.

2) Put your configuration file inside an "android-proj/app" or "android-proj/mobile" folder as advice by Google.

3) Run `cordova plugin add cordova-plugin-gcmpushplugin` to install the plugin

##<a name="usage"></a> Usage

### Registration

The `register` method will register your Token with GCM. The way to call it is the following:

```js
window.GcmPushPlugin.register(successHandler, errorHandler, {
    "senderId":"415175858509",
    "jsCallback":"onNotification"
});
```

The first parameter is the callback that will be fired once the Token is generated, the token will come in a json where the key is `gcm`.
```js
function successHandler(result) {
  console.log("Token: " + result.gcm);
}
```
The second parameter is the callback that will be fired if there was an error while generating the Token.
```js
function errorHandler(error) {
  console.log("Error: " + error);
}
```
The third parameter is a hash of options, in this case we need to set the senderId which we will be able to get it from the Android installation step and a jsCallback to be called once we get our notification.

If everything goes well and you are able to register, to send a push notification you should do something like this using cURL (change **SERVER_API_KEY** and **DEVICE_TOKEN** where appropiate):

```
curl --header "Authorization: key=SERVER_API_KEY" \
       --header Content-Type:"application/json" \
       https://gcm-http.googleapis.com/gcm/send \
       -d "{ \"data\" : { \"title\" : \"MyCoolApp\", \"text\" : \"MessageText\", \"extra\":{\"url\":\"someurl.js\"}}, \"to\" : \"$DEVICE_TOKEN\" }"
```

Then you will receive in your `onNotification` method, the **extra** parameter you pass to GCM in the cURL command:

```js
function onNotification(notification) {
  console.log("Event Received: " + e); // { "extra": {"url" : "someurl.js" } } 
}
```

### Unregister

The `unregister` method will unregister your Token from GCM. The way to call it is the following:

```js
window.GcmPushPlugin.unregister(unregisterSuccess, unregisterError);
```

The first parameter is the callback that will be fired once the unregistration is successful.
```js
function unregisterSuccess(result) {
  console.log("Unregister success: " + result);
}
```
The second parameter is the callback that will be fired if there was an error while unregistering.
```js
function unregisterError(error) {
  console.log("Error: " + error);
}
```

If everything goes well and you are able to unregister, you won't be able to send a push notification anymore getting a NotRegistered error from GCM:

```
curl --header "Authorization: key=SERVER_API_KEY" \
       --header Content-Type:"application/json" \
       https://gcm-http.googleapis.com/gcm/send \
       -d "{ \"data\" : { \"title\" : \"MyCoolApp\", \"text\" : \"MessageText\", \"extra\":{\"url\":\"someurl.js\"}}, \"to\" : \"$DEVICE_TOKEN\" }"
```
`{"multicast_id":xxxxxxxxxxxxxxx,"success":0,"failure":1,"canonical_ids":0,"results":[{"error":"NotRegistered"}]}%`

##<a name="ios-install-gcm"></a> iOS GCM installation

Assuming you have your Cordova application up and running:

1) Go to https://developers.google.com/cloud-messaging/ios/start and generate the configuration file and upload your APNS certificates.

2) Put your configuration file in the root of your project and make sure to add it to your target as advice by Google.

3) Run `cordova plugin add cordova-plugin-gcmpushplugin` to install the plugin

##<a name="ios-usage-gcm"></a> Usage

### Registration

The `register` method will register your Token with GCM. The way to call it is the following:

```js
window.GcmPushPlugin.register(successHandler, errorHandler, {
    "badge":"true",
    "sound":"true",
    "alert":"true",
    "usesGCM":true,
    "sandbox":true,
    "jsCallback":"onNotification"
});
```

The first parameter is the callback that will be fired once the Token is generated, the token will come in a json where the key is `gcm`.
```js
function successHandler(result) {
  console.log("Token: " + result.gcm);
}
```
The second parameter is the callback that will be fired if there was an error while generating the Token.
```js
function errorHandler(error) {
  console.log("Error: " + error);
}
```
The third parameter is a hash of options, in this case we have the following:
- 3 parameters that Apple asked us to register for push notifications (`badge`, `sound` and `alert`)
- `usesGCM` will tell the plugin if we want to register the device with GCM or using the native iOS method
- `sandbox` will tell the plugin to register using the sandbox or production environment.
- `jsCallback` is the callback to be called once we get our notification.

If everything goes well and you are able to register, to send a push notification you should do something like this using cURL (change **SERVER_API_KEY** and **DEVICE_TOKEN** where appropiate):

```
curl --header "Authorization: key=SERVER_API_KEY" \
       --header Content-Type:"application/json" \
       https://gcm-http.googleapis.com/gcm/send \
       -d "{ \"data\" : { \"title\" : \"MyCoolApp\", \"text\" : \"MessageText\", \"extra\":{\"url\":\"someurl.js\"}}, \"to\" : \"$DEVICE_TOKEN\" }"
```

Then you will receive in your `onNotification` method, the whole json you pass to GCM in the cURL command:

```js
function onNotification(notification) {
  console.log("Event Received: " + e); // { "extra": {"url" : "someurl.js" } } 
}
```

### Unregister

The `unregister` method will unregister your Token from GCM. The way to call it is the following:

```js
window.GcmPushPlugin.unregister(unregisterSuccess, unregisterError);
```

The first parameter is the callback that will be fired once the unregistration is successful.
```js
function unregisterSuccess(result) {
  console.log("Unregister success: " + result);
}
```
The second parameter is the callback that will be fired if there was an error while unregistering.
```js
function unregisterError(error) {
  console.log("Error: " + error);
}
```

If everything goes well and you are able to unregister, you won't be able to send a push notification anymore getting a NotRegistered error from GCM:

```
curl --header "Authorization: key=SERVER_API_KEY" \
       --header Content-Type:"application/json" \
       https://gcm-http.googleapis.com/gcm/send \
       -d "{ \"data\" : { \"title\" : \"MyCoolApp\", \"text\" : \"MessageText\", \"extra\":{\"url\":\"someurl.js\"}}, \"to\" : \"$DEVICE_TOKEN\" }"
```
`{"multicast_id":xxxxxxxxxxxxxxx,"success":0,"failure":1,"canonical_ids":0,"results":[{"error":"NotRegistered"}]}%`

### setApplicationIconBadgeNumber

The `setApplicationIconBadgeNumber` method will set (as the name says) the application badge icon number to whatever you want. The way to call it is the following:

```js
window.GcmPushPlugin.unregister({'badge':12});
```

##<a name="ios-install-native"></a> iOS native installation

Assuming you have your Cordova application up and running:

1) Run `cordova plugin add cordova-plugin-gcmpushplugin` to install the plugin and proceed to [Usage Native iOS](#ios-usage-native)

##<a name="ios-usage-native"></a> Usage

### Registration

The `register` method will attempt to register natively. The way to call it is the following:

```js
window.GcmPushPlugin.register(successHandler, errorHandler, {
    "badge":"true",
    "sound":"true",
    "alert":"true",
    "jsCallback":"onNotification"
});
```

The first parameter is the callback that will be fired once the Token is generated, the token will come in a json where the key is `ios`.
```js
function successHandler(result) {
  console.log("Token: " + result.ios);
}
```
The second parameter is the callback that will be fired if there was an error while generating the Token.
```js
function errorHandler(error) {
  console.log("Error: " + error);
}
```
The third parameter is a hash of options, in this case we have the following:
- 3 parameters that Apple asked us to register for push notifications (`badge`, `sound` and `alert`).
- `jsCallback` the callback that will be fired once we get our notification.

If everything goes well and you are able to register, you should be able to send already push notifications. I personally use a ruby script for that (using houston lib) to test:

```ruby
require 'houston'

# Environment variables are automatically read, or can be overridden by any specified options. You can also
# conveniently use `Houston::Client.development` or `Houston::Client.production`.
APN = Houston::Client.development
APN.certificate = File.read(PATH_TO_PEM)

# An example of the token sent back when a device registers for notifications
token = "TOKEN"

# Create a notification that alerts a message to the user, plays a sound, and sets the badge on the app
notification = Houston::Notification.new(device: token)
notification.alert = "Hello, World!"

# Notifications can also change the badge count, have a custom sound, have a category identifier, indicate available Newsstand content, or pass along arbitrary data.
notification.badge = 57
notification.sound = "default"
notification.content_available = true
notification.custom_data = {foo: "bar"}

# And... sent! That's all it takes.
APN.push(notification)
```

Then you will receive in your `onNotification` method the notification:

```js
function onNotification(notification) {
  console.log("Event Received: " + e); // {"content-available":"1","alert":"Hello, World!","badge":"57","sound":"default","foo":"bar"}
}
```

### Unregister

The `unregister` method will unregister your device. The way to call it is the following:

```js
window.GcmPushPlugin.unregister(unregisterSuccess, unregisterError);
```

The first parameter is the callback that will be fired once the unregistration is successful.
```js
function unregisterSuccess(result) {
  console.log("Unregister success: " + result);
}
```
The second parameter is the callback that will be fired if there was an error while unregistering.
```js
function unregisterError(error) {
  console.log("Error: " + error);
}
```

If everything goes well and you are able to unregister, you won't be able to send a push notification anymore.

### setApplicationIconBadgeNumber

The `setApplicationIconBadgeNumber` method will set (as the name says) the application badge icon number to whatever you want. The way to call it is the following:

```js
window.GcmPushPlugin.unregister({'badge':12});
```


##<a name="changelog"></a> Changelog

- 07/12/2015 Added **Unregister** method for Android
- 08/05/2015 Added **Register** method for iOS GCM
- 08/05/2015 Added **Unregister** method for iOS GCM
- 08/05/2015 Added **Register** method for iOS native
- 08/05/2015 Added **Unregister** method for iOS native
- 08/05/2015 Added **setApplicationBadgeNumber** method for iOS native

##<a name="upcomings"></a> Upcomings

- ~~**Unregister** method for Android~~
- ~~**Register** method for iOS GCM~~
- ~~**Unregister** method for iOS GCM~~
- ~~**Register** method for iOS native~~
- ~~**Unregister** method for iOS native~~
- ~~**setApplicationBadgeNumber** method for iOS native~~

##<a name="contribute"></a> Contribute
Feature requests, bug reports and pull requests are very much welcome. 
Please make sure to use a feature branch in your fork.
Please make sure you also update the README to reflect your changes. 

##<a name="license"></a> License
```
The MIT License

Copyright (c) 2015 Gonzalo Javier Aune.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
