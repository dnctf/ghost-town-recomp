# Moving Out 2 Only — Scope and Migration Plan


## Decision

Ghost Town Recomp is now exclusively for **Moving Out 2 PC** on Android ARM64. Overcooked! 2, the original Moving Out, multi-game catalogs/registries and generic game support are out of active scope. Existing components may be retained internally only if they demonstrably support the Moving Out 2 path.

This scope change does not assert that Moving Out 2 has been ported, recompiled, or made playable.

## Evidence baseline

- Game: Moving Out 2 (PC)
- Game version: 1.2.274
- Platform: Windows x64 / AMD64
- Engine: Unity 2021.3.16f1 baseline
- Scripting backend: IL2CPP
- Metadata: global-metadata.dat; IL2CPP metadata v29 identification recorded in research
- User-provided files inspected: mo2.exe, GameAssembly.dll, UnityPlayer.dll, baselib.dll, BuildInfo.json, Unity resource files and additional native/plugin files
- Plugin inventory includes FMOD, Resonance Audio, Burst, NanoSockets, Crashpad, Steam-related components
- Proprietary files remain local; never commit, upload, or attach them to CI.

Resource marker discrepancy: unity default resources was identified as 2021.3.13f1 while unity_builtin_extra was identified as 2021.3.16f1. The discrepancy remains unresolved; confirm provenance and runtime/player version before pinning dependencies.


## Compatibility reality

The target executable and native components are Windows x86_64, while the Android app is ARM64. IL2CPP metadata and Unity assets do not provide an Android-ready native game. A native recompilation/reimplementation path would be a separate high-risk research track requiring proof of recoverable game logic, Unity API replacement, physics, rendering, audio, platform services and networking.

Until a game-specific native port is proven, investigate a compatibility runtime that can execute the Windows x86_64 Unity Player on Android. Keep Wine/Box64/PRoot and graphics components as candidate dependencies—not mandatory defaults and not targets for wholesale removal. Pin, slim, or remove them only after Moving Out 2-specific tests establish the dependency.

## Repository migration checklist

### Target identity and product/UI
- [ ] Replace multi-game and generic launcher messaging with Moving Out 2-only identity.
- [ ] Remove demo placeholder game cards and fake readiness/runtime values.
- [ ] Keep UI actions truthful: detection, validation, runtime availability, launch, session, stop, failure.
- [ ] Remove support affordances for Overcooked! 2 and original Moving Out from active UI and flows.
- [ ] Review package namespace/application ID; migrate carefully with tests and data-preservation notes, not as a cosmetic rename.

### Detection and validation
- [ ] One canonical Moving Out 2 profile ID; resolve current hyphen/underscore mismatch.
- [ ] Validate actual Windows x64 executable identity (PE machine and PE32+), expected Unity player files, data directory, IL2CPP metadata, GameAssembly and required dependencies.
- [ ] Use the exact target's layout and allow documented Steam/Production layout variants.
- [ ] Fingerprint meaningful files; report missing, ambiguous and unsupported evidence explicitly.
- [ ] Do not declare Ready based on generic Unity markers or metadata alone.
- [ ] Preserve file-only verification; never execute game files during inspection.

### Runtime and compatibility
- [ ] Trace app launch path end-to-end; identify stubs, demo/simulation methods and unreachable code.
- [ ] Audit runtime pack versions, architecture, loader, glibc rootfs, Wine and Box64 dependencies.
- [ ] Determine actual Unity graphics API from binary/player evidence and controlled logs; do not assume D3D11/Vulkan.
- [ ] Verify guest Vulkan loader/ICD and host Android presentation separately.
- [ ] Prove actual game first frame, not a native clear frame.
- [ ] Validate IL2CPP startup/runtime behavior, native plugins and Steam-related services; document unavailable optional services.
- [ ] Test audio path, touch/gamepad mapping, lifecycle, suspend/resume, graceful exit and crash cleanup.
- [ ] Capture session-correlated, sanitized logs and performance/thermal data.

### Code and build cleanup
- [ ] Remove or isolate multi-game compiler registry and unrelated game-specific code after references are mapped.
- [ ] Remove misleading compileAndPrepare simulation and placeholder launch success; use explicit NOT_IMPLEMENTED/UNAVAILABLE until a real runtime path exists.
- [ ] Keep tests for validation, runtime lifecycle and first-frame gates.
- [ ] Reduce runtime bundle to verified Moving Out 2-required components only after dependency evidence and licensing review.
- [ ] Update workflows, package metadata, changelog, docs, tests and UI text to match target.
- [ ] Verify Android build and tests on the branch before proposing merge.

## Migration order (do not perform destructive removals ahead of audit)

1. Baseline branch and scope document.
2. Full source/workflow reference audit; identify all generic, Overcooked, original Moving Out, placeholder and runtime code.
3. Moving Out 2 installation profile and validator tests.
4. Dedicated launcher and truthful status/UI.
5. Compatibility runtime selection based on Unity 2021.3.16f1, IL2CPP, Windows x64 and plugin evidence.
6. Android surface/rendering and first-frame integration.
7. Input/audio/lifecycle.
8. Device gameplay and performance validation.
9. Runtime slimming and obsolete subsystem removal.
10. Release readiness.

## Acceptance gates

- Game installation is positively identified as Moving Out 2 1.2.274 and the required file set is validated.
- Unsupported, incomplete, wrong-game and wrong-architecture installations fail with actionable diagnostics.
- Runtime startup is supervised with real PID/session state and timeouts.
- The actual Moving Out 2 renderer presents a frame to Android.
- Main menu and gameplay are reached with working input and audio.
- A repeatable device gameplay test completes without fatal crash; test duration, FPS, memory, thermals and logs are recorded.
- CI/build success alone never means the game is compatible or playable.
