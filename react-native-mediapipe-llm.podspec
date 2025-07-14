require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

Pod::Spec.new do |s|
  s.name         = "react-native-mediapipe-llm"
  s.version      = "1.0.2"
  s.summary      = "React Native binding for Google AI Edge Gallery's MediaPipe on-device LLM inference engine"
  s.homepage     = "https://github.com/sbhjt-gr/react-native-mediapipe-llm"
  s.license      = "Apache-2.0"
  s.authors      = "Subhajit Gorai <sage_mastermind@outlook.com>"
  s.source       = { :git => "https://github.com/sbhjt-gr/react-native-mediapipe-llm.git", :tag => "#{s.version}" }

  s.platforms    = { :ios => "13.0" }
  s.source_files = "ios/**/*.{h,m,mm,swift}"

  s.dependency 'MediaPipeTasksGenAI'

  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES",
    "SWIFT_VERSION" => "5.0"
  }

  s.dependency "React-Core"
end 