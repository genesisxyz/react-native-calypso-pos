#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(PosSam, NSObject)

RCT_EXTERN_METHOD(init:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(close)

RCT_EXTERN_METHOD(readCardId:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(readRecordsFromCard:(NSDictionary *)options
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(writeToCardUpdate:(NSString *)adpu withOptions:(NSDictionary *)options
                  withResolver(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
