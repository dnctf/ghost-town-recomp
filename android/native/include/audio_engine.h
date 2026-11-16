#pragma once
#include <aaudio/AAudio.h>
#include <atomic>
#include <cstdint>
#include <vector>

namespace overcooked {

class AAudioMixer {
public:
    bool Initialize(int32_t sample_rate = 48000, int32_t channel_count = 2);
    void PlayOneShotPCM16(const int16_t* samples, size_t frame_count, float gain = 1.0f);
    void Shutdown();

private:
    static aaudio_data_callback_result_t DataCallback(
        AAudioStream* stream, void* user_data, void* audio_data, int32_t num_frames);

    AAudioStream* stream_ = nullptr;
    int32_t sample_rate_ = 48000;
    int32_t channel_count_ = 2;
    std::atomic<bool> running_{false};
};

} // namespace overcooked
