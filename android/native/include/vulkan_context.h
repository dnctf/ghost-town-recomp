#pragma once
#include <android/native_window.h>
#include <vulkan/vulkan.h>
#include <vulkan/vulkan_android.h>
#include <cstdint>
#include <vector>

namespace overcooked {

struct AdrenoDeviceCaps {
    bool is_adreno = false;
    uint32_t adreno_series = 0; // 630, 730, 735, 740, 750
    VkDeviceSize safe_max_storage_buffer_range = 128ULL * 1024ULL * 1024ULL; // 128 MB Adreno cap
    VkDeviceSize min_ubo_offset_alignment = 256;
    uint32_t max_anisotropy = 4;
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
    bool CreateFramebuffersAndSync();
    void DestroySwapchainResources();

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
