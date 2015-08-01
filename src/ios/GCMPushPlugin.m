#import "GCMPushPlugin.h"
#import <Cordova/CDV.h>

@implementation GCMPushPlugin

- (CDVPlugin *)initWithWebView:(UIWebView *)theWebView {
    NSLog(@"initializing GCMPushPlugin");
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(didRegisterForRemoteNotifications:)
                                                 name:CDVRemoteNotification
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(didFailToRegisterForRemoteNotifications:)
                                                 name:CDVRemoteNotificationError
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(didReceiveRemoteNotification:)
                                                 name:@"GCMPushPluginRemoteNotification"
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(applicationActive:)
                                                 name:UIApplicationDidBecomeActiveNotification
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(pageDidLoad:)
                                                 name:CDVPageDidLoadNotification
                                               object:self.webView];

    return self;
}

- (void) applicationActive:(NSNotification*)notification {
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"pushNotification"];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

- (void)pageDidLoad:(NSNotification*)notification {
    self.pushNotification = [[NSUserDefaults standardUserDefaults] objectForKey:@"pushNotification"];
    
    if (self.pushNotification) {
        //Debug callback
        [[NSUserDefaults standardUserDefaults] setObject:@"alert" forKey:@"jsCallback"];
        [[NSUserDefaults standardUserDefaults] synchronize];
        
        [[NSNotificationCenter defaultCenter] postNotificationName:@"GCMPushPluginRemoteNotification" object:self.pushNotification];
    }
}

- (void) didRegisterForRemoteNotifications:(NSNotification*)notification {
    NSString* token = [notification object];
    
//    NSLog(@"Token: %@", token);
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{@"gcm": token}];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.registerCallbackId];
}

- (void) didFailToRegisterForRemoteNotifications:(NSNotification*)notification {
    NSError *error = [notification object];
    
    NSString *errorMessage = [NSString stringWithFormat:@"Error while registering for remote notifications: %@", error.description];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.registerCallbackId];
}

- (void) didReceiveRemoteNotification:(NSNotification*)notification {
    NSDictionary *pushNotification = [notification object];
    
    self.jsCallback = [[NSUserDefaults standardUserDefaults] stringForKey:@"jsCallback"];
    if (pushNotification && self.jsCallback) {
//        NSLog(@"Received notification: %@", pushNotification);
        
        NSMutableString *jsonStr = [NSMutableString stringWithString:@"{"];
        
        [self parseDictionary:pushNotification intoJSON:jsonStr];
        
        [jsonStr appendString:@"}"];
        
//        NSLog(@"Msg: %@", jsonStr);
        
        NSString *js = [NSString stringWithFormat:@"%@(%@);", self.jsCallback, jsonStr];
        [self.commandDelegate evalJs:js];
    }
}

- (void)register:(CDVInvokedUrlCommand*)command {
    self.registerCallbackId = command.callbackId;
    
    NSDictionary *options = [command.arguments objectAtIndex:0];

    NSString *jsCallback = [options objectForKey:@"jsCallback"];
    if (jsCallback == nil) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Please provide a jsCallback to fully support notifications"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.registerCallbackId];
        return;
    }
    self.jsCallback = jsCallback;
    
    NSString *senderId = [options objectForKey:@"senderId"];
    if (senderId != nil) {
        self.senderId = senderId;
        self.usesGCM = YES;
    }

    [[NSUserDefaults standardUserDefaults] setObject:senderId forKey:@"senderId"];
    [[NSUserDefaults standardUserDefaults] setBool:self.usesGCM forKey:@"usesGCM"];
    [[NSUserDefaults standardUserDefaults] setObject:jsCallback forKey:@"jsCallback"];
    [[NSUserDefaults standardUserDefaults] synchronize];

    BOOL wantsBadge = [options objectForKey:@"badge"] != nil && [[options objectForKey:@"badge"] isEqualToString:@"true"];
    BOOL wantsSound = [options objectForKey:@"sound"] != nil && [[options objectForKey:@"sound"] isEqualToString:@"true"];
    BOOL wantsAlert = [options objectForKey:@"alert"] != nil && [[options objectForKey:@"alert"] isEqualToString:@"true"];
    
    [self.commandDelegate runInBackground:^{
        if (!self.usesGCM) {
            NSLog(@"Registering with native iOS hooks");
            
            //-- Set Notification
            if ([[UIApplication sharedApplication] respondsToSelector:@selector(isRegisteredForRemoteNotifications)]) {
                // iOS 8 Notifications
                UIUserNotificationType UserNotificationTypes = UIUserNotificationTypeNone;
                if (wantsBadge) UserNotificationTypes |= UIUserNotificationTypeBadge;
                if (wantsSound) UserNotificationTypes |= UIUserNotificationTypeSound;
                if (wantsAlert) UserNotificationTypes |= UIUserNotificationTypeAlert;
                
                [[UIApplication sharedApplication] registerUserNotificationSettings:[UIUserNotificationSettings settingsForTypes:UserNotificationTypes categories:nil]];
                [[UIApplication sharedApplication] registerForRemoteNotifications];
            } else {
                // iOS < 8 Notifications
                UIRemoteNotificationType notificationTypes = UIRemoteNotificationTypeNone;
                if (wantsBadge) notificationTypes |= UIRemoteNotificationTypeBadge;
                if (wantsSound) notificationTypes |= UIRemoteNotificationTypeSound;
                if (wantsAlert) notificationTypes |= UIRemoteNotificationTypeAlert;
                
                [[UIApplication sharedApplication] registerForRemoteNotificationTypes:
                 (UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeAlert | UIRemoteNotificationTypeSound)];
            }
        } else {
            NSLog(@"Registering with GCM implementation");
            
            
        }
    }];
}

- (void)unregister:(CDVInvokedUrlCommand*)command {
    if ([[UIApplication sharedApplication] isRegisteredForRemoteNotifications]) {
        [[UIApplication sharedApplication] unregisterForRemoteNotifications];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Successfully unregistered from iOS notifications"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.unregisterCallbackId];
        return;
    }
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"The device is not registered for remote notifications"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.unregisterCallbackId];
}

- (void)setApplicationIconBadgeNumber:(CDVInvokedUrlCommand*)command {
    NSMutableDictionary* options = [command.arguments objectAtIndex:0];
    int badge = [[options objectForKey:@"badge"] intValue] ?: 0;
    
    [[UIApplication sharedApplication] setApplicationIconBadgeNumber:badge];
}

-(void)parseDictionary:(NSDictionary *)inDictionary intoJSON:(NSMutableString *)jsonString {
    NSArray *keys = [inDictionary allKeys];
    NSString *key;
    
    for (key in keys) {
        id thisObject = [inDictionary objectForKey:key];
        
        if ([thisObject isKindOfClass:[NSDictionary class]]) {
            [self parseDictionary:thisObject intoJSON:jsonString];
        } else if ([thisObject isKindOfClass:[NSString class]]) {
            [jsonString appendFormat:@"\"%@\":\"%@\",",
             key,
             [[[[inDictionary objectForKey:key]
                stringByReplacingOccurrencesOfString:@"\\" withString:@"\\\\"]
               stringByReplacingOccurrencesOfString:@"\"" withString:@"\\\""]
              stringByReplacingOccurrencesOfString:@"\n" withString:@"\\n"]];
        } else {
            [jsonString appendFormat:@"\"%@\":\"%@\",", key, [inDictionary objectForKey:key]];
        }
    }
}

@end
