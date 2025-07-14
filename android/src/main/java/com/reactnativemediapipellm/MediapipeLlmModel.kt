package com.reactnativemediapipellm

import android.content.Context
import com.facebook.react.bridge.Promise
import com.google.mediapipe.tasks.genai.llminference.LlmInference

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
    private var llmInference: LlmInference
    private var requestResult: String = ""
    private var requestPromise: Promise? = null
    private var currentRequestId: Int = 0

    init {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(maxTokens)
            .setTopK(topK)
            .setTemperature(temperature)
            .setRandomSeed(randomSeed)
            .setResultListener { partialResult, done ->
                onPartial(currentRequestId, partialResult)
                requestResult += partialResult
                if (done) {
                    requestPromise?.resolve(requestResult)
                }
            }
            .setErrorListener { ex ->
                onError(currentRequestId, ex.message ?: "Unknown error")
                requestPromise?.reject("INFERENCE_ERROR", ex.message)
            }
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
    }

    fun generateResponseAsync(requestId: Int, prompt: String, promise: Promise) {
        this.currentRequestId = requestId
        this.requestResult = ""
        this.requestPromise = promise
        llmInference.generateResponseAsync(prompt)
    }
} 