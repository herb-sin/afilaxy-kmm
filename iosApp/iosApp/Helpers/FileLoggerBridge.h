#import <Foundation/Foundation.h>

@interface FileLoggerBridge : NSObject
+ (void)logWithLevel:(NSString *)level tag:(NSString *)tag message:(NSString *)message;
@end
