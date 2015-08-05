//
//  AppDelegate+Notification.m
//  GCMPushPlugin
//
//  Created by Gonzalo Aune on 7/27/15.
//
//

#import "AppDelegate+notification.h"

@implementation AppDelegate (notification)

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {
    [self delegateNotification:userInfo];
    
    completionHandler(UIBackgroundFetchResultNewData);
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo {
    [self delegateNotification:userInfo];
}

- (void) delegateNotification:(NSDictionary *)userInfo {
    [[NSUserDefaults standardUserDefaults] setObject:userInfo forKey:@"pushNotification"];
    [[NSUserDefaults standardUserDefaults] synchronize];
    
    [[NSNotificationCenter defaultCenter] postNotificationName:@"GCMPushPluginRemoteNotification" object:userInfo];
}

- (void) application:(UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken {
    // We need to implement this here because apache cordova shortens the token whereas for GCM registration we need the full token.    
    [[NSNotificationCenter defaultCenter] postNotificationName:CDVRemoteNotification object:deviceToken];
}

@end
