//
//  AppDelegate+Notification.h
//  GCMPushPlugin
//
//  Created by Gonzalo Aune on 7/27/15.
//
//

#ifndef TTTMobile_AppDelegate_Notification_h
#define TTTMobile_AppDelegate_Notification_h

#import "AppDelegate.h"

@interface AppDelegate (notification)

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler;
- (void) application:(UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken;

@end

#endif
