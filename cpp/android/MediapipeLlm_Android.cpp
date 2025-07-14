#include "../MediapipeLlm.h"
#include <jni.h>
#include <android/log.h>
#include <fbjni/fbjni.h>
#include <ReactCommon/CallInvokerHolder.h>
#include <ReactCommon/TurboModule.h>
#include <jsi/jsi.h>

#define LOG_TAG "MediapipeLlm"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace mediapipe_llm {

void MediapipeLlm::setupAndroidImageLoader() {
    LOGI("Setting up Android image loader");
}

std::string MediapipeLlm::loadImageFromUri(const std::string& uri) {
    LOGI("Loading image from URI: %s", uri.c_str());
    return uri;
}

class MediapipeLlmModule : public facebook::react::TurboModule {
private:
    std::shared_ptr<MediapipeLlm> module_;
    
public:
    MediapipeLlmModule(const facebook::react::TurboModuleSpec& spec)
        : TurboModule(spec) {
        module_ = std::make_shared<MediapipeLlm>();
    }
    
    facebook::jsi::Value get(facebook::jsi::Runtime& runtime, const facebook::jsi::PropNameID& propName) override {
        auto name = propName.utf8(runtime);
        
        if (name == "install") {
            return facebook::jsi::Function::createFromHostFunction(
                runtime,
                facebook::jsi::PropNameID::forAscii(runtime, "install"),
                0,
                [this](facebook::jsi::Runtime& runtime, const facebook::jsi::Value& thisValue, const facebook::jsi::Value* arguments, size_t count) -> facebook::jsi::Value {
                    module_->install(runtime);
                    return facebook::jsi::Value::undefined();
                }
            );
        }
        
        return facebook::jsi::Value::undefined();
    }
};

}

using namespace facebook;

class MediapipeLlmTurboModuleProvider : public react::TurboModuleProvider {
public:
    std::shared_ptr<react::TurboModule> getModule(const std::string& name, std::shared_ptr<react::CallInvoker> jsInvoker) override {
        if (name == "MediapipeLlm") {
            react::TurboModuleSpec spec;
            spec.jsInvoker = jsInvoker;
            return std::make_shared<mediapipe_llm::MediapipeLlmModule>(spec);
        }
        return nullptr;
    }
    
    std::vector<std::string> getEagerInitModuleNames() override {
        return {};
    }
};

extern "C" JNIEXPORT void JNICALL
Java_com_reactnativemediapipellm_MediapipeLlmModule_nativeInstall(JNIEnv *env, jobject thiz, jlong jsi) {
    auto runtime = reinterpret_cast<jsi::Runtime*>(jsi);
    auto module = std::make_shared<mediapipe_llm::MediapipeLlm>();
    module->install(*runtime);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_reactnativemediapipellm_MediapipeLlmModule_nativeMultiply(JNIEnv *env, jobject thiz, jdouble a, jdouble b) {
    return a * b;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    return facebook::jni::initialize(vm, [] {
        LOGI("MediapipeLlm JNI_OnLoad");
    });
} 