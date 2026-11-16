#pragma once
#include <atomic>
#include <cstdint>
#include <mutex>
#include <unordered_map>

namespace overcooked {

struct PlayerInputState {
    float move_x = 0.0f;
    float move_y = 0.0f;
    bool action_pickup_drop = false; // Button A / Cross (Grab di Moving Out & Pick di OC)
    bool action_chop = false;        // Button X / Square (Chop di OC, Slap di Moving Out)
    bool action_throw = false;       // Button Y / Triangle (Throw di OC2 & Jump di Moving Out)
    bool action_chop_throw = false;  // Backward-compatibility alias
    bool action_dash = false;        // Button B / Circle
    bool action_switch_chef = false; // L1 / LB
    bool action_emote = false;       // R1 / RB
    bool action_trigger_left = false;  // L2 / LT
    bool action_trigger_right = false; // R2 / RT
};

class InputMapper {
public:
    void SetSplitControllerMode(bool enabled) { split_controller_mode_.store(enabled); }
    void UpdateAxis(int32_t device_id, float lx, float ly, float rx, float ry, float hat_x, float hat_y);
    void UpdateButton(int32_t device_id, int32_t key_code, bool pressed);
    PlayerInputState GetPlayerState(int player_index) const;

private:
    std::atomic<bool> split_controller_mode_{false};
    mutable std::mutex mutex_;
    PlayerInputState players_[4]{};
    std::unordered_map<int32_t, int> device_to_player_;
};

} // namespace overcooked
