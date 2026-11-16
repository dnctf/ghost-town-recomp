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

enum class GameTitle : int {
    UNKNOWN = 0,
    OVERCOOKED_1 = 1,
    OVERCOOKED_2 = 2
};

class UnityRuntimeHost {
public:
    bool Initialize(const std::string& data_dir, int backend_ordinal, int game_version = 1);
    void TickFrame(float delta_time);
    void Shutdown();

    GameTitle GetGameTitle() const { return game_title_; }
    const char* GetGameTitleName() const;

    static void SetTransformPosition(int32_t handle, const Vector3& pos);
    static Vector3 GetTransformPosition(int32_t handle);

private:
    MonoRuntimeHost mono_host_;
    Il2CppMetadataLoader il2cpp_loader_;
    int active_backend_ = 0;
    GameTitle game_title_ = GameTitle::UNKNOWN;
};

} // namespace overcooked
