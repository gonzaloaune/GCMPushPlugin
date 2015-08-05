#import "GCMPushPlugin.h"
#import <Cordova/CDV.h>
#import <Google/CloudMessaging.h>

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
//        [[NSUserDefaults standardUserDefaults] setObject:@"alert" forKey:@"jsCallback"];
//        [[NSUserDefaults standardUserDefaults] synchronize];
        
        [[NSNotificationCenter defaultCenter] postNotificationName:@"GCMPushPluginRemoteNotification" object:self.pushNotification];
    }
}

- (void) didRegisterForRemoteNotifications:(NSNotification*)notification {
    NSData* token = [notification object];
    NSLog(@"Token: %@", token);
    
    if (self.usesGCM) {
        NSError* configureError;
        [[GGLContext sharedInstance] configureWithError:&configureError];
        NSAssert(!configureError, @"Error configuring Google services: %@", configureError);
        
        [[GCMService sharedInstance] startWithConfig:[GCMConfig defaultConfig]];
        
        __weak __block GCMPushPlugin *weakSelf = self;
        _registrationHandler = ^(NSString *registrationToken, NSError *error){
            if (registrationToken != nil) {
                NSLog(@"Registration Token: %@", registrationToken);
                NSDictionary *tokenResponse = @{@"gcm": registrationToken};
                
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:tokenResponse];
                [weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:weakSelf.registerCallbackId];
            } else {
                NSLog(@"Registration to GCM failed with error: %@", error.localizedDescription);
                
                NSString *errorMessage = [NSString stringWithFormat:@"Error while registering for remote notifications: %@", error.localizedDescription];
                
                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];
                [weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:weakSelf.registerCallbackId];
            }
        };
        
        [[GGLInstanceID sharedInstance] startWithConfig:[GGLInstanceIDConfig defaultConfig]];
        
        self.registrationOptions = @{kGGLInstanceIDRegisterAPNSOption:token,
                                     kGGLInstanceIDAPNSServerTypeSandboxOption:@(self.gcmSandbox)};
        
        [[GGLInstanceID sharedInstance] tokenWithAuthorizedEntity:[[[GGLContext sharedInstance] configuration] gcmSenderID]
                                                            scope:kGGLInstanceIDScopeGCM
                                                          options:self.registrationOptions
                                                          handler:_registrationHandler];
    } else {
        NSLog(@"Registering with native iOS hooks");
        NSDictionary *tokenResponse = @{@"ios": token};
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:tokenResponse];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.registerCallbackId];
    }
}

- (void) didFailToRegisterForRemoteNotifications:(NSNotification*)notification {
    NSError *error = [notification object];
    
    NSString *errorMessage = [NSString stringWithFormat:@"Error while registering for remote notifications: %@", error.localizedDescription];
    
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
        
        if (self.usesGCM) [[GCMService sharedInstance] appDidReceiveMessage:pushNotification];
        
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
    self.usesGCM = [options objectForKey:@"usesGCM"];
    self.gcmSandbox = [options objectForKey:@"sandbox"];
    
    [[NSUserDefaults standardUserDefaults] setBool:self.usesGCM forKey:@"usesGCM"];
    [[NSUserDefaults standardUserDefaults] setObject:jsCallback forKey:@"jsCallback"];
    [[NSUserDefaults standardUserDefaults] synchronize];

    BOOL wantsBadge = [options objectForKey:@"badge"] != nil && [[options objectForKey:@"badge"] isEqualToString:@"true"];
    BOOL wantsSound = [options objectForKey:@"sound"] != nil && [[options objectForKey:@"sound"] isEqualToString:@"true"];
    BOOL wantsAlert = [options objectForKey:@"alert"] != nil && [[options objectForKey:@"alert"] isEqualToString:@"true"];
    
    [self.commandDelegate runInBackground:^{
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

- (void)onTokenRefresh {
    // A rotation of the registration tokens is happening, so the app needs to request a new token.
    NSLog(@"The GCM registration token needs to be changed.");
    [[GGLInstanceID sharedInstance] tokenWithAuthorizedEntity:[[[GGLContext sharedInstance] configuration] gcmSenderID]
                                                        scope:kGGLInstanceIDScopeGCM
                                                      options:self.registrationOptions
                                                      handler:_registrationHandler];
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
