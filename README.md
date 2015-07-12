# Push Notifications over GCM (Android &amp; iOS) for Cordova

## Description

Implementation of the new GCM that Google featured in their 2015 Google I/O conference: https://developers.google.com/cloud-messaging/

### Attention!

This plugin only works with the latest Cordova 5 release, it uses Gradle for Android.

### Contents
- [Android Installation](#android-install)
- [Usage](#usage)
- [Upcomings](#upcomings)
- [LICENSE](#license)

##<a name="android-install"></a> Android installation

Assuming you have your Cordova application up and running:

1) Go to https://developers.google.com/cloud-messaging/android/start and generate the configuration file.

2) Put your configuration file inside an "android-proj/app" or "android-proj/mobile" folder as advice by Google.

3) Run `cordova plugin add cordova-plugin-gcmpushplugin` to install the plugin

##<a name="usage"></a> Usage

For now the only available method is the `register` method which will register your Token with GCM. The way to call it is the following:

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

##<a name="upcomings"></a> Upcomings

- **Unregister** method for Android
- **Register** method for iOS
- **Unregister** method for iOS
- **setApplicationBadgeNumber** method for iOS

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
