# Agent Continuation Prompt — Moving Out 2 Research for Ghost Town Recomp

## Role
Work as a Principal Android/Windows Compatibility Runtime Engineer, Unity IL2CPP Binary Analyst, CPU Translation Engineer, Graphics Runtime Engineer, and Android Systems Architect.

Continue the Moving Out 2 research using the baseline in this folder. Do not depend on the previous chat being available. Avoid duplicating already documented inventory work unless verification is needed.

## Repository
- `dnctf/ghost-town-recomp`
- Default branch: `main`
- Research branch: `research/moving-out-2-baseline`

Research baseline:
- `research/moving-out-2/README.md`

## Goal
Use the Moving Out 2 Windows x64 Unity IL2CPP build as a compatibility case study to inform Ghost Town Recomp's Android runtime architecture. Research execution, platform, CPU, graphics, audio, input, networking, native dependencies, lifecycle, diagnostics, and realistic compatibility gaps.

This is compatibility research only. Do not reproduce proprietary game code/assets, modify game content, or work on DRM, license, authentication, or ownership bypasses.

## Baseline facts and prior leads
Review `README.md` for sample/build details, file inventory, SHA-256 identifiers, dependency leads, confidence classification, and limitations.

The sample inventory includes `mo2.exe`, `UnityPlayer.dll`, `GameAssembly.dll`, `baselib.dll`, Unity resource files, FMOD/Resonance-related files, Burst generated native code, NanoSockets, Crashpad/Backtrace components, and Steam-related files.

Prior reports identified Unity 2021.3.16f1 and IL2CPP, but treat these as **leads to revalidate**, not unquestionable ground truth. Runtime behavior has not been proven.

## Immediate actions
1. Inspect the repository branch and research baseline; check current GitHub state before writing.
2. Confirm tools, file availability, analysis/build limitations.
3. Validate the baseline facts using actual files/tools where possible; correct any inaccurate documentation.
4. Look for the next useful metadata artifact, especially `global-metadata.dat`, and determine if it exists in the user's available files. Do not claim access to files that are not present.
5. Analyze PE headers/imports/exports/dependency relationships and relevant strings. Explain the method and limitations.
6. Identify game bootstrap/startup and lifecycle indicators; clearly separate evidence from inference.
7. Investigate Unity/IL2CPP, CPU instruction requirements and Burst, graphics APIs/backend requirements, audio middleware, input, networking, platform/Steam dependency indicators, crash reporting, memory, and Android lifecycle implications.
8. Audit Ghost Town Recomp's actual codebase and CI. Verify current implementations and test coverage rather than relying on old chat summaries.
9. Build an evidence-backed compatibility gap matrix and staged implementation roadmap.
10. Continue research and implement only small, justified, isolated improvements on the research branch when repository access and tests allow. Do not merge into `main` without explicit approval.

## Evidence standard
Label each finding:
- **VERIFIED** — directly supported by file/tool/source/test output.
- **INFERRED** — plausible conclusion supported by evidence but not runtime-confirmed.
- **HYPOTHESIS** — needs an experiment.
- **NOT TESTED** — no test was performed.

For each key finding, include: source/file, evidence or method, interpretation, confidence, impact on Android runtime, and suggested follow-up.

Strings alone do not prove that a code path executes. Static dependency presence does not prove a library is mandatory. Do not invent runtime behavior, test results, CI status, or Android device results.

## Suggested research documents
Maintain concise, cumulative Markdown documents under `research/moving-out-2/`:
- `FILE_INVENTORY.md`
- `BINARY_ANALYSIS.md`
- `CLASS_MAP.md` (only if metadata can be analyzed; otherwise explain blocker)
- `RUNTIME_FLOW.md`
- `GRAPHICS_ANALYSIS.md`
- `CPU_COMPATIBILITY.md`
- `AUDIO_ANALYSIS.md`
- `NETWORK_PLATFORM_ANALYSIS.md`
- `INPUT_ANDROID_ANALYSIS.md`
- `MOVING_OUT_2_VS_GHOST_TOWN.md`
- `IMPLEMENTATION_PLAN.md`

Update README with links and progress status. Do not create empty placeholder documents merely to fill the list; create documents when they contain meaningful findings.

## Research priorities
### IL2CPP
- Determine whether metadata is available; search for `global-metadata.dat`.
- If metadata exists and tools are available, map classes/methods/assemblies/subsystems without reproducing proprietary code.
- If missing, record what is blocked and continue with other static analysis.

### Startup/runtime
- Reconstruct process entry → Unity initialization → IL2CPP → game bootstrap → subsystem setup → scene/game state only where supported.
- Label sequence stages as verified/inferred.

### CPU/ARM64
- Examine x64 machine type, imports, instruction-set indicators, SSE/AVX usage, threading, memory and exception requirements.
- Assess relevance of Box64/Wine on Android ARM64 without promising compatibility.

### Graphics
- Identify graphics APIs and evidence of use/support.
- Evaluate Vulkan, DXVK, Turnip, VirGL, Vortek, Zink, surfaces, shaders and synchronization as relevant to Ghost Town.
- Recommend a backend only when evidence and current Ghost Town architecture support the recommendation.

### Audio/input/network/platform
- Classify dependencies and potential startup/gameplay/optional requirements.
- Evaluate FMOD/spatial audio, controller/input, sockets, sessions, Steam/platform services, offline and online behavior.
- Do not attempt authentication or DRM bypass.

### Ghost Town audit
- Review actual files, modules, branches, CI, tests and existing architecture.
- Categorize features as implemented, partial, stub/demo, missing, broken, or untested.
- Preserve existing Wine/Box64/PRoot/IPC/input/legacy display/VirGL/Vulkan/Turnip/DXVK components unless a change is evidence-backed and safely isolated.
- Keep game session lifecycle, process control, graphics, input, audio, diagnostics and compatibility profiles modular.

## Repository and execution guardrails
- Work on `research/moving-out-2-baseline` or a separate feature branch.
- No destructive cleanup or sweeping rewrites.
- Do not commit Moving Out 2 executables, DLLs, game assets, bundles, proprietary resource files, dumps, credentials, or sensitive configuration.
- Documentation and non-proprietary metadata only.
- Add tests for changes where practical.
- Clearly report tests that could not be run locally or in CI.
- Push changes and open a PR when GitHub access is available; do not merge without approval.
- Do not claim a build, game launch, device validation, or CI succeeded unless you verified the result.

## Progress reporting
At each milestone, report:
1. Completed work.
2. Verified findings.
3. Inferences/hypotheses.
4. Remaining unknowns.
5. Files needed next (only necessary items).
6. Ghost Town compatibility gaps.
7. Changes/commit/branch/PR URLs if created.
8. Tests and CI status.
9. Next actionable task.

## Start now
Begin by reviewing `research/moving-out-2/README.md` and the repository's current state. Verify the baseline, then continue with analysis and useful implementation work. Do not ask the user to restate the context.