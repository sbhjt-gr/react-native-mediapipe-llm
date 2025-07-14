package com.reactnativemediapipellm

import android.content.Context
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream

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
                { requestId, response -> emitEvent("onPartialResponse", createEventMap(modelHandle, requestId, response = response)) },
                { requestId, error -> emitEvent("onErrorResponse", createEventMap(modelHandle, requestId, error = error)) }
            )
            
            modelMap[modelHandle] = model
            promise.resolve(modelHandle)
        } catch (e: Exception) {
            promise.reject("MODEL_CREATION_FAILED", e.localizedMessage)
        }
    }

    @ReactMethod
    fun generateResponse(handle: Int, requestId: Int, prompt: String, promise: Promise) {
        modelMap[handle]?.let { 
            it.generateResponseAsync(requestId, prompt, promise) 
        } ?: promise.reject("INVALID_HANDLE", "No model found for handle $handle")
    }

    @ReactMethod
    fun releaseModel(handle: Int, promise: Promise) {
        modelMap.remove(handle)?.let { 
            promise.resolve(true) 
        } ?: promise.reject("INVALID_HANDLE", "No model found for handle $handle")
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for event emitter
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for event emitter
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