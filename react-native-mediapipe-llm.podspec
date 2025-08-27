require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

def min_ios_version_supported
  return "12.0"
end

Pod::Spec.new do |s|
  s.name         = "react-native-mediapipe-llm"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]
  s.source       = { :git => "https://github.com/sbhjt-gr/react-native-mediapipe-llm.git", :tag => "#{s.version}" }

  s.platforms    = { :ios => min_ios_version_supported }
  s.source_files = "ios/**/*.{h,m,mm,swift}"

  s.dependency 'MediaPipeTasksGenAI'

  if respond_to?(:install_modules_dependencies, true)
    install_modules_dependencies(s)
  else
    s.dependency "React-Core"

    if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
      s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
      s.pod_target_xcconfig = {
        "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"",
        "OTHER_CPLUSPLUSFLAGS" => "-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1",
        "CLANG_CXX_LANGUAGE_STANDARD" => "c++17",
        "DEFINES_MODULE" => "YES",
        "SWIFT_VERSION" => "5.0"
      }
      s.dependency "React-Codegen"
      s.dependency "RCT-Folly"
      s.dependency "RCTRequired"
      s.dependency "RCTTypeSafety"
      s.dependency "ReactCommon/turbomodule/core"
    else
      s.pod_target_xcconfig = {
        "DEFINES_MODULE" => "YES",
        "SWIFT_VERSION" => "5.0"
      }
    end
  end
end 