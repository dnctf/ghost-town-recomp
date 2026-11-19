# Ghost Town Recomp

> **Status: Experimental Android PC-game runtime research — no game is currently verified playable.**

Ghost Town Recomp is an Android ARM64 application and runtime research project exploring the launch and presentation of selected PC games in a game-focused, native fullscreen Android experience. The initial focus is Windows x86_64 Unity titles, with Overcooked! 2 and Moving Out 2 as research candidates.

## Purpose

- **Game library:** let users select and inspect their own game files.
- **Installation verification:** check executables, Unity data, Mono/IL2CPP markers, architecture, dependencies and installation fingerprints.
- **Compatibility profiles:** define per-game runtime, graphics, dependency, environment and input settings.
- **Runtime orchestration:** research and integrate PRoot, Box64 and Wine; supervise game process lifecycle, stop and recovery.
- **Graphics:** investigate Vulkan, DXVK, WineD3D, Turnip and Android surface presentation.
- **Android session experience:** target immersive fullscreen, touch/gamepad, lifecycle handling and diagnostics—not a desktop-style UI.
- **Diagnostics:** stage-aware reports to help isolate installation, runtime, graphics, input, audio and lifecycle failures.

## Target

- Device validation target: Huawei MatePad Air 2023 (ARM64 Snapdragon platform).
- Research candidates: Overcooked! 2 and Moving Out 2, using game files supplied by the user.
- Platform: Android ARM64.
- Game files and proprietary assets are not included; users must provide legally obtained copies.

## Current update — 19 November 2026

### Foundations present

- Android app and Compose dashboard/preview.
- SAF folder-picker foundation.
- Windows PE and Unity installation inspection helpers.
- Python game-installation probe tooling.
- Dedicated game-session lifecycle model.
- File-backed game catalog and file-only validation foundation.
- Runtime controller abstraction, graphics backend selection foundation, Android session-host contract and diagnostic logging foundation.
- Existing runtime/bootstrap research components for PRoot, Box64 and Wine, plus native Vulkan/audio/input scaffolding.

### Integration status

| Subsystem | Status |
|---|---|
| Android build/CI foundation | Verified in recorded CI runs; check current workflow for latest status |
| Game detection and file validation | Partial; file evidence only |
| SAF scanner and live catalog | Partial; picker exists, catalog integration remains |
| Profile JSON runtime resolver | Not integrated |
| Persistent game process supervisor | Not verified end-to-end |
| Wine/Box64 game launch | Not verified end-to-end |
| DXVK/Vulkan game rendering | Not verified |
| Android game-frame surface bridge | Not verified |
| First frame / black-screen gate | Not verified |
| Game input and audio transport | Not verified end-to-end |
| Pause/resume, recovery and stable session | Not verified on device |
| Gameplay on target device | Not verified |

Dedicated-session foundation commit: `db3051080566c92ae2050ca130848f2f3db7115f`. The lifecycle/catalog/validation/controller/graphics-selector/host modules are integration foundations, not a complete launcher. CI status should be checked against the latest commit/run before stating that the current branch is green.

## Intended architecture

```text
Android UI / Game Library
    -> SAF selection + persisted permission
    -> Scanner / Installation Validator
    -> Game Identity + Compatibility Profile
    -> Runtime Config Resolver
    -> Runtime Controller / Process Supervisor
    -> PRoot -> Box64 -> Wine -> Windows game
    -> Wine display backend / internal X11 compatibility
    -> Vulkan / DXVK or validated graphics fallback
    -> Android fullscreen presentation surface
    -> Input + Audio + Lifecycle + Diagnostics
```

This is the target architecture, not a statement that all connections exist. Retain legacy X11/desktop-related components, Wine, Box64, PRoot, IPC/shared-memory paths, Vulkan bridge, DXVK and Turnip while their roles are audited and integration/replacement paths are proven. Internal compatibility components must not become user-facing desktop controls.

## First-frame acceptance gates

Do not use a single generic GPU-ready flag. Required milestones remain distinct:

```text
GAME_VALIDATED
 -> RUNTIME_INITIALIZED
 -> GAME_PROCESS_STARTED (real PID)
 -> GPU_INITIALIZED
 -> SWAPCHAIN_CREATED
 -> SURFACE_ATTACHED
 -> FIRST_FRAME_PRESENTED
 -> RUNNING
```

These milestones must be proven by the actual renderer/session path. A native Vulkan clear-frame test is not evidence that a Wine/DXVK game frame is displayed.

## Roadmap

1. Build gate and audit actual runtime/native integration points.
2. SAF scanner/catalog, persisted permission recovery, deduplication, profile schema validation and resolver.
3. Concrete runtime driver with process ownership, PID/exit monitoring, stream capture, timeouts, cancellation, cleanup and recovery.
4. Graphics gates, DXVK/fallback initialization, swapchain/surface attachment and renderer first-frame event.
5. Lifecycle-safe session controls, touch/gamepad bridge and audio.
6. Correlated session diagnostics, export and first-game device validation.
7. Compatibility expansion only after repeatable per-game device evidence.

## Build and CI

Check [GitHub Actions](https://github.com/dnctf/ghost-town-recomp/actions) for workflow results. CI validates only the build/test/package steps included in that workflow; it does not establish Windows game execution, graphics presentation or gameplay. Development notes and audits are in `docs/` and [RUNTIME_AUDIT.md](RUNTIME_AUDIT.md).

## Legal

This independent research project is not affiliated with or endorsed by the game or technology rights holders. Game executables and proprietary assets are not distributed here. Users are responsible for providing game copies they are legally entitled to use.
