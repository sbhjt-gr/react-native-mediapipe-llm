#include "../MediapipeLlm.h"
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <React/RCTBridge+Private.h>
#import <React/RCTUtils.h>
#import <ReactCommon/RCTTurboModule.h>
#import <jsi/jsi.h>

using namespace facebook::jsi;
using namespace std;

namespace mediapipe_llm {

void MediapipeLlm::setupiOSImageLoader() {
    NSLog(@"Setting up iOS image loader");
}

std::string MediapipeLlm::loadImageFromUri(const std::string& uri) {
    NSString *uriString = [NSString stringWithUTF8String:uri.c_str()];
    NSLog(@"Loading image from URI: %@", uriString);
    
    NSData *imageData = nil;
    
    if ([uriString hasPrefix:@"file://"]) {
        NSURL *fileURL = [NSURL URLWithString:uriString];
        imageData = [NSData dataWithContentsOfURL:fileURL];
    } else if ([uriString hasPrefix:@"data:image"]) {
        NSRange range = [uriString rangeOfString:@"base64,"];
        if (range.location != NSNotFound) {
            NSString *base64String = [uriString substringFromIndex:range.location + range.length];
            imageData = [[NSData alloc] initWithBase64EncodedString:base64String options:0];
        }
    } else if ([uriString hasPrefix:@"assets-library://"] || [uriString hasPrefix:@"ph://"]) {
        NSLog(@"Photo library URIs not yet implemented");
    }
    
    if (imageData) {
        NSString *base64String = [imageData base64EncodedStringWithOptions:0];
        return std::string([base64String UTF8String]);
    }
    
    return "";
}

} // namespace mediapipe_llm

@interface MediapipeLlmModule : NSObject <RCTBridgeModule>
@property (nonatomic, assign) std::shared_ptr<mediapipe_llm::MediapipeLlm> module;
@end

@implementation MediapipeLlmModule

RCT_EXPORT_MODULE()

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _module = std::make_shared<mediapipe_llm::MediapipeLlm>();
    }
    return self;
}

- (void)setBridge:(RCTBridge *)bridge {
    RCTCxxBridge *cxxBridge = (RCTCxxBridge *)bridge;
    if (!cxxBridge.runtime) {
        return;
    }
    
    _module->install(*(facebook::jsi::Runtime *)cxxBridge.runtime);
}

RCT_EXPORT_METHOD(multiply:(double)a
                  b:(double)b
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    NSNumber *result = @(a * b);
    resolve(result);
}

RCT_EXPORT_METHOD(install:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    @try {
        RCTBridge *bridge = self.bridge;
        RCTCxxBridge *cxxBridge = (RCTCxxBridge *)bridge;
        
        if (cxxBridge.runtime) {
            _module->install(*(facebook::jsi::Runtime *)cxxBridge.runtime);
            resolve(@YES);
        } else {
            reject(@"RUNTIME_ERROR", @"JavaScript runtime not available", nil);
        }
    } @catch (NSException *exception) {
        reject(@"INSTALL_ERROR", [NSString stringWithFormat:@"Failed to install MediaPipe LLM: %@", exception.reason], nil);
    }
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
    return nullptr;
}

@end 