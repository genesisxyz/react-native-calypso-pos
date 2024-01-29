#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(MyEventEmitter, RCTEventEmitter)

RCT_EXTERN_METHOD(supportedEvents)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
