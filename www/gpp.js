"use strict";

var exec = require("cordova/exec");

var GcmPushPlugin = {

    register: function (successCB, errorCB, options) {
        cordova.exec(
                successCB,
                errorCB,
                "GCMPushPlugin",
                "register",
                [options]
              );
    },
    
    unregister: function (successCB, errorCB) {
        cordova.exec(
                successCB,
                errorCB,
                "GCMPushPlugin",
                "unregister",
                []
              );
    },
    
    setApplicationIconBadgeNumber: function (options) {
        cordova.exec(
                function(resp){},
                function(resp){},
                "GCMPushPlugin",
                "setApplicationIconBadgeNumber",
                [options]
              );
    },
    subscribeToTopic : function(successCB,errorCB, topic){
        cordova.exec(
                function(resp){},
                function(err){},
                "GCMPushPlugin",
                "subscribeToTopic",
                [topic])
    },
    unsubscribeToTopic :function(successCB,errorCB, topic){
        cordova.exec(
                function(resp){},
                function(err){},
                "GCMPushPlugin",
                "unsubscribeToTopic",
                [topic]
        )
    }
};
module.exports = GcmPushPlugin;
