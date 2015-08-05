#import <Cordova/CDVPlugin.h>

//NSString* const GCMPushPluginRemoteNotification = @"GCMPushPluginRemoteNotification";

@interface GCMPushPlugin : CDVPlugin

@property(nonatomic, strong) NSString *jsCallback;
@property(nonatomic, assign) BOOL usesGCM;
@property(nonatomic, assign) BOOL gcmSandbox;
@property(nonatomic, strong) UIWebView *theWebView;

@property(nonatomic, strong) NSString* registerCallbackId;
@property(nonatomic, strong) NSString* unregisterCallbackId;

@property(nonatomic, strong) NSDictionary *pushNotification;
@property(nonatomic, strong) NSDictionary *registrationOptions;
@property(nonatomic, strong) void (^registrationHandler) (NSString *registrationToken, NSError *error);

- (void)register:(CDVInvokedUrlCommand*)command;
- (void)unregister:(CDVInvokedUrlCommand*)command;
- (void)setApplicationIconBadgeNumber:(CDVInvokedUrlCommand*)command;

@end
