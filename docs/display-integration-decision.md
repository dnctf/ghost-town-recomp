# Display Integration Decision: XServerView / EGL / VirGL

## Scope

This decision addresses the display foundation only. Box64 ARM64 v0.4.5, Wine 11.19, Debian Bookworm, runtime packaging, and game-launch behavior are out of scope and must remain unchanged.

Repository baseline: `ecf0a8e8bf33a39e9cb9f91d3ddabedc71c3a558`.

WinlatorXR submodule pin: `39b270c6401a69e7c28ea6ffd61de3dbac9e9b1e` (`dawn-42`), repository `WinlatorXR/WinlatorXR`.

## Verified API comparison

| Integration call / assumption | Pinned WinlatorXR API | Result |
|---|---|---|
| `new XServerView(context, xServer, callback)` | `XServerView(Context, XServer)` | Constructor mismatch |
| `view.getRenderer()` | Public `getRenderer()`, returns `GLRenderer` | Compatible |
| XServer renderer registration | `xServer.setRenderer(renderer)` | Compatible |
| Surface-created callback on view | No callback API on `XServerView` | Not available |
| EGL lifecycle callback | `GLRenderer.onSurfaceCreated(GL10, EGLConfig)` | Exists on renderer |
| Surface size callback | `GLRenderer.onSurfaceChanged(GL10, int, int)` | Exists on renderer |
| Frame callback | `GLRenderer.onDrawFrame(GL10)` | Exists on renderer |

Pinned `XServerView` constructs an internal `XrRenderer`, registers it with GLSurfaceView via `setRenderer`, and exposes it through `getRenderer()`. Replacing that renderer after construction is not a safe integration assumption. The upstream repository has only the `cmod_bionic` branch at the time of this audit; no alternate branch containing the callback constructor was identified.

## Options

### Option A — Bump WinlatorXR submodule

**Not selected.** No alternate branch/version providing the expected callback constructor was identified during this audit. A speculative dependency bump would add unrelated source/native churn and regressions risk. Revisit only if a specific commit is found that provides a documented callback and is compatible with Ghost Town's renderer/native dependencies.

Trade-offs:
- Potentially cleaner upstream callback API if a compatible commit exists.
- Higher dependency/native compatibility risk and larger review surface.
- Reversibility: submodule pin can be reverted, but associated integration changes may be needed.

### Option B — Adapt the integration layer to the pinned two-argument API

**Preferred direction, but implementation is currently blocked.** Construct `XServerView(context, xServer)` and retain the upstream renderer. VirGL must not be started until a reliable EGL/surface-ready signal is available.

The pinned API exposes renderer lifecycle callbacks, but Ghost Town cannot install a callback into the already-registered internal renderer through the public `XServerView` API. Calling `queueEvent` alone is not accepted as proof that `onSurfaceCreated` has completed or that a valid shared EGL context can be obtained. Therefore, changing the constructor alone or starting components immediately after view creation would be unsafe.

Trade-offs:
- Smaller dependency change and preserves the existing renderer.
- Requires a reliable surface/EGL lifecycle bridge; the current integration API does not provide one.
- Reversibility: isolated integration changes are easy to revert once a callback bridge is available.

## Decision

Retain the pinned WinlatorXR commit and pursue Option B **only after a safe lifecycle bridge is identified**. Do not implement a constructor-only fix. The current public API does not provide the required callback bridge, so the requested integration-only implementation is blocked pending either a compatible upstream API/commit or an explicitly scoped change to the display-view component that exposes surface lifecycle without replacing `XrRenderer`.

No display state may be reported as READY merely because the view or service components started. The target state machine is:

`INIT -> SURFACE_READY -> EGL_READY -> FIRST_FRAME -> READY`

Any error or timeout must transition to `FAILED`. `READY` requires a verifiable first rendered frame (not just `onDrawFrame` invocation or a clear-only frame). A pixel/readback or other reliable visible-frame self-test is required before display readiness can be claimed.

## Required follow-up before integration patch

1. Identify a callback bridge that runs after the surface and EGL context are valid while preserving the pinned `XrRenderer`.
2. Confirm VirGL shared-context acquisition and cleanup thread requirements from native code.
3. Implement the display state machine, bounded timeout, diagnostics, and first-frame self-test only after the bridge is validated.
4. Add JVM/unit tests for state transitions and failure/timeouts.
5. Run `assembleDebug`, existing release build and runtime smoke workflows; device-dependent visible-frame verification must remain BLOCKED until actually tested on a device/emulator.

## Verification boundary

This document is based on source/API inspection through GitHub. No local checkout, compilation, device execution, or EGL/VirGL rendering test was available during this audit. No constructor fix, display READY claim, or game launch is authorized by this decision.
