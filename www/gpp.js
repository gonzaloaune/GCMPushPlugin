"use strict";

var exec = cordova.require("cordova/exec");

/**
 * PushNotification constructor.
 *
 * @param {Object} options to initiate Push Notifications.
 * @return {GcmPushPlugin} instance that can be monitored and cancelled.
 */
var GcmPushPlugin = function(options) {
    var self = this;

    this._handlers = {
        'registrationCompleted': [],
        'notificationClicked': [],
        'dataReceived': [],
        'error': []
    };

    /**
     *  This success callback will be used by all plugin methods
     */
    this.successCB = function(pluginResult) {
        if (pluginResult) {
             switch (pluginResult.eventName) {
                case "registrationCompleted":
                    delete pluginResult.eventName;
                    self.emit('registrationCompleted', pluginResult);
                    break;
                case "dataReceived":
                    delete pluginResult.eventName;
                    self.emit('dataReceived', pluginResult);
                    break;
                case "notificationClicked":
                    delete pluginResult.eventName;
                    self.emit('notificationClicked', pluginResult);
                    break;
             };
        };
    };

    /**
     *  This error callback will be used by all plugin methods
     */
    this.errorCB = function(err) {
         var e = (typeof err === 'string') ? new Error(err) : err;
         self.emit('error', e);
    };

    // require options parameter
    if (typeof options === 'undefined') {
        throw new Error("GcmPushPluginError", "The options argument is required.");
    }else{
        // store the options to this object instance
        //valid option are:
        //senderId, mandatory!
        //icon, the path to a local icon for the notification (type string, default empty)
        //iconColor, the background color of the icon (type string, default empty)
        //sound, should we play a sound when a notification is created? (type boolean default true)
        //vibrate, should the phone vibrate when a notification is created? (type boolean, default true)
        //clearNotifications, should all notifications be cleared if app goes to background? (type boolean, default true)
        //notificationInForeground, should a notification be created when the app is in foreground? (type boolean, default false)
        //dataInBackground, should the data be send to the application when the app is in background? (type boolean, default false)
         this.options = options;

        // wait at least one process tick to allow event subscriptions
        setTimeout(function() {
            //be sure to use 'self' here!
            exec(self.successCB, self.errorCB, "GCMPushPlugin", "register", [options]);
        }, 10);
    };
};

/**
 * Unregister from push notifications
 */
GcmPushPlugin.prototype.unregister = function () {
    exec(this.successCB, this.errorCB, "GCMPushPlugin", "unregister", []);
};

/**
 * Subscribe to a topic
 * @param registrationId, The registration id that was optained during registration
 * @param topics, an array of topics that may not be empty
 * @throws an error if not all parameters are provided or the plugin fails during the subscription
 */
GcmPushPlugin.prototype.subscribeTopics = function (registrationId, topics) {
    if(!registrationId || !topics || topics[0] == null){
        throw new Error("GcmPushPluginError", "Please provide the registrationId and at least one topic.")
    }else{
        exec(this.successCB, this.errorCB, "GCMPushPlugin", "subscribeTopics", [{registrationId: registrationId, topics: topics}]);
    };
};

/**
 * Unsubscribe from a topic
 * @param registrationId, The registration id that was optained during registration
 * @param topics, an array of topics that may not be empty
 * @throws an error if not all parameters are provided or the plugin fails during the unsubscription
 */
GcmPushPlugin.prototype.unsubscribeTopics = function (registrationId, topics) {
    if(!registrationId || !topics || topics[0] == null){
        throw new Error("GcmPushPluginError", "Please provide the registrationId and at least one topic.")
    }else{
        exec(this.successCB, this.errorCB, "GCMPushPlugin", "unsubscribeTopics", [{registrationId: registrationId, topics: topics}]);
    };
};

/**
 * Get the chached data (received while app was in background)
 * If there was data cached the dataReceived event will be emitted with an array of the received
 * data items
 */
GcmPushPlugin.prototype.getCachedData = function () {
    exec(this.successCB, this.errorCB, "GCMPushPlugin", "getCachedData", []);
};

/**
 * Listen for an event.
 *
 * The following events are supported:
 *
 *   - registrationCompleted
 *   - notificationClicked
 *   - dataReceived
 *   - error
 *
 * @param {String} eventName to subscribe to.
 * @param {Function} callback triggered on the event.
 */
GcmPushPlugin.prototype.on = function(eventName, callback) {
    if (this._handlers.hasOwnProperty(eventName)) {
        this._handlers[eventName].push(callback);
    }
};

/**
 * Emit an event.
 * This is intended for internal use only.
 * @param {String} eventName is the event to trigger.
 * @param {*} all arguments are passed to the event listeners.
 * @return {Boolean} is true when the event is triggered otherwise false.
 */
GcmPushPlugin.prototype.emit = function(){
    var args = Array.prototype.slice.call(arguments);
    var eventName = args.shift();

    if (!this._handlers.hasOwnProperty(eventName)) {
        return false;
    }

    for (var i = 0, length = this._handlers[eventName].length; i < length; i++) {
        console.log("emitting " + eventName);
        this._handlers[eventName][i].apply(undefined,args);
    }

    return true;
};


module.exports = {
    /**
     * Init and register for Push Notifications.
     *
     * This method will instantiate a new copy of the GcmPushPlugin object
     * and start the registration process.
     *
     * @param {Object} options
     * @return {GcmPushPlugin} instance
     */
    init: function(options) {
        return new GcmPushPlugin(options);
    },


};
