#pragma once
#include "mono_runtime_host.h"
#include "il2cpp_metadata.h"
#include <cstdint>
#include <string>

namespace overcooked {

struct Vector3 {
    float x = 0.0f;
    float y = 0.0f;
    float z = 0.0f;
};

struct NativeTransform {
    Vector3 position{0.0f, 0.0f, 0.0f};
    Vector3 rotation_euler{0.0f, 0.0f, 0.0f};
    Vector3 scale{1.0f, 1.0f, 1.0f};
    bool dirty = false;
};

class UnityRuntimeHost {
public:
    bool Initialize(const std::string& data_dir, int backend_ordinal);
    void TickFrame(float delta_time);
    void Shutdown();

    static void SetTransformPosition(int32_t handle, const Vector3& pos);
    static Vector3 GetTransformPosition(int32_t handle);

private:
    MonoRuntimeHost mono_host_;
    Il2CppMetadataLoader il2cpp_loader_;
    int active_backend_ = 0;
};

} // namespace overcooked
