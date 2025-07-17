package com.reactnativemediapipellm

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object MemoryManager {
    private const val TAG = "MemoryManager"
    
    data class MemoryAnalysis(
        val totalDeviceMemoryMB: Long,
        val availableMemoryMB: Long,
        val maxHeapMB: Long,
        val isLargeHeapEnabled: Boolean,
        val isHighMemoryDevice: Boolean
    )
    
    data class ModelLimits(
        val maxModelSizeMB: Long,
        val warningThresholdMB: Long,
        val deviceCategory: String,
        val recommendation: String
    )
    
    data class MemoryStatus(
        val usedMemoryMB: Long,
        val freeMemoryMB: Long,
        val maxMemoryMB: Long,
        val availableMemoryMB: Long,
        val memoryPressure: Float
    )
    
    data class ModelCapabilityResult(
        val canLoadModel: Boolean,
        val recommendedAction: String,
        val memoryRequiredMB: Long,
        val memoryAvailableMB: Long
    )
    
    fun analyzeDeviceMemory(context: Context): MemoryAnalysis {
        optimizeMemoryForLargeModels(context)
        
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val maxHeapMB = runtime.maxMemory() / (1024 * 1024)
        val totalDeviceMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        
        val isLargeHeapEnabled = isLargeHeapEnabled(context)
        val isHighMemoryDevice = maxHeapMB > 1000 || totalDeviceMemoryMB > 6000
        
        Log.i(TAG, "Device Memory Analysis:")
        Log.i(TAG, "  Total Device Memory: ${totalDeviceMemoryMB}MB")
        Log.i(TAG, "  Available Memory: ${availableMemoryMB}MB")
        Log.i(TAG, "  Max Heap: ${maxHeapMB}MB")
        Log.i(TAG, "  Large Heap Enabled: $isLargeHeapEnabled")
        Log.i(TAG, "  High Memory Device: $isHighMemoryDevice")
        
        return MemoryAnalysis(
            totalDeviceMemoryMB = totalDeviceMemoryMB,
            availableMemoryMB = availableMemoryMB,
            maxHeapMB = maxHeapMB,
            isLargeHeapEnabled = isLargeHeapEnabled,
            isHighMemoryDevice = isHighMemoryDevice
        )
    }
    
    fun calculateModelLimits(analysis: MemoryAnalysis): ModelLimits {
        val maxHeapMB = analysis.maxHeapMB
        
        return if (analysis.isHighMemoryDevice) {
            ModelLimits(
                maxModelSizeMB = (maxHeapMB * 0.8).toLong(),
                warningThresholdMB = (maxHeapMB * 0.6).toLong(),
                deviceCategory = "high_memory",
                recommendation = "Device can handle large models up to ${(maxHeapMB * 0.8).toInt()}MB"
            )
        } else {
            ModelLimits(
                maxModelSizeMB = (maxHeapMB * 0.6).toLong(),
                warningThresholdMB = (maxHeapMB * 0.3).toLong(),
                deviceCategory = "standard_memory",
                recommendation = "Use models under ${(maxHeapMB * 0.6).toInt()}MB for optimal performance"
            )
        }
    }
    
    fun getConfigurationRecommendations(analysis: MemoryAnalysis): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (!analysis.isLargeHeapEnabled && analysis.totalDeviceMemoryMB > 4000) {
            recommendations.add("Enable largeHeap=\"true\" in AndroidManifest.xml for better performance")
        }
        
        if (analysis.isHighMemoryDevice && analysis.maxHeapMB < 2000) {
            recommendations.add("Consider increasing JVM heap size in gradle.properties")
        }
        
        if (analysis.availableMemoryMB < 1000) {
            recommendations.add("Close other apps before loading large models")
        }
        
        return recommendations
    }
    
    fun checkModelCapability(context: Context, modelSizeMB: Long): ModelCapabilityResult {
        val analysis = analyzeDeviceMemory(context)
        val limits = calculateModelLimits(analysis)
        
        return when {
            modelSizeMB <= limits.warningThresholdMB -> {
                ModelCapabilityResult(
                    canLoadModel = true,
                    recommendedAction = "Model can be loaded safely",
                    memoryRequiredMB = modelSizeMB,
                    memoryAvailableMB = analysis.maxHeapMB
                )
            }
            modelSizeMB <= limits.maxModelSizeMB -> {
                ModelCapabilityResult(
                    canLoadModel = true,
                    recommendedAction = "Model can be loaded but monitor memory usage",
                    memoryRequiredMB = modelSizeMB,
                    memoryAvailableMB = analysis.maxHeapMB
                )
            }
            else -> {
                ModelCapabilityResult(
                    canLoadModel = false,
                    recommendedAction = "Model too large for device. Use smaller model or enable large heap.",
                    memoryRequiredMB = modelSizeMB,
                    memoryAvailableMB = analysis.maxHeapMB
                )
            }
        }
    }
    
    fun getCurrentMemoryStatus(): MemoryStatus {
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
        val totalMemoryMB = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMB = runtime.freeMemory() / (1024 * 1024)
        val usedMemoryMB = totalMemoryMB - freeMemoryMB
        val availableMemoryMB = maxMemoryMB - usedMemoryMB
        val memoryPressure = usedMemoryMB.toFloat() / maxMemoryMB.toFloat()
        
        return MemoryStatus(
            usedMemoryMB = usedMemoryMB,
            freeMemoryMB = freeMemoryMB,
            maxMemoryMB = maxMemoryMB,
            availableMemoryMB = availableMemoryMB,
            memoryPressure = memoryPressure
        )
    }
    
    fun optimizeMemoryForLargeModels(context: Context) {
        try {
            System.gc()
            System.runFinalization()
            
            try {
                val vmRuntimeClass = Class.forName("dalvik.system.VMRuntime")
                val getRuntimeMethod = vmRuntimeClass.getMethod("getRuntime")
                val vmRuntime = getRuntimeMethod.invoke(null)
                
                val trimHeapMethod = vmRuntimeClass.getMethod("trimHeap")
                trimHeapMethod.invoke(vmRuntime)
                
                val setTargetHeapUtilizationMethod = vmRuntimeClass.getMethod(
                    "setTargetHeapUtilization", Float::class.javaPrimitiveType
                )
                setTargetHeapUtilizationMethod.invoke(vmRuntime, 0.9f)
                
                Log.d(TAG, "Memory optimization applied successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Advanced memory optimization not available: ${e.message}")
            }
            
            Thread.sleep(100)
            
        } catch (e: Exception) {
            Log.w(TAG, "Memory optimization failed: ${e.message}")
        }
    }
    
    fun performAdvancedMemoryCleanup() {
        try {
            val beforeMemory = getCurrentMemoryStatus()
            
            System.gc()
            System.runFinalization()
            
            try {
                val vmRuntimeClass = Class.forName("dalvik.system.VMRuntime")
                val getRuntimeMethod = vmRuntimeClass.getMethod("getRuntime")
                val vmRuntime = getRuntimeMethod.invoke(null)
                
                val trimHeapMethod = vmRuntimeClass.getMethod("trimHeap")
                trimHeapMethod.invoke(vmRuntime)
                
                Log.d(TAG, "Advanced memory cleanup completed")
            } catch (e: Exception) {
                Log.w(TAG, "Advanced cleanup not available: ${e.message}")
            }
            
            Thread.sleep(200)
            
            val afterMemory = getCurrentMemoryStatus()
            val freedMB = beforeMemory.usedMemoryMB - afterMemory.usedMemoryMB
            
            Log.i(TAG, "Memory cleanup freed ${freedMB}MB")
            
        } catch (e: Exception) {
            Log.w(TAG, "Memory cleanup failed: ${e.message}")
        }
    }
    
    private fun isLargeHeapEnabled(context: Context): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            (appInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
        } catch (e: Exception) {
            false
        }
    }
}

class MemoryMappedModelLoader(private val context: Context) {
    companion object {
        private const val TAG = "MemoryMappedModelLoader"
    }
    
    data class ModelInfo(
        val filePath: String,
        val sizeBytes: Long,
        val isMemoryMapped: Boolean,
        val mappedBuffer: MappedByteBuffer?
    )
    
    fun loadModelWithMemoryMapping(modelPath: String): ModelInfo {
        val file = File(modelPath)
        
        if (!file.exists()) {
            throw IllegalArgumentException("Model file does not exist: $modelPath")
        }
        
        val fileSize = file.length()
        
        return try {
            val randomAccessFile = RandomAccessFile(file, "r")
            val fileChannel = randomAccessFile.channel
            
            val mappedBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                fileSize
            )
            
            Log.i(TAG, "Successfully memory-mapped model: $modelPath (${fileSize / 1024 / 1024}MB)")
            
            ModelInfo(
                filePath = modelPath,
                sizeBytes = fileSize,
                isMemoryMapped = true,
                mappedBuffer = mappedBuffer
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to memory-map model, using regular file access: ${e.message}")
            
            ModelInfo(
                filePath = modelPath,
                sizeBytes = fileSize,
                isMemoryMapped = false,
                mappedBuffer = null
            )
        }
    }
    
    fun releaseMemoryMapping(modelInfo: ModelInfo) {
        try {
            modelInfo.mappedBuffer?.let { buffer ->
                try {
                    if (buffer.isDirect) {
                        val cleanerMethod = buffer::class.java.getMethod("cleaner")
                        cleanerMethod.isAccessible = true
                        val cleaner = cleanerMethod.invoke(buffer)
                        if (cleaner != null) {
                            val cleanMethod = cleaner::class.java.getMethod("clean")
                            cleanMethod.invoke(cleaner)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clean memory mapped buffer: ${e.message}")
                }
            }
            
            Log.i(TAG, "Released memory mapping for model: ${modelInfo.filePath}")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release memory mapping: ${e.message}")
        }
    }
}
