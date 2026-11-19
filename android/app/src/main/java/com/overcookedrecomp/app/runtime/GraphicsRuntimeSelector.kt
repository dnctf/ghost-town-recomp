package com.overcookedrecomp.app.runtime
enum class GraphicsBackend{DXVK_TURNIP,DXVK_SYSTEM_VULKAN,WINED3D,BLOCKED}
data class GraphicsCapabilities(val hostVulkan:Boolean,val turnipAvailable:Boolean,val dxvkFilesPresent:Boolean,val guestVulkanLoaderPresent:Boolean,val hostOpenGlAvailable:Boolean)
data class GraphicsSelection(val backend:GraphicsBackend,val reason:String,val experimental:Boolean)
/** Presence-based decision only; backend execution must still be smoke-tested. */
object GraphicsRuntimeSelector{fun select(c:GraphicsCapabilities):GraphicsSelection=when{c.hostVulkan&&c.turnipAvailable&&c.guestVulkanLoaderPresent&&c.dxvkFilesPresent->GraphicsSelection(GraphicsBackend.DXVK_TURNIP,"Vulkan + Turnip + DXVK files found; guest presentation unverified.",true);c.hostVulkan&&c.guestVulkanLoaderPresent&&c.dxvkFilesPresent->GraphicsSelection(GraphicsBackend.DXVK_SYSTEM_VULKAN,"Vulkan loader + DXVK files found; compatibility unverified.",true);c.hostOpenGlAvailable->GraphicsSelection(GraphicsBackend.WINED3D,"Fallback OpenGL/WineD3D.",false);else->GraphicsSelection(GraphicsBackend.BLOCKED,"No graphics path available.",false)}}
