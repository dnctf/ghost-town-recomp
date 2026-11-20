# Moving Out 2 — Binary Analysis

## Scope and evidence boundary

The baseline reports the executable as Windows x64 Standalone and identifies Unity 2021.3.16f1 / IL2CPP from a prior PE/string inspection. The actual PE files are not available in the current repository tree or this continuation's mounted files. Therefore the details below distinguish baseline-reported facts from work that still requires the original files.

## Findings

- **VERIFIED (baseline record):** inventory contains `mo2.exe`, `GameAssembly.dll`, `UnityPlayer.dll`, and `baselib.dll`; SHA-256 values are in [FILE_INVENTORY.md](FILE_INVENTORY.md).
- **INFERRED (strong):** `GameAssembly.dll` is the IL2CPP-generated native game-code module; `UnityPlayer.dll` is the Unity native player runtime; `baselib.dll` is Unity's low-level platform/runtime layer. Presence alone does not prove load order or mandatory status.
- **INFERRED (baseline lead):** Unity 2021.3.16f1 and IL2CPP are likely build properties, but need a version string / PE / metadata revalidation.
- **NOT TESTED:** PE machine fields, optional-header type, import and delay-import tables, exports, TLS callbacks, exception/unwind metadata, subsystem flags, section alignment, CFG, load config, and actual dependency resolution.
- **NOT TESTED:** no Windows Process Monitor/API trace, loader trace, ETW, crash dump, or Android launch trace was captured in this continuation.

## Reproducible local inspection (read-only)

Run against a user-owned local game installation. Keep output text-only and sanitized; do not commit proprietary binaries.

```powershell
Get-FileHash .\mo2.exe -Algorithm SHA256
Get-FileHash .\GameAssembly.dll -Algorithm SHA256
Get-FileHash .\UnityPlayer.dll -Algorithm SHA256
dumpbin /headers .\mo2.exe
dumpbin /imports .\mo2.exe
dumpbin /exports .\GameAssembly.dll
dumpbin /imports .\GameAssembly.dll
```

Alternative GNU binutils:
```sh
sha256sum mo2.exe GameAssembly.dll UnityPlayer.dll
objdump -f mo2.exe
objdump -p mo2.exe
objdump -p GameAssembly.dll
```

Record normal and delay imports separately. Resolve imports against the exact package tree; do not assume a DLL is mandatory just because it is present.

## Dependency hypotheses to test

| Component | Baseline evidence | Interpretation | Confidence |
|---|---|---|---|
| UnityPlayer / baselib | inventory | engine/platform runtime | INFERRED |
| GameAssembly | inventory + IL2CPP lead | native IL2CPP game code | INFERRED |
| lib_burst_generated | inventory | Burst-generated optimized native routines | INFERRED |
| FMOD / Resonance Audio | inventory | audio and spatial audio middleware | INFERRED |
| NanoSockets | inventory | socket/network transport dependency | INFERRED |
| Steam API | inventory | platform integration; may be optional or gated | INFERRED |
| Crashpad / Backtrace | inventory | crash reporting/diagnostics | INFERRED |

## Next binary-analysis gate

Obtain fresh text-only PE reports and compare imports/exports against the current runtime bundle. Until then, no specific Windows API set, AVX/SSE minimum, DLL load order, or mandatory platform dependency is asserted.
