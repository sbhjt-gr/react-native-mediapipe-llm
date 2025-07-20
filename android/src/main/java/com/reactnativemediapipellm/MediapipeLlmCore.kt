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

    companion object {
        private const val TAG = "MediapipeLlmModule"
        
        init {
            try {
                System.loadLibrary("MediapipeLlm")
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }

    private var nextHandle = 1
    private val engineMap = mutableMapOf<Int, Long>()

    override fun getName(): String = "MediapipeLlm"

    // Native method declarations
    private external fun nativeCreateEngine(modelPath: String): Long
    private external fun nativeGenerateResponse(enginePtr: Long, prompt: String): String
    private external fun nativeDeleteEngine(enginePtr: Long)

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
            
            val enginePtr = nativeCreateEngine(modelPath)
            
            if (enginePtr == 0L) {
                promise.reject("MODEL_CREATION_FAILED", "Failed to create native engine")
                return
            }
            
            engineMap[modelHandle] = enginePtr
            promise.resolve(modelHandle)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create model from asset", e)
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
            
            val enginePtr = nativeCreateEngine(modelPath)
            
            if (enginePtr == 0L) {
                promise.reject("MODEL_CREATION_FAILED", "Failed to create native engine")
                return
            }
            
            engineMap[modelHandle] = enginePtr
            promise.resolve(modelHandle)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create model", e)
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
            val enginePtr = engineMap[modelHandle]
                ?: throw IllegalArgumentException("Model with handle $modelHandle not found")
            
            val response = nativeGenerateResponse(enginePtr, inputText)
            promise.resolve(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate response", e)
            promise.reject("GENERATION_FAILED", e.localizedMessage)
        }
    }

    @ReactMethod
    fun releaseModel(modelHandle: Int, promise: Promise) {
        try {
            val enginePtr = engineMap.remove(modelHandle)
                ?: throw IllegalArgumentException("Model with handle $modelHandle not found")
            
            nativeDeleteEngine(enginePtr)
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release model", e)
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

    @ReactMethod
    fun getMemoryConfiguration(promise: Promise) {
        try {
            val memoryInfo = MemoryManager.analyzeDeviceMemory(reactContext)
            val modelLimits = MemoryManager.calculateModelLimits(memoryInfo)
            val config = Arguments.createMap().apply {
                putDouble("totalRamGB", memoryInfo.totalDeviceMemoryMB / 1024.0)
                putInt("totalRamMB", memoryInfo.totalDeviceMemoryMB.toInt())
                putInt("availableRamMB", memoryInfo.availableMemoryMB.toInt())
                putInt("currentHeapMB", (Runtime.getRuntime().totalMemory() / (1024 * 1024)).toInt())
                putInt("potentialHeapMB", (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt())
                putString("deviceCategory", modelLimits.deviceCategory)
                putBoolean("isLargeHeapEnabled", memoryInfo.isLargeHeapEnabled)
                putInt("maxModelSizeMB", modelLimits.maxModelSizeMB.toInt())
                putInt("warningThresholdMB", modelLimits.warningThresholdMB.toInt())
                putString("recommendation", modelLimits.recommendation)
                putString("memoryStatus", when {
                    memoryInfo.availableMemoryMB > 2000 -> "excellent"
                    memoryInfo.availableMemoryMB > 1000 -> "good"
                    memoryInfo.availableMemoryMB > 500 -> "moderate"
                    else -> "limited"
                })
            }
            promise.resolve(config)
        } catch (e: Exception) {
            promise.reject("MEMORY_CONFIG_FAILED", e.localizedMessage)
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
