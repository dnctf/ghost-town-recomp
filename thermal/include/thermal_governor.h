#pragma once
#include <cstdint>

namespace overcooked {

struct RenderBudgetConfig {
    float resolution_scale = 1.0f;
    uint32_t target_fps = 60;
    uint32_t max_anisotropy = 4;
    int64_t spin_wait_sleep_us = 100;
};

class ThermalGovernor {
public:
    void Initialize();
    RenderBudgetConfig EvaluatePolicy();

private:
    int ReadSystemThermalStatus();
};

} // namespace overcooked
