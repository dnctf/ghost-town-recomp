# Source Engine Feasibility

**Status: research only. Source Engine is not implemented in this repository.**

## Architectural separation

Source Engine (including Half-Life 2 and Source-based mods) is a separate C++ engine codebase. It is not a Unity title and does not use Unity `*_Data` folders, Unity Mono CIL, IL2CPP metadata, or the repository's Unity asset/shader mapping as its engine runtime.

The current Android project is built around an Android launcher, a C++20 Vulkan-facing library, Unity-oriented runtime-shim files, and Unity asset helpers. No Source SDK code, `source-sdk-2013` submodule, Source client/server targets, or Source platform abstraction was found in the audited `main` paths.

## Work a real port would require

A credible Source Engine Android port would need, at minimum:

- A legally usable and technically suitable Source SDK/upstream codebase and an explicit review of its license and third-party notices.
- An ARM64 Android toolchain/build system and platform abstraction for filesystem, threads, sockets, timing, input, audio, and window/surface lifecycle.
- A renderer path compatible with the selected Android graphics APIs (likely a dedicated Vulkan or GLES adaptation, depending on the chosen upstream branch).
- SDL or another validated Android input/window integration where appropriate.
- Audio, controller, touch, storage, shader/material, and asset compatibility work.
- Separate packaging, user-supplied game data handling, and device performance testing.
- A game/mod-specific validation matrix; compiling the engine is not proof that Half-Life 2 or a mod is playable.

## Recommendation

Create a separate repository/fork for Source Engine work (for example, a carefully selected Source SDK 2013-based project if its license and target game/mod compatibility permit it). Do not add Source Engine dependencies to the Unity runtime CMake target or represent a stub as a successful port.

## Non-claim

This document is a feasibility assessment only. No Source Engine source, SDK, renderer, or game has been ported or tested by this change.
