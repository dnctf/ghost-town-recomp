#include "graphics_settings.h"
#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <fstream>
#include <sstream>

#define LOG_TAG "OvercookedGraphics"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace overcooked {

GraphicsSettingsManager& GraphicsSettingsManager::Instance() {
    static GraphicsSettingsManager instance;
    return instance;
}

void GraphicsSettingsManager::Initialize(uint32_t width, uint32_t height, float dpi, float refresh_rate,
                                        uint32_t adreno_series, uint32_t total_ram_mb) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    // Pastikan orientasi landscape (width >= height)
    if (width < height) {
        std::swap(width, height);
    }
    
    display_.native_width = (width > 0) ? width : 1920;
    display_.native_height = (height > 0) ? height : 1080;
    display_.density_dpi = (dpi > 0.0f) ? dpi : 400.0f;
    display_.refresh_rate = (refresh_rate > 0.0f) ? refresh_rate : 60.0f;
    display_.aspect_ratio = static_cast<float>(display_.native_width) / static_cast<float>(display_.native_height);

    adreno_series_ = adreno_series;
    total_ram_mb_ = total_ram_mb;

    current_preset_ = AutoDetectPreset(adreno_series, total_ram_mb);
    ApplyPreset(current_preset_);

    LOGI("GraphicsSettingsManager aktif: Layar HP %ux%u @ %.1fHz (%.0f dpi, ratio %.2f), Adreno %u, RAM %u MB",
         display_.native_width, display_.native_height, display_.refresh_rate, display_.density_dpi,
         display_.aspect_ratio, adreno_series_, total_ram_mb_);
}

void GraphicsSettingsManager::UpdateGpuCapabilities(uint32_t adreno_series) {
    std::lock_guard<std::mutex> lock(mutex_);
    adreno_series_ = adreno_series;
    current_preset_ = AutoDetectPreset(adreno_series_, total_ram_mb_);
    ApplyPreset(current_preset_);
    LOGI("GPU profile refreshed after Vulkan discovery: Adreno %u, RAM %u MB, preset %s",
         adreno_series_, total_ram_mb_, GetPresetString().c_str());
}

GraphicsPreset GraphicsSettingsManager::AutoDetectPreset(uint32_t adreno_series, uint32_t total_ram_mb) const {
    // Conservative starting presets: GPU identity and RAM are read at runtime.
    // Unknown GPUs stay LOW rather than being assigned an unverified profile.
    // Adreno 630 covers Snapdragon 845; 640/650/660 cover later pre-8-series
    // flagships; 7xx/8xx generations use progressively higher defaults.
    if (adreno_series >= 750 && total_ram_mb >= 12000) {
        return GraphicsPreset::ULTRA;
    }
    if (adreno_series >= 730 && total_ram_mb >= 8000) {
        return GraphicsPreset::HIGH;
    }
    if (adreno_series >= 640 && total_ram_mb >= 6000) {
        return GraphicsPreset::MEDIUM;
    }
    return GraphicsPreset::LOW;
}

void GraphicsSettingsManager::ApplyPreset(GraphicsPreset preset) {
    current_preset_ = preset;

    switch (preset) {
        case GraphicsPreset::LOW:
            config_.resolution_mode = ResolutionMode::FIXED_720P;
            config_.custom_resolution_scale = 0.75f;
            config_.anti_aliasing = AntiAliasingMode::OFF;
            config_.shadow_quality = ShadowQuality::OFF;
            config_.texture_quality = TextureQuality::LOW;
            config_.target_fps = 30;
            config_.vsync_enabled = true;
            config_.bloom_enabled = false;
            config_.ambient_occlusion = false;
            config_.render_distance = 0.75f;
            config_.anisotropic_level = 1;
            config_.tbdr_optimization = true;
            config_.adreno_binning_control = true;
            break;

        case GraphicsPreset::MEDIUM:
            config_.resolution_mode = ResolutionMode::ADAPTIVE;
            config_.custom_resolution_scale = 0.85f;
            config_.anti_aliasing = AntiAliasingMode::FXAA;
            config_.shadow_quality = ShadowQuality::LOW;
            config_.texture_quality = TextureQuality::MEDIUM;
            config_.target_fps = 30;
            config_.vsync_enabled = true;
            config_.bloom_enabled = true;
            config_.ambient_occlusion = false;
            config_.render_distance = 0.9f;
            config_.anisotropic_level = 2;
            config_.tbdr_optimization = true;
            config_.adreno_binning_control = true;
            break;

        case GraphicsPreset::HIGH:
            config_.resolution_mode = ResolutionMode::NATIVE_DISPLAY;
            config_.custom_resolution_scale = 1.0f;
            config_.anti_aliasing = AntiAliasingMode::FXAA;
            config_.shadow_quality = ShadowQuality::MEDIUM;
            config_.texture_quality = TextureQuality::HIGH;
            config_.target_fps = (display_.refresh_rate >= 90.0f) ? 60 : 60;
            config_.vsync_enabled = true;
            config_.bloom_enabled = true;
            config_.ambient_occlusion = true;
            config_.render_distance = 1.0f;
            config_.anisotropic_level = 8;
            config_.tbdr_optimization = true;
            config_.adreno_binning_control = false;
            break;

        case GraphicsPreset::ULTRA:
            config_.resolution_mode = ResolutionMode::NATIVE_DISPLAY;
            config_.custom_resolution_scale = 1.0f;
            config_.anti_aliasing = AntiAliasingMode::MSAA_4X;
            config_.shadow_quality = ShadowQuality::HIGH;
            config_.texture_quality = TextureQuality::ULTRA;
            config_.target_fps = 60;
            config_.vsync_enabled = true;
            config_.bloom_enabled = true;
            config_.ambient_occlusion = true;
            config_.render_distance = 1.25f;
            config_.anisotropic_level = 16;
            config_.tbdr_optimization = true;
            config_.adreno_binning_control = false;
            break;

        case GraphicsPreset::CUSTOM:
            break;
    }
}

void GraphicsSettingsManager::SetResolutionMode(ResolutionMode mode) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.resolution_mode = mode;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetCustomResolutionScale(float scale) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.custom_resolution_scale = std::clamp(scale, 0.5f, 1.5f);
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetTargetFPS(uint32_t fps) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.target_fps = (fps <= 30) ? 30 : 60;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetVsync(bool enabled) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.vsync_enabled = enabled;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetAntiAliasing(AntiAliasingMode mode) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.anti_aliasing = mode;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetShadowQuality(ShadowQuality quality) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.shadow_quality = quality;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetTextureQuality(TextureQuality quality) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.texture_quality = quality;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetBloom(bool enabled) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.bloom_enabled = enabled;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetAmbientOcclusion(bool enabled) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.ambient_occlusion = enabled;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetRenderDistance(float distance) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_.render_distance = std::clamp(distance, 0.5f, 1.5f);
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::SetAnisotropicLevel(uint32_t level) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (level <= 1) config_.anisotropic_level = 1;
    else if (level <= 2) config_.anisotropic_level = 2;
    else if (level <= 4) config_.anisotropic_level = 4;
    else if (level <= 8) config_.anisotropic_level = 8;
    else config_.anisotropic_level = 16;
    current_preset_ = GraphicsPreset::CUSTOM;
}

void GraphicsSettingsManager::ComputeEffectiveResolution(uint32_t& out_w, uint32_t& out_h, float thermal_scale) const {
    float scale = 1.0f;
    float aspect = (display_.aspect_ratio > 0.0f) ? display_.aspect_ratio : (16.0f / 9.0f);

    switch (config_.resolution_mode) {
        case ResolutionMode::NATIVE_DISPLAY: {
            scale = std::clamp(thermal_scale, 0.75f, 1.0f);
            out_w = static_cast<uint32_t>(static_cast<float>(display_.native_width) * scale);
            out_h = static_cast<uint32_t>(static_cast<float>(display_.native_height) * scale);
            break;
        }
        case ResolutionMode::ADAPTIVE: {
            scale = std::clamp(config_.custom_resolution_scale * thermal_scale, 0.5f, 1.25f);
            out_w = static_cast<uint32_t>(static_cast<float>(display_.native_width) * scale);
            out_h = static_cast<uint32_t>(static_cast<float>(display_.native_height) * scale);
            break;
        }
        case ResolutionMode::FIXED_720P: {
            // Menyesuaikan aspect ratio layar HP dengan basis tinggi 720p
            out_h = 720;
            out_w = static_cast<uint32_t>(720.0f * aspect);
            break;
        }
        case ResolutionMode::FIXED_1080P: {
            out_h = 1080;
            out_w = static_cast<uint32_t>(1080.0f * aspect);
            break;
        }
        case ResolutionMode::FIXED_540P: {
            out_h = 540;
            out_w = static_cast<uint32_t>(540.0f * aspect);
            break;
        }
        case ResolutionMode::CUSTOM_SCALE: {
            scale = std::clamp(config_.custom_resolution_scale * thermal_scale, 0.4f, 1.5f);
            out_w = static_cast<uint32_t>(static_cast<float>(display_.native_width) * scale);
            out_h = static_cast<uint32_t>(static_cast<float>(display_.native_height) * scale);
            break;
        }
    }

    // Pastikan resolusi genap (kelipatan 2) untuk framebuffers dan swapchain Vulkan
    out_w = (out_w / 2) * 2;
    out_h = (out_h / 2) * 2;
    if (out_w < 320) out_w = 320;
    if (out_h < 240) out_h = 240;
}

const GraphicsConfig& GraphicsSettingsManager::GetConfig() const {
    return config_;
}

const DeviceDisplayInfo& GraphicsSettingsManager::GetDisplayInfo() const {
    return display_;
}

GraphicsPreset GraphicsSettingsManager::GetCurrentPreset() const {
    return current_preset_;
}

bool GraphicsSettingsManager::SaveToFile(const std::string& path) const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::ofstream file(path);
    if (!file.is_open()) return false;

    file << "# Overcooked Recomp Graphics Config\n";
    file << "preset=" << static_cast<int>(current_preset_) << "\n";
    file << "resolution_mode=" << static_cast<int>(config_.resolution_mode) << "\n";
    file << "resolution_scale=" << config_.custom_resolution_scale << "\n";
    file << "target_fps=" << config_.target_fps << "\n";
    file << "vsync=" << (config_.vsync_enabled ? 1 : 0) << "\n";
    file << "anti_aliasing=" << static_cast<int>(config_.anti_aliasing) << "\n";
    file << "shadow_quality=" << static_cast<int>(config_.shadow_quality) << "\n";
    file << "texture_quality=" << static_cast<int>(config_.texture_quality) << "\n";
    file << "bloom=" << (config_.bloom_enabled ? 1 : 0) << "\n";
    file << "ambient_occlusion=" << (config_.ambient_occlusion ? 1 : 0) << "\n";
    file << "render_distance=" << config_.render_distance << "\n";
    file << "anisotropic=" << config_.anisotropic_level << "\n";
    return true;
}

bool GraphicsSettingsManager::LoadFromFile(const std::string& path) {
    std::lock_guard<std::mutex> lock(mutex_);
    std::ifstream file(path);
    if (!file.is_open()) return false;

    std::string line;
    while (std::getline(file, line)) {
        if (line.empty() || line[0] == '#') continue;
        size_t eq = line.find('=');
        if (eq == std::string::npos) continue;

        std::string key = line.substr(0, eq);
        std::string val = line.substr(eq + 1);

        if (key == "preset") current_preset_ = static_cast<GraphicsPreset>(std::stoi(val));
        else if (key == "resolution_mode") config_.resolution_mode = static_cast<ResolutionMode>(std::stoi(val));
        else if (key == "resolution_scale") config_.custom_resolution_scale = std::stof(val);
        else if (key == "target_fps") config_.target_fps = static_cast<uint32_t>(std::stoi(val));
        else if (key == "vsync") config_.vsync_enabled = (val == "1");
        else if (key == "anti_aliasing") config_.anti_aliasing = static_cast<AntiAliasingMode>(std::stoi(val));
        else if (key == "shadow_quality") config_.shadow_quality = static_cast<ShadowQuality>(std::stoi(val));
        else if (key == "texture_quality") config_.texture_quality = static_cast<TextureQuality>(std::stoi(val));
        else if (key == "bloom") config_.bloom_enabled = (val == "1");
        else if (key == "ambient_occlusion") config_.ambient_occlusion = (val == "1");
        else if (key == "render_distance") config_.render_distance = std::stof(val);
        else if (key == "anisotropic") config_.anisotropic_level = static_cast<uint32_t>(std::stoi(val));
    }
    return true;
}

std::string GraphicsSettingsManager::GetResolutionString() const {
    uint32_t w = 0, h = 0;
    ComputeEffectiveResolution(w, h, 1.0f);
    std::ostringstream ss;
    ss << w << "x" << h;
    switch (config_.resolution_mode) {
        case ResolutionMode::NATIVE_DISPLAY: ss << " (Layar Asli HP)"; break;
        case ResolutionMode::ADAPTIVE: ss << " (Adaptif Termal)"; break;
        case ResolutionMode::FIXED_720P: ss << " (Skala 720p)"; break;
        case ResolutionMode::FIXED_1080P: ss << " (Skala 1080p)"; break;
        case ResolutionMode::FIXED_540P: ss << " (Skala 540p)"; break;
        case ResolutionMode::CUSTOM_SCALE:
            ss << " (Kustom " << static_cast<int>(config_.custom_resolution_scale * 100.0f) << "%)";
            break;
    }
    return ss.str();
}

std::string GraphicsSettingsManager::GetPresetString() const {
    switch (current_preset_) {
        case GraphicsPreset::LOW: return "Rendah (Low - Hemat Daya)";
        case GraphicsPreset::MEDIUM: return "Sedang (Medium - Seimbang)";
        case GraphicsPreset::HIGH: return "Tinggi (High - Grafis Tajam)";
        case GraphicsPreset::ULTRA: return "Ultra (Flagship 60 FPS)";
        case GraphicsPreset::CUSTOM: return "Kustom (Custom)";
    }
    return "Unknown";
}

std::string GraphicsSettingsManager::GetSummaryString() const {
    std::ostringstream ss;
    ss << "=== Profil Pengaturan Grafis Overcooked Recomp ===\n";
    ss << "Preset: " << GetPresetString() << "\n";
    ss << "Resolusi Render: " << GetResolutionString() << "\n";
    ss << "Layar Fisik: " << display_.native_width << "x" << display_.native_height
       << " @ " << display_.refresh_rate << "Hz (" << display_.density_dpi << " dpi)\n";
    ss << "Target FPS: " << config_.target_fps << " | V-Sync: " << (config_.vsync_enabled ? "ON" : "OFF") << "\n";

    const char* aa_str[] = {"OFF", "FXAA", "MSAA 2x", "MSAA 4x"};
    ss << "Anti-Aliasing: " << aa_str[static_cast<int>(config_.anti_aliasing)] << "\n";

    const char* sh_str[] = {"OFF", "Low (512)", "Medium (1024)", "High (2048)"};
    ss << "Bayangan: " << sh_str[static_cast<int>(config_.shadow_quality)] << "\n";

    const char* tx_str[] = {"Low (1/4)", "Medium (1/2)", "High (1:1)", "Ultra (Aniso 16x)"};
    ss << "Tekstur: " << tx_str[static_cast<int>(config_.texture_quality)] << "\n";

    ss << "Bloom: " << (config_.bloom_enabled ? "ON" : "OFF")
       << " | AO: " << (config_.ambient_occlusion ? "ON" : "OFF") << "\n";
    ss << "Skala Jarak: " << static_cast<int>(config_.render_distance * 100) << "%\n";
    ss << "Anisotropy: " << config_.anisotropic_level << "x\n";
    return ss.str();
}

} // namespace overcooked
