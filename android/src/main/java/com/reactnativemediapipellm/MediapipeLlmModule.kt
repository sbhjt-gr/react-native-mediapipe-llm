package com.reactnativemediapipellm

import android.content.Context
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.*
import java.io.File

class MediapipeLlmModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  private var llmInference: LlmInference? = null
  private var isInitialized = false
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  @ReactMethod
  fun initialize(options: ReadableMap, promise: Promise) {
    scope.launch {
      try {
        val modelPath = options.getString("modelPath")
        if (modelPath == null) {
          promise.reject("INVALID_PARAMS", "modelPath is required")
          return@launch
        }

        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
          promise.reject("MODEL_NOT_FOUND", "Model file not found at: $modelPath")
          return@launch
        }

        val llmOptions = LlmInference.LlmInferenceOptions.builder()
          .setModelPath(modelPath)
          .setMaxTokens(options.getInt("maxTokens").takeIf { it > 0 } ?: 512)
          .setTemperature(options.getDouble("temperature").takeIf { it > 0 } ?: 0.8)
          .setTopK(options.getInt("topK").takeIf { it > 0 } ?: 40)
          .setTopP(options.getDouble("topP").takeIf { it > 0 } ?: 0.9)
          .build()

        llmInference = LlmInference.createFromOptions(reactApplicationContext, llmOptions)
        isInitialized = true
        promise.resolve(true)
      } catch (e: Exception) {
        isInitialized = false
        promise.reject("INIT_ERROR", "Failed to initialize LLM: ${e.message}", e)
      }
    }
  }

  @ReactMethod
  fun generateResponse(prompt: String, promise: Promise) {
    scope.launch {
      try {
        if (!isInitialized || llmInference == null) {
          promise.reject("NOT_INITIALIZED", "LLM not initialized")
          return@launch
        }

        val response = llmInference!!.generateResponse(prompt)
        promise.resolve(response)
      } catch (e: Exception) {
        promise.reject("GENERATION_ERROR", "Failed to generate response: ${e.message}", e)
      }
    }
  }

  @ReactMethod
  fun generateResponseWithCallback(
    prompt: String,
    successCallback: Callback,
    errorCallback: Callback
  ) {
    scope.launch {
      try {
        if (!isInitialized || llmInference == null) {
          errorCallback.invoke("LLM not initialized")
          return@launch
        }

        llmInference!!.generateResponseAsync(
          prompt
        ) { partialResult, done ->
          try {
            successCallback.invoke(partialResult, done)
          } catch (e: Exception) {
            errorCallback.invoke("Callback error: ${e.message}")
          }
        }
      } catch (e: Exception) {
        errorCallback.invoke("Failed to generate response: ${e.message}")
      }
    }
  }

  @ReactMethod
  fun isInitialized(promise: Promise) {
    promise.resolve(isInitialized)
  }

  @ReactMethod
  fun cleanup(promise: Promise) {
    try {
      llmInference?.close()
      llmInference = null
      isInitialized = false
      scope.cancel()
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject("CLEANUP_ERROR", "Failed to cleanup: ${e.message}", e)
    }
  }

  @ReactMethod
  fun addListener(eventName: String) {
  }

  @ReactMethod
  fun removeListeners(count: Int) {
  }

  override fun onCatalystInstanceDestroy() {
    super.onCatalystInstanceDestroy()
    llmInference?.close()
    scope.cancel()
  }

  companion object {
    const val NAME = "MediapipeLlm"
  }
} 