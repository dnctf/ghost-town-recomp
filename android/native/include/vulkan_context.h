#pragma once
#include <android/native_window.h>
#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <cstdint>
#include <vector>
#include <mutex>

namespace overcooked {

struct AdrenoDeviceCaps {
    bool is_adreno = false;
    uint32_t adreno_series = 0; // Parsed from the runtime Vulkan device name.
    uint32_t vendor_id = 0;
    uint32_t device_id = 0;
    uint32_t vulkan_api_version = VK_API_VERSION_1_0;
    uint32_t driver_version = 0;
    VkDeviceSize safe_max_storage_buffer_range = 0;
    VkDeviceSize min_ubo_offset_alignment = 256;
    float max_sampler_anisotropy = 1.0f;
    bool supports_sampler_anisotropy = false;
    bool supports_dynamic_rendering = false;
};

class VulkanContext {
public:
    bool Initialize(ANativeWindow* window);
    void Resize(uint32_t width, uint32_t height);
    void RenderClearFrame(float r, float g, float b, float resolution_scale);
    void Shutdown();

    const AdrenoDeviceCaps& DeviceCaps() const { return gpu_caps_; }
    VkDevice Device() const { return device_; }
    VkRenderPass MainRenderPass() const { return main_render_pass_; }

private:
    bool CreateInstance();
    bool CreateAndroidSurface(ANativeWindow* window);
    bool SelectPhysicalDevice();
    bool CreateLogicalDevice();
    bool CreateSwapchain(ANativeWindow* window);
    bool CreateTBDRRenderPass();
    bool CreateFramebuffers();
    bool CreateSyncObjects();
    void DestroySwapchainResources();

    ANativeWindow* window_ = nullptr;
    std::mutex render_mutex_;

    VkInstance instance_ = VK_NULL_HANDLE;
    VkSurfaceKHR surface_ = VK_NULL_HANDLE;
    VkPhysicalDevice physical_device_ = VK_NULL_HANDLE;
    VkDevice device_ = VK_NULL_HANDLE;
    VkQueue graphics_queue_ = VK_NULL_HANDLE;
    uint32_t graphics_queue_family_ = UINT32_MAX;

    VkSwapchainKHR swapchain_ = VK_NULL_HANDLE;
    VkFormat swapchain_format_ = VK_FORMAT_R8G8B8A8_UNORM;
    VkExtent2D swapchain_extent_{};
    std::vector<VkImage> swapchain_images_;
    std::vector<VkImageView> swapchain_image_views_;
    std::vector<VkFramebuffer> framebuffers_;

    VkRenderPass main_render_pass_ = VK_NULL_HANDLE;
    VkCommandPool command_pool_ = VK_NULL_HANDLE;
    std::vector<VkCommandBuffer> command_buffers_;

    VkSemaphore image_available_sem_ = VK_NULL_HANDLE;
    VkSemaphore render_finished_sem_ = VK_NULL_HANDLE;
    VkFence in_flight_fence_ = VK_NULL_HANDLE;

    AdrenoDeviceCaps gpu_caps_{};
};

} // namespace overcooked
