cmake_minimum_required(VERSION 3.15)
project(NativeService)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(WIN32)
    add_definitions(-DWIN32_LEAN_AND_MEAN)
    add_definitions(-DNOMINMAX)
    add_definitions(-D_CRT_SECURE_NO_WARNINGS)
    add_definitions(-D_WIN32_WINNT=0x0A00)
    add_definitions(-DNTDDI_VERSION=0x0A00000B)
    
    set(CMAKE_CXX_FLAGS "/EHsc /std:c++17 /utf-8 /await")
    
    set(CMAKE_SYSTEM_VERSION 10.0.26100.0 CACHE STRING "Windows SDK version" FORCE)
    
    set(WINDOWS_KITS_10_DIR "D:/Windows Kits/10" CACHE PATH "Windows Kits 10 directory")
    set(WINDOWS_SDK_VERSION "10.0.26100.0" CACHE STRING "Windows SDK version")
    
    set(WINDOWS_SDK_INCLUDE_DIR "${WINDOWS_KITS_10_DIR}/Include/${WINDOWS_SDK_VERSION}")
    set(WINDOWS_SDK_LIB_DIR "${WINDOWS_KITS_10_DIR}/Lib/${WINDOWS_SDK_VERSION}")
    
    include_directories(${WINDOWS_SDK_INCLUDE_DIR}/cppwinrt)
    include_directories(${WINDOWS_SDK_INCLUDE_DIR}/cppwinrt/winrt)
    include_directories(${WINDOWS_SDK_INCLUDE_DIR}/um)
    include_directories(${WINDOWS_SDK_INCLUDE_DIR}/ucrt)
    include_directories(${WINDOWS_SDK_INCLUDE_DIR}/shared)
    
    link_directories(${WINDOWS_SDK_LIB_DIR}/um/x64)
    link_directories(${WINDOWS_SDK_LIB_DIR}/ucrt/x64)
endif()

include(FetchContent)

FetchContent_Declare(
    flatbuffers
    GIT_REPOSITORY https://gitee.com/mirrors/flatbuffers.git
    GIT_TAG v24.3.25
)

FetchContent_MakeAvailable(flatbuffers)

set(FLATBUFFERS_SCHEMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/schema)
set(FLATBUFFERS_GENERATED_DIR ${CMAKE_CURRENT_BINARY_DIR}/generated)

file(MAKE_DIRECTORY ${FLATBUFFERS_GENERATED_DIR})

set(FLATBUFFERS_SCHEMA_FILE ${FLATBUFFERS_SCHEMA_DIR}/service.fbs)
set(FLATBUFFERS_GENERATED_HEADER ${FLATBUFFERS_GENERATED_DIR}/service_generated.h)

add_custom_command(
    OUTPUT ${FLATBUFFERS_GENERATED_HEADER}
    COMMAND flatc --cpp --no-union-value-namespacing -o ${FLATBUFFERS_GENERATED_DIR} ${FLATBUFFERS_SCHEMA_FILE}
    MAIN_DEPENDENCY ${FLATBUFFERS_SCHEMA_FILE}
    COMMENT "Generating FlatBuffers C++ headers..."
)

add_custom_target(generate_flatbuffers ALL DEPENDS ${FLATBUFFERS_GENERATED_HEADER})

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/nlohmann)

add_executable(NativeService
    native-service.cpp
    media-monitor/media-monitor.cpp
    media-monitor/media-monitor.h
    taskbar/taskbar-utils.cpp
    taskbar/taskbar-utils.h
    ${FLATBUFFERS_GENERATED_HEADER}
)

add_dependencies(NativeService generate_flatbuffers)

target_include_directories(NativeService PRIVATE 
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${FLATBUFFERS_GENERATED_DIR}
)

target_link_libraries(NativeService PRIVATE 
    flatbuffers
)

# DLL目标
add_library(NativeServiceDLL SHARED
    native-service-dll.cpp
    native-service-dll.h
    media-monitor/media-monitor.cpp
    media-monitor/media-monitor.h
    taskbar/taskbar-utils.cpp
    taskbar/taskbar-utils.h
)

add_dependencies(NativeServiceDLL generate_flatbuffers)

target_include_directories(NativeServiceDLL PRIVATE 
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${FLATBUFFERS_GENERATED_DIR}
)

target_link_libraries(NativeServiceDLL PRIVATE 
    flatbuffers
)

if(WIN32)
    target_link_libraries(NativeService PRIVATE 
        kernel32.lib
        user32.lib
        gdi32.lib
        gdiplus.lib
        winspool.lib
        comdlg32.lib
        advapi32.lib
        shell32.lib
        ole32.lib
        oleaut32.lib
        uuid.lib
        odbc32.lib
        odbccp32.lib
        winmm.lib
        ws2_32.lib
        winhttp.lib
        psapi.lib
        powrprof.lib
        windowsapp.lib
    )
    
    target_link_libraries(NativeServiceDLL PRIVATE 
        kernel32.lib
        user32.lib
        gdi32.lib
        gdiplus.lib
        winspool.lib
        comdlg32.lib
        advapi32.lib
        shell32.lib
        ole32.lib
        oleaut32.lib
        uuid.lib
        odbc32.lib
        odbccp32.lib
        winmm.lib
        ws2_32.lib
        winhttp.lib
        psapi.lib
        powrprof.lib
        windowsapp.lib
    )
    
    set_target_properties(NativeService PROPERTIES
        VS_GLOBAL_EnableCppWinRT true
        VS_GLOBAL_WindowsTargetPlatformVersion ${WINDOWS_SDK_VERSION}
        VS_GLOBAL_WindowsSDKDir ${WINDOWS_KITS_10_DIR}
        COMPILE_FLAGS "/EHsc /std:c++17 /utf-8 /await"
    )
    
    set_target_properties(NativeServiceDLL PROPERTIES
        VS_GLOBAL_EnableCppWinRT true
        VS_GLOBAL_WindowsTargetPlatformVersion ${WINDOWS_SDK_VERSION}
        VS_GLOBAL_WindowsSDKDir ${WINDOWS_KITS_10_DIR}
        COMPILE_FLAGS "/EHsc /std:c++17 /utf-8 /await"
        WINDOWS_EXPORT_ALL_SYMBOLS ON
    )
endif()