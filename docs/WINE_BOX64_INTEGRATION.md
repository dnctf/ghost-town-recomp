# Wine + Box64 integration plan

**Status: design and compatibility groundwork only. Wine and Box64 are not yet packaged or runnable in this APK.**

## Runtime model

- Android application and launcher: native `arm64-v8a`.
- Box64: ARM64 process that translates supported x86-64 Linux user-mode instructions.
- Wine: Windows API compatibility layer built for the chosen Android/Linux userspace environment.
- Graphics: per-title path selected from the game's actual Direct3D/OpenGL/Vulkan requirements.
- Game files: user-supplied, complete PC installation. A Unity `_Data` directory alone is not a complete Windows installation.

Box64 is not itself a Windows PE loader or a complete Windows runtime. The project must provide a compatible Wine build, the expected libc/linker/userspace arrangement, filesystem layout, process environment, and required 32-bit support strategy. A 32-bit Windows game cannot be assumed to work with a 64-bit-only configuration.

## Integration gates

1. **Select a known Android Wine/Box64 implementation as a reference.** Record upstream URL, commit, license, patches, toolchain, build instructions, and supported Android versions.
2. **Build runtime components separately from the launcher.** Do not add opaque prebuilt binaries or copy desktop Windows redistributables into the APK.
3. **Establish ARM64 process execution.** Confirm the Wine loader and Box64 work on the target Android userspace before attempting a game.
4. **Establish graphics.** Verify Vulkan on the device, then test the selected DXVK or alternative path independently.
5. **Inspect each game installation.** Determine PE architecture, engine version, Mono/IL2CPP backend, DirectX version, DRM/launcher requirements, and required DLLs.
6. **Prove one game.** Install/launch the original game, reach its menu, enter gameplay, verify audio/controller/input/save, then run a sustained thermal test.
7. **Add other titles only after the first game passes.** Keep one Wine prefix and profile per game where dependencies conflict.

## Packaging policy

- Keep the Android app's own ABI as `arm64-v8a`; do not bundle x86 APK libraries.
- Ship only the runtime binaries required by the selected architecture and document their licenses and provenance.
- Query device Vulkan capabilities at runtime. Never package a replacement Vulkan loader or assume a specific Adreno driver.
- Do not bundle arbitrary VC++/.NET/DirectX installers. Add a dependency only when the inspected game demonstrably requires it and the compatibility layer supports it.
- Keep runtime binaries modular so they can be updated independently of the UI.
- Preserve user-owned game data outside the APK; do not redistribute game assets.

## Acceptance criteria

The runtime is not considered integrated until a debug APK can initialize the Wine environment, invoke Box64, launch a known Windows executable, render through a verified graphics path, and produce actionable logs. Game support requires the separate per-title gameplay acceptance checklist in `RUNTIME_STATUS.md`.

A successful Android/NDK build alone proves only that the launcher compiles; it does not prove Wine, Box64, DXVK, or any Windows game works.
