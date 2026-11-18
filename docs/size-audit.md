# Size audit

## Baseline (before optimization)

Source: GitHub Release asset metadata for v0.4.0-alpha.1 and Actions artifact metadata for run [44048579420](https://github.com/dnctf/ghost-town-recomp/actions/runs/44048579420). These are byte-exact server-reported sizes, not estimates.

| Item | Bytes | MiB | Evidence / notes |
|---|---:|---:|---|
| Release app-release.apk | 378,569,945 | 361.03 | GitHub Release asset metadata |
| Release app-debug.apk | 420,050,499 | 400.59 | GitHub Release asset metadata |
| Runtime archive ghost-town-wine-runtime-arm64.tar.gz | 369,938,286 | 352.82 | GitHub Release asset metadata |
| Combined Actions artifact ZIP | 743,589,988 | 709.02 | Artifact ghost-town-recomp-0.5.0-alpha.1-arm64; contains both APKs and native outputs |

**Clarification:** the reported 709 MiB is the combined Actions artifact ZIP, not the release APK. The actual release APK is 361.03 MiB. The APK workflow downloads the 352.82 MiB runtime archive, splits it into 64 MiB assets, and embeds those assets into both APK variants. This is the dominant known source of APK bloat.

## Baseline CI duration

| Workflow run | Started (UTC) | Updated/completed (UTC) | Wall time | Result |
|---|---|---|---:|---|
| Android APK run [44048579420](https://github.com/dnctf/ghost-town-recomp/actions/runs/44048579420) | 2026-11-17 08:39:16 | 2026-11-17 08:43:41 | 4m 25s | success |
| Runtime build run [44048579505](https://github.com/dnctf/ghost-town-recomp/actions/runs/44048579505) | 2026-11-17 08:39:16 | 2026-11-17 08:46:05 | 6m 49s | failure |

Runtime run failed during Debian AMD64 package configuration with Exec format error from glib-compile-schemas and gio-querymodules. This is a failed-run baseline, not a successful runtime-build duration.

## Internal entry-level breakdown

The current GitHub connector exposes release/Actions metadata but does not provide the 378 MB APK bytes to the analysis runtime; the combined 743.6 MB artifact exceeds the connector's 512 MiB artifact-download limit. Therefore no entry-level sizes are fabricated here.

The repository now includes reproducible CI audit scripts:
- tools/audit_apk_size.py: top 50 ZIP entries, compressed/uncompressed sizes, top-level directory shares, lib/, assets/, res/, and DEX totals.
- tools/audit_runtime_tree.py: top 50 runtime files and logical sizes grouped by component (rootfs, PRoot, provenance, etc.).

The APK workflow emits the report as a CI artifact; the runtime assembly workflow emits the runtime-tree report. Populate this section with those measured reports before making compression/stripping/removal decisions.

## Optimization ledger

No APK/runtime stripping, compression-level changes, or package removals have been applied in this baseline audit commit. This preserves a clean before-measurement. Post-change measurements will be appended here only when backed by successful CI reports.
