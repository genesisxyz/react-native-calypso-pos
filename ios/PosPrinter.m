#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(PosPrinter, NSObject)

RCT_EXTERN_METHOD(checkStatus:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(open:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(setGrey:(int)level)

RCT_EXTERN_METHOD(setLineSpace:(int)lineSpace)

RCT_EXTERN_METHOD(setBold:(BOOL)isBold)

RCT_EXTERN_METHOD(setAlgin:(int)mode)

RCT_EXTERN_METHOD(setTextSize:(int)size)

RCT_EXTERN_METHOD(addString:(NSString *)content)

RCT_EXTERN_METHOD(printString)

RCT_EXTERN_METHOD(walkPaper:(int)line)

RCT_EXTERN_METHOD(printLogo:(NSString *)image withIsBuffer:(BOOL)isBuffer)

RCT_EXTERN_METHOD(printHTML:(NSString *)html
                  withResolver:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(close:(RCTPromiseResolveBlock)resolve
                  withRejecter:(RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
