cmake_minimum_required(VERSION 3.13.0)

set(CMAKE_VERBOSE_MAKEFILE ON)
set(CMAKE_CXX_STANDARD 17)

project(MediapipeLlm)

# Check if we're building within a React Native project context
# Look for React Native in multiple possible locations
set(RN_BUILD_CONTEXT FALSE)
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/../node_modules/react-native")
    set(RN_BUILD_CONTEXT TRUE)
    message(STATUS "Found React Native at ../node_modules/react-native")
elseif(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/../../react-native")
    set(RN_BUILD_CONTEXT TRUE)
    message(STATUS "Found React Native at ../../react-native")
elseif(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/../../../react-native")
    set(RN_BUILD_CONTEXT TRUE)
    message(STATUS "Found React Native at ../../../react-native")
elseif(ANDROID)
    # For Android builds, assume React Native context if building with gradle
    set(RN_BUILD_CONTEXT TRUE)
    message(STATUS "Android build detected, assuming React Native context")
else()
    message(STATUS "React Native not found, building validation-only library")
endif()

# Find required packages only when in React Native build context
if(RN_BUILD_CONTEXT AND ANDROID)
    # For Android React Native builds, find the prebuilt libraries
    find_library(LOG_LIB log)
    find_library(REACT_NATIVE_JNI_LIB reactnativejni)
    find_library(FBJNI_LIB fbjni)
    
    # If we can't find the libraries, try alternative approach
    if(NOT REACT_NATIVE_JNI_LIB OR NOT FBJNI_LIB)
        message(STATUS "Using alternative React Native library discovery")
        set(REACT_NATIVE_JNI_LIB "reactnativejni")
        set(FBJNI_LIB "fbjni")
    endif()
    
    # Find React Native include directories
    find_path(REACT_NATIVE_DIR 
        NAMES react-native.config.js
        PATHS 
            "${CMAKE_CURRENT_SOURCE_DIR}/../node_modules/react-native"
            "${CMAKE_CURRENT_SOURCE_DIR}/../../react-native" 
            "${CMAKE_CURRENT_SOURCE_DIR}/../../../react-native"
    )
    
    if(REACT_NATIVE_DIR)
        message(STATUS "Found React Native directory: ${REACT_NATIVE_DIR}")
        # Add React Native and fbjni include paths
        include_directories(
            "${REACT_NATIVE_DIR}/ReactCommon/jsi"
            "${REACT_NATIVE_DIR}/ReactCommon"
            "${REACT_NATIVE_DIR}/ReactAndroid/src/main/java/com/facebook/jni/cxx"
            "${REACT_NATIVE_DIR}/ReactAndroid/src/main/jni"
            "${REACT_NATIVE_DIR}/ReactAndroid/src/main/jni/first-party/fbjni/cxx"
            "${REACT_NATIVE_DIR}/third-party/glog-0.3.5/src"
        )
    endif()
    
    message(STATUS "React Native Android libraries configured")
elseif(RN_BUILD_CONTEXT)
    find_package(ReactAndroid REQUIRED CONFIG)
    find_package(fbjni REQUIRED CONFIG)
    message(STATUS "React Native packages found successfully")
endif()

# Add MediaPipe subdirectory
set(MEDIAPIPE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/mediapipe)

# Include directories
include_directories(
    ${MEDIAPIPE_DIR}
    ${MEDIAPIPE_DIR}/mediapipe
    ${CMAKE_CURRENT_SOURCE_DIR}/cpp
)

# Add external dependencies include paths
if(ANDROID)
    # Add bazel-built external dependencies for Android
    set(BAZEL_BIN_DIR "${MEDIAPIPE_DIR}/bazel-bin")
    set(BAZEL_GENFILES_DIR "${MEDIAPIPE_DIR}/bazel-genfiles")
    
    # Add common external dependency paths
    include_directories(
        ${MEDIAPIPE_DIR}/third_party/abseil-cpp
        ${MEDIAPIPE_DIR}/third_party/protobuf/src
        ${MEDIAPIPE_DIR}/third_party/eigen3
        ${MEDIAPIPE_DIR}/third_party/libyuv/include
        ${MEDIAPIPE_DIR}/third_party/opencv/include
    )
    
    # Try to find system-level external dependencies if not found in MediaPipe
    find_path(ABSL_INCLUDE_DIR 
        NAMES absl/container/flat_hash_map.h
        PATHS 
            ${MEDIAPIPE_DIR}/third_party/abseil-cpp
            /usr/local/include
            /usr/include
    )
    
    if(ABSL_INCLUDE_DIR)
        include_directories(${ABSL_INCLUDE_DIR})
        message(STATUS "Found Abseil headers at: ${ABSL_INCLUDE_DIR}")
    endif()
endif()

# Compile options based on platform
if(ANDROID)
    # Android-specific settings
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DMEDIAPIPE_ANDROID=1")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DMEDIAPIPE_MOBILE=1")
    
    # GPU acceleration flags
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DMEDIAPIPE_GPU_BUFFER_USE_AHWB=1")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DMEDIAPIPE_TENSOR_USE_AHWB=1")
    
    # OpenGL ES support
    find_library(GLES_LIB GLESv3)
    find_library(EGL_LIB EGL)
    find_library(ANDROID_LIB android)
    find_library(LOG_LIB log)
    
    set(PLATFORM_LIBS ${GLES_LIB} ${EGL_LIB} ${ANDROID_LIB} ${LOG_LIB})
    
elseif(IOS)
    # iOS-specific settings
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DMEDIAPIPE_IOS=1")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DMEDIAPIPE_MOBILE=1")
    
    # Metal support
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DMEDIAPIPE_METAL_ENABLED=1")
    
    # iOS frameworks
    find_library(FOUNDATION_FRAMEWORK Foundation)
    find_library(METAL_FRAMEWORK Metal)
    find_library(METALKIT_FRAMEWORK MetalKit)
    find_library(ACCELERATE_FRAMEWORK Accelerate)
    
    set(PLATFORM_LIBS 
        ${FOUNDATION_FRAMEWORK} 
        ${METAL_FRAMEWORK} 
        ${METALKIT_FRAMEWORK}
        ${ACCELERATE_FRAMEWORK}
    )
endif()

# Optimization flags
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O0 -g")

# MediaPipe LLM Inference Engine sources
# For now, create a stub implementation since MediaPipe dependencies require Bazel
set(MEDIAPIPE_LLM_SOURCES
    # Use a stub implementation that can be compiled without full MediaPipe build
    cpp/MediapipeStub.cpp
    # ${MEDIAPIPE_DIR}/mediapipe/tasks/cc/genai/inference/c/llm_inference_engine_cpu.cc
)

# For Android, we need to be more selective with TensorFlow Lite sources
if(ANDROID)
    # Core TensorFlow Lite sources (essential ones only to avoid conflicts)
    set(CORE_TFLITE_SOURCES
        ${MEDIAPIPE_DIR}/third_party/tensorflow/tensorflow/lite/core/api/tensor_utils.cc
        ${MEDIAPIPE_DIR}/third_party/tensorflow/tensorflow/lite/kernels/internal/utils.cc
    )
    
    # Only add if files exist to avoid build errors
    foreach(SOURCE_FILE ${CORE_TFLITE_SOURCES})
        if(EXISTS ${SOURCE_FILE})
            list(APPEND MEDIAPIPE_LLM_SOURCES ${SOURCE_FILE})
        endif()
    endforeach()
    
    message(STATUS "Android build: Added selective TensorFlow Lite sources")
endif()

# React Native wrapper sources
set(RN_WRAPPER_SOURCES
    cpp/MediapipeLlm.cpp
    cpp/JSI_Helpers.cpp
)

if(ANDROID)
    list(APPEND RN_WRAPPER_SOURCES cpp/android/MediapipeLlm_Android.cpp)
elseif(IOS)
    list(APPEND RN_WRAPPER_SOURCES cpp/ios/MediapipeLlm_iOS.mm)
endif()

# Create the library
if(RN_BUILD_CONTEXT)
    # Full React Native library
    add_library(${CMAKE_PROJECT_NAME} SHARED
        ${MEDIAPIPE_LLM_SOURCES}
        ${RN_WRAPPER_SOURCES}
    )
    
    # Link libraries
    if(ANDROID)
        target_link_libraries(${CMAKE_PROJECT_NAME}
            ${REACT_NATIVE_JNI_LIB}
            ${FBJNI_LIB}
            ${LOG_LIB}
            ${PLATFORM_LIBS}
        )
    else()
        target_link_libraries(${CMAKE_PROJECT_NAME}
            ReactAndroid::react_nativejni
            ReactAndroid::react_render_core
            ReactAndroid::rrc_view
            ReactAndroid::turbomodulejsijni
            fbjni::fbjni
            ${PLATFORM_LIBS}
        )
    endif()
else()
    # Validation-only static library
    add_library(${CMAKE_PROJECT_NAME} STATIC
        ${RN_WRAPPER_SOURCES}
    )
    
    # Link only platform libraries for validation
    if(PLATFORM_LIBS)
        target_link_libraries(${CMAKE_PROJECT_NAME} ${PLATFORM_LIBS})
    endif()
endif()

# Preprocessor definitions
if(RN_BUILD_CONTEXT)
    target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
        WITH_INSPECTOR=1
        HERMES_ENABLE_DEBUGGER=1
    )
endif()

# Include TensorFlow Lite
find_path(TENSORFLOW_LITE_INCLUDE_DIR 
    NAMES tensorflow/lite/interpreter.h
    PATHS ${MEDIAPIPE_DIR}/third_party/tensorflow
)

if(TENSORFLOW_LITE_INCLUDE_DIR)
    target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE ${TENSORFLOW_LITE_INCLUDE_DIR})
endif()

# Platform-specific linking
if(RN_BUILD_CONTEXT)
    if(ANDROID)
        target_link_libraries(${CMAKE_PROJECT_NAME} ${ANDROID_LIB} ${LOG_LIB})
        
        # Set proper library name for Android JNI
        set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES
            OUTPUT_NAME "MediapipeLlm"
        )
        
        # XNNPACK delegate for CPU acceleration
        find_library(XNNPACK_LIB xnnpack)
        if(XNNPACK_LIB)
            target_link_libraries(${CMAKE_PROJECT_NAME} ${XNNPACK_LIB})
        endif()
        
        # GPU delegate
        find_library(TENSORFLOW_LITE_GPU_DELEGATE_LIB tensorflowlite_gpu_delegate)
        if(TENSORFLOW_LITE_GPU_DELEGATE_LIB)
            target_link_libraries(${CMAKE_PROJECT_NAME} ${TENSORFLOW_LITE_GPU_DELEGATE_LIB})
        endif()
        
    elseif(IOS)
        set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES
            FRAMEWORK TRUE
            MACOSX_FRAMEWORK_IDENTIFIER com.reactnative.mediapipellm
        )
    endif()
endif() 