#import "FileLoggerBridge.h"
#import "iosApp-Swift.h"

@implementation FileLoggerBridge

+ (void)logWithLevel:(NSString *)level tag:(NSString *)tag message:(NSString *)message {
    [[FileLogger shared] writeWithLevel:level tag:tag message:message];
}

@end
