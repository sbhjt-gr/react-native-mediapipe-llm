#include "MediapipeLlm.h"

// Stub implementation of MediaPipe LLM functions
// This allows the module to compile without full MediaPipe dependencies
// TODO: Replace with actual MediaPipe integration once dependencies are resolved

extern "C" {

// Stub LLM inference engine functions
typedef void* LlmInferenceEngineStruct;

LlmInferenceEngineStruct* LlmInferenceEngineCreate(const char* model_path) {
    // Return a dummy pointer for now
    return reinterpret_cast<LlmInferenceEngineStruct*>(0x1);
}

const char* LlmInferenceEngineGenerateResponse(LlmInferenceEngineStruct* engine, const char* prompt) {
    // Return a stub response
    static const char* stub_response = "Stub response: MediaPipe LLM integration pending";
    return stub_response;
}

void LlmInferenceEngineDelete(LlmInferenceEngineStruct* engine) {
    // Nothing to clean up in stub implementation
}

}
