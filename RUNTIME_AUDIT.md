# Runtime Audit

**Audit date:** 2026-11-16  
**Audited ref:** `main` baseline and `audit/runtime-wine-box64-source-engine` working branch  
**Scope:** README, Android Gradle/NDK build, native CMake sources, runtime and asset-pipeline source files, shader bridge, manifest, and GitHub Actions workflow.

## Executive finding

Ghost Town Recomp is an Android ARM64 launcher and an early Unity-oriented native runtime/recompilation research scaffold. It contains Unity-named host abstractions, metadata/file inspection helpers, and a Vulkan-facing native layer. It does **not** currently contain a complete Unity Mono/CIL execution runtime, an IL2CPP-to-native recompilation pipeline, a Windows compatibility runtime, or a Source Engine port. Detecting game data or creating a Vulkan surface is not evidence of playable game execution.

## Findings by subsystem

### Android and native build
- `android/app/build.gradle` configures an Android application, ABI `arm64-v8a`, NDK `26.3.11579264`, C++20, minSdk 29, and AndroidX UI/document dependencies.
- `android/native/CMakeLists.txt` builds `overcooked_native` and links Android, log, Vulkan, and AAudio.
- The CMake target includes Unity-named metadata/host and asset/shader helper sources, but does not link Wine, Box64, Box86, DXVK, VKD3D-Proton, or a Mono runtime library.

### Unity CIL / Mono / IL2CPP
- `runtime/mono/src/mono_runtime_host.cpp` checks that `Assembly-CSharp.dll` exists and validates its DOS MZ header. It does not call Mono initialization/assembly APIs, load a Mono shared library, or execute CIL. `TickUpdate` is a placeholder.
- `runtime/il2cpp/src/il2cpp_metadata_parser.cpp` reads and validates the `global-metadata.dat` header. It does not translate IL2CPP methods or generate ARM64 machine code.
- `runtime/interop/src/unity_method_invoker.cpp` selects nominal backends and stores transform values; it is not a complete Unity engine or managed-method invocation layer.
- `assets-pipeline/assetstudio-wrapper/src/serialized_file_parser.cpp` reads a limited serialized-file header and checks a version range; it is not a complete AssetStudio extraction/rebuild pipeline.
- `shader-bridge/src/shader_bridge.cpp` maps a small number of shader names to expected precompiled SPIR-V asset paths and checks the SPIR-V magic. It does not compile arbitrary Unity ShaderLab source into SPIR-V.

**Conclusion:** Unity Mono/CIL and IL2CPP are represented by scaffolding and metadata checks, not a complete CIL execution or IL2CPP recompilation pipeline. Vulkan integration is a native Android graphics foundation, not a complete Unity renderer.

### Wine / Box64
No Wine, Box64, Box86, PRoot/rootfs, DXVK, or VKD3D-Proton dependency was found in the inspected Gradle files, native CMake target, or runtime source paths.

**Conclusion:** Wine/Box64 is not integrated. Android ARM64 support alone does not execute Windows PE files.

### Source Engine
No `source-sdk-2013`, Source Engine SDK, Source client/server target, or Source-specific Android port was found. The repository has no `.gitmodules` file at the audited baseline.

**Conclusion:** Source Engine is not supported by the current runtime.

### GitHub Actions and local build constraints
- `.github/workflows/build-android.yml` checks out recursive submodules, configures Android SDK/NDK and CMake, builds the native arm64-v8a target, runs `gradle assembleRelease assembleDebug`, and runs `gradle lint`.
- The repository does not include `android/gradlew` or `android/gradle/wrapper/gradle-wrapper.properties` at the audited baseline. Therefore the requested exact command `./gradlew assembleDebug` cannot run from this checkout without adding a verified Gradle wrapper.
- CI compilation/lint does not test Mono execution, IL2CPP recompilation, Windows EXE execution, or gameplay.

## Runtime classification

| Runtime / subsystem | Actual state |
|---|---|
| Unity Mono/CIL | Placeholder host; no CLR/Mono runtime linked |
| Unity IL2CPP | Metadata header parser only; no AOT recompilation |
| Unity asset pipeline | Limited serialized-file/texture helper scaffolding |
| ShaderLab → SPIR-V | Name-to-precompiled-SPIR-V lookup; no ShaderLab compiler |
| Vulkan | Android Vulkan integration scaffold |
| Wine/Box64 | Not integrated |
| Source Engine | Not integrated |

## Architecture recommendation

Adopt a **hybrid modular launcher** with isolated runtime adapters:

1. Keep Unity-native research in this repository, but label it experimental until managed execution, IL2CPP handling, asset/shader pipeline, and per-title gameplay tests are implemented.
2. Evaluate Wine + Box64 as an optional external runtime module or companion app. Do not link it into the Unity CMake target. The Windows path requires its own userspace, Wine, x86-64 translation, graphics compatibility, prefixes, and separate 32-bit strategy.
3. Develop Source Engine in a separate upstream-based fork/port. Integrate it with Ghost Town only after a stable runtime interface and reproducible Android game boot exist.

The launcher should own game discovery, user-selected paths, runtime selection, per-game configuration, lifecycle, logs, and capability reporting. Each runtime owns engine-specific execution and graphics compatibility.

## Explicit non-claims

No listed game is certified playable by this audit. File detection, metadata parsing, native compilation, and Vulkan surface creation are not equivalent to successful gameplay.

## Dedicated game-session lifecycle refactor (2026-11-19)

- Added a platform-independent `GameSessionStateMachine` to validate launch/session transitions without coupling it to Android Activity, Wine, X11, or Vulkan operations.
- States cover preparation, file validation, runtime initialization, GPU initialization, loading, session start, running, stopping, stopped, failed, and crashed.
- Failed/crashed states preserve the stage at which failure occurred and can retry from preparation. Invalid transitions are rejected.
- Foundation only: this lifecycle is not yet wired into the existing Activity/runtime pipeline and does not claim to launch or render a game. Integration is gated on build/test verification.
