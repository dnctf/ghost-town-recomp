# Moving Out 2 Dedicated Refactor — Initial Code Audit

Date: 2026-11-20
Branch: moving-out-2/dedicated-compatibility
Audit level: source-path review; no local Android build or device test performed.

## Scope decision

Moving Out 2 PC is the only active game target. The app should become a dedicated Android ARM64 game runtime/session application. Overcooked! 2, original Moving Out, game selection, multi-game compiler architecture, and generic game compatibility are no longer active product requirements.

## Findings confirmed from source inspection

| Severity | File / component | Finding | Required action |
|---|---|---|---|
| Critical | MainActivity.java | Multi-game selection/cards and import/detection logic still include Overcooked 1/2 and original Moving Out | Replace with single Moving Out 2 import/verification/launch flow |
| Critical | MainActivity.java | Runtime preparation calls WineRuntimeSession.prepareOvercooked2 | Rename/generalize only to MO2-specific session or replace with dedicated driver; remove Overcooked assumptions |
| Critical | MovingOut2Compiler.kt | compileAndPrepare simulates work using a short sleep and progress strings, then reports success | Remove fake compiler-success semantics; report explicit unsupported/not implemented until a real runtime preparation stage exists |
| Critical | MovingOutLauncherActivity.kt | Activity defaults to original Moving Out compiler and can switch based on title intent; contains compile/launcher semantics | Make Moving Out 2 explicit, remove original Moving Out branch and misleading compile button/status |
| High | CompilerRegistry.kt | Registers Overcooked 1/2 and both Moving Out titles | Replace registry with a single Moving Out 2-specific installation/runtime service or remove registry abstraction |
| High | GameStorageVerifier.java | Multi-game enum and detection logic for Overcooked 1/2, Moving Out, Moving Out 2 | Reduce to MO2-specific identity and layout; reject wrong game with clear diagnostics |
| High | GameValidationPipeline.kt | Generic Unity file validation; accepts a generic Unity core marker and either Mono assembly or IL2CPP metadata | Tighten to the known Windows x64 Unity IL2CPP MO2 baseline, validate required player/native files, metadata, PE architecture and safe canonical paths |
| High | GameInstallationRegistry.kt | Persisted generic game catalog and generic identifiers | Keep only one MO2 installation record; migration of old data should be non-destructive and explicit |
| High | runtime/manifest.json + Android runtime-manifest asset | Runtime pack describes a general Wine stack and includes Overcooked-specific status and release gate | Create MO2 runtime manifest/gates; retain candidate dependencies until dependency audit and smoke evidence |
| High | DashboardActivity.kt | Contains demo placeholder games (Ember Circuit, Cloud Harbor, Mosslight, Signal Drift) and generic multi-game UI | Replace with truthful Moving Out 2 dashboard and real readiness diagnostics |
| High | app/build.gradle / AndroidManifest | Package namespace remains com.overcookedrecomp.app | Plan controlled namespace/application-id migration, including tests, native names, assets, saved-data migration and workflow references |
| Medium | MovingOut2Compiler.kt | Developer identity is assigned to Ghost Town Games | Correct game metadata: Moving Out 2 developers SMG Studio / DevM Games; Team17 is publisher |
| Medium | MovingOut2Compiler.kt | Detection checks a small subset of Unity data files and allows recursive nested folder fallback | Use observed PC installation layout, robust import root detection and positive executable/player/plugin checks |
| Medium | MainActivity / launcher | UI readiness and launch paths need review for false-positive status | Status must distinguish files detected, files validated, runtime installed, process started, first frame and playable |
| Medium | Workflow files | Multiple runtime build/download/inspection workflows remain | Audit triggers and artifacts for relevance; do not remove build dependencies until runtime architecture is selected |

## Moving Out 2 data baseline

Evidence recorded from user-supplied files and prior local inspection:

- Windows x64 / AMD64 PE build
- Moving Out 2 version 1.2.274
- Unity 2021.3.16f1 baseline
- IL2CPP
- mo2.exe
- GameAssembly.dll
- UnityPlayer.dll
- baselib.dll
- BuildInfo.json
- global-metadata.dat, IL2CPP metadata version 29 identification
- plugin inventory includes FMOD, Resonance Audio, Burst, NanoSockets, Crashpad and Steam-related components

Unity resource marker discrepancy remains: unity default resources marker identified 2021.3.13f1; unity_builtin_extra marker identified 2021.3.16f1. Resolve before pinning Unity resource versions. Graphics API remains unverified from authoritative player configuration/runtime evidence.

## Compatibility architecture guardrail

Moving Out 2's Windows x64 IL2CPP executable/native plugins cannot run directly on Android ARM64. Metadata availability is not proof of a feasible recompilation. Until a game-specific native port is demonstrated, the project must evaluate Windows compatibility execution (Wine and Box64) or another verified runtime route.

Do not delete PRoot, Box64, Wine, X11/display bridge, VirGL, Vulkan loader/bridge, DXVK or Turnip during scope cleanup. Each should be retained, disabled, slimmed or removed only after an MO2 dependency audit and regression evidence. Avoid adding new unverified graphics layers in parallel.

## Immediate work order

1. Audit MainActivity and dashboard UI behavior; identify every game selector/import/status and runtime call.
2. Define MO2 installation layout/profile and canonical game ID across Kotlin, Java, JSON, native and tests.
3. Replace generic/multi-game verifier with file-only MO2 identity validation and unit tests.
4. Replace fake compiler/launcher flow with explicit runtime availability and actual supervised launch.
5. Trace and fix the runtime pack installer, PRoot/Box64/Wine session, process supervision, X11/display and graphics presentation.
6. Wire first-frame confirmation to actual game rendering; no synthetic clear-frame proof.
7. Integrate controller/touch input, audio and lifecycle.
8. Remove obsolete game code/UI/profile and generic paths after references/tests are verified.
9. Update package namespace/app branding/workflows/changelog and preserve user settings/game paths.
10. Run Android build, unit tests, workflow, then physical device validation. Report each gate separately.

## Validation status

- GitHub source files reviewed: README, Android app entry points, dashboard, Moving Out 2 compiler/launcher, compiler registry, verifier, profile/schema, graphics selector, runtime manifest, architecture/graphics docs.
- Scope documentation and Moving Out 2 profile changes committed on the dedicated branch.
- No local Gradle build, APK build, Android device launch, first frame or Moving Out 2 gameplay test has been executed during this audit.
- Do not claim Moving Out 2 is playable or compatible yet.
