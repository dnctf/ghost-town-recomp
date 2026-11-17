#include "native_engine.h"
#include "graphics_settings.h"
#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <chrono>
#include <cmath>
#include <thread>
#include <algorithm>
#include <sstream>
#include <vector>

#define LOG_TAG "OvercookedNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace overcooked {

NativeEngine::NativeEngine() {
    arena_ = std::make_unique<LinearMemoryArena>(32ULL * 1024ULL * 1024ULL);
}

NativeEngine& NativeEngine::Instance() {
    static NativeEngine instance;
    return instance;
}

void NativeEngine::Initialize(const std::string& data_dir, int backend_ordinal, int game_version) {
    data_dir_ = data_dir;
    backend_ordinal_ = backend_ordinal;

    if (!arena_) {
        arena_ = std::make_unique<LinearMemoryArena>(32ULL * 1024ULL * 1024ULL);
    }
    thermal_.Initialize();
    audio_.Initialize(48000, 2);
    runtime_host_.Initialize(data_dir_, backend_ordinal_, game_version);
    LOGI("NativeEngine diinisialisasi untuk %s (versi %d) di: %s",
         runtime_host_.GetGameTitleName(), game_version, data_dir_.c_str());
}

void NativeEngine::InitDisplayInfo(uint32_t width, uint32_t height, float dpi, float refresh_rate, uint32_t ram_mb) {
    uint32_t adreno_series = vulkan_.DeviceCaps().adreno_series;
    GraphicsSettingsManager::Instance().Initialize(width, height, dpi, refresh_rate, adreno_series, ram_mb);
}

void NativeEngine::OnSurfaceCreated(ANativeWindow* window) {
    if (render_thread_.joinable()) {
        running_.store(false);
        render_thread_.join();
    }
    if (window_) {
        ANativeWindow_release(window_);
        window_ = nullptr;
    }
    // Do not start the old orange clear-only demo loop. A Vulkan surface is not
    // a Unity player; rendering stays disabled until a real game runtime exists.
    window_ = window;
    LOGI("Native surface acquired; Unity rendering is unavailable in this build.");
}

void NativeEngine::OnSurfaceChanged(uint32_t width, uint32_t height) {
    (void) width;
    (void) height;
    // No game renderer is active yet. Avoid resizing an uninitialized Vulkan swapchain.
}

void NativeEngine::OnSurfaceDestroyed() {
    running_.store(false);
    if (render_thread_.joinable()) {
        render_thread_.join();
    }
    if (window_) {
        ANativeWindow_release(window_);
        window_ = nullptr;
    }
}

void NativeEngine::Pause() {
    paused_.store(true);
}

void NativeEngine::Resume() {
    paused_.store(false);
}

void NativeEngine::RenderLoop() {
    float phase = 0.0f;
    constexpr float kTwoPi = 6.28318530718f;

    while (running_.load()) {
        if (paused_.load()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
            continue;
        }

        auto frame_start = std::chrono::steady_clock::now();

        RenderBudgetConfig budget = thermal_.EvaluatePolicy();
        auto& gfx = GraphicsSettingsManager::Instance();
        const auto& gfx_cfg = gfx.GetConfig();

        // Target FPS dari pengaturan grafik atau regulasi termal
        uint32_t target_fps = std::min(gfx_cfg.target_fps, budget.target_fps);
        if (target_fps == 0) target_fps = 30;

        runtime_host_.TickFrame(1.0f / static_cast<float>(target_fps));

        // Hindari floating point overflow setelah running lama
        phase = std::fmod(phase + 0.02f, kTwoPi);
        // Warna tema Overcooked (oranye hangat / gold dapur) agar tidak black screen saat standby
        float r = 0.88f + 0.08f * std::sin(phase);
        float g = 0.42f + 0.08f * std::cos(phase);
        float b = 0.15f;

        // Hitung skala resolusi render aktif
        float active_scale = std::clamp(budget.resolution_scale * gfx_cfg.custom_resolution_scale, 0.4f, 1.25f);
        vulkan_.RenderClearFrame(r, g, b, active_scale);
        if (arena_) {
            arena_->ResetFrameScratch();
        }

        // Thermal-aware frame pacing & spin-wait sleep
        auto target_duration = std::chrono::microseconds(1000000 / target_fps);
        auto elapsed = std::chrono::steady_clock::now() - frame_start;
        if (elapsed < target_duration) {
            auto remaining = std::chrono::duration_cast<std::chrono::microseconds>(target_duration - elapsed);
            if (remaining.count() > budget.spin_wait_sleep_us) {
                std::this_thread::sleep_for(remaining - std::chrono::microseconds(budget.spin_wait_sleep_us));
            }
            while (std::chrono::steady_clock::now() - frame_start < target_duration) {
                std::this_thread::yield();
            }
        }
    }
}

void NativeEngine::Shutdown() {
    OnSurfaceDestroyed();
    audio_.Shutdown();
    runtime_host_.Shutdown();
}

} // namespace overcooked

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeGetVulkanDiagnostics(
        JNIEnv* env, jobject /*thiz*/) {
    std::ostringstream report;
    report << "Host Vulkan loader diagnostic (system loader; custom Turnip not selected).\\n";
    uint32_t loader_version = VK_API_VERSION_1_0;
    auto enumerate_version = reinterpret_cast<PFN_vkEnumerateInstanceVersion>(vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkEnumerateInstanceVersion"));
    if (enumerate_version && enumerate_version(&loader_version) == VK_SUCCESS) {
        report << "Loader API: " << VK_VERSION_MAJOR(loader_version) << "." << VK_VERSION_MINOR(loader_version) << "." << VK_VERSION_PATCH(loader_version) << "\\n";
    } else report << "Loader API: Vulkan 1.0 or version query unavailable\\n";
    VkApplicationInfo app{}; app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.pApplicationName = "GhostTownGraphicsDiagnostics";
    app.apiVersion = std::min(loader_version, VK_API_VERSION_1_1);
    VkInstanceCreateInfo create{}; create.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO; create.pApplicationInfo = &app;
    VkInstance instance = VK_NULL_HANDLE;
    VkResult result = vkCreateInstance(&create, nullptr, &instance);
    if (result != VK_SUCCESS) { report << "vkCreateInstance failed: " << static_cast<int>(result) << "\\n"; return env->NewStringUTF(report.str().c_str()); }
    uint32_t count = 0; result = vkEnumeratePhysicalDevices(instance, &count, nullptr);
    report << "Physical device enumeration: " << static_cast<int>(result) << ", count=" << count << "\\n";
    if (result == VK_SUCCESS && count > 0) {
        std::vector<VkPhysicalDevice> devices(count);
        result = vkEnumeratePhysicalDevices(instance, &count, devices.data());
        for (uint32_t i = 0; result == VK_SUCCESS && i < count; ++i) {
            VkPhysicalDeviceProperties props{}; vkGetPhysicalDeviceProperties(devices[i], &props);
            report << "GPU[" << i << "]: " << props.deviceName << "\\n"
                   << "  API: " << VK_VERSION_MAJOR(props.apiVersion) << "." << VK_VERSION_MINOR(props.apiVersion) << "." << VK_VERSION_PATCH(props.apiVersion) << "\\n"
                   << "  Driver version raw: " << props.driverVersion << "\\n"
                   << "  Vendor ID: 0x" << std::hex << props.vendorID << " Device ID: 0x" << props.deviceID << std::dec << "\\n";
            if (props.apiVersion >= VK_API_VERSION_1_1) {
                VkPhysicalDeviceDriverProperties driver{}; driver.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES;
                VkPhysicalDeviceProperties2 props2{}; props2.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2; props2.pNext = &driver;
                vkGetPhysicalDeviceProperties2(devices[i], &props2);
                report << "  Driver name: " << driver.driverName << "\\n  Driver info: " << driver.driverInfo << "\\n";
            } else report << "  Driver name/info: VK_KHR_driver_properties query unavailable.\\n";
        }
    }
    vkDestroyInstance(instance, nullptr);
    return env->NewStringUTF(report.str().c_str());
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeInitEngine(
        JNIEnv* env, jobject /*thiz*/, jstring data_dir, jint backend_ordinal, jint game_version) {
    const char* raw_path = env->GetStringUTFChars(data_dir, nullptr);
    std::string path(raw_path ? raw_path : "");
    if (raw_path) env->ReleaseStringUTFChars(data_dir, raw_path);
    overcooked::NativeEngine::Instance().Initialize(path, backend_ordinal, game_version);
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeInitDisplayInfo(
        JNIEnv* /*env*/, jobject /*thiz*/,
        jint width, jint height, jfloat dpi, jfloat refresh_rate, jint ram_mb) {
    overcooked::NativeEngine::Instance().InitDisplayInfo(
        static_cast<uint32_t>(width),
        static_cast<uint32_t>(height),
        dpi,
        refresh_rate,
        static_cast<uint32_t>(ram_mb)
    );
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeSetGraphicsPreset(
        JNIEnv* /*env*/, jobject /*thiz*/, jint preset_ordinal) {
    overcooked::GraphicsSettingsManager::Instance().ApplyPreset(
        static_cast<overcooked::GraphicsPreset>(preset_ordinal)
    );
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeSetResolutionMode(
        JNIEnv* /*env*/, jobject /*thiz*/, jint mode_ordinal, jfloat custom_scale) {
    auto& mgr = overcooked::GraphicsSettingsManager::Instance();
    mgr.SetResolutionMode(static_cast<overcooked::ResolutionMode>(mode_ordinal));
    if (custom_scale > 0.0f) {
        mgr.SetCustomResolutionScale(custom_scale);
    }
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeSetTargetFPS(
        JNIEnv* /*env*/, jobject /*thiz*/, jint fps) {
    overcooked::GraphicsSettingsManager::Instance().SetTargetFPS(static_cast<uint32_t>(fps));
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeSetVsync(
        JNIEnv* /*env*/, jobject /*thiz*/, jboolean enabled) {
    overcooked::GraphicsSettingsManager::Instance().SetVsync(enabled == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeSetAntiAliasing(
        JNIEnv* /*env*/, jobject /*thiz*/, jint mode_ordinal) {
    overcooked::GraphicsSettingsManager::Instance().SetAntiAliasing(
        static_cast<overcooked::AntiAliasingMode>(mode_ordinal)
    );
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeSetShadowQuality(
        JNIEnv* /*env*/, jobject /*thiz*/, jint quality_ordinal) {
    overcooked::GraphicsSettingsManager::Instance().SetShadowQuality(
        static_cast<overcooked::ShadowQuality>(quality_ordinal)
    );
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeSetTextureQuality(
        JNIEnv* /*env*/, jobject /*thiz*/, jint quality_ordinal) {
    overcooked::GraphicsSettingsManager::Instance().SetTextureQuality(
        static_cast<overcooked::TextureQuality>(quality_ordinal)
    );
}

JNIEXPORT jstring JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeGetGraphicsSummary(
        JNIEnv* env, jobject /*thiz*/) {
    std::string summary = overcooked::GraphicsSettingsManager::Instance().GetSummaryString();
    return env->NewStringUTF(summary.c_str());
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativePauseEngine(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    overcooked::NativeEngine::Instance().Pause();
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeResumeEngine(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    overcooked::NativeEngine::Instance().Resume();
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeOnSurfaceCreated(
        JNIEnv* env, jobject /*thiz*/, jobject surface) {
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    overcooked::NativeEngine::Instance().OnSurfaceCreated(window);
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeOnSurfaceChanged(
        JNIEnv* /*env*/, jobject /*thiz*/, jint width, jint height) {
    overcooked::NativeEngine::Instance().OnSurfaceChanged(
        static_cast<uint32_t>(width), static_cast<uint32_t>(height));
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeOnSurfaceDestroyed(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    overcooked::NativeEngine::Instance().OnSurfaceDestroyed();
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeOnGamepadAxis(
        JNIEnv* /*env*/, jobject /*thiz*/, jint device_id,
        jfloat lx, jfloat ly, jfloat rx, jfloat ry, jfloat hat_x, jfloat hat_y) {
    overcooked::NativeEngine::Instance().GetInputMapper().UpdateAxis(
        device_id, lx, ly, rx, ry, hat_x, hat_y);
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeOnGamepadButton(
        JNIEnv* /*env*/, jobject /*thiz*/, jint device_id, jint key_code, jboolean pressed) {
    overcooked::NativeEngine::Instance().GetInputMapper().UpdateButton(
        device_id, key_code, pressed == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_overcookedrecomp_app_MainActivity_nativeShutdownEngine(
        JNIEnv* /*env*/, jobject /*thiz*/) {
    overcooked::NativeEngine::Instance().Shutdown();
}

} // extern "C"
