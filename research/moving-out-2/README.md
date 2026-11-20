# Moving Out 2 Compatibility Research Baseline

> Research notes for Ghost Town Recomp. This repository contains documentation and non-proprietary technical metadata only. It does not contain the game, binaries, assets, or redistribution material.

## Purpose
Study Windows Unity/IL2CPP game compatibility requirements and use evidence-backed findings to guide Ghost Town Recomp's Android runtime architecture. This is compatibility research—not game reconstruction, asset extraction, or license/authentication bypass.

## Sample identification
- Executable: `mo2.exe`
- Target: Windows x64 / StandaloneWindows64
- Engine: Unity 2021.3.16f1 (reported from prior binary string inspection; revalidate where possible)
- Scripting backend: IL2CPP (reported from binary inventory)
- Build version: 1.2.274
- Release channel: Steam
- Branch: Production
- Build metadata timestamp: 03/08/2023 06:15:41 (format/time zone not verified)
- Commit field: empty

## Supplied file inventory
The sample files were available in the analysis session. SHA-256 values below were calculated from those files in that session. Human-readable file sizes are approximate (reported by `ls -lh`), not byte-exact. Hashes identify these files but do not prove provenance, authenticity, or official distribution status.

| File | Approx. size | SHA-256 |
|---|---:|---|
| GameAssembly.dll | 76 MiB | 2b97f2108b7cfe300a797ec1d6f12ba7695697871e2489020ffc1d583f4d0b23 |
| mo2.exe | 639 KiB | 032a97e36ddead78ca45007013088818198f18c28f94747dd1431a78291339b0 |
| UnityPlayer.dll | 28 MiB | cd715e67296377198ace9433a0f357056cfb5241da427131f31db3bcb1edab61 |
| BuildInfo.json | 170 B | d1329d94e01f015cbd7451a44470014bd20dc2976fa92a887f2c04cd4b6314ad |
| baselib.dll | 396 KiB | 46efae8b155e3f4649758b3e23902cd61558079bed47e7caf36b8a4a3d605d33 |
| unity default resources | 4.7 MiB | b9fa3d6bc9e3bb89e2be542b00a644971aedeab200ab86bee4354cc4efba848b |
| unity_builtin_extra | 360 KiB | dd34bb48f7bc83dd48bf48a2244a15dd82332a70f3dbe2b46fd87695172292a3 |
| BacktraceCrashpadWindows.dll | 132 KiB | bc03f282062b1010a30d99a44ffbe97fe9f459298b9754e0a00d98f5b70c6cff |
| core_stub.dll | 324 KiB | 62872f6ec39831e9d270f3ad49f773f447237195e9318b761b4699a1504b20e8 |
| crashpad_handler.dll | 782 KiB | e1eb08309dd8fb388431304f351b5a41c8740d6542b6390807fdaf43d5edf941 |
| fmodstudio.dll | 3.1 MiB | f9ec72222802e8868856becb6000a175203cc60a8b822a7fd9377c757b3a6ba7 |
| lib_burst_generated.dll | 73 KiB | 3c123c19e53dd4026f0f1e566023c7b35d6ec7737b65f7dfd1d2543582462b6a |
| nanosockets.dll | 95 KiB | 037985bd36c9b6984e8680e9f508491a7056f010bdb7207e32f07aa149eaaf00 |
| resonanceaudio.dll | 798 KiB | e1667e5336ae067ab1d9aa04e7bcca9789ff5573ed4c7f6fa9305404d0485f99 |
| steam_api64.dll | 417 KiB | 51425709b07d703f2b675828520c8d38af2d014f40d175695bc299c32b37f685 |
| steam_api64.rne | 260 KiB | ca1fa9a7609ab10b7926400559cf073e5888423cc156af72c6027d72a89eea73 |
| steam_emu.ini | 2.7 KiB | 97c9cb581792fd475592d71c230416a75ebfc7029b6db061fa7b713746af4d48 |

## Preliminary subsystem map (static-analysis leads; runtime unverified)
- Unity engine/runtime: `UnityPlayer.dll`, `baselib.dll`
- IL2CPP game code: `GameAssembly.dll`
- Audio middleware/spatial audio: FMOD-related and Resonance Audio DLLs
- CPU optimized native code: Unity Burst generated library; architecture-specific paths need inspection
- Networking: NanoSockets library
- Crash reporting/diagnostics: Crashpad/Backtrace-related components
- Platform integration: Steam API DLLs and additional Steam-related files; sample package provenance is not independently established

These are dependency indicators, not proof that every component is loaded or required during gameplay. Do not assume an optional subsystem is required for startup.

## Evidence classification
- **Verified file facts:** file existence and SHA-256 in the supplied working session; JSON metadata values.
- **Static-analysis leads:** version/backend/dependency identification previously reported from PE headers/strings. Revalidate independently when tools/files are available.
- **Inferences:** likely subsystem roles and compatibility impacts.
- **Not tested:** runtime tracing, Windows execution, Android execution, gameplay, graphics backend selection, load order, online/offline dependency behavior.

## Research priorities
1. Find Unity IL2CPP metadata, especially `global-metadata.dat` (often under `*_Data/il2cpp_data/Metadata/`).
2. Inspect `boot.config`, plugin folder listing, and relevant config without bulk-uploading proprietary game assets.
3. Analyze PE headers/imports/exports/dependency graph and verify Unity/IL2CPP version.
4. Reconstruct startup and lifecycle only to the extent supported by static evidence; clearly label inference.
5. Research graphics/API requirements, shader behavior, CPU instruction requirements, audio, input, and platform dependencies.
6. Audit Ghost Town Recomp's current code and tests before recommending or making changes.
7. Produce compatibility gap analysis and staged implementation plan. Any code changes must be isolated on a feature/research branch, tested where possible, and offered as a PR; do not merge into main without owner approval.

## Do not commit
- Game executables or DLLs
- Game assets, textures, audio, scenes, bundles, proprietary resource archives
- Game content dumps or reconstructed proprietary code
- Credentials, Steam account information, tokens, or sensitive configs
- License/DRM bypass tools or instructions

## Deliverables
See `AGENT_CONTINUATION_PROMPT.md` for the next-agent instructions. Add research outputs here as markdown, with a clear evidence label and reproducible method.