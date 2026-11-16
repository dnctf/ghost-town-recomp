#include "unity_shim.h"
#include <unordered_map>
#include <mutex>

namespace overcooked {

namespace {
std::unordered_map<int32_t, NativeTransform> g_transforms;
std::mutex g_transform_mutex;
} // namespace

bool UnityRuntimeHost::Initialize(const std::string& data_dir, int backend_ordinal) {
    active_backend_ = backend_ordinal;
    if (backend_ordinal == 1) { // MONO_CIL
        return mono_host_.Initialize(data_dir + "/Managed", data_dir + "/Managed/Assembly-CSharp.dll");
    } else if (backend_ordinal == 2) { // IL2CPP_AOT
        return il2cpp_loader_.LoadFromFile(data_dir + "/il2cpp_data/Metadata/global-metadata.dat");
    }
    return false;
}

void UnityRuntimeHost::TickFrame(float delta_time) {
    if (active_backend_ == 1) {
        mono_host_.TickUpdate(delta_time);
    }
}

void UnityRuntimeHost::Shutdown() {
    mono_host_.Shutdown();
}

void UnityRuntimeHost::SetTransformPosition(int32_t handle, const Vector3& pos) {
    std::lock_guard<std::mutex> lock(g_transform_mutex);
    auto& tf = g_transforms[handle];
    tf.position = pos;
    tf.dirty = true;
}

Vector3 UnityRuntimeHost::GetTransformPosition(int32_t handle) {
    std::lock_guard<std::mutex> lock(g_transform_mutex);
    auto it = g_transforms.find(handle);
    return (it != g_transforms.end()) ? it->second.position : Vector3{0.0f, 0.0f, 0.0f};
}

} // namespace overcooked
