# Source Engine Feasibility

**Status:** research only. No Source Engine SDK, port, or game runtime is included.

## Architectural separation

Source Engine (including Half-Life 2 and Source-based mods) is a separate C++ engine codebase. It is not a Unity title and does not use Unity `*_Data` layouts, Mono/CIL assemblies, or IL2CPP metadata. The current Unity asset/runtime scaffolding cannot execute Source Engine games.

No `source-sdk-2013` submodule, Source Engine SDK integration, Source-specific build target, or Source platform abstraction was found in the audited repository. The root `.gitmodules` file is absent.

## Recommended project boundary

Create a separate repository/fork based on an appropriate upstream such as Source SDK 2013, after verifying the upstream's current availability, license terms, redistribution conditions, third-party notices, and compatibility with the target game/mod.

A real Android port would require:

- Android ARM64 toolchain and build-system adaptation.
- SDL or another maintained Android window/input integration.
- Platform adaptation for filesystem, threads, timing, sockets, audio, and lifecycle.
- Renderer port/adaptation. Vulkan support must be implemented and tested, not inferred from this launcher.
- VPK/content mounting and user-provided game content handling.
- Controller/touch input, save/config paths, suspend/resume, thermal and memory management.
- Per-title/mod build and gameplay tests on physical devices.

Compiling an engine is not proof that Half-Life 2 or a particular mod is playable.

## Launcher integration

After the separate project exposes a stable runtime interface, Ghost Town can register it as an external provider. The launcher should pass a validated user-selected content path and profile, then receive structured capability, launch, exit, and log events. Source Engine objects should not be linked into the Unity runtime target.

## Conclusion

Source Engine support is a plausible independent porting project, but not a small extension to the current Unity/Vulkan scaffolding. Keep it in a separate fork/repository and integrate it only after a real Android build and game boot test.

No Source Engine compatibility or successful port is claimed by this document.
