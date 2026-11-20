# Ghost Town Recomp

> **Moving Out 2 only — Android ARM64 compatibility/runtime research. Gameplay is not verified playable.**

Ghost Town Recomp is a dedicated Android application and game-specific compatibility project for **Moving Out 2 PC**. The project is no longer pursuing Overcooked! 2, the original Moving Out, a multi-game launcher, or generic PC-game compatibility.

## Project scope

- **Only target game:** Moving Out 2 PC, user-provided game files.
- **Known baseline:** version 1.2.274, Windows x64 / AMD64, Unity 2021.3.16f1, IL2CPP, Production/Steam build evidence.
- **Android target:** ARM64 (AArch64), with device-specific graphics/runtime testing.
- **Application purpose:** dedicated Moving Out 2 setup, compatibility verification, runtime configuration, launch/session control, game input, diagnostics and Android experience.
- **Game data:** executable, Unity assets, native libraries, metadata, saves and proprietary game content are not distributed or committed.
- **Status:** experimental. No end-to-end game boot, first frame, menu, or gameplay is claimed.

## Moving Out 2 baseline

Based on files supplied for local inspection (not stored in this repository):

- mo2.exe
- GameAssembly.dll
- UnityPlayer.dll
- baselib.dll
- BuildInfo.json
- global-metadata.dat (IL2CPP metadata version 29 identified in research)
- Unity resource files and native plugin inventory, including Crashpad, FMOD, Resonance Audio, Burst, NanoSockets and Steam-related components.

Unity resource marker versions showed a discrepancy: one resource marker identified 2021.3.13f1 while another identified 2021.3.16f1. The provenance of the discrepancy is not yet resolved. The baseline Unity version above is based on BuildInfo/metadata evidence; verify the exact player build before compatibility decisions.

## Technical direction

Moving Out 2 is a **Windows x86_64 Unity IL2CPP game**. Android ARM64 cannot execute its Windows PE executable and x86_64 native dependencies directly. Until a viable game-specific native recompilation/port is demonstrated, the compatibility strategy must keep a Windows execution layer (for example Wine + Box64) under evaluation. Unity IL2CPP metadata availability is not proof that gameplay can be rebuilt or ported.

The runtime stack is selected from test evidence—not by assuming every existing component is required:

- Windows execution/compatibility layer and x86_64 translation
- Unity Player and native plugin compatibility
- graphics API translation and Android Vulkan/GPU driver compatibility
- Android surface/presentation path
- game input and local controller support
- audio transport
- lifecycle, process supervision, storage, diagnostics and performance/thermal controls

**Do not remove Wine, Box64, PRoot, graphics bridges, DXVK, Turnip, or other compatibility components solely because the project is now game-specific.** Audit each component against Moving Out 2 and remove or disable it only after a replacement path and regression checks are established. Game-specific scope is not the same as native recompilation.

## Development priorities

1. Freeze Moving Out 2's verified game/build/engine facts and data fingerprints.
2. Audit existing code and workflows; identify Overcooked/original Moving Out/multi-game remnants and classify them for removal, isolation, or reuse.
3. Fix installation validation to recognize the actual Moving Out 2 PC layout and required files; never mark a game ready from a single generic Unity marker.
4. Audit and choose a realistic runtime path for Windows x64 Unity IL2CPP on Android ARM64.
5. Prove runtime initialization, process launch, graphics device, swapchain/presentation, and the actual game's first frame separately.
6. Integrate input, audio, pause/resume, termination, crash recovery and diagnostics.
7. Validate on a physical device; record game build, device, Android version, driver/runtime versions, resolution, frame rate, memory, thermals, stability and test evidence.
8. Only then claim a compatibility milestone.

## Architecture principle

This repository is **game-specific**, not a universal PC launcher:


Moving Out 2 installation (Windows x86_64 / Unity IL2CPP)
        -> Moving Out 2-specific installation validator + profile
        -> Android app / session manager / diagnostics
        -> Pinned and tested compatibility runtime (selected by evidence)
        -> Windows x86_64 Unity Player + Moving Out 2
        -> Android display, input, audio and lifecycle bridges

A component's presence in the repository or runtime pack is not evidence that it is integrated or that the game runs.

## Research and audit documents

- MOVING_OUT_2_ONLY_SCOPE.md: migration decisions, audit/removal policy, and dedicated target acceptance gates.
- MOVING_OUT_2_COMPATIBILITY_AUDIT.md: initial source-level findings and ordered refactor plan.
- RUNTIME_AUDIT.md: historical runtime integration gaps; reconcile with Moving Out 2-only scope.
- docs/: architecture and runtime research; Overcooked! 2 documents are historical inputs and not active requirements.

## Build and CI

Use GitHub Actions to verify Android source/build/package steps. CI passing is not game-playability evidence. No proprietary game files may be uploaded as artifacts or committed.

## Legal / distribution

Independent research; not affiliated with or endorsed by the rights holders. Moving Out 2 executable, assets, metadata, save data and proprietary plugins are not redistributed. Users supply game files they are legally entitled to use.
