"use strict";

var exec = require("cordova/exec");

var GcmPushPlugin = {

    register: function (successCB, errorCB, options) {
        alert('register!');
        cordova.exec(
                successCB,
                errorCB,
                "GCMPushPlugin",
                "register",
                [options]
              );
    },
    
    unregister: function (successCB, errorCB) {
        alert('unregister!');
        cordova.exec(
                successCB,
                errorCB,
                "GCMPushPlugin",
                "unregister",
                []
              );
    },
    
    setApplicationIconBadgeNumber: function (options) {
        alert('setApplicationIconBadgeNumber!');
        cordova.exec(
                function(resp){},
                function(resp){},
                "GCMPushPlugin",
                "setApplicationIconBadgeNumber",
                [options]
              );
    },
};
module.exports = GcmPushPlugin;
