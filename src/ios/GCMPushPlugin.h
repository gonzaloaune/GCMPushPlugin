#import <Cordova/CDVPlugin.h>

//NSString* const GCMPushPluginRemoteNotification = @"GCMPushPluginRemoteNotification";

@interface GCMPushPlugin : CDVPlugin

@property (nonatomic, strong) NSString *jsCallback;
@property (nonatomic, strong) NSString *senderId;
@property (nonatomic, assign) BOOL usesGCM;
@property (nonatomic, strong) UIWebView *theWebView;

@property (nonatomic, strong) NSString* registerCallbackId;
@property (nonatomic, strong) NSString* unregisterCallbackId;

@property (nonatomic, strong) NSDictionary *pushNotification;

- (void)register:(CDVInvokedUrlCommand*)command;
- (void)unregister:(CDVInvokedUrlCommand*)command;
- (void)setApplicationIconBadgeNumber:(CDVInvokedUrlCommand*)command;

@end
