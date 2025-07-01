require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

Pod::Spec.new do |s|
  s.name         = "react-native-mediapipe-llm"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "11.0" }
  s.source       = { :git => "https://github.com/yourusername/react-native-mediapipe-llm.git", :tag => "#{s.version}" }

  s.source_files = [
    "ios/**/*.{h,m,mm}",
    "cpp/**/*.{h,hpp,cpp,mm}",
    "mediapipe/mediapipe/tasks/cc/genai/inference/c/*.{h,cc}",
    "mediapipe/mediapipe/calculators/tensor/*.{h,cc}",
    "mediapipe/mediapipe/calculators/tflite/*.{h,cc}",
    "mediapipe/mediapipe/framework/*.{h,cc}",
    "mediapipe/mediapipe/framework/formats/*.{h,cc}"
  ]

  s.public_header_files = [
    "cpp/MediapipeLlm.h"
  ]

  # MediaPipe Framework
  s.vendored_frameworks = "mediapipe/mediapipe/objc/MediaPipe.framework"

  s.preserve_paths = [
    "mediapipe/**/*"
  ]

  # Compiler settings
  s.compiler_flags = folly_compiler_flags + ' -DMEDIAPIPE_IOS=1 -DMEDIAPIPE_MOBILE=1 -DMEDIAPIPE_METAL_ENABLED=1'
  
  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES",
    "HEADER_SEARCH_PATHS" => "\"$(PODS_TARGET_SRCROOT)/mediapipe\" \"$(PODS_TARGET_SRCROOT)/mediapipe/mediapipe\" \"$(PODS_TARGET_SRCROOT)/cpp\"",
    "OTHER_CPLUSPLUSFLAGS" => "-DMEDIAPIPE_IOS=1 -DMEDIAPIPE_MOBILE=1 -DMEDIAPIPE_METAL_ENABLED=1 -std=c++17",
    "CLANG_CXX_LANGUAGE_STANDARD" => "c++17",
    "CLANG_CXX_LIBRARY" => "libc++",
    "ENABLE_BITCODE" => "NO"
  }

  # iOS Frameworks
  s.frameworks = [
    "Foundation", 
    "UIKit", 
    "CoreGraphics", 
    "CoreImage",
    "AVFoundation",
    "CoreVideo",
    "CoreMedia",
    "Metal",
    "MetalKit",
    "Accelerate"
  ]

  # TensorFlow Lite
  s.dependency "TensorFlowLiteObjC", "~> 2.13.0"
  s.dependency "TensorFlowLiteObjC/Metal", "~> 2.13.0"
  
  # React Native dependencies
  s.dependency "React-Core"
  s.dependency "React-callinvoker"
  s.dependency "React-jsi"
  s.dependency "ReactCommon/turbomodule/core"

  # Exclude conflicting files
  s.exclude_files = [
    "mediapipe/**/test/**/*",
    "mediapipe/**/*_test.*",
    "mediapipe/**/*_benchmark.*"
  ]

  # iOS specific settings
  s.ios.deployment_target = "11.0"
  s.swift_version = "5.0"

  # Library search paths
  s.library = "c++"
  
  # Weak frameworks for compatibility
  s.weak_frameworks = ["CoreML", "Vision"]
end 