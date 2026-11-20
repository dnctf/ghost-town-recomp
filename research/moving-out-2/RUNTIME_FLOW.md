# Moving Out 2 — Runtime / Startup Flow

## Evidence-backed flow model

| Stage | Evidence | Status |
|---|---|---|
| Windows process starts at PE entry point | `mo2.exe` is in baseline inventory; entry-point bytes not re-inspected | INFERRED |
| Unity native player loads | `UnityPlayer.dll` and `baselib.dll` in inventory | INFERRED |
| IL2CPP native game code becomes available | `GameAssembly.dll` in inventory; IL2CPP is a baseline lead | INFERRED |
| Game-specific managed metadata is consumed | no metadata artifact in inventory | NOT TESTED |
| Unity subsystems initialize | engine dependencies suggest runtime components | HYPOTHESIS |
| Game bootstrap and scene load | no Player.log, trace, or metadata mapping | NOT TESTED |
| First frame / input / audio / networking | no runtime evidence | NOT TESTED |

This is a candidate architecture sequence, not a captured trace. Do not infer a precise load order from file names.

## Ghost Town implications

The app currently contains a platform-independent state machine and a Boolean-based runtime driver abstraction. The controller previously advanced directly to `RUNNING` after `startSession() == true`, which could represent only process/session acceptance. This continuation adds `WAITING_FOR_FIRST_FRAME` and an explicit `confirmFirstFramePresented()` gate.

**Important integration limitation:** the renderer callback has not yet been wired to this controller in an actual Wine/game session. The state machine improvement prevents a false positive in controller semantics, but does not establish a real first-frame path.

## Required runtime evidence

Capture separate milestones from the real renderer/session:
- process created and PID observed;
- Wine/Box64 bootstrap completed;
- Unity player initialization observed;
- guest window created;
- Android surface attached;
- swapchain created;
- first game frame presented;
- input/audio path confirmed.

A host-side clear-frame smoke test is not proof of a Unity game's first frame.
