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
};
module.exports = GcmPushPlugin;
