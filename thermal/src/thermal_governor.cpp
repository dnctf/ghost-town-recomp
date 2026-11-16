#include "thermal_governor.h"
#include <android/thermal.h>
#include <android/log.h>

#define LOG_TAG "OvercookedThermal"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace overcooked {

void ThermalGovernor::Initialize() {
    LOGI("ThermalGovernor aktif (AThermal NDK API 29+ & Dynamic Resolution Scaling)");
}

int ThermalGovernor::ReadSystemThermalStatus() {
#if __ANDROID_API__ >= 30
    AThermalManager* mgr = AThermal_acquireManager();
    if (mgr != nullptr) {
        AThermalStatus status = AThermal_getCurrentThermalStatus(mgr);
        AThermal_releaseManager(mgr);
        return static_cast<int>(status);
    }
#endif
    return 0; // THERMAL_STATUS_NONE
}

RenderBudgetConfig ThermalGovernor::EvaluatePolicy() {
    RenderBudgetConfig cfg{};
    int status = ReadSystemThermalStatus();

    // Pemetaan kebijakan berdasarkan status termal Snapdragon
    if (status >= 3) { // THERMAL_STATUS_SEVERE atau lebih tinggi
        cfg.resolution_scale = 0.75f; // Turunkan ke ~540p pada target 720p
        cfg.target_fps = 30;
        cfg.max_anisotropy = 1;
        cfg.spin_wait_sleep_us = 100;
    } else if (status == 2) { // THERMAL_STATUS_MODERATE
        cfg.resolution_scale = 0.85f;
        cfg.target_fps = 30;
        cfg.max_anisotropy = 2;
        cfg.spin_wait_sleep_us = 100;
    } else {
        cfg.resolution_scale = 1.0f;
        cfg.target_fps = 60;
        cfg.max_anisotropy = 4;
        cfg.spin_wait_sleep_us = 50;
    }
    return cfg;
}

} // namespace overcooked
