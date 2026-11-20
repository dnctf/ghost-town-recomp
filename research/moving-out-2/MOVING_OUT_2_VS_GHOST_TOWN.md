# Moving Out 2 vs Ghost Town — Compatibility Gap Matrix

Status labels: **Implemented and verified** means verified by a relevant test/runtime result, not merely source presence. This audit is repository-source inspection; it does not imply target-device execution.

| Component | Game requirement / evidence | Current Ghost Town | Gap | Evidence | Priority |
|---|---|---|---|---|---|
| Windows x64 PE | Windows x64 reported | PE64 header inspection exists in importer/validator | Does not inspect imports, delay imports, exports, or resolve full DLL dependency closure; validator must be run on actual game folder | Baseline lead + source | P1 |
| Unity runtime | Unity 2021.3.16f1 reported | Wine/Box64 bundle; WineRuntimeSession prepares an Overcooked2 prefix only | No Moving Out 2 process launch or game-specific Unity startup proof | Source audit | P0 |
| IL2CPP | GameAssembly present; IL2CPP reported | Metadata header-only loader | No versioned metadata parser, no GameAssembly loading/IL2CPP runtime bridge; GameRuntimeController has no production RuntimeSessionDriver implementation in inspected source | VERIFIED source gap | P0 |
| global-metadata.dat | Not listed in baseline inventory | Validator looks for it; parser stub | Artifact availability unknown; no class/method map | NOT TESTED | P0 |
| CPU translation | x86-64 guest on ARM64 | Box64 pinned; CI smoke reported | Device dynarec and Burst ISA path untested | Manifest | P0 |
| Wine | Windows API environment | Wine 11.19 bundle; headless prefix/probe | No game process lifecycle | Manifest + source | P0 |
| Native DLLs | FMOD, Resonance, NanoSockets, Crashpad, Steam listed | rootfs package + Wine only | Exact imports/loads and DLL availability unknown | Inventory / no PE report | P1 |
| Graphics | Actual API unknown | X11/VirGL host code; WineD3D baseline; DXVK/Turnip deferred | No guest ICD / game frame / target-device proof | Manifest + source | P0 |
| Vulkan / Turnip | API not yet confirmed | selector checks capability flags; Turnip not bundled | no device validation or actual guest Vulkan proof | Source + manifest | P0 |
| DXVK | D3D API not yet confirmed | not bundled by design | enable only after guest Vulkan + visible D3D11 frame | Manifest | P1 |
| Vortek | no game evidence | no verified integration in inspected source | Not currently justified | NOT TESTED | P3 |
| VirGL | API path unconfirmed | X11/VirGL host components and Mesa guest package | guest transport compatibility unproven | Manifest + source | P1 |
| Audio | FMOD + Resonance DLLs listed | app AAudio source only | no Wine guest audio bridge | Manifest | P1 |
| Input | game APIs unknown | gamepad UI + X11 touch pointer | no verified Wine controller bridge | Source | P1 |
| Networking / Steam | NanoSockets + Steam API listed | no verified platform bridge | normal Steam/game service behavior unknown | Inventory | P2 |
| Android surface | Windows game needs a presented guest window | Android host interface + X11 session | no verified lifecycle/swapchain/first game frame | Source | P0 |
| Diagnostics | Crashpad components listed | app log and CI diagnostics | no correlated process/renderer crash telemetry | Source | P1 |
| Memory | large native game and engine modules | native memory arena / app memory code | peak RSS, address-space pressure, OOM and allocator behavior unmeasured | NOT TESTED | P1 |
| Game session | process must remain supervised | state machine + Boolean driver abstraction; controller not wired to a production driver | renderer first-frame callback was not gated; state semantics patched, but no renderer/driver integration | VERIFIED source defect; partial fix in this branch | P0 |

## Classification summary

- **Implemented and verified:** only narrow CI/unit-test/source-level items when their exact tests pass. No game-playability feature qualifies.
- **Implemented but untested:** Wine bundle on CI, Box64 smoke rungs A–E on Debian CI, Android X11/VirGL components, Android Vulkan source.
- **Partial:** file validator, game profile, gamepad UI, diagnostics, runtime session orchestration.
- **Stub/demo:** MovingOut2Compiler compile method simulates work with a sleep; MovingOutLauncherActivity launch button only displays a toast and finishes the activity; IL2CPP parser only reads a short header.
- **Missing:** real Unity IL2CPP runtime bridge, Moving Out 2 game launch, game-specific first-frame proof, Wine guest audio/input bridge.
- **Broken / correctness concern:** controller marked RUNNING immediately after Boolean startSession; this continuation adds a first-frame gate, but renderer integration remains outstanding.

## Additional verified implementation gaps\n\n- `MovingOut2Compiler.compileAndPrepare()` sleeps briefly and returns success; it does not compile, convert, or prepare game assets.\n- `MovingOutLauncherActivity` enables the launch control after directory checks, but its click handler displays a toast and finishes the activity; it does not start a Windows process.\n- `WineRuntimeBridge` is a fixed headless Wine version probe and diagnostics helper. `WineRuntimeSession` creates an Overcooked2 Wine prefix; it is not a general game-session driver.\n- `GameRuntimeController` is a platform-neutral orchestration class with a driver interface; inspected production sources do not supply a concrete implementation.\n- `GameValidationPipeline` checks PE64 AMD64 and selected Unity data markers, then hashes a metadata/managed file. It does not parse PE imports or verify every native DLL.\n- `Il2CppMetadataLoader` checks the metadata magic after reading a short legacy header; its estimated method count is not a valid class/method map.\n\n## First blockers

1. Real IL2CPP execution path (not a metadata parser alone).
2. Wine/Box64 game process creation and supervision.
3. Real guest graphics presentation on Android.
4. Device CPU/graphics validation.
5. Audio and controller bridge.
