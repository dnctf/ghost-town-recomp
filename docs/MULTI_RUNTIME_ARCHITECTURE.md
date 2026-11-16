# Hybrid Multi-Runtime Architecture

## Goal

Ghost Town remains the Android game library and runtime coordinator. Engine-specific execution stays in isolated backends so one runtime's dependencies and failures do not destabilize another.

```text
Android Launcher / Game Library
             |
             v
Game Manifest + Install Inspector
             |
             v
Runtime Registry / Capability Resolver
       |             |              |
       v             v              v
 Unity Native    Wine + Box64    Source Engine
 (experimental) (external)      (separate port)
```

## Launcher responsibilities

- Import and inspect user-owned game installations.
- Store game metadata, executable/data paths, and runtime preferences.
- Select a backend from an explicit game profile and detected capabilities.
- Report truthful states: detected, runtime unavailable, ready to launch, running, exited, or failed.
- Manage per-game settings, controller mapping, logs, and lifecycle.
- Never infer that a game is playable because an executable or `*_Data` folder exists.

## Runtime adapter contract

Each backend should eventually expose equivalent operations:

- `probe()`: installed state, version, ABI requirements, and capabilities.
- `validate(game)`: validate paths and required runtime/game files.
- `launch(game, profile)`: start the game and return a session/result.
- `stop(session)`: request graceful shutdown.
- `collectLogs(session)`: return diagnostics.

Adapters must return explicit unavailable/unsupported/error states. A stub must never report a successful launch.

## Isolation rules

- Unity-native, Wine/Box64, and Source Engine have separate binaries and dependency graphs.
- Wine prefixes and Source content/configuration are isolated per game.
- Optional runtimes are versioned independently and are not linked into `overcooked_native`.
- User-owned game assets and executables stay local and are never committed to this repository.
- The launcher-to-runtime boundary should use validated paths and a documented IPC/deep-link contract, not arbitrary shell strings.

## Current implementation status

| Component | Status |
|---|---|
| Android launcher/importer | Existing experimental implementation |
| Unity Mono/CIL | Placeholder host; no managed runtime linked |
| Unity IL2CPP | Metadata header parser only |
| Vulkan | Android Vulkan integration scaffold |
| Wine/Box64 | Evaluation docs + inactive bridge stub |
| Source Engine | Separate project feasibility note |
| Runtime registry / IPC | Not implemented |
| End-to-end game execution | Not verified |

## Quality gates

A backend is supported only after CI builds it, backend self-tests pass, and physical-device tests demonstrate a reproducible game launch and gameplay path. Reports must identify device SoC/GPU, Android/HarmonyOS version, game build, runtime versions, graphics backend, and test outcome.
