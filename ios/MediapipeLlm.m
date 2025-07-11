#import "MediapipeLlm.h"
#import <React/RCTLog.h>
#import <React/RCTUtils.h>
#import <MediaPipeTasksGenAI/MediaPipeTasksGenAI.h>

@interface MediapipeLlm ()
@property (nonatomic, strong) MPPLlmInference *llmInference;
@property (nonatomic, assign) BOOL isInitialized;
@property (nonatomic, strong) dispatch_queue_t processingQueue;
@end

@implementation MediapipeLlm

RCT_EXPORT_MODULE()

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _isInitialized = NO;
        _processingQueue = dispatch_queue_create("com.mediapipellm.processing", DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

- (NSArray<NSString *> *)supportedEvents {
    return @[];
}

RCT_EXPORT_METHOD(initialize:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(self.processingQueue, ^{
        @try {
            NSString *modelPath = options[@"modelPath"];
            if (!modelPath) {
                reject(@"INVALID_PARAMS", @"modelPath is required", nil);
                return;
            }
            
            if (![[NSFileManager defaultManager] fileExistsAtPath:modelPath]) {
                reject(@"MODEL_NOT_FOUND", [NSString stringWithFormat:@"Model file not found at: %@", modelPath], nil);
                return;
            }
            
            MPPLlmInferenceOptions *llmOptions = [[MPPLlmInferenceOptions alloc] init];
            llmOptions.modelPath = modelPath;
            
            // Set optional parameters with defaults
            if (options[@"maxTokens"] && [options[@"maxTokens"] intValue] > 0) {
                llmOptions.maxTokens = [options[@"maxTokens"] intValue];
            } else {
                llmOptions.maxTokens = 512;
            }
            
            if (options[@"temperature"] && [options[@"temperature"] doubleValue] > 0) {
                llmOptions.temperature = [options[@"temperature"] floatValue];
            } else {
                llmOptions.temperature = 0.8f;
            }
            
            if (options[@"topK"] && [options[@"topK"] intValue] > 0) {
                llmOptions.topK = [options[@"topK"] intValue];
            } else {
                llmOptions.topK = 40;
            }
            
            if (options[@"topP"] && [options[@"topP"] doubleValue] > 0) {
                llmOptions.topP = [options[@"topP"] floatValue];
            } else {
                llmOptions.topP = 0.9f;
            }
            
            NSError *error = nil;
            self.llmInference = [[MPPLlmInference alloc] initWithOptions:llmOptions error:&error];
            
            if (error) {
                reject(@"INIT_ERROR", [NSString stringWithFormat:@"Failed to initialize LLM: %@", error.localizedDescription], error);
                return;
            }
            
            self.isInitialized = YES;
            resolve(@YES);
        } @catch (NSException *exception) {
            self.isInitialized = NO;
            reject(@"INIT_ERROR", [NSString stringWithFormat:@"Failed to initialize LLM: %@", exception.reason], nil);
        }
    });
}

RCT_EXPORT_METHOD(generateResponse:(NSString *)prompt
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(self.processingQueue, ^{
        @try {
            if (!self.isInitialized || !self.llmInference) {
                reject(@"NOT_INITIALIZED", @"LLM not initialized", nil);
                return;
            }
            
            NSError *error = nil;
            NSString *response = [self.llmInference generateResponseWithInputText:prompt error:&error];
            
            if (error) {
                reject(@"GENERATION_ERROR", [NSString stringWithFormat:@"Failed to generate response: %@", error.localizedDescription], error);
                return;
            }
            
            resolve(response);
        } @catch (NSException *exception) {
            reject(@"GENERATION_ERROR", [NSString stringWithFormat:@"Failed to generate response: %@", exception.reason], nil);
        }
    });
}

RCT_EXPORT_METHOD(generateResponseWithCallback:(NSString *)prompt
                  successCallback:(RCTResponseSenderBlock)successCallback
                  errorCallback:(RCTResponseSenderBlock)errorCallback) {
    dispatch_async(self.processingQueue, ^{
        @try {
            if (!self.isInitialized || !self.llmInference) {
                errorCallback(@[@"LLM not initialized"]);
                return;
            }
            
            [self.llmInference generateResponseWithInputText:prompt
                                              progress:^(NSString * _Nonnull partialResult, BOOL done) {
                successCallback(@[partialResult, @(done)]);
            }
                                                 error:^(NSError * _Nonnull error) {
                errorCallback(@[[NSString stringWithFormat:@"Failed to generate response: %@", error.localizedDescription]]);
            }];
        } @catch (NSException *exception) {
            errorCallback(@[[NSString stringWithFormat:@"Failed to generate response: %@", exception.reason]]);
        }
    });
}

RCT_EXPORT_METHOD(isInitialized:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@(self.isInitialized));
}

RCT_EXPORT_METHOD(cleanup:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(self.processingQueue, ^{
        @try {
            self.llmInference = nil;
            self.isInitialized = NO;
            resolve(@YES);
        } @catch (NSException *exception) {
            reject(@"CLEANUP_ERROR", [NSString stringWithFormat:@"Failed to cleanup: %@", exception.reason], nil);
        }
    });
}

- (void)dealloc {
    self.llmInference = nil;
}

@end 