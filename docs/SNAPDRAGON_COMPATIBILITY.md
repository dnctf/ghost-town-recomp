# Snapdragon / Adreno device compatibility

## Scope

The Android application is built for the Android `arm64-v8a` ABI. Snapdragon 845 and later Snapdragon mobile platforms are expected to use this ABI, but that does **not** mean every game or Wine/Box64 runtime combination is compatible.

The app must use a conservative ARM64 baseline. Do not compile the general APK with `-march=native`, a Snapdragon-specific `-mcpu`, or mandatory optional ARM instructions. Optional CPU optimizations belong in separately dispatched runtime code after checking CPU features.

## Runtime capability detection

The Vulkan backend reads the active physical device's properties at runtime:

- Vulkan device name, vendor ID, and device ID
- Vulkan API and driver versions
- Adreno model parsed from the reported device name (rather than a short hard-coded list)
- Maximum storage-buffer range and uniform-buffer alignment
- Maximum sampler anisotropy and whether the sampler-anisotropy feature is supported

The graphics profile is refreshed after Vulkan device discovery. This is important because Android display and RAM information are available before the app's Vulkan surface exists; choosing a preset before Vulkan initialization would otherwise see an unknown GPU and incorrectly retain the fallback profile.

## Conservative initial graphics tiers

| GPU family / example | Example Snapdragon generation | Initial profile policy |
|---|---|---|
| Adreno 630 | 845 | Low; target 30 FPS |
| Adreno 640 / 650 / 660 | 855 / 865 / 888 | Medium when system RAM is at least 6 GB |
| Adreno 730 / 740 | 8 Gen 1 / 8 Gen 2 | High when system RAM is at least 8 GB |
| Adreno 750 and later parsed 7xx/8xx models | 8 Gen 3 and newer families | Ultra only with at least 12 GB; otherwise High where the current tier rules permit |
| Unknown GPU / no Vulkan device | Unverified | Low fallback; do not claim support |

These are **starting presets**, not benchmark results or guarantees. Device cooling, vendor driver quality, memory pressure, display resolution, and the actual game workload can change the result. A Snapdragon model name alone is not enough to select a safe renderer or promise a frame rate.

## Vulkan and graphics constraints

- Query the actual Android Vulkan implementation; never ship or replace the device's Vulkan loader/driver.
- Do not assume Turnip is available or supported on every stock Android/HarmonyOS device.
- Use DXVK only for Direct3D 9/10/11 titles with a working Vulkan path. Direct3D 12 requires a different compatibility path.
- Use the physical device's reported limits instead of fixed Adreno limits.
- Optional Vulkan features must be queried and enabled explicitly before use.
- Preserve a low-resolution/30 FPS fallback for sustained thermal load.

## Snapdragon 845 baseline and newer devices

Snapdragon 845 / Adreno 630 is the minimum target for the current optimization policy. Newer Adreno generations are detected from Vulkan at runtime; the app should not need one APK per Snapdragon generation.

The current repository still does **not** include Wine, Box64, DXVK, or a Windows game execution backend. The capability work in this branch improves device detection and native graphics defaults only. It does not make any of the four Windows games playable.

## Validation matrix

| Device class | Required validation |
|---|---|
| Snapdragon 845 / Adreno 630 | APK install, Vulkan surface, 30 FPS fallback, thermal behavior |
| Snapdragon 855–865 / Adreno 640–650 | Vulkan limits, preset selection, memory stability |
| Snapdragon 888 / Adreno 660 | Confirm Adreno 660 is detected and Medium profile is selected with 8 GB RAM |
| Snapdragon 8 Gen 1–2 / Adreno 730–740 | Vulkan/driver stability and High profile |
| Snapdragon 8 Gen 3 / newer Adreno | Runtime detection, actual feature support, sustained thermal test |

A row is not considered validated until the APK has been installed and tested on physical hardware and the device/driver details plus test logs have been recorded.
