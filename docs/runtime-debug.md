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

## Baseline: commits after 5e2a8d5

The remote `main` comparison is `5e2a8d5a9f9174536db69a2810c7afa98a1320d7..cb462b7a5ecc495b21ab38294581acba11d8a61c`: **4 commits ahead, 0 behind**. No rebase or merge was needed; the remote branch already contains the requested commits and no newer divergent history was observed.

Commits in order:
- `efa9e2f4b6ad072eb10ca18f2275cee35e1d0ac3` — ignore package-list comments in runtime manifest test.
- `283f24d778ae318e492c546d8b83f3aef00c5c61` — add runtime stage tracing and debug notes.
- `4c9650f0ed9e7a8cfb4db205950911aefa608fb3` — preserve package command behavior while instrumenting.
- `cb462b7a5ecc495b21ab38294581acba11d8a61c` — add staged diagnostics and always-uploaded artifact.

Changed files across that range:
- `.github/workflows/build-box64-android.yml`: 2 additions / 8 deletions (workflow adjustment; not a runtime package/manifest change).
- `.github/workflows/build-wine-runtime-bundle.yml`: stage split, diagnostics, trace/trap, artifact collection.
- `runtime/scripts/ci-diagnostics.sh`: new shared strict-mode/xtrace/ERR-trap helper.
- `docs/runtime-debug.md`: new baseline/debug record.
- `tools/tests/test_runtime_manifest.py`: one-line test correction for package-list comments.
- No `runtime/manifest.json` change and no runtime package-list change in this range. The manifest still declares pack `0.5.0-alpha.2` as pending with empty URL, zero size, and empty SHA-256.
- `runtime/rootfs/packages-amd64.txt` unchanged.

## Run #36: instrumented baseline evidence

- Run: [44226255594](https://github.com/dnctf/ghost-town-recomp/actions/runs/44226255594), commit `cb462b7a5ecc495b21ab38294581acba11d8a61c`, completed **failure**.
- Box64 build job succeeded. Rootfs job failed in step 9, `Validate Wine and graphics payloads`. Bootstrap, Debian package download, and package/Wine extraction all succeeded.
- Diagnostic artifact: [runtime-build-diagnostics](https://github.com/dnctf/ghost-town-recomp/actions/runs/44226255594/artifacts/14392997611), SHA-256 `dc1ba8a4b3dd1db35018ac2d1fb9992e559c53bf9e2ac6d8381f2570a9d59631`, 40,797 bytes.
- Exact trap evidence from `logs/04-validate-runtime-payloads.log`:
  ```
  + ...sh:2:main: test -x .../opt/wine/bin/wine
  + ...sh:3:main: sudo readelf -h .../opt/wine/bin/wine
  + ...sh:3:main: grep -q 'Machine:.*Advanced Micro Devices X86-64'
  + ...sh:4:main: test -s .../usr/lib/x86_64-linux-gnu/dri/virgl_dri.so
  [ERROR] stage=04-validate-runtime-payloads line=4 exit_code=1 command=test -s "$ROOTFS/usr/lib/x86_64-linux-gnu/dri/virgl_dri.so"
  ```
- The Wine executable existence and x86-64 ELF checks passed. The failed assertion is the expected Mesa filename, not a Wine entry-point failure.
- The extraction trace proves the package was present and extracted: `dpkg-deb -f .../libgl1-mesa-dri_22.3.6-1+deb12u2_amd64.deb Architecture` returned `amd64`, followed by successful `sudo dpkg-deb -x ...libgl1-mesa-dri...`.
- Debian's Bookworm amd64 file list for that exact package version lists `/usr/lib/x86_64-linux-gnu/dri/virtio_gpu_dri.so`, not `virgl_dri.so` ([Debian package file list](https://packages.debian.org/bookworm/amd64/libgl1-mesa-dri/filelist)). This is consistent with the assertion failure: the workflow validates a filename that the pinned Bookworm package does not ship.
- Classification: **(e) other — incorrect validation filename/assumption**, not strict-mode false failure (a), dpkg extraction failure (b), AMD64 execution (c), or Wine path validation (d).
- Last 50 lines are preserved in the stage log via ERR trap. The rootfs inventory artifact was uploaded, but the original `find | head` and `ls -lR | head` collectors emitted SIGPIPE/Broken pipe noise and truncated before useful `/usr/lib` detail. Rootfs byte size and tool versions were not captured in this run; diagnostics are being tightened before any functional change.
- Rootfs snapshot confirms `/opt/wine/bin/wine` exists and is executable. The failed condition establishes that the requested `virgl_dri.so` path is absent. `dpkg --audit` output is empty; runner disk had 104 GiB available and memory showed 14 GiB available.
- Tool versions for dpkg, ar, tar, xz, zstd and binfmt/QEMU were not captured by run #36; the next diagnostic artifact will record them.

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

## Diagnostic follow-up

The next diagnostics collection writes complete inventories to files before truncating display copies (no producer-to-`head` SIGPIPE), captures symlinks, key directories, rootfs byte size, and host tool versions including dpkg, dpkg-deb, ar, tar, xz, zstd, QEMU and binfmt_misc. This is instrumentation-only; package selection and validation behavior remain unchanged until the current evidence is recorded and reviewed.
