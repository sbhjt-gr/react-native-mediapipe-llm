package com.reactnativemediapipellm

import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
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
      val model = MediapipeLlmModel(
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
      val model = MediapipeLlmModel(
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

    val assetList = context.assets.list("") 
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

class MediapipeLlmModel(
  private var context: Context,
  private val modelPath: String,
  val maxTokens: Int,
  val topK: Int,
  val temperature: Float,
  val randomSeed: Int,
  val inferenceListener: InferenceListener? = null,
) {
  private var llmInference: LlmInference? = null
  private var llmSession: LlmInferenceSession? = null
  private var isInitialized = false
  private var initializationError: String? = null

  private var requestId: Int = 0
  private var requestResult: String = ""
  private var requestPromise: Promise? = null

  init {
    initializeModel()
  }

  private fun initializeModel() {
    Thread {
      try {
        android.util.Log.d("MediapipeLlmModel", "Initializing model at path: $modelPath")
        
        val file = java.io.File(modelPath)
        if (!file.exists()) {
          throw IllegalArgumentException("Model file does not exist: $modelPath")
        }
        if (!file.canRead()) {
          throw IllegalArgumentException("Cannot read model file: $modelPath")
        }
        
        android.util.Log.d("MediapipeLlmModel", "Model file validated. Size: ${file.length()} bytes")

        val options = LlmInference.LlmInferenceOptions.builder()
          .setModelPath(modelPath)
          .setMaxTokens(maxTokens)
          .build()

        android.util.Log.d("MediapipeLlmModel", "Creating LlmInference with options...")
        llmInference = LlmInference.createFromOptions(context, options)
        
        android.util.Log.d("MediapipeLlmModel", "Creating LlmInferenceSession with temperature: $temperature, topK: $topK...")
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
          .setTopK(topK)
          .setTemperature(temperature)
          .build()
        
        llmSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
        isInitialized = true
        android.util.Log.d("MediapipeLlmModel", "Model and session initialized successfully")
        
      } catch (e: Exception) {
        android.util.Log.e("MediapipeLlmModel", "Failed to initialize model", e)
        initializationError = e.localizedMessage ?: "Unknown initialization error"
        isInitialized = false
      }
    }.start()
  }

  fun generateResponseAsync(requestId: Int, prompt: String, promise: Promise) {
    if (!isInitialized) {
      if (initializationError != null) {
        promise.reject("MODEL_NOT_INITIALIZED", initializationError)
      } else {
        promise.reject("MODEL_NOT_INITIALIZED", "Model is still initializing")
      }
      return
    }

    val session = llmSession
    if (session == null) {
      promise.reject("SESSION_NOT_AVAILABLE", "LLM inference session is not available")
      return
    }

    this.requestId = requestId
    this.requestResult = ""
    this.requestPromise = promise
    
    try {
      session.generateResponseAsync(prompt) { partialResult: String, done: Boolean ->
        inferenceListener?.onResults(this@MediapipeLlmModel, requestId, partialResult)
        requestResult += partialResult
        if (done) {
          requestPromise?.resolve(requestResult)
        }
      }
    } catch (e: Exception) {
      promise.reject("GENERATION_FAILED", e.localizedMessage ?: "Failed to generate response")
    }
  }

  fun isReady(): Boolean = isInitialized && llmInference != null && llmSession != null
}

interface InferenceListener {
  fun onError(model: MediapipeLlmModel, requestId: Int, error: String)
  fun onResults(model: MediapipeLlmModel, requestId: Int, response: String)
}