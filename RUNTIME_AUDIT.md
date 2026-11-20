## Project overview and runtime integration status update (2026-11-20)

The Moving Out 2 continuation added a first-frame gate to the platform-independent lifecycle controller and expanded the compatibility research documents. This is a source-level correctness improvement only; it does not claim game playability.

### Integration status

| Area | Status | Remaining gap |
|---|---|---|
| Android build foundation | CI workflow triggered for PR #11; pending at last check | Wait for completed run and inspect logs |
| Game validation | Partial | File checks are not runtime compatibility proof |
| SAF/catalog | Partial | Persisted URI, scanner and catalog integration need verification |
| Profiles | Partial | JSON/schema exist; runtime validation/resolution not integrated |
| Runtime driver | Blocked | Persistent Wine/Box64 game launch and process supervision not proven |
| Graphics | Blocked | Vulkan/native surface does not prove Wine/DXVK game presentation |
| First-frame state semantics | Implemented, tests updated | Real renderer callback is not yet wired to `confirmFirstFramePresented()` |
| Input/audio | Partial | Android UI/native scaffolding exists; Wine guest bridge unverified |
| Diagnostics | Partial | Correlation ID, unified event bus and export incomplete |
| Device gameplay | Not verified | No repeatable device gameplay evidence |

### First-frame policy

Keep graphics milestones distinct: `GPU_INITIALIZED`, `SWAPCHAIN_CREATED`, `SURFACE_ATTACHED`, and `FIRST_FRAME_PRESENTED`. The controller now transitions from `STARTING_SESSION` to `WAITING_FOR_FIRST_FRAME`; only an explicit call to `confirmFirstFramePresented()` can transition it to `RUNNING`. The method must be called only by the actual game renderer after presenting its first frame, never by surface attachment or a host-side clear-frame test.

### Scope of this update

Source-level change: `GameSessionStateMachine`, `GameRuntimeController`, and their JVM regression tests. Research documentation was expanded. No local Gradle/NDK build, APK build, Windows launch, Android device test, or game session was run in this continuation.
