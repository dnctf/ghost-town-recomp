#include "texture_transcoder.h"

namespace overcooked {

MobileTextureFormat TextureTranscoder::SelectOptimalFormat(uint32_t adreno_series, bool is_ui_or_hero) {
    if (adreno_series >= 750 && is_ui_or_hero) {
        return MobileTextureFormat::ASTC_4x4_RGBA;
    }
    if (adreno_series >= 600) {
        return MobileTextureFormat::ASTC_6x6_RGBA;
    }
    return MobileTextureFormat::ETC2_RGBA8;
}

size_t TextureTranscoder::EstimateCompressedBytes(uint32_t width, uint32_t height, MobileTextureFormat fmt) {
    uint32_t block_w = (fmt == MobileTextureFormat::ASTC_6x6_RGBA) ? 6 : 4;
    uint32_t block_h = block_w;
    uint32_t blocks_x = (width + block_w - 1) / block_w;
    uint32_t blocks_y = (height + block_h - 1) / block_h;
    return static_cast<size_t>(blocks_x) * blocks_y * 16; // 128-bit per ASTC/ETC2 RGBA block
}

} // namespace overcooked
