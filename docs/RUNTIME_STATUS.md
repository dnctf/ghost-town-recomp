# Runtime status and porting plan

**Status: experimental prototype — PC game execution is not implemented.**

## What the current APK can do

- Import/copy selected PC game data folders.
- Perform basic folder and metadata checks.
- Display four separate game entries and their detected data state.
- Initialize an Android native library and create a Vulkan surface on supported devices.
- Accept some Android gamepad and touch input events.

## What it cannot do yet

- It cannot execute a Windows PE .exe directly on Android.
- It does not contain Wine, Box64, a Windows syscall/loader layer, or a configured DirectX-to-Vulkan stack.
- The current native render loop calls a clear-frame operation. It does not render Unity scenes, meshes, materials, animation, UI, or gameplay.
- The Mono host only validates the presence of an assembly/header; it does not link and initialize a complete Mono runtime, create a managed domain, resolve UnityEngine internal calls, or invoke game methods.
- The IL2CPP code reads metadata headers; it does not reconstruct and compile the game logic into Android ARM64 code.
- Shader name mapping or sample shaders are not a complete Unity ShaderLab/HLSL translation pipeline.
- The Moving Out launcher screen is not a game execution backend.

Consequently, a successful Gradle/NDK build or a detected game folder must never be described as a playable game.

## Two technically distinct routes

### Route A — Native port/recompilation

This is not a generic conversion of an arbitrary .exe into an APK. It requires legally usable source code or a sufficiently complete, game-specific reverse-engineering/reimplementation effort. For Unity titles, it also requires a replacement or port of the relevant Unity runtime subsystems and platform integrations: asset loading, scripting, scene graph, physics, animation, audio, input, UI, rendering, save systems, and platform services. IL2CPP metadata alone is not executable game logic.

### Route B — Windows compatibility runtime

For an unmodified Windows PC build, investigate a compatible Wine + Box64 path on ARM64, then add the correct graphics translation path for the title (for example, D3D-to-Vulkan where applicable). This is a separate compatibility-runtime project, not native recompilation. It requires per-title testing of executable architecture, Unity version, graphics API, anti-cheat/DRM, audio, controller support, memory, and thermal behavior. Do not assume that every Windows Unity game works.

## Dependencies: do not bundle blindly

- Android provides its own platform Vulkan loader/driver; the app must query actual device/driver capabilities.
- Windows VC++ redistributables, DirectX redistributables, and Windows desktop .NET runtimes do not become Android dependencies merely by copying them into an APK.
- Unity Mono and IL2CPP builds need different execution strategies. A Unity Mono game needs a compatible managed runtime plus Unity API bindings; an IL2CPP game needs the corresponding native player/runtime and game-specific native code.
- Any third-party runtime/library must be added with its license, ABI, version, source provenance, and checksum documented.

## Device target and validation gate

Primary target: Huawei MatePad Air 2023, Snapdragon 888 / Adreno 660, ARM64, HarmonyOS 4.2.

A playable milestone requires all of the following on a real device:
1. Identify one exact game build and its executable architecture, Unity version, graphics API, and Mono/IL2CPP backend.
2. Import the complete legitimate installation, not only the _Data directory if the selected execution path needs the .exe, DLLs, and adjacent files.
3. Boot through a real runtime and reach the game's title/menu scene.
4. Render actual game geometry/materials and execute gameplay logic (not a clear-color screen).
5. Verify audio, touch/controller input, save/load, suspend/resume, and repeated launches.
6. Record frame pacing, memory use, thermal behavior, and crashes during a sustained session.

Until those gates pass, show **Data detected / Runtime unavailable**, not **Ready to play**.
