package com.reactnativemediapipellm

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

object ModelLoader {
    private const val TAG = "ModelLoader"
    
    data class ConfigurationAnalysis(
        val recommendations: List<String>,
        val warnings: List<String>,
        val info: List<String>,
        val isLargeHeapEnabled: Boolean,
        val currentHeapMB: Long,
        val recommendedMaxModelMB: Int,
        val deviceCategory: String
    )
    
    data class ModelValidationResult(
        val isValid: Boolean,
        val errorMessage: String?,
        val modelType: String,
        val sizeBytes: Long,
        val recommendations: List<String>
    )
    
    fun analyzeAndRecommend(context: Context): ConfigurationAnalysis {
        val recommendations = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val info = mutableListOf<String>()
        
        val isLargeHeapEnabled = try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            (appInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
        } catch (e: Exception) {
            false
        }
        
        val runtime = Runtime.getRuntime()
        val maxHeapMB = runtime.maxMemory() / (1024 * 1024)
        
        val memoryAnalysis = MemoryManager.analyzeDeviceMemory(context)
        
        if (!isLargeHeapEnabled && memoryAnalysis.totalDeviceMemoryMB > 4000) {
            recommendations.add("Add android:largeHeap=\"true\" to <application> in AndroidManifest.xml")
            recommendations.add("This can increase available heap from ${maxHeapMB}MB to potentially 1-4GB")
        }
        
        if (maxHeapMB < 1000 && memoryAnalysis.totalDeviceMemoryMB > 6000) {
            recommendations.add("For large models on ${memoryAnalysis.totalDeviceMemoryMB}MB devices, consider:")
            recommendations.add("  - Adding org.gradle.jvmargs=-Xmx4g to gradle.properties")
            recommendations.add("  - Using android:vmSafeMode=\"false\" in AndroidManifest.xml")
        }
        
        val availableRatio = memoryAnalysis.availableMemoryMB.toFloat() / memoryAnalysis.totalDeviceMemoryMB
        if (availableRatio < 0.3) {
            warnings.add("Low available memory (${memoryAnalysis.availableMemoryMB}MB/${memoryAnalysis.totalDeviceMemoryMB}MB)")
            warnings.add("Close other apps before loading large models")
        }
        
        val safeModelSize = if (memoryAnalysis.isHighMemoryDevice) {
            (maxHeapMB * 0.8).toInt()
        } else {
            (maxHeapMB * 0.6).toInt()
        }
        
        info.add("Current heap limit: ${maxHeapMB}MB")
        info.add("Recommended max model size: ${safeModelSize}MB")
        info.add("Device category: ${if (memoryAnalysis.isHighMemoryDevice) "High-memory" else "Standard"}")
        
        Log.i(TAG, "=== Configuration Analysis ===")
        recommendations.forEach { Log.i(TAG, "RECOMMENDATION: $it") }
        warnings.forEach { Log.w(TAG, "WARNING: $it") }
        info.forEach { Log.i(TAG, "INFO: $it") }
        
        return ConfigurationAnalysis(
            recommendations = recommendations,
            warnings = warnings,
            info = info,
            isLargeHeapEnabled = isLargeHeapEnabled,
            currentHeapMB = maxHeapMB,
            recommendedMaxModelMB = safeModelSize,
            deviceCategory = if (memoryAnalysis.isHighMemoryDevice) "high_memory" else "standard"
        )
    }
    
    fun getGradlePropertiesRecommendations(context: Context): List<String> {
        val memoryAnalysis = MemoryManager.analyzeDeviceMemory(context)
        val recommendations = mutableListOf<String>()
        
        if (memoryAnalysis.isHighMemoryDevice) {
            recommendations.add("# Add these to gradle.properties for large models:")
            recommendations.add("org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g")
            recommendations.add("org.gradle.parallel=true")
            recommendations.add("org.gradle.caching=true")
            recommendations.add("android.enableR8.fullMode=true")
        }
        
        return recommendations
    }
    
    fun validateModel(modelPath: String): ModelValidationResult {
        val file = File(modelPath)
        val recommendations = mutableListOf<String>()
        
        if (!file.exists()) {
            return ModelValidationResult(
                isValid = false,
                errorMessage = "Model file does not exist at path: $modelPath",
                modelType = "unknown",
                sizeBytes = 0,
                recommendations = listOf("Check file path and permissions")
            )
        }
        
        if (!file.canRead()) {
            return ModelValidationResult(
                isValid = false,
                errorMessage = "Cannot read model file: $modelPath",
                modelType = "unknown",
                sizeBytes = file.length(),
                recommendations = listOf("Check file permissions")
            )
        }
        
        if (file.length() == 0L) {
            return ModelValidationResult(
                isValid = false,
                errorMessage = "Model file is empty",
                modelType = "unknown",
                sizeBytes = 0,
                recommendations = listOf("Re-download or regenerate the model file")
            )
        }
        
        val modelType = when {
            modelPath.endsWith(".task", ignoreCase = true) -> "task"
            modelPath.endsWith(".tflite", ignoreCase = true) -> "tflite"
            else -> "unknown"
        }
        
        val sizeBytes = file.length()
        val sizeMB = sizeBytes / (1024 * 1024)
        
        // Check file header to validate format
        val isValidFormat = try {
            val bytes = file.readBytes().take(16)
            when (modelType) {
                "task" -> {
                    // Task files typically start with specific bytes
                    val hexDump = bytes.joinToString(" ") { "%02x".format(it) }
                    Log.d(TAG, "Task file header: $hexDump")
                    true // For now, assume valid if readable
                }
                "tflite" -> {
                    // TFLite files start with "TFL3" magic bytes
                    bytes.size >= 4 && 
                    bytes[0] == 0x54.toByte() && 
                    bytes[1] == 0x46.toByte() && 
                    bytes[2] == 0x4C.toByte() && 
                    bytes[3] == 0x33.toByte()
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to validate model format: ${e.message}")
            false
        }
        
        if (!isValidFormat && modelType != "unknown") {
            return ModelValidationResult(
                isValid = false,
                errorMessage = "Invalid $modelType file format",
                modelType = modelType,
                sizeBytes = sizeBytes,
                recommendations = listOf(
                    "Verify the model file is not corrupted",
                    "Check if the file was properly downloaded",
                    "Try using a different model format"
                )
            )
        }
        
        when {
            sizeMB > 2000 -> {
                recommendations.add("Very large model (${sizeMB}MB) - ensure device has sufficient memory")
                recommendations.add("Consider using a smaller model for better performance")
            }
            sizeMB > 1000 -> {
                recommendations.add("Large model (${sizeMB}MB) - monitor memory usage during inference")
            }
            sizeMB < 10 -> {
                recommendations.add("Small model (${sizeMB}MB) - may have limited capabilities")
            }
        }
        
        when (modelType) {
            "task" -> {
                recommendations.add("Task file detected - ensure MediaPipe 0.10.24 compatibility")
                recommendations.add("If loading fails, try converting to .tflite format")
            }
            "tflite" -> {
                recommendations.add("TensorFlow Lite model detected - good compatibility")
            }
            "unknown" -> {
                recommendations.add("Unknown file format - ensure it's a valid MediaPipe model")
            }
        }
        
        return ModelValidationResult(
            isValid = true,
            errorMessage = null,
            modelType = modelType,
            sizeBytes = sizeBytes,
            recommendations = recommendations
        )
    }
    
    fun analyzeModelCompatibility(modelPath: String): List<String> {
        val analysis = mutableListOf<String>()
        val file = File(modelPath)
        
        if (!file.exists()) {
            analysis.add("ERROR: Model file does not exist")
            return analysis
        }
        
        try {
            val firstBytes = file.readBytes().take(64)
            val hexDump = firstBytes.take(32).joinToString(" ") { "%02x".format(it) }
            
            analysis.add("Model Analysis:")
            analysis.add("  File: ${file.name}")
            analysis.add("  Size: ${file.length() / 1024 / 1024}MB")
            analysis.add("  First 32 bytes: $hexDump")
            
            val isTaskFile = modelPath.endsWith(".task", ignoreCase = true)
            val isTfLiteFile = firstBytes.size >= 4 && 
                firstBytes[0] == 0x54.toByte() && 
                firstBytes[1] == 0x46.toByte() && 
                firstBytes[2] == 0x4C.toByte() && 
                firstBytes[3] == 0x33.toByte()
            
            when {
                isTaskFile && !isTfLiteFile -> {
                    analysis.add("  Format: MediaPipe Task (.task)")
                    analysis.add("  Compatibility: MediaPipe 0.10.24 with limitations")
                    analysis.add("  Recommendation: If loading fails, convert to .tflite")
                }
                isTfLiteFile -> {
                    analysis.add("  Format: TensorFlow Lite (.tflite)")
                    analysis.add("  Compatibility: Good with MediaPipe 0.10.24")
                }
                else -> {
                    analysis.add("  Format: Unknown or unsupported")
                    analysis.add("  Compatibility: May not work with MediaPipe")
                }
            }
            
        } catch (e: Exception) {
            analysis.add("ERROR: Failed to analyze model file: ${e.message}")
        }
        
        return analysis
    }
}
