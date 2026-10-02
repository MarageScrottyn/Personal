#pragma once
#include <nlohmann/json.hpp>
#include <string>

using json = nlohmann::json;

inline void json_set(json& j, const char* key, const char* value) {
    j[std::string(key)] = std::string(value);
}

template<typename T>
inline void json_set(json& j, const char* key, T&& value) {
    j[std::string(key)] = std::forward<T>(value);
}

template<typename T>
inline T json_get(const json& j, const char* key, T&& default_value) {
    if (j.contains(std::string(key))) {
        return j[std::string(key)].get<T>();
    }
    return std::forward<T>(default_value);
}