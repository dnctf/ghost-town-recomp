# Wine + Box64 Evaluation

**Status:** architecture evaluation only. No Wine/Box64 code or binary is integrated in this repository.

## Feasibility

Wine combined with Box64 can provide a route for running supported **x86-64 Windows applications** on ARM64 Linux-based environments. Wine implements Windows API compatibility; Box64 translates x86-64 Linux user-mode instructions needed by the Wine stack.

On Android this normally requires a compatible Android runtime/userspace arrangement, ARM64 Box64, a compatible Wine build, process/filesystem setup, and a graphics stack. It is not enough to place a Box64 executable inside an APK: Box64 is not itself a Windows PE loader and does not provide Wine, Windows APIs, a Linux root filesystem, or DirectX translation.

This does not guarantee arbitrary game compatibility. Executable architecture, Wine version, CPU features, libc/userspace, game dependencies, DRM/anti-cheat, graphics API, and GPU driver all matter. A 32-bit x86 executable also needs a separately validated WoW64/Box32/Box86 strategy; Box64 alone must not be advertised as sufficient.

## Why it is separate from Unity recompilation

The current Unity-oriented path is Android-native C++ with metadata and asset inspection scaffolding. Wine + Box64 hosts a Windows process in a compatibility environment. It does not convert Unity CIL or IL2CPP code into this repository's native object model, and it does not turn the current Vulkan surface into a Unity renderer.

Combining Wine/Box64 directly into `overcooked_native` would mix different process models, dependency graphs, filesystem assumptions, and graphics responsibilities. Keep the Windows runtime as an independently versioned module.

## Integration options

1. **Separate `wine-runtime/` module or companion app (preferred):** own the Linux userspace, Wine prefixes, Box64/Box86, graphics translation, runtime downloads/updates, and logs. The launcher can later hand off a selected game to a supported external runtime interface.
2. **User-managed Termux/PRoot:** useful for an early proof of concept. However, inter-app process hand-off, Android storage permissions, Vulkan access, lifecycle, and environment setup require explicit validation. Do not assume arbitrary Termux installations expose a stable public API for other apps.
3. **Embedded runtime bundle (defer):** only after reproducible builds, license/provenance review, ABI/userspace validation, security review, storage sizing, and update strategy.

## Graphics path

- Use the Android device's Vulkan implementation as the host driver.
- DXVK translates supported Direct3D 9/10/11 workloads to Vulkan.
- Direct3D 12 needs a VKD3D-family path.
- Do not bundle or replace the Android GPU driver as part of this initial integration.
- Select graphics components per title and test them on physical devices.

## Required proof-of-concept gates

1. Pin upstream source revisions and record licenses, notices, patches, checksums, and source-distribution obligations.
2. Decide and validate x86-64 and 32-bit x86 support separately.
3. Establish a reproducible ARM64 userspace and verify loader/libc compatibility.
4. Start Box64 (and the 32-bit counterpart if required) plus the corresponding Wine loader in the intended Android process model.
5. Run a small redistributable Windows test executable and capture exit code and logs.
6. Test Vulkan and the chosen DXVK/VKD3D path independently.
7. Test a user-owned game installation, including DRM/dependencies, audio, input, save paths, menus, and actual gameplay.

## Stub in this PR

`WineRuntimeBridge.kt` is a deliberately inactive boundary for future integration. It reports the backend as unavailable. It does not download runtime files, create Wine prefixes, invoke shell commands, or launch executables. The launcher must not expose a functional Run EXE action until a real backend passes the proof-of-concept gates.

## Recommendation

Keep Wine/Box64 optional and separate from the Unity native library. Start with a separately maintained runtime module/companion app; only consider embedding after a reproducible and legally reviewed Android runtime is demonstrated.
