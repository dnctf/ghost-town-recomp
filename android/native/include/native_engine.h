#pragma once
#include "vulkan_context.h"
#include "memory_arena.h"
#include "audio_engine.h"
#include "input_mapper.h"
#include "thermal_governor.h"
#include "unity_shim.h"
#include "graphics_settings.h"
#include <atomic>
#include <memory>
#include <string>
#include <thread>

namespace overcooked {

class NativeEngine {
public:
    static NativeEngine& Instance();

    void Initialize(const std::string& data_dir, int backend_ordinal, int game_version = 1);
    void InitDisplayInfo(uint32_t width, uint32_t height, float dpi, float refresh_rate, uint32_t ram_mb);
    void OnSurfaceCreated(ANativeWindow* window);
    void OnSurfaceChanged(uint32_t width, uint32_t height);
    void OnSurfaceDestroyed();
    void Pause();
    void Resume();
    void Shutdown();

    InputMapper& GetInputMapper() { return input_mapper_; }
    GraphicsSettingsManager& GetGraphicsSettings() { return GraphicsSettingsManager::Instance(); }

private:
    NativeEngine();
    void RenderLoop();

    std::string data_dir_;
    int backend_ordinal_ = 0;

    std::unique_ptr<LinearMemoryArena> arena_;
    VulkanContext vulkan_;
    AAudioMixer audio_;
    InputMapper input_mapper_;
    ThermalGovernor thermal_;
    UnityRuntimeHost runtime_host_;

    ANativeWindow* window_ = nullptr;
    std::atomic<bool> running_{false};
    std::atomic<bool> paused_{false};
    std::thread render_thread_;
};

} // namespace overcooked
