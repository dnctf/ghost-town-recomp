#include "vulkan_context.h"
#include "graphics_settings.h"
#include <android/log.h>
#include <algorithm>
#include <cstring>
#include <cctype>
#include <cstdlib>

#define LOG_TAG "OvercookedVulkan"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace overcooked {

bool VulkanContext::Initialize(ANativeWindow* window) {
    std::lock_guard<std::mutex> lock(render_mutex_);
    window_ = window;
    if (!CreateInstance()) { LOGE("Vulkan: CreateInstance gagal"); return false; }
    if (!CreateAndroidSurface(window)) { LOGE("Vulkan: CreateAndroidSurface gagal"); return false; }
    if (!SelectPhysicalDevice()) { LOGE("Vulkan: SelectPhysicalDevice gagal"); return false; }
    if (!CreateLogicalDevice()) { LOGE("Vulkan: CreateLogicalDevice gagal"); return false; }
    if (!CreateSwapchain(window)) { LOGE("Vulkan: CreateSwapchain gagal"); return false; }
    if (!CreateTBDRRenderPass()) { LOGE("Vulkan: CreateTBDRRenderPass gagal"); return false; }
    if (!CreateSyncObjects()) { LOGE("Vulkan: CreateSyncObjects gagal"); return false; }
    if (!CreateFramebuffers()) { LOGE("Vulkan: CreateFramebuffers gagal"); return false; }
    LOGI("VulkanContext berhasil diinisialisasi sepenuhnya!");
    return true;
}

bool VulkanContext::CreateInstance() {
    VkApplicationInfo app_info{};
    app_info.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app_info.pApplicationName = "overcooked-recomp";
    app_info.applicationVersion = VK_MAKE_VERSION(0, 1, 0);
    app_info.pEngineName = "OvercookedNativeShim";
    app_info.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    app_info.apiVersion = VK_API_VERSION_1_0;

    const char* extensions[] = {
        VK_KHR_SURFACE_EXTENSION_NAME,
        VK_KHR_ANDROID_SURFACE_EXTENSION_NAME
    };

    VkInstanceCreateInfo info{};
    info.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    info.pApplicationInfo = &app_info;
    info.enabledExtensionCount = 2;
    info.ppEnabledExtensionNames = extensions;

    return vkCreateInstance(&info, nullptr, &instance_) == VK_SUCCESS;
}

bool VulkanContext::CreateAndroidSurface(ANativeWindow* window) {
    VkAndroidSurfaceCreateInfoKHR info{};
    info.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    info.window = window;
    return vkCreateAndroidSurfaceKHR(instance_, &info, nullptr, &surface_) == VK_SUCCESS;
}

bool VulkanContext::SelectPhysicalDevice() {
    uint32_t count = 0;
    vkEnumeratePhysicalDevices(instance_, &count, nullptr);
    if (count == 0) return false;

    std::vector<VkPhysicalDevice> devices(count);
    vkEnumeratePhysicalDevices(instance_, &count, devices.data());
    physical_device_ = devices[0];

    VkPhysicalDeviceProperties props{};
    vkGetPhysicalDeviceProperties(physical_device_, &props);
    LOGI("GPU Detected: %s (Vulkan %u.%u)", props.deviceName,
         VK_VERSION_MAJOR(props.apiVersion), VK_VERSION_MINOR(props.apiVersion));

    gpu_caps_.vendor_id = props.vendorID;
    gpu_caps_.device_id = props.deviceID;
    gpu_caps_.vulkan_api_version = props.apiVersion;
    gpu_caps_.driver_version = props.driverVersion;
    gpu_caps_.min_ubo_offset_alignment = props.limits.minUniformBufferOffsetAlignment;
    gpu_caps_.safe_max_storage_buffer_range = props.limits.maxStorageBufferRange;
    gpu_caps_.max_sampler_anisotropy = std::max(1.0f, props.limits.maxSamplerAnisotropy);

    VkPhysicalDeviceFeatures supported_features{};
    vkGetPhysicalDeviceFeatures(physical_device_, &supported_features);
    gpu_caps_.supports_sampler_anisotropy = supported_features.samplerAnisotropy == VK_TRUE;

    // Parse the Adreno model from the Vulkan device name. This avoids a short
    // allow-list that silently misses Adreno 640/650/660 and newer generations.
    const char* adreno = std::strstr(props.deviceName, "Adreno");
    if (adreno != nullptr) {
        gpu_caps_.is_adreno = true;
        while (*adreno != '\0' && !std::isdigit(static_cast<unsigned char>(*adreno))) {
            ++adreno;
        }
        if (*adreno != '\0') {
            char* end = nullptr;
            const unsigned long parsed = std::strtoul(adreno, &end, 10);
            if (end != adreno && parsed <= UINT32_MAX) {
                gpu_caps_.adreno_series = static_cast<uint32_t>(parsed);
            }
        }
    }

    LOGI("GPU capabilities: name=%s vendor=0x%04x device=0x%04x Vulkan=%u.%u driver=%u "
         "Adreno=%u maxStorage=%llu maxAnisotropy=%.1f anisotropyFeature=%s",
         props.deviceName, props.vendorID, props.deviceID,
         VK_VERSION_MAJOR(props.apiVersion), VK_VERSION_MINOR(props.apiVersion),
         props.driverVersion, gpu_caps_.adreno_series,
         static_cast<unsigned long long>(gpu_caps_.safe_max_storage_buffer_range),
         gpu_caps_.max_sampler_anisotropy,
         gpu_caps_.supports_sampler_anisotropy ? "yes" : "no");

    // Java reports display/RAM before the Surface exists. Refresh the automatic
    // profile now that the real Vulkan GPU has been discovered.
    GraphicsSettingsManager::Instance().UpdateGpuCapabilities(gpu_caps_.adreno_series);

    uint32_t queue_count = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(physical_device_, &queue_count, nullptr);
    std::vector<VkQueueFamilyProperties> queues(queue_count);
    vkGetPhysicalDeviceQueueFamilyProperties(physical_device_, &queue_count, queues.data());

    for (uint32_t i = 0; i < queue_count; ++i) {
        VkBool32 present = VK_FALSE;
        vkGetPhysicalDeviceSurfaceSupportKHR(physical_device_, i, surface_, &present);
        if ((queues[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) && present) {
            graphics_queue_family_ = i;
            break;
        }
    }
    return graphics_queue_family_ != UINT32_MAX;
}

bool VulkanContext::CreateLogicalDevice() {
    float priority = 1.0f;
    VkDeviceQueueCreateInfo q_info{};
    q_info.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    q_info.queueFamilyIndex = graphics_queue_family_;
    q_info.queueCount = 1;
    q_info.pQueuePriorities = &priority;

    const char* ext[] = { VK_KHR_SWAPCHAIN_EXTENSION_NAME };
    VkDeviceCreateInfo dev_info{};
    dev_info.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    dev_info.queueCreateInfoCount = 1;
    dev_info.pQueueCreateInfos = &q_info;
    dev_info.enabledExtensionCount = 1;
    dev_info.ppEnabledExtensionNames = ext;

    if (vkCreateDevice(physical_device_, &dev_info, nullptr, &device_) != VK_SUCCESS) return false;
    vkGetDeviceQueue(device_, graphics_queue_family_, 0, &graphics_queue_);
    return true;
}

bool VulkanContext::CreateSwapchain(ANativeWindow* window) {
    VkSurfaceCapabilitiesKHR caps{};
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physical_device_, surface_, &caps);

    // Kueri format warna yang didukung oleh layar HP
    uint32_t format_count = 0;
    vkGetPhysicalDeviceSurfaceFormatsKHR(physical_device_, surface_, &format_count, nullptr);
    if (format_count > 0) {
        std::vector<VkSurfaceFormatKHR> formats(format_count);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physical_device_, surface_, &format_count, formats.data());
        swapchain_format_ = formats[0].format;
        for (const auto& f : formats) {
            if (f.format == VK_FORMAT_R8G8B8A8_UNORM || f.format == VK_FORMAT_B8G8R8A8_UNORM) {
                swapchain_format_ = f.format;
                break;
            }
        }
    }

    if (caps.currentExtent.width != UINT32_MAX) {
        swapchain_extent_ = caps.currentExtent;
    } else {
        uint32_t w = static_cast<uint32_t>(ANativeWindow_getWidth(window));
        uint32_t h = static_cast<uint32_t>(ANativeWindow_getHeight(window));
        swapchain_extent_.width = std::clamp(w, caps.minImageExtent.width, caps.maxImageExtent.width);
        swapchain_extent_.height = std::clamp(h, caps.minImageExtent.height, caps.maxImageExtent.height);
    }

    // Triple buffering mencegah black frames dan stall pada Adreno
    uint32_t max_count = caps.maxImageCount > 0 ? caps.maxImageCount : 3u;
    uint32_t img_count = std::clamp(3u, caps.minImageCount, max_count);

    // Composite alpha yang kompatibel dengan Android OS (OPAQUE adalah standar Android)
    VkCompositeAlphaFlagBitsKHR composite_alpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    if (!(caps.supportedCompositeAlpha & VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)) {
        if (caps.supportedCompositeAlpha & VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR) {
            composite_alpha = VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR;
        } else if (caps.supportedCompositeAlpha & VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR) {
            composite_alpha = VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR;
        }
    }

    VkSurfaceTransformFlagBitsKHR pre_transform = caps.currentTransform;
    if (caps.supportedTransforms & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) {
        pre_transform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
    }

    VkSwapchainCreateInfoKHR sc_info{};
    sc_info.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    sc_info.surface = surface_;
    sc_info.minImageCount = img_count;
    sc_info.imageFormat = swapchain_format_;
    sc_info.imageColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    sc_info.imageExtent = swapchain_extent_;
    sc_info.imageArrayLayers = 1;
    sc_info.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    sc_info.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    sc_info.preTransform = pre_transform;
    sc_info.compositeAlpha = composite_alpha;
    sc_info.presentMode = VK_PRESENT_MODE_FIFO_KHR;
    sc_info.clipped = VK_TRUE;

    VkResult res = vkCreateSwapchainKHR(device_, &sc_info, nullptr, &swapchain_);
    if (res != VK_SUCCESS) {
        LOGE("vkCreateSwapchainKHR gagal dengan error code: %d", res);
        return false;
    }

    uint32_t actual_count = 0;
    vkGetSwapchainImagesKHR(device_, swapchain_, &actual_count, nullptr);
    swapchain_images_.resize(actual_count);
    vkGetSwapchainImagesKHR(device_, swapchain_, &actual_count, swapchain_images_.data());

    swapchain_image_views_.resize(actual_count);
    for (uint32_t i = 0; i < actual_count; ++i) {
        VkImageViewCreateInfo view_info{};
        view_info.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        view_info.image = swapchain_images_[i];
        view_info.viewType = VK_IMAGE_VIEW_TYPE_2D;
        view_info.format = swapchain_format_;
        view_info.subresourceRange = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1 };
        if (vkCreateImageView(device_, &view_info, nullptr, &swapchain_image_views_[i]) != VK_SUCCESS) {
            return false;
        }
    }
    return true;
}

bool VulkanContext::CreateTBDRRenderPass() {
    VkAttachmentDescription color_att{};
    color_att.format = swapchain_format_;
    color_att.samples = VK_SAMPLE_COUNT_1_BIT;
    color_att.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    color_att.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    color_att.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    color_att.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    color_att.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    color_att.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

    VkAttachmentReference color_ref{ 0, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL };

    VkSubpassDescription subpass{};
    subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    subpass.colorAttachmentCount = 1;
    subpass.pColorAttachments = &color_ref;

    VkRenderPassCreateInfo rp_info{};
    rp_info.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    rp_info.attachmentCount = 1;
    rp_info.pAttachments = &color_att;
    rp_info.subpassCount = 1;
    rp_info.pSubpasses = &subpass;

    return vkCreateRenderPass(device_, &rp_info, nullptr, &main_render_pass_) == VK_SUCCESS;
}

bool VulkanContext::CreateSyncObjects() {
    VkCommandPoolCreateInfo pool_info{};
    pool_info.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    pool_info.queueFamilyIndex = graphics_queue_family_;
    pool_info.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    if (vkCreateCommandPool(device_, &pool_info, nullptr, &command_pool_) != VK_SUCCESS) return false;

    VkSemaphoreCreateInfo sem_info{ VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO, nullptr, 0 };
    VkFenceCreateInfo fence_info{ VK_STRUCTURE_TYPE_FENCE_CREATE_INFO, nullptr, VK_FENCE_CREATE_SIGNALED_BIT };
    return vkCreateSemaphore(device_, &sem_info, nullptr, &image_available_sem_) == VK_SUCCESS &&
           vkCreateSemaphore(device_, &sem_info, nullptr, &render_finished_sem_) == VK_SUCCESS &&
           vkCreateFence(device_, &fence_info, nullptr, &in_flight_fence_) == VK_SUCCESS;
}

bool VulkanContext::CreateFramebuffers() {
    framebuffers_.resize(swapchain_image_views_.size());
    for (size_t i = 0; i < swapchain_image_views_.size(); ++i) {
        VkFramebufferCreateInfo fb_info{};
        fb_info.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        fb_info.renderPass = main_render_pass_;
        fb_info.attachmentCount = 1;
        fb_info.pAttachments = &swapchain_image_views_[i];
        fb_info.width = swapchain_extent_.width;
        fb_info.height = swapchain_extent_.height;
        fb_info.layers = 1;
        if (vkCreateFramebuffer(device_, &fb_info, nullptr, &framebuffers_[i]) != VK_SUCCESS) return false;
    }

    if (!command_buffers_.empty()) {
        vkFreeCommandBuffers(device_, command_pool_, static_cast<uint32_t>(command_buffers_.size()), command_buffers_.data());
        command_buffers_.clear();
    }

    command_buffers_.resize(framebuffers_.size());
    VkCommandBufferAllocateInfo alloc_info{};
    alloc_info.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    alloc_info.commandPool = command_pool_;
    alloc_info.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    alloc_info.commandBufferCount = static_cast<uint32_t>(command_buffers_.size());
    return vkAllocateCommandBuffers(device_, &alloc_info, command_buffers_.data()) == VK_SUCCESS;
}

void VulkanContext::RenderClearFrame(float r, float g, float b, float resolution_scale) {
    std::lock_guard<std::mutex> lock(render_mutex_);
    if (device_ == VK_NULL_HANDLE || swapchain_ == VK_NULL_HANDLE || framebuffers_.empty()) return;

    // Timeout 500ms mencegah ANR jika display pipeline sedang terhambat
    VkResult fence_wait = vkWaitForFences(device_, 1, &in_flight_fence_, VK_TRUE, 500000000ULL);
    if (fence_wait != VK_SUCCESS) {
        return;
    }

    uint32_t image_index = 0;
    VkResult acq_res = vkAcquireNextImageKHR(device_, swapchain_, 500000000ULL, image_available_sem_,
                                             VK_NULL_HANDLE, &image_index);
    if (acq_res == VK_ERROR_OUT_OF_DATE_KHR) {
        // Swapchain kadaluarsa (misal perubahan orientasi layar), perlu refresh
        return;
    } else if (acq_res != VK_SUCCESS && acq_res != VK_SUBOPTIMAL_KHR) {
        return;
    }

    // Reset fence HANYA setelah image berhasil di-acquire untuk mencegah permanent deadlock
    vkResetFences(device_, 1, &in_flight_fence_);

    if (image_index >= command_buffers_.size() || image_index >= framebuffers_.size()) {
        return;
    }

    VkCommandBuffer cmd = command_buffers_[image_index];
    vkResetCommandBuffer(cmd, 0);

    VkCommandBufferBeginInfo begin_info{ VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO, nullptr, 0, nullptr };
    vkBeginCommandBuffer(cmd, &begin_info);

    VkClearValue clear_val{};
    clear_val.color = {{ r, g, b, 1.0f }};

    VkRenderPassBeginInfo rp_begin{};
    rp_begin.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    rp_begin.renderPass = main_render_pass_;
    rp_begin.framebuffer = framebuffers_[image_index];
    rp_begin.renderArea.offset = { 0, 0 };
    rp_begin.renderArea.extent = swapchain_extent_;
    rp_begin.clearValueCount = 1;
    rp_begin.pClearValues = &clear_val;

    vkCmdBeginRenderPass(cmd, &rp_begin, VK_SUBPASS_CONTENTS_INLINE);

    // Dynamic Resolution Viewport Scaling berdasarkan ThermalGovernor
    VkViewport viewport{};
    viewport.x = 0.0f;
    viewport.y = 0.0f;
    viewport.width = static_cast<float>(swapchain_extent_.width) * resolution_scale;
    viewport.height = static_cast<float>(swapchain_extent_.height) * resolution_scale;
    viewport.minDepth = 0.0f;
    viewport.maxDepth = 1.0f;
    vkCmdSetViewport(cmd, 0, 1, &viewport);

    VkRect2D scissor{};
    scissor.offset = { 0, 0 };
    scissor.extent = {
        std::min<uint32_t>(static_cast<uint32_t>(viewport.width), swapchain_extent_.width),
        std::min<uint32_t>(static_cast<uint32_t>(viewport.height), swapchain_extent_.height)
    };
    vkCmdSetScissor(cmd, 0, 1, &scissor);

    vkCmdEndRenderPass(cmd);
    vkEndCommandBuffer(cmd);

    VkPipelineStageFlags wait_stage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    VkSubmitInfo submit_info{};
    submit_info.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submit_info.waitSemaphoreCount = 1;
    submit_info.pWaitSemaphores = &image_available_sem_;
    submit_info.pWaitDstStageMask = &wait_stage;
    submit_info.commandBufferCount = 1;
    submit_info.pCommandBuffers = &cmd;
    submit_info.signalSemaphoreCount = 1;
    submit_info.pSignalSemaphores = &render_finished_sem_;

    vkQueueSubmit(graphics_queue_, 1, &submit_info, in_flight_fence_);

    VkPresentInfoKHR present_info{};
    present_info.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    present_info.waitSemaphoreCount = 1;
    present_info.pWaitSemaphores = &render_finished_sem_;
    present_info.swapchainCount = 1;
    present_info.pSwapchains = &swapchain_;
    present_info.pImageIndices = &image_index;

    vkQueuePresentKHR(graphics_queue_, &present_info);
}

void VulkanContext::Resize(uint32_t width, uint32_t height) {
    if (device_ == VK_NULL_HANDLE || width == 0 || height == 0) return;
    std::lock_guard<std::mutex> lock(render_mutex_);
    vkDeviceWaitIdle(device_);
    DestroySwapchainResources();
    swapchain_extent_.width = width;
    swapchain_extent_.height = height;
    if (CreateSwapchain(window_)) {
        CreateFramebuffers();
        LOGI("Vulkan swapchain berhasil di-resize: %u x %u", width, height);
    }
}

void VulkanContext::DestroySwapchainResources() {
    for (auto fb : framebuffers_) {
        if (fb != VK_NULL_HANDLE) vkDestroyFramebuffer(device_, fb, nullptr);
    }
    framebuffers_.clear();
    for (auto iv : swapchain_image_views_) {
        if (iv != VK_NULL_HANDLE) vkDestroyImageView(device_, iv, nullptr);
    }
    swapchain_image_views_.clear();
    if (swapchain_ != VK_NULL_HANDLE) {
        vkDestroySwapchainKHR(device_, swapchain_, nullptr);
        swapchain_ = VK_NULL_HANDLE;
    }
}

void VulkanContext::Shutdown() {
    std::lock_guard<std::mutex> lock(render_mutex_);
    if (device_ != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device_);
        DestroySwapchainResources();
        if (!command_buffers_.empty() && command_pool_ != VK_NULL_HANDLE) {
            vkFreeCommandBuffers(device_, command_pool_, static_cast<uint32_t>(command_buffers_.size()), command_buffers_.data());
            command_buffers_.clear();
        }
        if (image_available_sem_ != VK_NULL_HANDLE) vkDestroySemaphore(device_, image_available_sem_, nullptr);
        if (render_finished_sem_ != VK_NULL_HANDLE) vkDestroySemaphore(device_, render_finished_sem_, nullptr);
        if (in_flight_fence_ != VK_NULL_HANDLE) vkDestroyFence(device_, in_flight_fence_, nullptr);
        if (command_pool_ != VK_NULL_HANDLE) vkDestroyCommandPool(device_, command_pool_, nullptr);
        if (main_render_pass_ != VK_NULL_HANDLE) vkDestroyRenderPass(device_, main_render_pass_, nullptr);
        vkDestroyDevice(device_, nullptr);
        device_ = VK_NULL_HANDLE;
    }
    if (surface_ != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(instance_, surface_, nullptr);
        surface_ = VK_NULL_HANDLE;
    }
    if (instance_ != VK_NULL_HANDLE) {
        vkDestroyInstance(instance_, nullptr);
        instance_ = VK_NULL_HANDLE;
    }
    window_ = nullptr;
}

} // namespace overcooked
