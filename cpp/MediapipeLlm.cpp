#include "MediapipeLlm.h"
#include "JSI_Helpers.h"
#include <random>
#include <sstream>
#include <thread>

namespace mediapipe_llm {

MediapipeLlm::MediapipeLlm() {
#if HAS_JSI
#ifdef __ANDROID__
    setupAndroidImageLoader();
#endif
#ifdef __APPLE__
    setupiOSImageLoader();
#endif
#endif
}

MediapipeLlm::~MediapipeLlm() {
#if HAS_JSI
    sessions_.clear();
    engines_.clear();
#endif
}

#if HAS_JSI
void MediapipeLlm::install(Runtime& runtime) {
    auto mediapipeLlm = Object(runtime);
    
    mediapipeLlm.setProperty(runtime, "createEngine",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "createEngine"), 1,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return createEngine(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "deleteEngine",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "deleteEngine"), 1,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return deleteEngine(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "createSession",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "createSession"), 2,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return createSession(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "deleteSession",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "deleteSession"), 1,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return deleteSession(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "updateRuntimeConfig",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "updateRuntimeConfig"), 2,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return updateRuntimeConfig(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "addQueryChunk",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "addQueryChunk"), 2,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return addQueryChunk(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "addImage",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "addImage"), 2,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return addImage(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "addAudio",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "addAudio"), 2,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return addAudio(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "predictSync",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "predictSync"), 1,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return predictSync(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "predictAsync",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "predictAsync"), 2,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return predictAsync(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "cloneSession",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "cloneSession"), 1,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return cloneSession(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "sizeInTokens",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "sizeInTokens"), 2,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return sizeInTokens(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "cancelPendingProcess",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "cancelPendingProcess"), 1,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return cancelPendingProcess(runtime, thisValue, arguments, count);
            }));
    
    mediapipeLlm.setProperty(runtime, "multiply",
        Function::createFromHostFunction(runtime, PropNameID::forAscii(runtime, "multiply"), 2,
            [this](Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) -> Value {
                return multiply(runtime, thisValue, arguments, count);
            }));
    
    runtime.global().setProperty(runtime, "MediapipeLlm", mediapipeLlm);
}
#endif

#if HAS_JSI
std::string MediapipeLlm::generateId() {
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 15);
    std::stringstream ss;
    
    for (int i = 0; i < 16; ++i) {
        ss << std::hex << dis(gen);
    }
    
    return ss.str();
}

Value MediapipeLlm::createEngine(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) {
    if (count == 0 || !arguments[0].isObject()) {
        throw JSError(runtime, "createEngine requires a settings object");
    }
    
    auto settings = parseModelSettings(runtime, arguments[0].asObject(runtime));
    
    LlmInferenceEngine_Engine* engine = nullptr;
    char* error_msg = nullptr;
    
    int result = LlmInferenceEngine_CreateEngine(&settings, &engine, &error_msg);
    
    if (result != 0 || engine == nullptr) {
        std::string errorStr = error_msg ? error_msg : "Unknown error creating engine";
        if (error_msg) free(error_msg);
        throw JSError(runtime, "Failed to create engine: " + errorStr);
    }
    
    std::string engineId = generateId();
    engines_[engineId] = std::make_unique<EngineWrapper>(engine, engineId);
    
    return String::createFromUtf8(runtime, engineId);
}

Value MediapipeLlm::deleteEngine(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) {
    if (count == 0 || !arguments[0].isString()) {
        throw JSError(runtime, "deleteEngine requires an engine ID string");
    }
    
    std::string engineId = arguments[0].asString(runtime).utf8(runtime);
    
    auto it = engines_.find(engineId);
    if (it != engines_.end()) {
        for (auto sessionIt = sessions_.begin(); sessionIt != sessions_.end();) {
            if (sessionIt->second->engineId == engineId) {
                sessionIt = sessions_.erase(sessionIt);
            } else {
                ++sessionIt;
            }
        }
        engines_.erase(it);
    }
    
    return Value::undefined();
}

Value MediapipeLlm::createSession(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) {
    if (count < 2 || !arguments[0].isString() || !arguments[1].isObject()) {
        throw JSError(runtime, "createSession requires engine ID and config object");
    }
    
    std::string engineId = arguments[0].asString(runtime).utf8(runtime);
    auto engineIt = engines_.find(engineId);
    if (engineIt == engines_.end()) {
        throw JSError(runtime, "Engine not found");
    }
    
    auto config = parseSessionConfig(runtime, arguments[1].asObject(runtime));
    
    LlmInferenceEngine_Session* session = nullptr;
    char* error_msg = nullptr;
    
    int result = LlmInferenceEngine_CreateSession(engineIt->second->engine, &config, &session, &error_msg);
    
    if (result != 0 || session == nullptr) {
        std::string errorStr = error_msg ? error_msg : "Unknown error creating session";
        if (error_msg) free(error_msg);
        throw JSError(runtime, "Failed to create session: " + errorStr);
    }
    
    std::string sessionId = generateId();
    sessions_[sessionId] = std::make_unique<SessionWrapper>(session, sessionId, engineId);
    
    return String::createFromUtf8(runtime, sessionId);
}

Value MediapipeLlm::predictSync(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) {
    if (count == 0 || !arguments[0].isString()) {
        throw JSError(runtime, "predictSync requires a session ID string");
    }
    
    std::string sessionId = arguments[0].asString(runtime).utf8(runtime);
    auto sessionIt = sessions_.find(sessionId);
    if (sessionIt == sessions_.end()) {
        throw JSError(runtime, "Session not found");
    }
    
    LlmResponseContext response = {};
    char* error_msg = nullptr;
    
    int result = LlmInferenceEngine_Session_PredictSync(sessionIt->second->session, &response, &error_msg);
    
    if (result != 0) {
        std::string errorStr = error_msg ? error_msg : "Unknown error during prediction";
        if (error_msg) free(error_msg);
        throw JSError(runtime, "Prediction failed: " + errorStr);
    }
    
    auto responseObj = createResponseObject(runtime, response);
    LlmInferenceEngine_CloseResponseContext(&response);
    
    return responseObj;
}

Value MediapipeLlm::multiply(Runtime& runtime, const Value& thisValue, const Value* arguments, size_t count) {
    if (count < 2 || !arguments[0].isNumber() || !arguments[1].isNumber()) {
        throw JSError(runtime, "multiply requires two numbers");
    }
    
    double a = arguments[0].asNumber();
    double b = arguments[1].asNumber();
    
    return Value(a * b);
}

LlmModelSettings MediapipeLlm::parseModelSettings(Runtime& runtime, const Object& settings) {
    LlmModelSettings modelSettings = {};
    
    if (settings.hasProperty(runtime, "modelPath")) {
        auto modelPath = settings.getProperty(runtime, "modelPath").asString(runtime).utf8(runtime);
        modelSettings.model_path = strdup(modelPath.c_str());
    }
    
    if (settings.hasProperty(runtime, "maxNumTokens")) {
        modelSettings.max_num_tokens = static_cast<size_t>(settings.getProperty(runtime, "maxNumTokens").asNumber());
    } else {
        modelSettings.max_num_tokens = 2048;
    }
    
    if (settings.hasProperty(runtime, "maxNumImages")) {
        modelSettings.max_num_images = static_cast<size_t>(settings.getProperty(runtime, "maxNumImages").asNumber());
    }
    
    if (settings.hasProperty(runtime, "activationDataType")) {
        modelSettings.llm_activation_data_type = static_cast<LlmActivationDataType>(
            static_cast<int>(settings.getProperty(runtime, "activationDataType").asNumber())
        );
    }
    
    if (settings.hasProperty(runtime, "preferredBackend")) {
        modelSettings.preferred_backend = static_cast<LlmPreferredBackend>(
            static_cast<int>(settings.getProperty(runtime, "preferredBackend").asNumber())
        );
    }
    
    return modelSettings;
}

LlmSessionConfig MediapipeLlm::parseSessionConfig(Runtime& runtime, const Object& config) {
    LlmSessionConfig sessionConfig = {};
    
    if (config.hasProperty(runtime, "topK")) {
        sessionConfig.topk = static_cast<size_t>(config.getProperty(runtime, "topK").asNumber());
    }
    
    if (config.hasProperty(runtime, "topP")) {
        sessionConfig.topp = static_cast<float>(config.getProperty(runtime, "topP").asNumber());
    }
    
    if (config.hasProperty(runtime, "temperature")) {
        sessionConfig.temperature = static_cast<float>(config.getProperty(runtime, "temperature").asNumber());
    }
    
    if (config.hasProperty(runtime, "randomSeed")) {
        sessionConfig.random_seed = static_cast<size_t>(config.getProperty(runtime, "randomSeed").asNumber());
    }
    
    return sessionConfig;
}

Object MediapipeLlm::createResponseObject(Runtime& runtime, const LlmResponseContext& response) {
    auto responseObj = Object(runtime);
    
    auto responsesArray = Array(runtime, response.response_count);
    for (int i = 0; i < response.response_count; ++i) {
        responsesArray.setValueAtIndex(runtime, i, String::createFromUtf8(runtime, response.response_array[i]));
    }
    
    responseObj.setProperty(runtime, "responses", responsesArray);
    responseObj.setProperty(runtime, "done", Value(response.done));
    
    return responseObj;
}

#endif // HAS_JSI

} // namespace mediapipe_llm 