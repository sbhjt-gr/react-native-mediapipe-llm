package com.reactnativemediapipellm

import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.io.File
import java.io.FileOutputStream

class MediapipeLlmModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private var nextHandle = 1
  private val modelMap = mutableMapOf<Int, MediapipeLlmModel>()

  override fun getName(): String {
    return "MediapipeLlm"
  }

  private class InferenceModelListener(
    private val module: MediapipeLlmModule,
    private val handle: Int
  ) : InferenceListener {
    override fun logging(model: MediapipeLlmModel, message: String) {
      module.emitEvent(
        "logging",
        Arguments.createMap().apply {
          this.putInt("handle", this@InferenceModelListener.handle)
          this.putString("message", message)
        }
      )
    }

    override fun onError(model: MediapipeLlmModel, requestId: Int, error: String) {
      module.emitEvent(
        "onErrorResponse",
        Arguments.createMap().apply {
          this.putInt("handle", this@InferenceModelListener.handle)
          this.putInt("requestId", requestId)
          this.putString("error", error)
        }
      )
    }

    override fun onResults(model: MediapipeLlmModel, requestId: Int, response: String) {
      module.emitEvent(
        "onPartialResponse",
        Arguments.createMap().apply {
          this.putInt("handle", this@InferenceModelListener.handle)
          this.putInt("requestId", requestId)
          this.putString("response", response)
        }
      )
    }
  }

  @ReactMethod
  fun createModel(
    modelPath: String,
    maxTokens: Int,
    topK: Int,
    temperature: Double,
    randomSeed: Int,
    promise: Promise
  ) {
    try {
      val modelHandle = nextHandle++
      val model =
        MediapipeLlmModel(
          this.reactContext,
          modelPath,
          maxTokens,
          topK,
          temperature.toFloat(),
          randomSeed,
          inferenceListener = InferenceModelListener(this, modelHandle)
        )
      modelMap[modelHandle] = model
      promise.resolve(modelHandle)
    } catch (e: Exception) {
      promise.reject("Model Creation Failed", e.localizedMessage)
    }
  }

  @ReactMethod
  fun createModelFromAsset(
    modelName: String,
    maxTokens: Int,
    topK: Int,
    temperature: Double,
    randomSeed: Int,
    promise: Promise
  ) {
    try {
      val modelPath = copyFileToInternalStorageIfNeeded(modelName, this.reactContext).path

      val modelHandle = nextHandle++
      val model =
        MediapipeLlmModel(
          this.reactContext,
          modelPath,
          maxTokens,
          topK,
          temperature.toFloat(),
          randomSeed,
          inferenceListener = InferenceModelListener(this, modelHandle)
        )
      modelMap[modelHandle] = model
      promise.resolve(modelHandle)
    } catch (e: Exception) {
      promise.reject("Model Creation Failed", e.localizedMessage)
    }
  }

  @ReactMethod
  fun releaseModel(handle: Int, promise: Promise) {
    modelMap.remove(handle)?.let { promise.resolve(true) }
      ?: promise.reject("INVALID_HANDLE", "No model found for handle $handle")
  }

  @ReactMethod
  fun generateResponse(handle: Int, requestId: Int, prompt: String, promise: Promise) {
    modelMap[handle]?.let { it.generateResponseAsync(requestId, prompt, promise) }
      ?: promise.reject("INVALID_HANDLE", "No model found for handle $handle")
  }

  @ReactMethod
  fun addListener(eventName: String?) {
  }

  @ReactMethod
  fun removeListeners(count: Int?) {
  }

  private fun copyFileToInternalStorageIfNeeded(modelName: String, context: Context): File {
    val outputFile = File(context.filesDir, modelName)

    if (outputFile.exists()) {
      return outputFile
    }

    val assetList =
      context.assets.list(
        ""
      )
    if (modelName !in assetList.orEmpty()) {
      throw IllegalArgumentException("Asset file ${modelName} does not exist.")
    }
    context.assets.open(modelName).use { inputStream ->
      FileOutputStream(outputFile).use { outputStream -> inputStream.copyTo(outputStream) }
    }

    return outputFile
  }

  private fun emitEvent(eventName: String, eventData: WritableMap) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, eventData)
  }
}