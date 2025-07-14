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

#include <string>

namespace mediapipe_llm {

#if HAS_JSI
using namespace facebook::jsi;

class JSI_Helpers {
public:
    static std::string getString(Runtime& runtime, const Value& value) {
        if (value.isString()) {
            return value.asString(runtime).utf8(runtime);
        }
        return "";
    }
    
    static double getNumber(Runtime& runtime, const Value& value, double defaultValue = 0.0) {
        if (value.isNumber()) {
            return value.asNumber();
        }
        return defaultValue;
    }
    
    static bool getBool(Runtime& runtime, const Value& value, bool defaultValue = false) {
        if (value.isBool()) {
            return value.asBool();
        }
        return defaultValue;
    }
    
    static std::string getOptionalString(Runtime& runtime, const Object& obj, const std::string& key) {
        if (obj.hasProperty(runtime, key.c_str())) {
            auto prop = obj.getProperty(runtime, key.c_str());
            if (prop.isString()) {
                return prop.asString(runtime).utf8(runtime);
            }
        }
        return "";
    }
    
    static double getOptionalNumber(Runtime& runtime, const Object& obj, const std::string& key, double defaultValue = 0.0) {
        if (obj.hasProperty(runtime, key.c_str())) {
            auto prop = obj.getProperty(runtime, key.c_str());
            if (prop.isNumber()) {
                return prop.asNumber();
            }
        }
        return defaultValue;
    }
    
    static bool getOptionalBool(Runtime& runtime, const Object& obj, const std::string& key, bool defaultValue = false) {
        if (obj.hasProperty(runtime, key.c_str())) {
            auto prop = obj.getProperty(runtime, key.c_str());
            if (prop.isBool()) {
                return prop.asBool();
            }
        }
        return defaultValue;
    }
    
    static Array getOptionalArray(Runtime& runtime, const Object& obj, const std::string& key) {
        if (obj.hasProperty(runtime, key.c_str())) {
            auto prop = obj.getProperty(runtime, key.c_str());
            if (prop.isObject() && prop.asObject(runtime).isArray(runtime)) {
                return prop.asObject(runtime).asArray(runtime);
            }
        }
        return Array(runtime, 0);
    }
    
    static Object getOptionalObject(Runtime& runtime, const Object& obj, const std::string& key) {
        if (obj.hasProperty(runtime, key.c_str())) {
            auto prop = obj.getProperty(runtime, key.c_str());
            if (prop.isObject()) {
                return prop.asObject(runtime);
            }
        }
        return Object(runtime);
    }
};

#else
// Fallback class for validation builds without JSI
class JSI_Helpers {
public:
    // Minimal stubs for validation
    static std::string getString(void* runtime, void* value) { return ""; }
    static double getNumber(void* runtime, void* value, double defaultValue = 0.0) { return defaultValue; }
    static bool getBool(void* runtime, void* value, bool defaultValue = false) { return defaultValue; }
};
#endif

} // namespace mediapipe_llm 