#pragma once
#include <cstdint>
#include <vector>

namespace overcooked {

enum class MobileTextureFormat {
    ASTC_4x4_RGBA,
    ASTC_6x6_RGBA,
    ETC2_RGBA8
};

class TextureTranscoder {
public:
    static MobileTextureFormat SelectOptimalFormat(uint32_t adreno_series, bool is_ui_or_hero);
    static size_t EstimateCompressedBytes(uint32_t width, uint32_t height, MobileTextureFormat fmt);
};

} // namespace overcooked
