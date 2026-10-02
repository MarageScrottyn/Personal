#ifndef NLOHMANN_JSON_HPP
#define NLOHMANN_JSON_HPP

#include <algorithm>
#include <array>
#include <cassert>
#include <cctype>
#include <charconv>
#include <chrono>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <exception>
#include <forward_list>
#include <functional>
#include <initializer_list>
#include <iomanip>
#include <iostream>
#include <iterator>
#include <limits>
#include <list>
#include <map>
#include <memory>
#include <numeric>
#include <optional>
#include <queue>
#include <ratio>
#include <regex>
#include <set>
#include <sstream>
#include <stack>
#include <stdexcept>
#include <string>
#include <string_view>
#include <tuple>
#include <type_traits>
#include <typeinfo>
#include <unordered_map>
#include <unordered_set>
#include <utility>
#include <valarray>
#include <vector>

namespace nlohmann {

template <typename T>
using enable_if_t = typename std::enable_if<T::value>::type;

template<bool B, typename T = void>
using enable_if = std::enable_if<B, T>;

class json {
public:
    enum class value_t {
        null,
        object,
        array,
        string,
        boolean,
        number_integer,
        number_unsigned,
        number_float
    };

private:
    value_t m_type = value_t::null;
    std::vector<json> m_array;
    std::map<std::string, json> m_object;
    std::string m_string;
    bool m_boolean = false;
    int64_t m_integer = 0;
    uint64_t m_unsigned = 0;
    double m_float = 0.0;

public:
    json() = default;
    
    json(std::nullptr_t) : m_type(value_t::null) {}
    
    json(bool value) : m_type(value_t::boolean), m_boolean(value) {}
    
    json(int value) : m_type(value_t::number_integer), m_integer(value) {}
    json(long value) : m_type(value_t::number_integer), m_integer(value) {}
    json(long long value) : m_type(value_t::number_integer), m_integer(value) {}
    
    json(unsigned int value) : m_type(value_t::number_unsigned), m_unsigned(value) {}
    json(unsigned long value) : m_type(value_t::number_unsigned), m_unsigned(value) {}
    json(unsigned long long value) : m_type(value_t::number_unsigned), m_unsigned(value) {}
    
    json(double value) : m_type(value_t::number_float), m_float(value) {}
    
    json(const std::string& value) : m_type(value_t::string), m_string(value) {}
    json(const char* value) : m_type(value_t::string), m_string(value) {}
    
    json(std::initializer_list<std::pair<const std::string, json>> init) 
        : m_type(value_t::object), m_object(init) {}
    
    json(std::initializer_list<json> init) 
        : m_type(value_t::array), m_array(init) {}

    json& operator[](const std::string& key) {
        if (m_type != value_t::object) {
            m_type = value_t::object;
            m_object.clear();
        }
        return m_object[key];
    }

    json& operator[](size_t index) {
        if (m_type != value_t::array) {
            m_type = value_t::array;
            m_array.clear();
        }
        if (index >= m_array.size()) {
            m_array.resize(index + 1);
        }
        return m_array[index];
    }

    const json& operator[](const std::string& key) const {
        static json null_json;
        if (m_type != value_t::object) {
            return null_json;
        }
        auto it = m_object.find(key);
        if (it != m_object.end()) {
            return it->second;
        }
        return null_json;
    }

    void push_back(const json& value) {
        if (m_type != value_t::array) {
            m_type = value_t::array;
            m_array.clear();
        }
        m_array.push_back(value);
    }

    size_t size() const {
        switch (m_type) {
            case value_t::array: return m_array.size();
            case value_t::object: return m_object.size();
            default: return 0;
        }
    }

    bool empty() const {
        return size() == 0;
    }

    value_t type() const { return m_type; }

    bool is_null() const { return m_type == value_t::null; }
    bool is_object() const { return m_type == value_t::object; }
    bool is_array() const { return m_type == value_t::array; }
    bool is_string() const { return m_type == value_t::string; }
    bool is_boolean() const { return m_type == value_t::boolean; }
    bool is_number() const { 
        return m_type == value_t::number_integer || 
               m_type == value_t::number_unsigned || 
               m_type == value_t::number_float; 
    }

    const std::string& get_string() const { return m_string; }
    bool get_boolean() const { return m_boolean; }
    int64_t get_integer() const { return m_integer; }
    uint64_t get_unsigned() const { return m_unsigned; }
    double get_float() const { return m_float; }

    operator bool() const {
        if (m_type == value_t::boolean) return m_boolean;
        if (m_type == value_t::number_integer) return m_integer != 0;
        if (m_type == value_t::number_unsigned) return m_unsigned != 0;
        if (m_type == value_t::number_float) return m_float != 0.0;
        if (m_type == value_t::string) return !m_string.empty();
        if (m_type == value_t::array) return !m_array.empty();
        if (m_type == value_t::object) return !m_object.empty();
        return false;
    }

    static json parse(const std::string& s) {
        json result;
        parse_internal(s, 0, result);
        return result;
    }

    std::string dump() const {
        std::stringstream ss;
        serialize(ss, *this, 0);
        return ss.str();
    }

    bool contains(const std::string& key) const {
        if (m_type != value_t::object) {
            return false;
        }
        return m_object.find(key) != m_object.end();
    }

    template<typename T>
    T get() const {
        if constexpr (std::is_same_v<T, std::string>) {
            return m_string;
        } else if constexpr (std::is_same_v<T, bool>) {
            return m_boolean;
        } else if constexpr (std::is_same_v<T, int64_t>) {
            return m_integer;
        } else if constexpr (std::is_same_v<T, uint64_t>) {
            return m_unsigned;
        } else if constexpr (std::is_same_v<T, double>) {
            return m_float;
        } else if constexpr (std::is_same_v<T, int>) {
            return static_cast<int>(m_integer);
        } else {
            return T();
        }
    }

private:
    static void serialize(std::ostream& os, const json& j, int depth) {
        switch (j.m_type) {
            case value_t::null:
                os << "null";
                break;
            case value_t::boolean:
                os << (j.m_boolean ? "true" : "false");
                break;
            case value_t::number_integer:
                os << j.m_integer;
                break;
            case value_t::number_unsigned:
                os << j.m_unsigned;
                break;
            case value_t::number_float:
                os << j.m_float;
                break;
            case value_t::string:
                os << "\"" << escape_string(j.m_string) << "\"";
                break;
            case value_t::array: {
                os << "[";
                for (size_t i = 0; i < j.m_array.size(); ++i) {
                    if (i > 0) os << ",";
                    serialize(os, j.m_array[i], depth + 1);
                }
                os << "]";
                break;
            }
            case value_t::object: {
                os << "{";
                size_t i = 0;
                for (const auto& pair : j.m_object) {
                    if (i > 0) os << ",";
                    os << "\"" << escape_string(pair.first) << "\":";
                    serialize(os, pair.second, depth + 1);
                    ++i;
                }
                os << "}";
                break;
            }
        }
    }

    static std::string escape_string(const std::string& s) {
        std::string result;
        for (char c : s) {
            switch (c) {
                case '"': result += "\\\""; break;
                case '\\': result += "\\\\"; break;
                case '\n': result += "\\n"; break;
                case '\r': result += "\\r"; break;
                case '\t': result += "\\t"; break;
                default: result += c; break;
            }
        }
        return result;
    }

    static size_t parse_internal(const std::string& s, size_t pos, json& result) {
        pos = skip_ws(s, pos);
        if (pos >= s.size()) return pos;

        switch (s[pos]) {
            case '{': return parse_object(s, pos + 1, result);
            case '[': return parse_array(s, pos + 1, result);
            case '"': return parse_string(s, pos + 1, result);
            case 't': return parse_true(s, pos, result);
            case 'f': return parse_false(s, pos, result);
            case 'n': return parse_null(s, pos, result);
            default: return parse_number(s, pos, result);
        }
    }

    static size_t skip_ws(const std::string& s, size_t pos) {
        while (pos < s.size() && std::isspace(s[pos])) ++pos;
        return pos;
    }

    static size_t parse_object(const std::string& s, size_t pos, json& result) {
        result.m_type = value_t::object;
        result.m_object.clear();
        
        pos = skip_ws(s, pos);
        if (pos < s.size() && s[pos] == '}') return pos + 1;

        while (pos < s.size()) {
            pos = skip_ws(s, pos);
            if (s[pos] != '"') break;
            
            std::string key;
            pos = parse_string_internal(s, pos + 1, key);
            
            pos = skip_ws(s, pos);
            if (pos >= s.size() || s[pos] != ':') break;
            pos++;

            json value;
            pos = parse_internal(s, pos, value);
            
            result.m_object[key] = value;

            pos = skip_ws(s, pos);
            if (pos >= s.size()) break;
            if (s[pos] == '}') return pos + 1;
            if (s[pos] != ',') break;
            pos++;
        }
        
        return pos;
    }

    static size_t parse_array(const std::string& s, size_t pos, json& result) {
        result.m_type = value_t::array;
        result.m_array.clear();
        
        pos = skip_ws(s, pos);
        if (pos < s.size() && s[pos] == ']') return pos + 1;

        while (pos < s.size()) {
            json value;
            pos = parse_internal(s, pos, value);
            result.m_array.push_back(value);

            pos = skip_ws(s, pos);
            if (pos >= s.size()) break;
            if (s[pos] == ']') return pos + 1;
            if (s[pos] != ',') break;
            pos++;
        }
        
        return pos;
    }

    static size_t parse_string(const std::string& s, size_t pos, json& result) {
        std::string value;
        pos = parse_string_internal(s, pos, value);
        result.m_type = value_t::string;
        result.m_string = value;
        return pos;
    }

    static size_t parse_string_internal(const std::string& s, size_t pos, std::string& result) {
        result.clear();
        while (pos < s.size() && s[pos] != '"') {
            if (s[pos] == '\\' && pos + 1 < s.size()) {
                pos++;
                switch (s[pos]) {
                    case '"': result += '"'; break;
                    case '\\': result += '\\'; break;
                    case 'n': result += '\n'; break;
                    case 'r': result += '\r'; break;
                    case 't': result += '\t'; break;
                    default: result += s[pos]; break;
                }
            } else {
                result += s[pos];
            }
            pos++;
        }
        return pos + 1;
    }

    static size_t parse_true(const std::string& s, size_t pos, json& result) {
        if (s.substr(pos, 4) == "true") {
            result.m_type = value_t::boolean;
            result.m_boolean = true;
            return pos + 4;
        }
        return pos;
    }

    static size_t parse_false(const std::string& s, size_t pos, json& result) {
        if (s.substr(pos, 5) == "false") {
            result.m_type = value_t::boolean;
            result.m_boolean = false;
            return pos + 5;
        }
        return pos;
    }

    static size_t parse_null(const std::string& s, size_t pos, json& result) {
        if (s.substr(pos, 4) == "null") {
            result.m_type = value_t::null;
            return pos + 4;
        }
        return pos;
    }

    static size_t parse_number(const std::string& s, size_t pos, json& result) {
        size_t start = pos;
        bool has_dot = false;
        bool has_e = false;

        while (pos < s.size()) {
            if (s[pos] == '.') {
                if (has_dot) break;
                has_dot = true;
            } else if (s[pos] == 'e' || s[pos] == 'E') {
                if (has_e) break;
                has_e = true;
            } else if (!std::isdigit(s[pos]) && s[pos] != '-' && s[pos] != '+') {
                break;
            }
            pos++;
        }

        std::string num_str = s.substr(start, pos - start);
        
        try {
            if (has_dot || has_e) {
                result.m_type = value_t::number_float;
                result.m_float = std::stod(num_str);
            } else {
                try {
                    result.m_type = value_t::number_integer;
                    result.m_integer = std::stoll(num_str);
                } catch (...) {
                    result.m_type = value_t::number_unsigned;
                    result.m_unsigned = std::stoull(num_str);
                }
            }
        } catch (...) {
            result.m_type = value_t::null;
        }

        return pos;
    }
};

} // namespace nlohmann

#endif // NLOHMANN_JSON_HPP