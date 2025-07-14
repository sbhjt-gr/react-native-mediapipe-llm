#pragma once

#ifdef __has_include
  #if __has_include(<jsi/jsi.h>)
    #define HAS_JSI 1
    #include <jsi/jsi.h>
  #else
    #define HAS_JSI 0
  #endif
#else
  #define HAS_JSI 0
#endif

#include <memory>
#include <string>
#include <unordered_map>
#include <functional>

#if HAS_JSI
extern "C" {
#include "mediapipe/mediapipe/tasks/cc/genai/inference/c/llm_inference_engine.h"
}
#endif

namespace mediapipe_llm {

#if HAS_JSI
using namespace facebook::jsi;
#else
// Minimal type definitions for validation builds
class Runtime {};
class Value {};
class Object {};
#endif

#if HAS_JSI
struct EngineWrapper {
    LlmInferenceEngine_Engine* engine;
    std::string engineId;
    
    EngineWrapper(LlmInferenceEngine_Engine* eng, const std::string& id)
        : engine(eng), engineId(id) {}
    
    ~EngineWrapper() {
        if (engine) {
            LlmInferenceEngine_Engine_Delete(engine);
        }
    }
};

struct SessionWrapper {
    LlmInferenceEngine_Session* session;
    std::string sessionId;
    std::string engineId;
    
    SessionWrapper(LlmInferenceEngine_Session* sess, const std::string& id, const std::string& engId)
        : session(sess), sessionId(id), engineId(engId) {}
    
    ~SessionWrapper() {
        if (session) {
            LlmInferenceEngine_Session_Delete(session);
        }
    }
};
#endif

class MediapipeLlm {
public:
    MediapipeLlm();
    ~MediapipeLlm();
    
#if HAS_JSI
    void install(Runtime& runtime);
    
private:
    std::unordered_map<std::string, std::unique_ptr<EngineWrapper>> engines_;
    std::unordered_map<std::string, std::unique_ptr<SessionWrapper>> sessions_;
    
    std::string generateId();
    
    Value createEngine(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value deleteEngine(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value createSession(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value deleteSession(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value updateRuntimeConfig(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value addQueryChunk(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value addImage(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value addAudio(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value predictSync(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value predictAsync(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value cloneSession(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value sizeInTokens(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value cancelPendingProcess(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    Value multiply(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count);
    
    LlmModelSettings parseModelSettings(Runtime& runtime, const Object& settings);
    LlmSessionConfig parseSessionConfig(Runtime& runtime, const Object& config);
    SessionRuntimeConfig parseRuntimeConfig(Runtime& runtime, const Object& config);
    LlmPromptTemplates parsePromptTemplates(Runtime& runtime, const Object& templates);
    Object createResponseObject(Runtime& runtime, const LlmResponseContext& response);
#else
    // Minimal interface for validation builds
    void validateBuild() { /* Basic validation */ }
#endif
    
#ifdef __ANDROID__
    void setupAndroidImageLoader();
    std::string loadImageFromUri(const std::string& uri);
#endif
    
#ifdef __APPLE__
    void setupiOSImageLoader();
    std::string loadImageFromUri(const std::string& uri);
#endif
};

} // namespace mediapipe_llm 