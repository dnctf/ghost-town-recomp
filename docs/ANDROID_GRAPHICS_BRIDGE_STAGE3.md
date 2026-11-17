# Android graphics bridge — Stage 3 implementation notes

## Target device

- Huawei MatePad Air 2023
- Snapdragon 888 / Adreno 660
- Android-compatible ARM64 userspace (HarmonyOS 4.2)

## Required split: Android host vs Wine guest

The project has two separate graphics environments. They must not be conflated:

1. **Android host process (Bionic):** owns the Android `SurfaceView`, `ANativeWindow`, Android Vulkan loader, and presentation surface. A rootless driver hook such as libadrenotools can load a compatible custom Adreno driver into this process.
2. **Wine guest process (glibc under PRoot + Box64):** loads Wine, DXVK, and a Linux Vulkan loader/ICD. Loading a Bionic Turnip driver into the Android host does not, by itself, make that driver loadable by the glibc guest process.

A successful host Vulkan query is therefore only a host-GPU check. It is not proof that DXVK inside Wine can enumerate Vulkan or present frames.

## Stage 3 work order

1. Build a pinned Android ARM64 Turnip/Freedreno driver from Mesa for the Android KGSL platform, with the matching Android Vulkan loader integration.
2. Integrate libadrenotools in the Android host process and load the custom driver before the first Vulkan instance is created. Keep the system-driver fallback and log which driver was selected.
3. Prove Android host Vulkan instance/device/surface/swapchain creation on the MatePad and render a visible diagnostic frame.
4. Separately build or package a glibc-compatible Vulkan userspace/ICD for the PRoot guest. Verify Box64 can load it and that DXVK can enumerate the Adreno 660 device.
5. Establish a supported guest-to-host presentation path (for example a tested X11/Wayland compositor path or a maintained direct presentation bridge). Do not assume the Android `SurfaceView` is automatically a Wine display server.
6. Run a small Direct3D 11 test program through Wine + Box64 + DXVK before enabling the Overcooked! 2 Play action.

## Release gates

- [ ] Host reports Adreno 660 and the selected driver name/version.
- [ ] Host Vulkan creates an Android surface and swapchain and presents a visible test frame.
- [ ] Guest glibc Vulkan loader resolves its ICD without loading a Bionic-only library.
- [ ] DXVK logs a Vulkan physical device from the Wine guest.
- [ ] Guest display backend presents a D3D test window on the Android surface.
- [ ] Audio/input forwarding works independently of the renderer.

## Current honest status (v0.5.0-alpha.1)

The repository has an Android-native Vulkan surface/swapchain implementation, but the native engine currently deliberately does not initialize it for game rendering. No Turnip/libadrenotools integration, Wine-guest Vulkan ICD, or guest display server/presentation bridge is wired end-to-end. The existing Wine version probe is headless and does not exercise DXVK.

Do not mark Stage 3 complete until the device-level gates above pass. A driver archive in an APK or a successful Android Vulkan enumeration alone is insufficient.


## Diagnostics added in the Android app

The dashboard now has **GPU / TURNIP / DXVK LOG**. It writes a persistent app-private
log under `filesDir/logs/graphics-runtime.log`, displays Android host Vulkan loader/device
and driver properties, checks guest ICD manifests and DXVK DLL presence, and includes
these records in the shareable diagnostic report.

This is diagnostic integration, not a claim that Turnip is active: the current native
probe uses the Android system Vulkan loader. Turnip must be opened through libadrenotools
before any Vulkan instance is created; current direct Vulkan linkage does not yet route
calls through that custom loader. Wine guest still needs its own glibc-compatible ICD and
a working display/presentation bridge. DXVK launch script now prepares per-prefix logs,
but still blocks game launch until the display gate is explicitly validated.
