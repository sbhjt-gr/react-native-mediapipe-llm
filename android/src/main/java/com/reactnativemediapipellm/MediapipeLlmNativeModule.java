package com.reactnativemediapipellm;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class MediapipeLlmNativeModule extends ReactContextBaseJavaModule {
    
    static {
        try {
            System.loadLibrary("MediapipeLlm");
        } catch (UnsatisfiedLinkError e) {
            // Library not found, will be handled in methods
        }
    }

    public MediapipeLlmNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "MediapipeLlmNative";
    }

    @ReactMethod
    public void multiply(double a, double b, Promise promise) {
        try {
            promise.resolve(a * b);
        } catch (Exception e) {
            promise.reject("MULTIPLY_ERROR", e.getMessage());
        }
    }
}
