require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-mediapipe-llm"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "15.0" }
  s.source       = { :git => "https://github.com/sbhjt-gr/react-native-mediapipe-llm.git", :tag => "#{s.version}" }

  s.source_files = [
    "ios/**/*.{h,m,mm}"
  ]

  s.public_header_files = [
    "ios/**/*.h"
  ]

  # React Native dependencies
  s.dependency "React-Core"

  # MediaPipe Tasks GenAI framework
  s.dependency "MediaPipeTasksGenAI", "~> 0.10.14"

  # iOS Frameworks
  s.frameworks = [
    "Foundation", 
    "UIKit", 
    "CoreML",
    "Accelerate"
  ]

  # Compiler settings
  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES",
    "CLANG_CXX_LANGUAGE_STANDARD" => "c++17",
    "CLANG_CXX_LIBRARY" => "libc++",
    "OTHER_CPLUSPLUSFLAGS" => "-std=c++17"
  }

  # iOS specific settings
  s.ios.deployment_target = "15.0"
  s.swift_version = "5.0"

  # Library search paths
  s.library = "c++"
  
  # Weak frameworks for compatibility
  s.weak_frameworks = ["CoreML", "Vision"]
end 