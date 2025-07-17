# MediaPipe LLM Module ProGuard Rules

# Keep all MediaPipe classes
-keep class com.google.mediapipe.** { *; }
-keep class mediapipe.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep React Native bridge classes
-keep class com.reactnativemediapipellm.** { *; }
-keep class com.facebook.react.** { *; }

# Keep model loading classes
-keep class * extends com.google.mediapipe.tasks.** { *; }

# Memory optimization - keep memory-related classes
-keep class java.lang.Runtime { *; }
-keep class java.lang.System { *; }

# Prevent obfuscation of error messages for debugging
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Keep Kotlin metadata
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Optimize but don't remove unused code aggressively
-dontoptimize
-dontpreverify
