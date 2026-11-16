#include "il2cpp_metadata.h"
#include <android/log.h>
#include <fstream>

#define LOG_TAG "OvercookedIL2CPP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace overcooked {

bool Il2CppMetadataLoader::LoadFromFile(const std::string& metadata_path) {
    std::ifstream file(metadata_path, std::ios::binary);
    if (!file.is_open()) {
        LOGE("Gagal membuka global-metadata.dat di: %s", metadata_path.c_str());
        return false;
    }

    file.read(reinterpret_cast<char*>(&header_), sizeof(header_));
    if (!file || header_.sanity != 0xFAB11BAF) {
        LOGE("Magic bytes global-metadata.dat tidak valid: 0x%X", header_.sanity);
        return false;
    }

    LOGI("IL2CPP Metadata valid (v%d), estimasi %d methods siap dipetakan ke ARM64 AOT.",
         header_.version, EstimatedMethodCount());
    return true;
}

} // namespace overcooked
