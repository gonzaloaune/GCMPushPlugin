# Push Notifications for Cordova

## Description

Implementation of Push Notifications using the new GCM that Google featured in their 2015 Google I/O conference: https://developers.google.com/cloud-messaging/

### Attention!
This plugin only works with the latest Cordova 5 release, it uses Gradle for Android.

As the time of this writing it is tested to the extend "It works for me" :-)

If you're looking for a node module that implements all the GCM functionality on the server side, I may propose
[node-gcm](https://github.com/ToothlessGear/node-gcm)!

### Contents
- [Android Installation](#android-install)
- [Android Usage](#usage)
- [Notes on iOS](#ios-install-gcm)
- [Contribute](#contribute)
- [LICENSE](#license)

##<a name="android-install"></a> Android installation

Assuming you have your Cordova application up and running:

1) Go to https://developers.google.com/cloud-messaging/android/start and generate the configuration file.

2) Apperantly Google advices to put your configuration file inside an "android-proj/app" or "android-proj/mobile" folder. 
However, this doesn't seem to work, so I ended up putting this file in *../platforms/android* **AND** *../platforms/android/CordovaLib*

3) Apply the Gradle configuration from this site [https://developers.google.com/cloud-messaging/android/client](https://developers.google.com/cloud-messaging/android/client) The important lines are:
```
//Add the dependency to your project's top-level build.gradle:
classpath 'com.google.gms:google-services:1.3.1'

//Add the plugin to your app-level build.gradle:
apply plugin: 'com.google.gms.google-services'

//Set Up Google Play Services
dependencies {
  compile "com.google.android.gms:play-services:7.8.0"
}
```
**ATTENTION:** Watch out for new versions of *play-services* and *google-services*. The initial build may fail if the given versions are not available for download anymore!
And you may also add
```
repositories {
	jcenter()
}
```
to tell Gradle where to find the downloads!

**ATTENTION:** Cordova will overwrite the build.gradle files everytime you run *cordova build*! The advice is to edit the file *build-extras.gradle* instead. However, I could not figure out how these files get merged during the build. So again I ended up editing the original files for the time being. I appreciate every hint on this.
 
4) Run `cordova plugin add https://github.com/akreienbring/GCMPushPlugin.git` to install the plugin

##<a name="usage"></a> Usage

### Registration

The `init` method will register your Token with GCM. The way to call it is the following:

```js
pushPlugin = window.GcmPushPlugin.init({
	"senderId": <YOUR TOKEN>
});

```
#### Options (Android only)
The following options may be provided besides the mandatory senderId:

| Option        			| Description           								| Type  	| Default  	|
| --------------------------|:-----------------------------------------------------:| --------:	| ---------:|
| icon	      				| the path to a local icon for the notification 		| String	| ""	 	|
| iconColor    				| the background color of the icon 						| String	| ""	 	|
| sound	    				| play a sound when a notification is created?			| boolean	| true	 	|
| vibrate    				| vibrate when a notification is created?				| boolean	| true	 	|
| clearNotifications		| clear notifications when app goes to background?		| boolean	| true	 	|
| notificationInForeground	| create a notification when the app is in foreground?	| boolean	| false	 	|
| dataInBackground			| emit 'dataReceived' when the app is in background?	| boolean	| false	 	|

### Events (Android only)
You may listen to the follwing events once you called the `init` method.
```
pushPlugin.on('registrationCompleted', function(result) {
  var registrationId = result.registrationId;
}

pushPlugin.on('error', function(err) {
	console.log(err);
});

pushPlugin.on('dataReceived', function(data) {
	//data is an object that has a 'messages' property
	//there may be multiple messages when they were received while the app was in background
	var messages = data.messages;
});

pushPlugin.on('notificationClicked', function(data) {
	//data is an object with the properties of the data section of the received gcm message
	console.log(JSON.stringify(data));
});
```
### Methods (Android only)
#### Unregister
The `unregister` method will unregister your Token / RegistrationId from GCM. There's no event fired when you unregister, but an error is emitted if something fails.
```
pushPlugin.unregister();
```
#### Subscribe to a topic
You may also subscribe to one ore more topics.
```
pushPlugin.subscribeTopics(registrationId, ["topic1", "topic2"]);
```

#### Get cached data
When the app is in background mode and the `dataInBackground` is not set to true (which is the default), the plugin caches all incoming data.
To get the cached data from the plugin you may use
```
pushPlugin.getCachedData();
```
when the app resumes. The date will then be emitted with the `dataReceived` event.

##<a name="ios-install-gcm"></a> Notes on iOS
This is a fork of [https://github.com/gonzaloaune/GCMPushPlugin](https://github.com/gonzaloaune/GCMPushPlugin)
BTW: Thanks for the work to Gonzalo Aune! 
His plugin has iOS support, but I think I broke the compatibility with my changes.
However: I submitted a Pullrequest to Gonzalo and hopefully he will integrate my changes and restore the iOs functionality!

##<a name="contribute"></a> Contribute
Feel free to contribute. But.. as I said, this is a fork. So once merged by Gonzalo all contributions should go to the original. 

##<a name="license"></a> License
```
The MIT License

Copyright (c) 2015 André Kreienbring.

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
