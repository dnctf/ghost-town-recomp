# Runtime Audit

**Audit date:** 2026-11-16  
**Audited ref:** `main`  
**Scope:** README, Android Gradle/NDK build, native CMake sources, runtime and asset-pipeline source files, shader bridge, and GitHub Actions workflow.

## Executive finding

The repository is structured as an **early Unity-oriented native recompilation/runtime-shim prototype**, not as a general Windows executable compatibility layer. It contains Android Java/Kotlin launcher code, an ARM64 C++20 native library using Android Vulkan, placeholder Mono CIL and IL2CPP metadata components, basic Unity serialized-file/texture helpers, and a small shader-name-to-precompiled-SPIR-V lookup. The inspected build does not link Wine, Box64, Box86, a Linux userspace, DXVK, VKD3D-Proton, Mono runtime libraries, or an IL2CPP AOT compiler. Source Engine SDK code or a Source SDK submodule was not found in the audited paths; `.gitmodules` is absent (GitHub returned 404 for that path).

## Findings by subsystem

### 1. Android application and native build

- `android/app/build.gradle` defines an Android application with ABI filter `arm64-v8a`, NDK `26.3.11579264`, C++20 flags, and Android API 29 minimum.
- Gradle dependencies are AndroidX AppCompat, Core KTX, DocumentFile, and CardView only.
- `android/native/CMakeLists.txt` builds `overcooked_native` from JNI, Vulkan surface, memory/audio/graphics code, Unity runtime-shim sources, asset parser/transcoder, shader bridge, input mapper, and thermal governor.
- Native links are Android, log, Vulkan, and AAudio. No Wine/Box64/Box86/DXVK/VKD3D or Mono shared library is linked.

### 2. Unity CIL / Mono / IL2CPP

The source tree contains Unity-oriented names and scaffolding:

- `runtime/mono/src/mono_runtime_host.cpp` checks for `Managed/Assembly-CSharp.dll` and validates its DOS MZ header. The actual `mono_jit_init`, assembly loading, method invocation, and Mono library linkage are not implemented in the inspected source. `TickUpdate` is a placeholder comment.
- `runtime/il2cpp/src/il2cpp_metadata_parser.cpp` reads and validates the IL2CPP metadata magic/header. It does not compile IL2CPP metadata or game code into ARM64 native code.
- `runtime/interop/src/unity_method_invoker.cpp` routes a selected backend to those components and contains a small transform map. This is not a complete Unity engine implementation.
- `assets-pipeline/assetstudio-wrapper/src/serialized_file_parser.cpp` reads a limited serialized-file header and accepts versions 9–22. It is not a full Unity AssetStudio integration in the inspected CMake target.
- `assets-pipeline/texture-compressor/src/texture_transcoder.cpp` selects format labels and estimates block sizes; the inspected implementation does not show an actual ASTC/ETC2 encoder.
- `shader-bridge/src/shader_bridge.cpp` maps a small set of Unity shader names to precompiled `.spv` asset paths and checks the SPIR-V magic. It does **not** translate arbitrary Unity ShaderLab source into SPIR-V.

**Conclusion:** Unity CIL/Mono and IL2CPP are represented by scaffolding and metadata checks, not a complete CIL execution or IL2CPP recompilation pipeline. Vulkan is a native rendering integration point, but a Vulkan surface/render loop alone is not the Unity engine.

### 3. Wine / Box64

No Wine, Box64, Box86, PRoot/rootfs, DXVK, or VKD3D-Proton dependency was found in the inspected Gradle dependencies, native CMake source list/link libraries, or repository paths examined for this audit. The APK is built for ARM64 Android, but that does not itself execute Windows PE files.

**Conclusion:** Wine/Box64 is not integrated.

### 4. Source Engine

No `source-sdk-2013`, Source Engine SDK, HL2 SDK, Source client/server code, SDL-based Source port, or Source-specific build target was found in the audited repository paths. There is no `.gitmodules` file on the audited `main` ref.

**Conclusion:** Source Engine is not supported by this repository's current runtime.

### 5. Build and test infrastructure

- `.github/workflows/build-android.yml` configures recursive submodule checkout, Android SDK/NDK, a native CMake build, and Gradle debug/release APK packaging.
- The inspected workflow does not run `./gradlew lint`, JVM unit tests, instrumentation tests, or native unit tests.
- No test source set or CTest target was identified in the audited build files.

The GitHub Actions build can prove that the current launcher/native library compiles and packages. It does not prove a Unity game runs, that Mono CIL executes, that IL2CPP is recompiled, or that Windows executables are supported.

## Architecture recommendation

1. **Keep the Unity native/recompilation work in this repository**, but describe the current state accurately as prototype scaffolding until managed execution, asset extraction, shader translation, gameplay systems, and per-title tests are complete.
2. **Evaluate Wine/Box64 as an optional external runtime module or separate app/project.** Do not inject it into the Unity asset pipeline or claim that Box64 alone loads Windows PE. A 32-bit x86 game (as shown by the current Overcooked 2 preflight screenshot) additionally needs a compatible 32-bit execution strategy such as a supported WoW64 configuration or Box86 path; x86-64 support alone is not enough.
3. **Treat Source Engine as a separate port/fork.** It needs a Source SDK-compatible codebase, platform layer, renderer/audio/input integration, and separate licensing/build/device validation.

## Explicit non-claims

This audit does not certify any listed game as playable. The README's previous "Working" labels and launch wording exceed what the inspected runtime sources substantiate; the runtime support matrix is corrected in this branch to distinguish code scaffolding from tested gameplay.
