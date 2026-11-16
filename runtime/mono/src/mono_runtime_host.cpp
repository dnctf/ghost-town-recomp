#include "mono_runtime_host.h"
#include <android/log.h>
#include <fstream>

#define LOG_TAG "OvercookedMono"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace overcooked {

bool MonoRuntimeHost::Initialize(const std::string& /*managed_dir*/, const std::string& assembly_path) {
    assembly_path_ = assembly_path;
    std::ifstream dll(assembly_path_, std::ios::binary);
    if (!dll.is_open()) {
        LOGE("Assembly-CSharp.dll tidak ditemukan di: %s", assembly_path_.c_str());
        return false;
    }

    // Validasi DOS MZ header ('MZ' = 0x5A4D) sebelum diteruskan ke libmonosgen-2.0.so
    uint16_t dos_magic = 0;
    dll.read(reinterpret_cast<char*>(&dos_magic), sizeof(dos_magic));
    if (dos_magic != 0x5A4D) {
        LOGE("Header PE/CLI tidak valid pada %s", assembly_path_.c_str());
        return false;
    }

    initialized_ = true;
    LOGI("MonoRuntimeHost siap memuat bytecode CIL dari %s", assembly_path_.c_str());
    return true;
}

void MonoRuntimeHost::TickUpdate(float /*delta_time*/) {
    if (!initialized_) return;
    // Memanggil mono_runtime_invoke() pada daftar MonoBehaviour aktif
}

void MonoRuntimeHost::Shutdown() {
    initialized_ = false;
}

} // namespace overcooked
