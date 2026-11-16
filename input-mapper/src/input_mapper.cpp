#include "input_mapper.h"
#include <android/keycodes.h>
#include <cmath>

namespace overcooked {

namespace {
float ApplyDeadzone(float v, float deadzone = 0.15f) {
    return (std::fabs(v) < deadzone) ? 0.0f : v;
}
} // namespace

void InputMapper::UpdateAxis(int32_t device_id, float lx, float ly, float rx, float ry, float hat_x, float hat_y) {
    if (split_controller_mode_.load()) {
        std::lock_guard<std::mutex> lock(mutex_);
        // 1 Gamepad fisik dibagi untuk Player 0 (Stik Kiri) & Player 1 (Stik Kanan)
        players_[0].move_x = ApplyDeadzone(lx);
        players_[0].move_y = ApplyDeadzone(ly);
        players_[1].move_x = ApplyDeadzone(rx);
        players_[1].move_y = ApplyDeadzone(ry);
        return;
    }

    std::lock_guard<std::mutex> lock(mutex_);
    auto it = device_to_player_.find(device_id);
    if (it == device_to_player_.end()) {
        int assigned = static_cast<int>(device_to_player_.size());
        if (assigned >= 4) return;
        it = device_to_player_.emplace(device_id, assigned).first;
    }

    int p = it->second;
    float x = ApplyDeadzone(lx);
    float y = ApplyDeadzone(ly);
    if (x == 0.0f && y == 0.0f) {
        x = ApplyDeadzone(hat_x);
        y = ApplyDeadzone(hat_y);
    }
    players_[p].move_x = x;
    players_[p].move_y = y;
}

void InputMapper::UpdateButton(int32_t device_id, int32_t key_code, bool pressed) {
    if (split_controller_mode_.load()) {
        std::lock_guard<std::mutex> lock(mutex_);
        // Player 0: L1/L2; Player 1: R1/R2
        if (key_code == AKEYCODE_BUTTON_L1) players_[0].action_pickup_drop = pressed;
        if (key_code == AKEYCODE_BUTTON_L2) players_[0].action_chop_throw = pressed;
        if (key_code == AKEYCODE_BUTTON_R1) players_[1].action_pickup_drop = pressed;
        if (key_code == AKEYCODE_BUTTON_R2) players_[1].action_chop_throw = pressed;
        return;
    }

    std::lock_guard<std::mutex> lock(mutex_);
    auto it = device_to_player_.find(device_id);
    if (it == device_to_player_.end()) {
        int assigned = static_cast<int>(device_to_player_.size());
        if (assigned >= 4) return;
        it = device_to_player_.emplace(device_id, assigned).first;
    }

    int p = it->second;

    switch (key_code) {
        case AKEYCODE_BUTTON_A:
            players_[p].action_pickup_drop = pressed;
            break;
        case AKEYCODE_BUTTON_X:
            players_[p].action_chop_throw = pressed;
            break;
        case AKEYCODE_BUTTON_B:
            players_[p].action_dash = pressed;
            break;
        default:
            break;
    }
}

PlayerInputState InputMapper::GetPlayerState(int player_index) const {
    std::lock_guard<std::mutex> lock(mutex_);
    if (player_index < 0 || player_index >= 4) return {};
    return players_[player_index];
}

} // namespace overcooked
