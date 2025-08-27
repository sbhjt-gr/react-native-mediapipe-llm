#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(MediapipeLlm, RCTEventEmitter)

RCT_EXTERN_METHOD(createModel:(NSString *)modelPath
                  withMaxTokens:(NSInteger)maxTokens
                  withTopK:(NSInteger)topK
                  withTemperature:(NSNumber *)temperature
                  withRandomSeed:(NSInteger)randomSeed
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(createModelFromAsset:(NSString *)modelName
                  withMaxTokens:(NSInteger)maxTokens
                  withTopK:(NSInteger)topK
                  withTemperature:(NSNumber *)temperature
                  withRandomSeed:(NSInteger)randomSeed
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(generateResponse:(NSInteger)handle
                  withRequestId:(NSInteger)requestId
                  withPrompt:(NSString *)prompt
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(releaseModel:(NSInteger)handle
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return YES;
}

@end