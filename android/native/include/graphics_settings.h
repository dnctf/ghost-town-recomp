#pragma once
#include <android/native_window.h>
#include <algorithm>
#include <atomic>
#include <cstdint>
#include <mutex>
#include <string>

namespace overcooked {

// Preset kualitas grafik yang bisa dipilih pengguna
enum class GraphicsPreset : int {
    LOW = 0,       // Untuk HP entry/low-end (Snapdragon 845, 4GB RAM)
    MEDIUM = 1,    // Untuk HP mid-range (Snapdragon 870/888, 6-8GB RAM)
    HIGH = 2,      // Untuk HP high-end (Snapdragon 8 Gen 1/2, 8-12GB RAM)
    ULTRA = 3,     // Untuk HP flagship (Snapdragon 8 Gen 3+, 12GB+ RAM)
    CUSTOM = 4     // Pengaturan manual pengguna
};

// Mode resolusi yang mengikuti layar HP
enum class ResolutionMode : int {
    NATIVE_DISPLAY = 0, // 100% resolusi layar HP asli
    ADAPTIVE = 1,       // Otomatis turun/naik sesuai termal Snapdragon (75% - 100%)
    FIXED_720P = 2,     // 1280x720 (Hemat daya & baterai)
    FIXED_1080P = 3,    // 1920x1080 (FHD standar)
    FIXED_540P = 4,     // 960x540 (Performa maksimal pada SoC lama)
    CUSTOM_SCALE = 5    // Skala kustom (50% - 150%)
};

// Anti-aliasing
enum class AntiAliasingMode : int {
    OFF = 0,
    FXAA = 1,
    MSAA_2X = 2,
    MSAA_4X = 3
};

// Kualitas Shadow
enum class ShadowQuality : int {
    OFF = 0,
    LOW = 1,
    MEDIUM = 2,
    HIGH = 3
};

// Kualitas Tekstur
enum class TextureQuality : int {
    LOW = 0,
    MEDIUM = 1,
    HIGH = 2,
    ULTRA = 3
};

// Konfigurasi lengkap pengaturan grafik
struct GraphicsConfig {
    ResolutionMode resolution_mode = ResolutionMode::NATIVE_DISPLAY;
    float custom_resolution_scale = 1.0f; // 0.5f - 1.5f
    
    AntiAliasingMode anti_aliasing = AntiAliasingMode::FXAA;
    ShadowQuality shadow_quality = ShadowQuality::MEDIUM;
    TextureQuality texture_quality = TextureQuality::HIGH;

    uint32_t target_fps = 60;
    bool vsync_enabled = true;

    bool bloom_enabled = true;
    bool ambient_occlusion = false;
    float render_distance = 1.0f;
    uint32_t anisotropic_level = 4;

    bool tbdr_optimization = true;
    bool adreno_binning_control = true;
};

// Informasi Layar Ponsel
struct DeviceDisplayInfo {
    uint32_t native_width = 1920;
    uint32_t native_height = 1080;
    float density_dpi = 400.0f;
    float refresh_rate = 60.0f;
    float aspect_ratio = 16.0f / 9.0f;
};

class GraphicsSettingsManager {
public:
    static GraphicsSettingsManager& Instance();

    void Initialize(uint32_t width, uint32_t height, float dpi, float refresh_rate,
                    uint32_t adreno_series, uint32_t total_ram_mb);
    void UpdateGpuCapabilities(uint32_t adreno_series);

    // Deteksi otomatis preset terbaik untuk SoC HP
    GraphicsPreset AutoDetectPreset(uint32_t adreno_series, uint32_t total_ram_mb) const;
    void ApplyPreset(GraphicsPreset preset);

    // Pengaturan resolusi & grafik
    void SetResolutionMode(ResolutionMode mode);
    void SetCustomResolutionScale(float scale);
    void SetTargetFPS(uint32_t fps);
    void SetVsync(bool enabled);
    void SetAntiAliasing(AntiAliasingMode mode);
    void SetShadowQuality(ShadowQuality quality);
    void SetTextureQuality(TextureQuality quality);
    void SetBloom(bool enabled);
    void SetAmbientOcclusion(bool enabled);
    void SetRenderDistance(float distance);
    void SetAnisotropicLevel(uint32_t level);

    // Perhitungan resolusi render aktif berdasarkan layar HP & faktor termal
    void ComputeEffectiveResolution(uint32_t& out_w, uint32_t& out_h, float thermal_scale = 1.0f) const;

    // Getters
    const GraphicsConfig& GetConfig() const;
    const DeviceDisplayInfo& GetDisplayInfo() const;
    GraphicsPreset GetCurrentPreset() const;

    // Simpan & Muat konfigurasi
    bool SaveToFile(const std::string& path) const;
    bool LoadFromFile(const std::string& path);

    // Summary strings untuk UI Android
    std::string GetResolutionString() const;
    std::string GetPresetString() const;
    std::string GetSummaryString() const;

private:
    GraphicsSettingsManager() = default;

    mutable std::mutex mutex_;
    GraphicsConfig config_{};
    DeviceDisplayInfo display_{};
    GraphicsPreset current_preset_ = GraphicsPreset::MEDIUM;
    uint32_t adreno_series_ = 0;
    uint32_t total_ram_mb_ = 0;
};

} // namespace overcooked
