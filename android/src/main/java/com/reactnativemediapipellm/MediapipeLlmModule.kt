package com.reactnativemediapipellm

import android.content.Context
import android.net.Uri
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
            val resolvedPath = resolveModelPath(modelPath)
            
            // Create model asynchronously to avoid blocking the main thread
            Thread {
                try {
                    val model = MediapipeLlmModel(
                        reactContext,
                        resolvedPath,
                        maxTokens,
                        topK,
                        temperature.toFloat(),
                        randomSeed,
                        { requestId, response -> emitEvent("onPartialResponse", createEventMap(modelHandle, requestId, response = response)) },
                        { requestId, error -> emitEvent("onErrorResponse", createEventMap(modelHandle, requestId, error = error)) }
                    )
                    
                    modelMap[modelHandle] = model
                    promise.resolve(modelHandle)
                } catch (e: Exception) {
                    promise.reject("MODEL_CREATION_FAILED", e.message ?: "Unknown error")
                }
            }.start()
            
        } catch (e: Exception) {
            promise.reject("MODEL_CREATION_FAILED", e.message ?: "Unknown error")
        }
    }

    @ReactMethod
    fun generateResponse(handle: Int, requestId: Int, prompt: String, promise: Promise) {
        try {
            val model = modelMap[handle]
            if (model == null) {
                promise.reject("INVALID_HANDLE", "No model found for handle $handle")
                return
            }
            
            if (!model.isReady()) {
                promise.reject("MODEL_NOT_READY", "Model is still loading, please wait")
                return
            }
            
            model.generateResponseAsync(requestId, prompt, promise)
        } catch (e: Exception) {
            promise.reject("GENERATION_FAILED", e.message ?: "Unknown error")
        }
    }

    @ReactMethod
    fun releaseModel(handle: Int, promise: Promise) {
        try {
            val model = modelMap.remove(handle)
            if (model != null) {
                model.release()
                promise.resolve(true)
            } else {
                promise.reject("INVALID_HANDLE", "No model found for handle $handle")
            }
        } catch (e: Exception) {
            promise.reject("RELEASE_FAILED", e.message ?: "Unknown error")
        }
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for event emitter
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for event emitter
    }

    private fun resolveModelPath(modelPath: String): String {
        return if (modelPath.startsWith("content://")) {
            copyContentUriToInternalStorage(modelPath)
        } else {
            modelPath
        }
    }

    private fun copyContentUriToInternalStorage(contentUriString: String): String {
        val uri = Uri.parse(contentUriString)
        val inputStream: InputStream = reactContext.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open content URI: $contentUriString")
        
        val fileName = "model_${System.currentTimeMillis()}.task"
        val outputFile = File(reactContext.filesDir, fileName)
        
        inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return outputFile.absolutePath
    }

    private fun copyAssetToInternalStorage(modelName: String): String {
        val outputFile = File(reactContext.filesDir, modelName)
        
        if (outputFile.exists()) return outputFile.path
        
        reactContext.assets.open(modelName).use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        return outputFile.path
    }

    private fun emitEvent(eventName: String, eventData: WritableMap) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, eventData)
    }

    private fun createEventMap(handle: Int, requestId: Int, response: String? = null, error: String? = null): WritableMap {
        return Arguments.createMap().apply {
            putInt("handle", handle)
            putInt("requestId", requestId)
            if (response != null) putString("response", response)
            if (error != null) putString("error", error)
        }
    }
}