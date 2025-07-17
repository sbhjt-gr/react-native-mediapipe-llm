package com.reactnativemediapipellm

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.ViewManager
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Collections

class MediapipeLlmPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(MediapipeLlmModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return Collections.emptyList()
    }
}

class MediapipeLlmModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private var nextHandle = 1
    private val modelMap = mutableMapOf<Int, MediapipeLlmModel>()

    override fun getName(): String = "MediapipeLlm"

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
            val modelPath = copyAssetToInternalStorage(modelName)
            val modelHandle = nextHandle++
            
            val model = MediapipeLlmModel(
                reactContext,
                modelPath,
                maxTokens,
                topK,
                temperature.toFloat(),
                randomSeed,
                { requestId, response -> emitEvent("onPartialResponse", createEventMap(modelHandle, requestId, response)) },
                { requestId, error -> emitEvent("onErrorResponse", createEventMap(modelHandle, requestId, error)) }
            )
            
            modelMap[modelHandle] = model
            promise.resolve(modelHandle)
        } catch (e: Exception) {
            promise.reject("MODEL_CREATION_FAILED", e.localizedMessage)
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
                reactContext,
                modelPath,
                maxTokens,
                topK,
                temperature.toFloat(),
                randomSeed,
                { requestId, response -> emitEvent("onPartialResponse", createEventMap(modelHandle, requestId, response)) },
                { requestId, error -> emitEvent("onErrorResponse", createEventMap(modelHandle, requestId, error)) }
            )
            
            modelMap[modelHandle] = model
            promise.resolve(modelHandle)
        } catch (e: Exception) {
            promise.reject("MODEL_CREATION_FAILED", e.localizedMessage)
        }
    }

    @ReactMethod
    fun generateResponse(
        modelHandle: Int,
        inputText: String,
        requestId: String,
        promise: Promise
    ) {
        try {
            val model = modelMap[modelHandle]
                ?: throw IllegalArgumentException("Model with handle $modelHandle not found")
            
            model.generateResponse(inputText, requestId, promise)
        } catch (e: Exception) {
            promise.reject("GENERATION_FAILED", e.localizedMessage)
        }
    }

    @ReactMethod
    fun releaseModel(modelHandle: Int, promise: Promise) {
        try {
            val model = modelMap.remove(modelHandle)
                ?: throw IllegalArgumentException("Model with handle $modelHandle not found")
            
            model.release()
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("RELEASE_FAILED", e.localizedMessage)
        }
    }

    @ReactMethod
    fun isAvailable(promise: Promise) {
        try {
            promise.resolve(true)
        } catch (e: Exception) {
            promise.resolve(false)
        }
    }

    private fun copyAssetToInternalStorage(assetFileName: String): String {
        val inputStream: InputStream = reactContext.assets.open(assetFileName)
        val outputFile = File(reactContext.filesDir, assetFileName)
        val outputStream = FileOutputStream(outputFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return outputFile.absolutePath
    }

    private fun emitEvent(eventName: String, params: WritableMap) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun createEventMap(modelHandle: Int, requestId: String, data: String): WritableMap {
        return Arguments.createMap().apply {
            putInt("modelHandle", modelHandle)
            putString("requestId", requestId)
            putString("data", data)
        }
    }
}

class MediapipeLlmModel(
    private val context: Context,
    private val modelPath: String,
    private val maxTokens: Int,
    private val topK: Int,
    private val temperature: Float,
    private val randomSeed: Int,
    private val onPartialResponse: (String, String) -> Unit,
    private val onError: (String, String) -> Unit
) {
    companion object {
        private const val TAG = "MediapipeLlmModel"
    }

    private var llmInference: LlmInference? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            if (!performPreFlightSafetyCheck(modelPath)) {
                throw IllegalArgumentException("Model failed pre-flight safety check")
            }
            
            val memoryAnalysis = MemoryManager.analyzeDeviceMemory(context)
            val modelValidation = ModelLoader.validateModel(modelPath)
            
            if (!modelValidation.isValid) {
                throw IllegalArgumentException(modelValidation.errorMessage ?: "Model validation failed")
            }
            
            Log.i(TAG, "Model validation passed: ${modelValidation.modelType} format, ${modelValidation.sizeBytes / 1024 / 1024}MB")
            
            val memoryStatus = MemoryManager.getCurrentMemoryStatus()
            Log.i(TAG, "Memory status: ${memoryStatus.availableMemoryMB}MB available, ${memoryStatus.memoryPressure * 100}% pressure")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .setTopK(topK)
                .setTemperature(temperature)
                .setRandomSeed(randomSeed)
                .build()

            MemoryManager.performAdvancedMemoryCleanup()
            Thread.sleep(300)
            
            try {
                Log.i(TAG, "Creating LlmInference instance...")
                llmInference = LlmInference.createFromOptions(context, options)
                Log.i(TAG, "LlmInference created successfully with MediaPipe 0.10.24!")
                
                if (llmInference != null) {
                    Log.i(TAG, "Model instance verified successfully")
                    val finalMemoryStatus = MemoryManager.getCurrentMemoryStatus()
                    Log.i(TAG, "Final memory usage: ${finalMemoryStatus.usedMemoryMB}MB used, ${finalMemoryStatus.availableMemoryMB}MB available")
                    
                } else {
                    throw IllegalStateException("LlmInference creation returned null")
                }
                
            } catch (e: Throwable) {
                Log.e(TAG, "Detailed error during model creation:")
                Log.e(TAG, "Error type: ${e::class.java.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Error cause: ${e.cause}")
                
                val isTaskFile = modelPath.endsWith(".task", ignoreCase = true)
                
                val errorMessage = when {
                    e.message?.contains("STABLEHLO_COMPOSITE") == true -> {
                        buildString {
                            append("Model uses newer TensorFlow Lite operations not supported by MediaPipe 0.10.24\n\n")
                            append("Your .task file contains STABLEHLO_COMPOSITE operations that require a newer MediaPipe version.\n")
                            append("This is a version compatibility issue between your model and MediaPipe runtime.\n\n")
                            append("SOLUTIONS:\n")
                            append("1. Use a model compatible with MediaPipe 0.10.24\n")
                            append("2. Use a .task file created with MediaPipe 0.10.24 tools\n")
                            append("3. Convert to .tflite format using compatible tools\n")
                            append("4. Use a different model that's compatible with 0.10.24\n\n")
                            append("Technical details:\n")
                            append("• Error: Missing STABLEHLO_COMPOSITE operation\n")
                            append("• Model TFLite version: Newer than MediaPipe 0.10.24\n")
                            append("• Solution: Use MediaPipe 0.10.24 compatible model")
                        }
                    }
                    isTaskFile -> {
                        buildString {
                            append(".task file may be incompatible with MediaPipe 0.10.24\n\n")
                            append("Your .task file might be using features not available in this MediaPipe version.\n")
                            append("MediaPipe 0.10.24 supports .task files but with some limitations.\n\n")
                            append("SOLUTIONS:\n")
                            append("1. Use a TensorFlow Lite (.tflite) model instead\n")
                            append("2. Convert your .task model to .tflite format\n")
                            append("3. Use a .task file compatible with MediaPipe 0.10.24\n\n")
                            append("Model details:\n")
                            append("• File: ${File(modelPath).name}\n")
                            append("• Size: ${File(modelPath).length() / 1024 / 1024}MB\n")
                            append("• Format: MediaPipe Task (.task)\n")
                            append("• MediaPipe version: 0.10.24\n\n")
                            append("Technical error: ${e.message}")
                        }
                    }
                    else -> {
                        buildString {
                            append("Failed to load model: ${e.message}\n\n")
                            append("Model information:\n")
                            append("• Path: $modelPath\n")
                            append("• File exists: ${File(modelPath).exists()}\n")
                            append("• File size: ${if (File(modelPath).exists()) "${File(modelPath).length() / 1024 / 1024}MB" else "N/A"}\n")
                            append("• MediaPipe version: 0.10.24\n\n")
                            append("Possible causes:\n")
                            append("• Incompatible model format\n")
                            append("• Insufficient memory\n")
                            append("• Corrupted model file\n")
                            append("• Unsupported model architecture")
                        }
                    }
                }
                
                throw RuntimeException(errorMessage, e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            throw e
        }
    }

    private fun performPreFlightSafetyCheck(modelPath: String): Boolean {
        return try {
            val file = File(modelPath)
            when {
                !file.exists() -> {
                    Log.e(TAG, "Model file does not exist: $modelPath")
                    false
                }
                !file.canRead() -> {
                    Log.e(TAG, "Cannot read model file: $modelPath")
                    false
                }
                file.length() == 0L -> {
                    Log.e(TAG, "Model file is empty: $modelPath")
                    false
                }
                file.length() > 2L * 1024 * 1024 * 1024 -> {
                    Log.w(TAG, "Model file is very large (${file.length() / 1024 / 1024}MB): $modelPath")
                    true
                }
                else -> true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pre-flight check failed", e)
            false
        }
    }

    fun generateResponse(inputText: String, requestId: String, promise: Promise) {
        try {
            val inference = llmInference ?: throw IllegalStateException("Model not loaded")
            
            Log.d(TAG, "Generating response for request: $requestId")
            
            val response = inference.generateResponse(inputText)
            
            mainHandler.post {
                promise.resolve(response)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate response", e)
            mainHandler.post {
                promise.reject("GENERATION_FAILED", e.localizedMessage)
            }
        }
    }

    fun release() {
        try {
            llmInference?.close()
            llmInference = null
            Log.i(TAG, "Model resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release model", e)
        }
    }
}
