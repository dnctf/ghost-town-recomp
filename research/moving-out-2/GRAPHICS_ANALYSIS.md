# Moving Out 2 — Graphics Analysis

## Moving Out 2 evidence

- **VERIFIED (baseline inventory):** UnityPlayer and Unity resource files are present.
- **NOT TESTED:** actual graphics API selection, Unity Player settings, D3D11 vs D3D12 vs Vulkan use, shader variants, feature level, swapchain format, MSAA, HDR, render threading, or required extensions.
- **HYPOTHESIS:** a Windows x64 Unity 2021 LTS build commonly exercises a Direct3D backend on Windows, but this specific game's active API must be verified from Player.log / command-line diagnostics or runtime capture.

No claim is made that Moving Out 2 uses DXVK, Turnip, VirGL, Vortek, or a specific Vulkan feature set.

## Ghost Town current architecture (source audit)

- **VERIFIED:** `GraphicsRuntimeSelector` selects a backend from capability/file-presence Booleans. It does not probe guest Vulkan or render a guest frame.
- **VERIFIED:** the runtime manifest explicitly keeps DXVK and Turnip unbundled; guest Vulkan and D3D11 visible-frame validation are release gates.
- **VERIFIED:** `X11DisplaySession` starts XServer, SysV SHM, and VirGL host components after EGL callback. It does not prove guest GLX/X11 window creation or Unity presentation.
- **VERIFIED:** native CMake builds Android Vulkan surface and VirGL/X11 components, but source presence is not runtime validation.
- **NOT TESTED:** Adreno 660 / HarmonyOS target surface lifecycle, swapchain recreation, guest ICD loading, Turnip, Vortek, DXVK, D3D11 presentation, and game shader compatibility.

## Backend recommendation

Keep the manifest's current conservative WineD3D/VirGL path as the first compatibility experiment. Do not enable DXVK or bundle Turnip merely because the target GPU is Adreno 660. A valid DXVK gate requires a guest Vulkan self-test, ICD/loader compatibility, visible D3D11 smoke frame, then a real game frame. VirGL and Vortek are distinct transport/driver paths; do not treat their names as interchangeable.

## Evidence required

Record Vulkan instance/device extensions, selected ICD, renderer string, GLX/EGL capabilities, X11 window creation, swapchain format/present mode, and screenshot/frame counter from the target device.
