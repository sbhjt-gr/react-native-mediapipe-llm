package com.reactnativemediapipellm

import android.content.Context
import android.util.Log
import com.facebook.react.bridge.Promise
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class MediapipeLlmModel(
    private val context: Context,
    private val modelPath: String,
    private val maxTokens: Int,
    private val topK: Int,
    private val temperature: Float,
    private val randomSeed: Int,
    private val onPartial: (Int, String) -> Unit,
    private val onError: (Int, String) -> Unit
) {
    private var llmInference: LlmInference? = null
    private var requestResult: String = ""
    private var requestPromise: Promise? = null
    private var currentRequestId: Int = 0
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val executor = Executors.newSingleThreadExecutor()
    
    companion object {
        private const val TAG = "MediapipeLlmModel"
        private const val MAX_MODEL_SIZE_BYTES = 10L * 1024 * 1024 * 1024 
    }

    init {
        validateModelFile()
        initializeModelAsync()
    }
    
    private fun validateModelFile() {
        val modelFile = File(modelPath)
        
        if (!modelFile.exists()) {
            throw IllegalArgumentException("Model file does not exist: $modelPath")
        }
        
        if (!modelFile.canRead()) {
            throw IllegalArgumentException("Model file is not readable: $modelPath")
        }
        
        val fileSize = modelFile.length()
        Log.i(TAG, "Model file size: $fileSize bytes (${fileSize / 1024 / 1024} MB)")
        
        if (fileSize == 0L) {
            throw IllegalArgumentException("Model file is empty: $modelPath")
        }
        
        if (fileSize > MAX_MODEL_SIZE_BYTES) {
            throw IllegalArgumentException("Model file too large: ${fileSize / 1024 / 1024} MB (max: ${MAX_MODEL_SIZE_BYTES / 1024 / 1024} MB)")
        }
        
        // Minimum memory requirement check
        val minRequiredMemory = 512L * 1024 * 1024 // 512MB minimum
        if (fileSize < minRequiredMemory) {
            Log.i(TAG, "Small model detected, should load quickly")
        }
        
        // Check available memory
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val availableMemory = maxMemory - totalMemory + freeMemory
        
        Log.i(TAG, "Available memory: ${availableMemory / 1024 / 1024} MB")
        Log.i(TAG, "Model size: ${fileSize / 1024 / 1024} MB")
        
        // Memory validation - model should not exceed 70% of available memory
        // This allows for larger models while maintaining stability
        if (fileSize > availableMemory * 0.7) {
            throw IllegalArgumentException("Model too large for available memory. Model: ${fileSize / 1024 / 1024} MB, Available: ${availableMemory / 1024 / 1024} MB. Please use a smaller model or free up memory.")
        }
        
        // Warning for models that are close to the limit
        if (fileSize > availableMemory * 0.5) {
            Log.w(TAG, "Model is large relative to available memory, loading may be slow or require background app termination")
        }
    }
    
    private fun initializeModelAsync() {
        backgroundScope.launch {
            try {
                val loadTime = measureTimeMillis {
                    withContext(Dispatchers.IO) {
                        initializeModel()
                    }
                }
                Log.i(TAG, "Model initialized in ${loadTime}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize model", e)
                onError(0, "Model initialization failed: ${e.message}")
            }
        }
    }
    
    private fun initializeModel() {
        try {
            System.gc() // Suggest garbage collection before loading
            
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .setTopK(topK)
                .setTemperature(temperature)
                .setRandomSeed(randomSeed)
                .setResultListener { partialResult, done ->
                    try {
                        onPartial(currentRequestId, partialResult)
                        requestResult += partialResult
                        if (done) {
                            requestPromise?.resolve(requestResult)
                            requestPromise = null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in result listener", e)
                        requestPromise?.reject("RESULT_LISTENER_ERROR", e.message)
                        requestPromise = null
                    }
                }
                .setErrorListener { ex ->
                    Log.e(TAG, "LLM inference error", ex)
                    onError(currentRequestId, ex.message ?: "Unknown error")
                    requestPromise?.reject("INFERENCE_ERROR", ex.message)
                    requestPromise = null
                }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.i(TAG, "LlmInference created successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create LlmInference", e)
            throw e
        }
    }

    fun generateResponseAsync(requestId: Int, prompt: String, promise: Promise) {
        executor.execute {
            try {
                if (llmInference == null) {
                    promise.reject("MODEL_NOT_READY", "Model is not ready yet")
                    return@execute
                }
                
                this.currentRequestId = requestId
                this.requestResult = ""
                this.requestPromise = promise
                
                Log.i(TAG, "Starting inference for request $requestId")
                llmInference?.generateResponseAsync(prompt)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in generateResponseAsync", e)
                promise.reject("GENERATION_ERROR", e.message)
            }
        }
    }
    
    fun isReady(): Boolean {
        return llmInference != null
    }
    
    fun release() {
        try {
            llmInference?.close()
            llmInference = null
            executor.shutdown()
            Log.i(TAG, "Model released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing model", e)
        }
    }
} 