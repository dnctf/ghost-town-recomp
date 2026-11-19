

## Project overview and runtime integration status update (2026-11-19)

README was refreshed to describe the app's purpose, intended architecture, implementation foundations and roadmap. This documentation update does not claim game playability or a first-frame result.


### Project identity

Ghost Town Recomp is experimental Android ARM64 launcher/runtime research focused on game-oriented Android sessions for user-provided Windows PC games. Initial candidates are Overcooked! 2 and Moving Out 2. The goals are installation inspection, compatibility profiles, runtime orchestration, graphics presentation, input/audio, lifecycle and diagnostics while keeping compatibility/desktop components internal.


### Integration status

| Area | Status | Remaining gap |
|---|---|---|
| Android build foundation | CI evidence exists | Recheck newest branch workflow before claiming current-green status |
| Game validation | Partial | File checks are not runtime compatibility proof |
| SAF/catalog | Partial | Persisted URI, scanner and catalog integration need verification |
| Profiles | Partial | JSON/schema exist; runtime validation/resolution not integrated |
| Runtime driver | Blocked | Persistent game launch and process supervision not proven |
| Graphics | Blocked | Vulkan/native surface does not prove Wine/DXVK game presentation |
| First frame | Blocked | Renderer-confirmed event absent |
| Input/audio | Partial | Native scaffolding exists; game bridge unverified |
| Diagnostics | Partial | Correlation ID, unified event bus and export incomplete |
| Device gameplay | Not verified | No repeatable device gameplay evidence |

### First-frame policy

Keep graphics milestones distinct: `GPU_INITIALIZED`, `SWAPCHAIN_CREATED`, `SURFACE_ATTACHED`, and `FIRST_FRAME_PRESENTED`. The controller must not transition to `RUNNING` based only on a Boolean launch result. First-frame evidence must originate from the actual renderer/session path, not a native clear-frame test.

### Scope of this update

Documentation only: README, runtime status document, and audit notes. No runtime code was changed and no build/device test was run in this documentation update.
