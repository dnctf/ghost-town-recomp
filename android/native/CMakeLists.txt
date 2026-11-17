cmake_minimum_required(VERSION 3.22.1)
project(overcooked_recomp_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(REPO_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../..")

set(ADRENOTOOLS_DIR "${REPO_ROOT}/external/libadrenotools")
if (EXISTS "${ADRENOTOOLS_DIR}/CMakeLists.txt")
    add_subdirectory("${ADRENOTOOLS_DIR}" "${CMAKE_BINARY_DIR}/adrenotools")
else()
    message(FATAL_ERROR "Pinned libadrenotools submodule is missing; checkout submodules recursively.")
endif()

add_library(overcooked_native SHARED
    src/main_jni.cpp
    src/vulkan_surface.cpp
    src/memory_arena.cpp
    src/audio_engine.cpp
    src/graphics_settings.cpp
    ${REPO_ROOT}/runtime/il2cpp/src/il2cpp_metadata_parser.cpp
    ${REPO_ROOT}/runtime/mono/src/mono_runtime_host.cpp
    ${REPO_ROOT}/runtime/interop/src/unity_method_invoker.cpp
    ${REPO_ROOT}/assets-pipeline/assetstudio-wrapper/src/serialized_file_parser.cpp
    ${REPO_ROOT}/assets-pipeline/texture-compressor/src/texture_transcoder.cpp
    ${REPO_ROOT}/shader-bridge/src/shader_bridge.cpp
    ${REPO_ROOT}/input-mapper/src/input_mapper.cpp
    ${REPO_ROOT}/thermal/src/thermal_governor.cpp
)

target_include_directories(overcooked_native PRIVATE
    include
    ${REPO_ROOT}/runtime/il2cpp/include
    ${REPO_ROOT}/runtime/mono/include
    ${REPO_ROOT}/runtime/interop/include
    ${REPO_ROOT}/assets-pipeline/assetstudio-wrapper/include
    ${REPO_ROOT}/assets-pipeline/texture-compressor/include
    ${REPO_ROOT}/shader-bridge/include
    ${REPO_ROOT}/input-mapper/include
    ${REPO_ROOT}/thermal/include
)

target_compile_definitions(overcooked_native PRIVATE
    VK_USE_PLATFORM_ANDROID_KHR=1
)

find_library(ANDROID_LIB android REQUIRED)
find_library(LOG_LIB log REQUIRED)
find_library(VULKAN_LIB vulkan REQUIRED)
find_library(AAUDIO_LIB aaudio REQUIRED)

target_link_libraries(overcooked_native
    ${ANDROID_LIB}
    ${LOG_LIB}
    ${VULKAN_LIB}
    ${AAUDIO_LIB}
    adrenotools
    ${CMAKE_DL_LIBS}
)

# Kesiapan 16 KB Page Alignment untuk Android 15+
target_link_options(overcooked_native PRIVATE
    "-Wl,-z,max-page-size=16384"
)
