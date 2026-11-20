# Moving Out 2 — CPU / x86-64 Compatibility

## Moving Out 2 evidence

- **VERIFIED (baseline record):** target is reported as Windows x64 / StandaloneWindows64.
- **VERIFIED (baseline inventory):** `GameAssembly.dll` and `lib_burst_generated.dll` exist in the earlier inventory.
- **INFERRED:** the Windows x64 game code and native dependencies require x86-64 translation on an ARM64 Android host; Box64 is the relevant user-mode translation layer in this project.
- **NOT TESTED:** PE machine fields, instruction histogram, minimum x86-64 ISA, SSE2/SSSE3/SSE4.x, AVX/AVX2, FMA, BMI, atomics, CPUID checks, TLS, unwind/SEH, and Burst dispatch paths.

## Burst considerations

Burst can produce architecture- and CPU-feature-specialized native code. The presence of a generated DLL does not establish which variants are selected or whether the game uses them at startup. Do not assume AVX is required; inspect code and dispatch paths before drawing that conclusion.

## Ghost Town source audit

- **VERIFIED:** runtime manifest pins an ARM64 Box64 binary and reports Wine smoke rungs A–E passed on Debian Bookworm CI.
- **VERIFIED:** the same manifest says device dynarec validation is pending.
- **NOT TESTED:** Box64 execution on Huawei MatePad Air / Snapdragon 888, performance under sustained load, CPUID masking, SSE/AVX translation, and this game's CPU path.

## Test plan

1. Inspect PE architecture and disassemble representative non-proprietary instruction summaries locally.
2. Run Box64 diagnostic/test programs in the same rootfs and capture Box64 version, dynarec mode, CPUID flags, and logs.
3. Benchmark a small fixed x86-64 workload before the full game; record thermals and sustained clocks.
4. Only then attempt the game's own startup; preserve logs and distinguish translation faults from missing Windows APIs or graphics failures.

No CPU compatibility guarantee is made from architecture labels alone.
