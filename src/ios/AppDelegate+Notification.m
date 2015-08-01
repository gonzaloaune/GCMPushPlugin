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
    [[NSUserDefaults standardUserDefaults] setObject:userInfo forKey:@"pushNotification"];
    [[NSUserDefaults standardUserDefaults] synchronize];

    [[NSNotificationCenter defaultCenter] postNotificationName:@"GCMPushPluginRemoteNotification" object:userInfo];
    
    completionHandler(UIBackgroundFetchResultNewData);
}

@end
