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

## Game-library integration audit (2026-11-19)

The current branch was rechecked after the dedicated-session lifecycle addition. Existing components should be reused rather than duplicated:

- `GameStorageVerifier.java` detects Unity data-folder markers, Mono `Assembly-CSharp.dll`, IL2CPP `global-metadata.dat`, parses Windows PE executable headers, and calculates SHA-256 for inspected binaries.
- `GameFileImporter.java` supports SAF-based EXE import, ZIP extraction, and recursive folder copying into app-private storage.
- `tools/game_installation_probe.py` independently inspects a Windows Unity installation layout and reports whether executable, UnityPlayer, data directory, and Mono/IL2CPP metadata are present. It correctly labels this as installation completeness, not runtime compatibility.
- `RuntimeLaunchGate.kt` gates on installed runtime and a display marker; it does not prove a game-specific session is launchable.
- `DashboardActivity.kt` still uses demo game entries and the UI-preview preference for the selected library folder. The library picker currently records the SAF URI; the demo dashboard is not yet a live game catalog.

### Integration gaps / next sequence

1. Replace demo game records with a real catalog backed by the selected SAF directory or imported app-private files.
2. Make scanning asynchronous and bounded; report scan progress and cancellation, and avoid hashing entire game payloads during each dashboard refresh.
3. Reuse `GameStorageVerifier` for PE and Unity metadata checks. Distinguish `DATA_ONLY`, `EXE_ONLY`, `INCOMPLETE`, `INSTALLATION_COMPLETE`, `UNSUPPORTED_ARCH`, and `RUNTIME_UNAVAILABLE`; never translate file detection into `READY_TO_PLAY`.
4. Persist catalog records and their source URI/path with permission revalidation. Do not assume persisted SAF access remains valid.
5. Integrate the state machine only after catalog validation is connected to the launch controller. Keep legacy Wine/X11/Vulkan compatibility code until replacement paths have CI and device evidence.

### CI verification for this branch

GitHub Actions run #319 completed successfully on the branch commit. Reported jobs passed: Python probe tests, Android ARM64 native/APK build (debug and release), Android lint, APK size audit, and artifact upload. The workflow runs `gradle testDebugUnitTest` as part of the Android build step. CI success is compile/test/package evidence only; it is not physical-device or gameplay evidence.
