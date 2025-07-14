cmake_minimum_required(VERSION 3.13.0)

set(CMAKE_VERBOSE_MAKEFILE ON)
set(CMAKE_CXX_STANDARD 17)

project(MediapipeLlm)

# Check if we're building within a React Native project context
# If not, skip React Native dependencies for validation builds
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/../node_modules/react-native")
    set(RN_BUILD_CONTEXT TRUE)
else()
    set(RN_BUILD_CONTEXT FALSE)
endif()

# Find required packages only when in React Native build context
if(RN_BUILD_CONTEXT)
    find_package(ReactAndroid REQUIRED CONFIG)
    find_package(fbjni REQUIRED CONFIG)
endif()

# Add MediaPipe subdirectory
set(MEDIAPIPE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/mediapipe)

# Include directories
include_directories(
    ${MEDIAPIPE_DIR}
    ${MEDIAPIPE_DIR}/mediapipe
    ${CMAKE_CURRENT_SOURCE_DIR}/cpp
)

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
set(MEDIAPIPE_LLM_SOURCES
    ${MEDIAPIPE_DIR}/mediapipe/tasks/cc/genai/inference/c/llm_inference_engine_cpu.cc
    ${MEDIAPIPE_DIR}/mediapipe/calculators/tensor/inference_calculator_utils.cc
    ${MEDIAPIPE_DIR}/mediapipe/calculators/tflite/tflite_inference_calculator.cc
    ${MEDIAPIPE_DIR}/mediapipe/framework/calculator_base.cc
    ${MEDIAPIPE_DIR}/mediapipe/framework/calculator_graph.cc
    ${MEDIAPIPE_DIR}/mediapipe/framework/packet.cc
    ${MEDIAPIPE_DIR}/mediapipe/framework/timestamp.cc
    ${MEDIAPIPE_DIR}/mediapipe/framework/formats/tensor.cc
)

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
    target_link_libraries(${CMAKE_PROJECT_NAME}
        ReactAndroid::react_nativejni
        ReactAndroid::react_render_core
        ReactAndroid::rrc_view
        ReactAndroid::turbomodulejsijni
        fbjni::fbjni
        ${PLATFORM_LIBS}
    )
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