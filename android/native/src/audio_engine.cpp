#include "audio_engine.h"
#include <android/log.h>
#include <cstring>

#define LOG_TAG "OvercookedAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace overcooked {

bool AAudioMixer::Initialize(int32_t sample_rate, int32_t channel_count) {
    sample_rate_ = sample_rate;
    channel_count_ = channel_count;

    AAudioStreamBuilder* builder = nullptr;
    if (AAudio_createStreamBuilder(&builder) != AAUDIO_OK) return false;

    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
    AAudioStreamBuilder_setSampleRate(builder, sample_rate_);
    AAudioStreamBuilder_setChannelCount(builder, channel_count_);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setDataCallback(builder, &AAudioMixer::DataCallback, this);

    aaudio_result_t res = AAudioStreamBuilder_openStream(builder, &stream_);
    AAudioStreamBuilder_delete(builder);
    if (res != AAUDIO_OK) return false;

    aaudio_result_t start_res = AAudioStream_requestStart(stream_);
    if (start_res != AAUDIO_OK) {
        LOGI("AAudioStream_requestStart gagal: %d", start_res);
        AAudioStream_close(stream_);
        stream_ = nullptr;
        return false;
    }

    running_.store(true);
    LOGI("AAudio Low-Latency Stream aktif (%d Hz, %d ch)", sample_rate_, channel_count_);
    return true;
}

aaudio_data_callback_result_t AAudioMixer::DataCallback(
    AAudioStream* /*stream*/, void* user_data, void* audio_data, int32_t num_frames) {
    auto* mixer = static_cast<AAudioMixer*>(user_data);
    int32_t channels = (mixer != nullptr) ? mixer->channel_count_ : 2;
    // Output silence sampai buffer mixer FMOD/AudioClip terisi
    std::memset(audio_data, 0, static_cast<size_t>(num_frames) * static_cast<size_t>(channels) * sizeof(float));
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AAudioMixer::PlayOneShotPCM16(const int16_t* /*samples*/, size_t /*frame_count*/, float /*gain*/) {
    // Antrekan PCM16 ke ring-buffer lock-free mixer
}

void AAudioMixer::Shutdown() {
    running_.store(false);
    if (stream_) {
        AAudioStream_requestStop(stream_);
        AAudioStream_close(stream_);
        stream_ = nullptr;
    }
}

} // namespace overcooked
