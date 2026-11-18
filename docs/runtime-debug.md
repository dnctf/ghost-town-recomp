# Runtime build debugging

## Baseline: run 44224241488

- Workflow: [Build Complete ARM64 Wine Runtime Bundle](https://github.com/dnctf/ghost-town-recomp/actions/runs/44224241488)
- Failed job: `Build ARM64 glibc rootfs and Windows runtime components`
- Failed step: `Bootstrap Debian ARM64 glibc rootfs and amd64 Wine libraries`
- Run checked out commit `5e2a8d5a9f9174536db69a2810c7afa98a1320d7`, the parent of current `main` commit `efa9e2f4b6ad072eb10ca18f2275cee35e1d0ac3`.
- The pinned Wine download and checksum step passed.
- Debian package download completed: `Fetched 80.3 MB in 1s (79.5 MB/s)` and `Download complete and in download only mode`.
- The next visible failure was `Process completed with exit code 1` at 2026-11-18T02:35:29.7423181Z.
- The diagnostic artifact reported an empty `dpkg --audit`, 104 GiB available disk, and 14 GiB available memory.
- Artifact: [runtime-build-failure-logs](https://github.com/dnctf/ghost-town-recomp/actions/runs/44224241488/artifacts/14392635840).

## Root-cause status

**Not established yet.** The original bootstrap was one long `run:` block using `set -euo pipefail` without xtrace or an ERR trap. The job log does not expose which command after package download returned non-zero. The artifact contains the apt download transcript and host resource checks, but no command trace, rootfs inventory, or failed-command metadata. It would be speculation to blame debootstrap, dpkg extraction, Wine layout, lddtree, or archive creation.

The diagnostic change therefore does not alter package selection, extraction, Wine layout, trimming, or archive semantics. It adds stage boundaries, command tracing, failure metadata, and always-uploaded rootfs/environment diagnostics. The next CI run must identify the exact failing stage and command before any functional repair is attempted.

## Instrumentation

Runtime workflow stages are now separated into bootstrap, Debian package download, package/Wine extraction, payload validation, rootfs trim/package, and final archive assembly. Each instrumented stage writes to `logs/`, enables `set -Eeuo pipefail` and `set -x`, and installs an ERR trap that reports source line, command, exit code, and the last 50 log lines. Diagnostics upload runs with `if: always()`.

## Guardrails retained

- Do not publish or activate runtime manifest `0.5.0-alpha.2` until a real pack has passed the required CI smoke gate.
- Keep `GHOST_TOWN_DISPLAY_READY=1` as the launch gate.
- Do not claim physical-device validation unless performed on the target device.
- Do not start APK/UI optimization while runtime smoke remains blocked.
