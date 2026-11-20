# Moving Out 2 Compatibility Research

> Research notes for Ghost Town Recomp. Documentation and non-proprietary technical metadata only; no game binaries, DLLs, assets, dumps, or redistribution material.

## Current status (2026-11-20)

- Research branch: `research/moving-out-2-baseline`
- Research PR: [#11](https://github.com/dnctf/ghost-town-recomp/pull/11) — open, draft; not merged.
- Moving Out 2 sample: Windows x64 Unity IL2CPP, Unity 2021.3.16f1 / build 1.2.274 are **baseline leads** from earlier binary inspection. The original binaries are not mounted in this continuation.
- `global-metadata.dat`: not listed in the baseline inventory; class/method mapping is blocked until a user-provided local artifact is available.
- Ghost Town: source audit found Wine/Box64 bootstrap and X11/VirGL scaffolding, but no verified Moving Out 2 launch, IL2CPP execution, guest audio/input bridge, or game first frame.
- Lifecycle correctness: controller now waits in `WAITING_FOR_FIRST_FRAME` after session start and requires explicit renderer confirmation before `RUNNING`. The real renderer callback is not yet wired.
- Tests and CI: source-level regression tests were added; execution status must be checked in the PR workflow before claiming they pass.

## Documents

| Document | Scope |
|---|---|
| [FILE_INVENTORY.md](FILE_INVENTORY.md) | Baseline inventory, hashes, missing artifacts, next required inputs |
| [BINARY_ANALYSIS.md](BINARY_ANALYSIS.md) | PE/dependency analysis, reproducible read-only commands, evidence limits |
| [CLASS_MAP.md](CLASS_MAP.md) | IL2CPP metadata availability and parser limitations |
| [RUNTIME_FLOW.md](RUNTIME_FLOW.md) | Startup/lifecycle evidence model and first-frame gate |
| [GRAPHICS_ANALYSIS.md](GRAPHICS_ANALYSIS.md) | D3D/Vulkan uncertainty, DXVK, Turnip, VirGL and Android surface |
| [CPU_COMPATIBILITY.md](CPU_COMPATIBILITY.md) | x86-64, Burst, Box64 and ARM64 validation |
| [AUDIO_ANALYSIS.md](AUDIO_ANALYSIS.md) | FMOD, Resonance Audio and Wine/Android audio gaps |
| [NETWORK_PLATFORM_ANALYSIS.md](NETWORK_PLATFORM_ANALYSIS.md) | NanoSockets, Steam integration and safe validation |
| [INPUT_ANDROID_ANALYSIS.md](INPUT_ANDROID_ANALYSIS.md) | Controller, touch, X11 and Android lifecycle |
| [MOVING_OUT_2_VS_GHOST_TOWN.md](MOVING_OUT_2_VS_GHOST_TOWN.md) | Compatibility gap matrix and feature classification |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | Prioritized, testable engineering plan |
| [AGENT_CONTINUATION_PROMPT.md](AGENT_CONTINUATION_PROMPT.md) | Autonomous continuation instructions |

## Evidence standard

- **VERIFIED** — directly supported by source, tool output, or an executed test.
- **INFERRED** — plausible interpretation supported by evidence but not runtime-confirmed.
- **HYPOTHESIS** — requires an experiment.
- **NOT TESTED** — no relevant test or artifact was available.

File presence, class names, UI elements, manifest claims, and CI smoke tests do not prove game compatibility. A successful Wine prefix bootstrap does not prove Unity startup. A surface attach does not prove a game frame. A metadata parser does not execute IL2CPP.

## Sample identification (baseline)

- Executable: `mo2.exe`
- Target: Windows x64 / StandaloneWindows64
- Engine: Unity 2021.3.16f1 (prior string-inspection lead; revalidation pending)
- Scripting backend: IL2CPP (prior inventory lead; revalidation pending)
- Build: 1.2.274
- Release channel: Steam / Production
- Build metadata timestamp: 03/08/2023 06:15:41 (format/time zone unverified)

The SHA-256 values and approximate sizes for the 17 previously supplied files are retained in [FILE_INVENTORY.md](FILE_INVENTORY.md). They identify prior-session inputs and do not establish provenance.

## Immediate next requirements

1. If available, analyze a local `global-metadata.dat` in-session; do not commit it.
2. Obtain text-only PE headers/imports/exports and plugin-directory listing from the user's legitimate Windows installation.
3. Wire actual renderer first-present callback to the new controller gate.
4. Implement a supervised Wine/Box64 game process driver.
5. Validate guest X11/VirGL or Vulkan presentation on target Android hardware before enabling DXVK/Turnip.
6. Validate guest input/audio and collect reproducible device diagnostics.

## Do not commit

- Game executables or DLLs
- Game assets, textures, audio, scenes, bundles, proprietary resource archives
- Game dumps or reconstructed proprietary code
- Credentials, Steam account information, tokens, or sensitive configs
- DRM/license/authentication bypass tools or instructions

This is compatibility architecture research, not game reconstruction or an ownership/authentication bypass project.
