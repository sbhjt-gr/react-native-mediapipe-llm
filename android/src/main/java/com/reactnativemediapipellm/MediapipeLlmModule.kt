package com.reactnativemediapipellm

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.bridge.Callback

class MediapipeLlmModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  init {
    try {
      System.loadLibrary("MediapipeLlm")
    } catch (e: UnsatisfiedLinkError) {
      System.loadLibrary("mediapipellm")
    }
  }

  @ReactMethod
  fun multiply(a: Double, b: Double, promise: Promise) {
    promise.resolve(nativeMultiply(a, b))
  }

  @ReactMethod
  fun install(promise: Promise) {
    try {
      val reactApplicationContext = reactApplicationContext
      val catalystInstance = reactApplicationContext.catalystInstance
      val runtime = catalystInstance.javaScriptContextHolder.get()
      
      if (runtime != 0L) {
        nativeInstall(runtime)
        promise.resolve(true)
      } else {
        promise.reject("RUNTIME_ERROR", "JavaScript runtime not available")
      }
    } catch (exception: Exception) {
      promise.reject("INSTALL_ERROR", "Failed to install MediaPipe LLM: ${exception.message}", exception)
    }
  }

  @ReactMethod
  fun addListener(eventName: String) {
  }

  @ReactMethod
  fun removeListeners(count: Int) {
  }

  private external fun nativeMultiply(a: Double, b: Double): Double
  private external fun nativeInstall(jsiPtr: Long)

  companion object {
    const val NAME = "MediapipeLlm"
  }
} 