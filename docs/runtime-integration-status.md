# Runtime Integration Status

**Updated:** 2026-11-19  
**Repository:** `dnctf/ghost-town-recomp`  
**Branch reviewed:** `refactor/dedicated-runtime-session`  
**Baseline:** `db3051080566c92ae2050ca130848f2f3db7115f`

> Source/CI audit only. No first frame or playable game is verified by this update.

## Summary

Ghost Town Recomp is an experimental Android ARM64 game-runtime research application. Its goal is a game-focused Android session for user-provided Windows PC games, including installation inspection, compatibility profiles, runtime supervision, graphics presentation, input/audio, lifecycle and diagnostics.

Dedicated-session foundations exist, but the lifecycle/catalog/validation/controller/graphics-selector/host modules are not yet proven to connect to a persistent Wine/Box64 game launch and actual renderer presentation.

## Status matrix

| Milestone | Status | Notes |
|---|---|---|
| P0.1 CI/build/backend audit | PARTIAL | Recorded CI passed build/test/lint/package gates; verify latest branch run. Backend audit still needs complete mapping. |
| P0.2 profiles and SAF scanner | PARTIAL | JSON/schema and folder-picker foundations exist. Resolver, live catalog, fingerprint dedupe, permission recovery and schema migration need implementation/verification. |
| P0.3 process driver/supervisor | BLOCKED | Persistent game driver with PID supervision, exit status, stage timeouts, output capture and process-tree cleanup not verified. |
| P0.4 graphics/surface | BLOCKED | Vulkan/native surface foundations exist, but Wine/DXVK game presentation and first-frame callback are unproven. |
| P0.5 input/audio/lifecycle | PARTIAL | Input and audio scaffolding exist; game bridge and session lifecycle are not verified. |
| P0.6 diagnostics/first game | PARTIAL | Log foundation exists; correlation ID, unified event bus, failure mapping/export and first-game device test remain. |

## Foundations

- `GameSessionStateMachine.kt`: session lifecycle model.
- `GameInstallationRegistry.kt`: file-backed catalog foundation.
- `GameValidationPipeline.kt`: file-only validation and executable/fingerprint inspection.
- `GameRuntimeController.kt`: lifecycle orchestration contract.
- `GraphicsRuntimeSelector.kt`: capability-based selection foundation, not a renderer test.
- `AndroidGameSessionHost.kt`: Android session/surface/input lifecycle contract.
- `RuntimeDiagnostics.kt`: stage-aware logging foundation.

## P0.2 gaps

- Load and validate profile JSON against schema at runtime.
- Implement engine, architecture and dependency detection plus profile-to-runtime resolver.
- Integrate SAF tree URI and persisted permission with scan/catalog.
- Revalidate access after restart and recover revoked permission.
- Deduplicate using game identity/fingerprint, not path alone.
- Add bounded/cancellable scanning and catalog migration/recovery.

## P0.3 gaps

- Concrete driver using existing bootstrap/session APIs rather than duplicating runtime components.
- Process ownership and real PID capture.
- stdout/stderr and exit-code capture.
- Per-stage timeouts, cancellation and failure propagation.
- Liveness/hang monitoring and complete process cleanup.
- Prevent `RUNNING` based solely on a Boolean launch result.

## P0.4 gaps

Keep GPU milestones separate: `GPU_INITIALIZED`, `SWAPCHAIN_CREATED`, `SURFACE_ATTACHED`, `FIRST_FRAME_PRESENTED`. Validate loader, physical device, extensions and driver; then prove DXVK or a tested fallback initializes in the Wine game session. Surface ownership, recreation, synchronization and first-frame evidence must come from the actual game renderer. Native clear-frame rendering is not game presentation proof.

## P0.5 gaps

- Forward touch/virtual/physical gamepad input into Windows game through a validated backend.
- Connect Wine audio output to Android audio transport; a native stream scaffold is not game audio.
- Define pause/resume/stop across Activity lifecycle, surface recreation, focus loss and memory pressure.
- Avoid orphan processes and resource leaks during repeated transitions.

## P0.6 gaps

Use a `SESSION-XXXXXXXX` correlation ID and structured timeline with timestamp, stage, severity, code, message and process identity. Map actionable errors (missing DLL, bootstrap failure, Box64 crash, Vulkan device failure, swapchain failure, surface loss, first-frame timeout). Provide privacy-safe diagnostic export.

Test order: unit -> integration/smoke -> runtime launch -> first frame -> input/audio -> pause/resume/stop -> repeatable device stability test. Overcooked! 2 and Moving Out 2 are candidate targets, not verified supported titles.

## Acceptance evidence

Mark gates PASS only with artifacts/logs. CI compilation is not gameplay proof; process creation is not first-frame proof; Vulkan enumeration is not DXVK game-render proof; first frame is not full playability. Device evidence should include device, build/commit, profile, session log and test outcome.

## Scope of this update

Documentation-only: README overview, runtime integration status, and audit notes. No runtime source or behavior was changed; no build or device test was run in this documentation commit.
