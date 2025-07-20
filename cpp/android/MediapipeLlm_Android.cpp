#include <jni.h>
#include <string>
#include <android/log.h>
#include "../MediapipeLlm.h"

#define LOG_TAG "MediapipeLlm"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// External function declarations from stub
extern "C" {
    void* LlmInferenceEngineCreate(const char* model_path);
    const char* LlmInferenceEngineGenerateResponse(void* engine, const char* prompt);
    void LlmInferenceEngineDelete(void* engine);
}

extern "C" {

// JNI function implementations for MediapipeLlmModule

JNIEXPORT jlong JNICALL 
Java_com_reactnativemediapipellm_MediapipeLlmModule_nativeCreateEngine(
    JNIEnv *env, 
    jobject thiz, 
    jstring modelPath
) {
    const char* model_path_c = env->GetStringUTFChars(modelPath, nullptr);
    
    LOGI("Creating LLM engine with model: %s", model_path_c);
    
    // Call the stub implementation
    void* engine = LlmInferenceEngineCreate(model_path_c);
    
    env->ReleaseStringUTFChars(modelPath, model_path_c);
    
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT jstring JNICALL 
Java_com_reactnativemediapipellm_MediapipeLlmModule_nativeGenerateResponse(
    JNIEnv *env, 
    jobject thiz, 
    jlong enginePtr, 
    jstring prompt
) {
    const char* prompt_c = env->GetStringUTFChars(prompt, nullptr);
    
    LOGI("Generating response for prompt: %s", prompt_c);
    
    void* engine = reinterpret_cast<void*>(enginePtr);
    const char* response = LlmInferenceEngineGenerateResponse(engine, prompt_c);
    
    env->ReleaseStringUTFChars(prompt, prompt_c);
    
    return env->NewStringUTF(response);
}

JNIEXPORT void JNICALL 
Java_com_reactnativemediapipellm_MediapipeLlmModule_nativeDeleteEngine(
    JNIEnv *env, 
    jobject thiz, 
    jlong enginePtr
) {
    LOGI("Deleting LLM engine");
    
    void* engine = reinterpret_cast<void*>(enginePtr);
    LlmInferenceEngineDelete(engine);
}

}
        settings.model_path = path;
        settings.max_num_tokens = max_tokens;
        settings.max_top_k = top_k;
        
        LlmInferenceEngine_Engine* engine = nullptr;
        char* error_msg = nullptr;
        
        int result = LlmInferenceEngine_CreateEngine(&settings, &engine, &error_msg);
        
        env->ReleaseStringUTFChars(model_path, path);
        
        if (result == 0 && engine != nullptr) {
            // Store engine pointer as string ID
            char engine_id[32];
            snprintf(engine_id, sizeof(engine_id), "%p", engine);
            return env->NewStringUTF(engine_id);
        } else {
            jstring error = env->NewStringUTF(error_msg ? error_msg : "Unknown error");
            if (error_msg) {
                LlmInferenceEngine_CloseErrorMessage(error_msg);
            }
            return error;
        }
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(model_path, path);
        return env->NewStringUTF(e.what());
    }
}

// JNI method to generate response
extern "C" JNIEXPORT jstring JNICALL
Java_com_reactnativemediapipellm_MediapipeLlmModule_nativeGenerateResponse(
    JNIEnv *env, jobject thiz, jstring engine_id, jstring prompt) {
    
    const char *id_str = env->GetStringUTFChars(engine_id, nullptr);
    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);
    
    try {
        // Convert string back to pointer
        LlmInferenceEngine_Engine* engine = nullptr;
        sscanf(id_str, "%p", &engine);
        
        if (!engine) {
            env->ReleaseStringUTFChars(engine_id, id_str);
            env->ReleaseStringUTFChars(prompt, prompt_str);
            return env->NewStringUTF("Invalid engine");
        }
        
        LlmSessionConfig config = {};
        config.topk = 40;  // Default value
        config.temperature = 0.8f;  // Default value
        config.random_seed = 0;  // Default value
        
        LlmInferenceEngine_Session* session = nullptr;
        char* error_msg = nullptr;
        
        int result = LlmInferenceEngine_CreateSession(engine, &config, &session, &error_msg);
        
        if (result != 0 || !session) {
            env->ReleaseStringUTFChars(engine_id, id_str);
            env->ReleaseStringUTFChars(prompt, prompt_str);
            jstring error = env->NewStringUTF(error_msg ? error_msg : "Failed to create session");
            if (error_msg) {
                LlmInferenceEngine_CloseErrorMessage(error_msg);
            }
            return error;
        }
        
        // Add query
        result = LlmInferenceEngine_Session_AddQueryChunk(session, prompt_str, &error_msg);
        if (result != 0) {
            LlmInferenceEngine_Session_Delete(session);
            env->ReleaseStringUTFChars(engine_id, id_str);
            env->ReleaseStringUTFChars(prompt, prompt_str);
            jstring error = env->NewStringUTF(error_msg ? error_msg : "Failed to add query");
            if (error_msg) {
                LlmInferenceEngine_CloseErrorMessage(error_msg);
            }
            return error;
        }
        
        // Generate response
        LlmResponseContext response = {};
        result = LlmInferenceEngine_Session_PredictSync(session, &response, &error_msg);
        
        env->ReleaseStringUTFChars(engine_id, id_str);
        env->ReleaseStringUTFChars(prompt, prompt_str);
        
        if (result == 0 && response.response_array && response.response_count > 0) {
            jstring response_str = env->NewStringUTF(response.response_array[0]);
            LlmInferenceEngine_CloseResponseContext(&response);
            LlmInferenceEngine_Session_Delete(session);
            return response_str;
        } else {
            LlmInferenceEngine_Session_Delete(session);
            jstring error = env->NewStringUTF(error_msg ? error_msg : "Generation failed");
            if (error_msg) {
                LlmInferenceEngine_CloseErrorMessage(error_msg);
            }
            return error;
        }
        
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(engine_id, id_str);
        env->ReleaseStringUTFChars(prompt, prompt_str);
        return env->NewStringUTF(e.what());
    }
}

// JNI method to delete engine
extern "C" JNIEXPORT void JNICALL
Java_com_reactnativemediapipellm_MediapipeLlmModule_nativeDeleteEngine(
    JNIEnv *env, jobject thiz, jstring engine_id) {
    
    const char *id_str = env->GetStringUTFChars(engine_id, nullptr);
    
    try {
        LlmInferenceEngine_Engine* engine = nullptr;
        sscanf(id_str, "%p", &engine);
        
        if (engine) {
            LlmInferenceEngine_Engine_Delete(engine);
        }
    } catch (const std::exception& e) {
        LOGE("Error deleting engine: %s", e.what());
    }
    
    env->ReleaseStringUTFChars(engine_id, id_str);
}

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