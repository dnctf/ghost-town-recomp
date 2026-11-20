# Moving Out 2 — Implementation Plan

## Completed in this continuation

- Repository baseline and PR #11 checked.
- Audited core Kotlin runtime, Wine adapters, X11/VirGL display session, compiler/launcher, native CMake, runtime manifest, profile validation, unit tests, and Android CI workflow.
- Added `WAITING_FOR_FIRST_FRAME` state. The controller now remains there after a Boolean session start and transitions to `RUNNING` only through explicit `confirmFirstFramePresented()`.
- Added controller/state-machine regression tests for first-frame gating, invalid confirmation, stop-before-frame, and retry.

## Priority plan

### P0 — Runtime correctness
1. Wire renderer's actual first-present event to `confirmFirstFramePresented()`; never call it from surface attach or host clear-frame.
2. Implement process driver that launches Wine/Box64 with argv (no shell), captures PID/output, supervises exit, and owns cancellation/cleanup.
3. Validate executable and all data paths through SAF/canonical URI-safe access; avoid assuming a raw filesystem path is available for arbitrary document providers.
4. Replace metadata header stub with versioned, bounds-checked parser only if metadata is provided; do not confuse parsing metadata with executing IL2CPP.
5. Validate a real Windows guest window and visible game frame on target hardware.

### P1 — subsystem bridges
1. Verify guest Vulkan/GLX and X11 transport; keep WineD3D/VirGL conservative baseline.
2. Add guest audio output through Wine's supported backend and Android audio focus lifecycle.
3. Bridge Android physical controllers to guest XInput/DirectInput; test multi-controller identity and hotplug.
4. Add process/renderer correlation IDs, bounded logs, crash reason and cleanup telemetry.
5. Measure peak memory, frame pacing, thermal throttling, and sustained Box64 translation cost.

### P2 — optional platform behavior
1. Determine Steam/NanoSockets dependency from imports and normal runtime logs.
2. Test offline/online behavior without bypassing authentication or licensing.
3. Test optional crash reporting behavior and disable external reporting only through documented app settings.

### Defer / do not implement without evidence
- DXVK / Turnip enablement before guest Vulkan and visible D3D11 frame pass.
- Vortek without a demonstrated compatibility or performance advantage.
- Custom IL2CPP AOT recompilation or proprietary code reconstruction.
- DRM/authentication bypass or redistributable game payload.

## Test gates

- JVM tests: state transitions, controller first-frame gate, validator boundaries, retry, stop, process exit, timeout and cleanup.
- CI: Python validation + Gradle unit tests + native ARM64 build + APK packaging + lint.
- Device: Wine/Box64 bootstrap; guest window; renderer first frame; input; audio; suspend/resume; crash/cleanup; sustained performance.
- Game: only user-owned legitimate Moving Out 2 installation; report each stage separately.

No APK, game launch, or target-device test is claimed by this document.
