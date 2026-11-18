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


## Run #37: collector verified; repeated graphics filename assumption

- Run: [44226671035](https://github.com/dnctf/ghost-town-recomp/actions/runs/44226671035), commit `8263d9b50f24222b8a1fa4c378e14222eb056032`, completed **failure**.
- Box64 build, ARM64 rootfs bootstrap, Debian amd64 package download, Wine download/checksum, and payload extraction succeeded. The validation step failed again.
- Exact log evidence from job `134541397722` / `logs/04-validate-runtime-payloads.log`:
  ```
  + ...sh:2:main: test -x .../opt/wine/bin/wine
  + ...sh:3:main: sudo readelf -h .../opt/wine/bin/wine
  + ...sh:3:main: grep -q 'Machine:.*Advanced Micro Devices X86-64'
  + ...sh:4:main: test -s .../usr/lib/x86_64-linux-gnu/dri/virgl_dri.so
  [ERROR] stage=04-validate-runtime-payloads line=4 exit_code=1 command=test -s "$ROOTFS/usr/lib/x86_64-linux-gnu/dri/virgl_dri.so"
  ```
- This is the second consecutive occurrence of the same known root cause (Run #36 and #37), not a new package-extraction fault. Stop threshold remains three identical failures.
- Diagnostic artifact: [runtime-build-diagnostics](https://github.com/dnctf/ghost-town-recomp/actions/runs/44226671035/artifacts/14392094201), 400,395 bytes. The full artifact was inspected: no `Broken pipe` or `SIGPIPE` text; full find/list inventories, symlink inventory, key directories, tool versions and rootfs sizes were all written. Rootfs size at failure: 1,724,121,646 bytes (1.7G). `dpkg --audit` was empty. `/opt/wine/bin` includes executable `wine` and `wineboot -> wine`.
- Host diagnostic tools: dpkg/dpkg-deb 1.22.6 (arm64), GNU ar 2.42, GNU tar 1.35, xz 5.4.5, zstd 1.5.7; qemu-aarch64, qemu-aarch64-static and update-binfmts were not installed on the host runner.
- Collector verification: successful always-upload step; no SIGPIPE observed. Full inventory files are saved, with concise first-2500-line views retained for convenience.
- Corrective change is now to make Wine/ELF/dependency checks fatal and graphics-driver inventory advisory. The actual package filename is represented in the manifest as `virtio_gpu_dri.so`; absence of an expected driver is not allowed to block the Wine smoke ladder.
- The graphics transport risk and source references are recorded in `docs/graphics-plan.md`. See also [Mesa VirGL](https://docs.mesa3d.org/drivers/virgl.html), [Mesa vtest/virpipe driver commit](https://cgit.freedesktop.org/mesa/mesa/commit/?id=b3b82fe8ea1f7d02c93513920143cba2fe145b6c), and [virglrenderer vtest protocol](https://android.googlesource.com/platform/external/virglrenderer/+/68429e8e1106d0861d9f9f180583bd8381b8bf96/vtest/vtest_protocol.h).

## Smoke ladder implementation

- Added `runtime/scripts/ci-wine-smoke.sh`: sequential rungs a-e; exits at first failed rung; dynamic `wine`/`wine64` and `wineboot` detection; per-rung logs and selected entry-point listing.
- Runtime smoke logs upload with `if: always()`. The smoke is executed on a native ARM64 CI runner and does not imply Android-device validation.
- Standalone Wine experiment is deferred until rung c or d fails, or Debian Wine proves unstable. No alternate Wine build is added now.


## Smoke ladder Run #38

- Run: [44227356269](https://github.com/dnctf/ghost-town-recomp/actions/runs/44227356269), commit `6259ffee02969d8206edd7bad754d80a5c0574f2`, completed **failure**.
- Box64 ARM64 build succeeded and uploaded artifact `box64-arm64-glibc-sd888` (6,694,447 bytes). Rootfs bootstrap, package download/extraction, Wine fatal validation and advisory graphics inventory all passed.
- Rootfs failed at `Trim and package rootfs component`; assemble was skipped. **No rung a-e ran** and there are no rung exit codes, durations, or pass/fail log lines.
- Exact evidence from `logs/05-trim-package-rootfs.log`:
  ```
  du: cannot read directory '.../rootfs/var/cache/ldconfig': Permission denied
  du: cannot read directory '.../rootfs/var/cache/apt/archives/partial': Permission denied
  du: cannot read directory '.../rootfs/etc/ssl/private': Permission denied
  [ERROR] stage=05-trim-package-rootfs line=2 exit_code=1 command=du -sb "$ROOTFS" > rootfs-size-before-trim.txt
  ```
- Root cause: root-owned rootfs directories are unreadable by the runner account; unprivileged GNU `du` returned exit code 1. This is a permissions/measurement failure, not a Wine/Box64 execution failure.
- Minimal correction: pre-trim and post-trim byte measurements now use `sudo du -sb`; rootfs content and trim policy are unchanged.
- Downloaded and inspected [runtime-build-diagnostics artifact](https://github.com/dnctf/ghost-town-recomp/actions/runs/44227356269/artifacts/14392959332). It contains stage logs, full find/list inventories, symlink inventory, key directories, package audit, tool versions and rootfs size. No smoke artifact exists because assemble was skipped.
- Rootfs size from the always-run diagnostic collector: `1,724,121,646` bytes. No valid before/after trimming measurement was produced.
- Native runner and chroot mount details were not explicitly logged as smoke diagnostics in Run #38.

| Rung | Status | Command | Exit code | Duration | Evidence |
|---|---|---|---:|---:|---|
| a | NOT RUN | `box64 --version` | — | — | Assemble job skipped |
| b | NOT RUN | Box64 runs x86_64 `wineserver --version` | — | — | Assemble job skipped |
| c | NOT RUN | `box64 wine --version` | — | — | Assemble job skipped |
| d | NOT RUN | `wineboot` with temporary prefix and `WINEDLLOVERRIDES=mscoree,mshtml=` | — | — | Assemble job skipped |
| e | NOT RUN | `wine cmd /c ver` | — | — | Assemble job skipped |

- Standalone Wine remains deferred: rung c/d has not been reached and Debian Wine has not demonstrated instability.


## Smoke runner instrumentation (next CI)

- Both rootfs and assemble jobs assert a native ARM64 runner (`uname -m=aarch64`, `RUNNER_ARCH=ARM64`) and log `nproc`, `lscpu`, and a compact `/proc/cpuinfo` sample. The assemble runner record is copied into runtime provenance.
- Chroot smoke owns its mount lifecycle: bind-mount `/dev` and `/dev/pts`, mount `/proc` and `/sys`, log `findmnt -R` before tests and after cleanup, and unmount in an EXIT trap.
- Per-rung timeouts are a/b 30s, c 60s, d/e 180s. Each rung logs command, diagnostic environment, exit code, and elapsed seconds. The ladder stops at the first failed rung. Rung d runs `wineboot` with a fresh temporary prefix and `WINEDLLOVERRIDES=mscoree,mshtml=`.
- The assemble-stage rootfs size measurements also use sudo because extracted root-owned directories are not readable by the runner user.


## Run after Run #38 fix: chroot apt-get cleanup failure

- Run: [44228629627](https://github.com/dnctf/ghost-town-recomp/actions/runs/44228629627), commit `74472ecf8983b14005a77bdb5c9830be33ba2f58`, completed **failure**.
- Native runner assertion passed (the step `Verify native ARM64 runner and record CPU` completed successfully). Box64 build succeeded and uploaded its artifact. Rootfs bootstrap, package download/extraction, and Wine/ELF/dependency plus advisory graphics validation passed.
- The previous `du` permission failure is fixed: log now shows `sudo du -sb "$ROOTFS"` succeeded.
- New failure is at trim stage line 3:
  ```
  + ...:3:main: sudo chroot "$ROOTFS" /usr/bin/apt-get -o Dpkg::Use-Pty=0 clean
  chroot: failed to run command ‘/usr/bin/apt-get’: No such file or directory
  [ERROR] stage=05-trim-package-rootfs line=3 exit_code=127 command=sudo chroot "$ROOTFS" /usr/bin/apt-get -o Dpkg::Use-Pty=0 clean
  ```
- The diagnostic inventory confirms `$ROOTFS/usr/bin/apt-get` exists as a regular executable (mode 0755, 67,832 bytes). Thus the chroot message does **not** prove the file itself is absent; an ELF interpreter/runtime-loader resolution problem is a leading hypothesis, not yet confirmed.
- The next instrumentation records `readelf -l`'s requested interpreter and checks the expected ARM64 loader paths before this command. No Wine smoke rung ran; assemble was skipped. No standalone Wine experiment is justified yet.


## Run #39: ELF interpreter evidence and loader-path repair

- Run: [44229072945](https://github.com/dnctf/ghost-town-recomp/actions/runs/44229072945), commit `c3a826917749c18532d406283c4959380fb3e1f2`, completed **failure** at trim.
- Exact stage failure:
  ```
  [Requesting program interpreter: /lib/ld-linux-aarch64.so.1]
  ls: cannot access '.../rootfs/lib/ld-linux-aarch64.so.1': No such file or directory
  .../rootfs/usr/lib/ld-linux-aarch64.so.1 -> aarch64-linux-gnu/ld-linux-aarch64.so.1
  .../rootfs/usr/lib/aarch64-linux-gnu/ld-linux-aarch64.so.1
  chroot: failed to run command ‘/usr/bin/apt-get’: No such file or directory
  [ERROR] stage=05-trim-package-rootfs line=11 exit_code=127
  ```
- Artifact: [runtime-build-diagnostics](https://github.com/dnctf/ghost-town-recomp/actions/runs/44229072945/artifacts/14394212441), 405,460 bytes. The artifact's `rootfs-find-full.txt` confirms `/usr/bin/apt-get` is a regular executable, mode 0755, 67,832 bytes; the requested interpreter path is missing while the loader file exists at `/usr/lib/aarch64-linux-gnu/ld-linux-aarch64.so.1`.
- `rootfs-symlinks.txt` shows `/usr/lib/ld-linux-aarch64.so.1 -> aarch64-linux-gnu/ld-linux-aarch64.so.1`, but no `/lib -> usr/lib` merge link and no `/lib/ld-linux-aarch64.so.1`. It also shows the AMD64 `/lib64/ld-linux-x86-64.so.2` link. This proves the guest ARM64 interpreter path/layout is incomplete. It does **not** prove that AMD64 extraction caused the layout split because no pre-extraction symlink inventory was captured.
- Classification: **(a) missing interpreter path / incomplete usr-merge layout**. (b) AMD64 package overwrite is **not proven** by this post-extraction artifact. (c) the previous artifact did not contain `readelf -h apt-get`, so its ELF machine was not yet independently captured; next run adds the header and asserts AArch64. (d) no other root cause is supported by this evidence.
- The repair adds only the two compatibility paths required by the observed Bookworm layout: `/lib/ld-linux-aarch64.so.1` and `/lib/aarch64-linux-gnu`, both targeting the existing ARM64 files under `/usr/lib/aarch64-linux-gnu`. It records `readelf -h`, `readelf -l`, `ls -ld`, and pre-repair symlinks before applying the repair.
- Separate confirmed compatibility finding from the same run's Box64 artifact: `objdump -T box64` reports a maximum `GLIBC_2.39` requirement, while debootstrap retrieved Bookworm `libc6 2.36-9+deb12u14`. The Ubuntu 24.04 runner build therefore is not compatible with the Bookworm rootfs; the next change moves Box64 compilation into a Debian Bookworm ARM64 container and enforces the GLIBC ceiling.


## Run #39 follow-up: smoke-before-trim and glibc compatibility gate

- The Run #39 Box64 artifact was downloaded and inspected independently. `readelf -h` reports ELF64/AArch64. `objdump -T box64 | grep -o 'GLIBC_[0-9.]*' | sort -Vu | tail -3` returned `GLIBC_2.35`, `GLIBC_2.38`, `GLIBC_2.39`; maximum required symbol version is **2.39**.
- The runner is Ubuntu 24.04 ARM64 (host glibc 2.39), but the rootfs bootstrap log says `Retrieving libc6 2.36-9+deb12u14`. Therefore the previous host-built Box64 binary exceeds the rootfs glibc ceiling and is **not compatible**. It must not be copied into the Bookworm rootfs.
- Box64 is now built inside a native ARM64 `debian:bookworm` job container. The build records `ldd --version`, the top three GLIBC symbol versions, and fails if the binary requires a version newer than the build container. The rootfs job independently compares the binary's maximum required GLIBC symbol against its installed `libc6:arm64` package before rung a.
- The rootfs job now depends on the Box64 build artifact, repairs the evidenced ARM64 `/lib` loader/library paths, installs Box64, and executes rungs a-e on the **untrimmed** rootfs. The smoke artifact upload is a separate `if: always()` step before trim, so a trim failure cannot erase the smoke result. Trim uses host-side `sudo rm -rf` only; no rootfs executable is run during cleanup. Before/after sizes remain measured with `sudo du -sb`.
- Run #39 itself did not run any smoke rung: the workflow stopped at trim before assembly. All rung results remain **NOT RUN** until a later run provides per-rung logs.


## Run #40: Bookworm container shell selection

- Run: [44234796366](https://github.com/dnctf/ghost-town-recomp/actions/runs/44234796366), commit `583fbb33d408dc379a16baaefa69568cc37ea986`, failed in the first Box64 container setup step before package installation or compilation.
- Exact log:
  ```
  shell: sh -e {0}
  /__w/_temp/748e20fe-b3da-486f-8106-6962e59c80ce.sh: 1: set: Illegal option -o pipefail
  [error] Process completed with exit code 2.
  ```
- Root cause: GitHub Actions selected POSIX `sh` inside the Debian job container, while the step uses Bash's `pipefail` option. This is a shell-selection/configuration failure, not a package or compiler failure.
- Fix: set job-level `defaults.run.shell: bash` for the Bookworm Box64 container job. No package, compiler, Box64 source, or runtime behavior changed.
- Box64 compilation did not run; the rootfs dependency job was not reached. Smoke ladder a-e remains **NOT RUN**.


## Run #45: Bookworm Box64 build dependency

- Run: [44234907745](https://github.com/dnctf/ghost-town-recomp/actions/runs/44234907745), commit `7cc0e70f79263a371174042a9dcf64c472679ad6`, failed during CMake configuration in the Bookworm container.
- Exact log:
  ```
  -- Could NOT find Python3 (missing: Python3_EXECUTABLE Interpreter) (Required is at least version "3.7")
  CMake Error at CMakeLists.txt:150 (message):
    You need a Python 3.7 or newer interpreter, CMake will exit.
  [error] Process completed with exit code 1.
  ```
- Root cause: the minimal Debian Bookworm container did not include the Python 3 interpreter required by the pinned Box64 CMake configuration. The shell selection and apt package installation both succeeded; compilation did not start.
- Fix: add `python3` to the Bookworm build-container tool installation. No Box64 source or runtime files changed.


## Run #46: smoke runner berhenti saat inventarisasi mount

- Run: [44235028738](https://github.com/dnctf/ghost-town-recomp/actions/runs/44235028738), commit `ba6ebb7f5d7ce24fa1cd8a32839925e0fd6c93a3`, completed **failure**.
- Job Box64 berhasil. Rootfs job lulus bootstrap, download paket amd64, ekstraksi payload, validasi Wine, verifikasi loader, dan pemeriksaan glibc. Step `Run Wine smoke ladder on untrimmed rootfs` gagal; trim dan assemble dilewati.
- Artifact:
  - [box64-arm64-glibc-sd888](https://github.com/dnctf/ghost-town-recomp/actions/runs/44235028738/artifacts/14397371600), 6,623,356 bytes, artifact ZIP SHA-256 `e2ed91e2b25c9ae202d86f06044f3a5f4e7650d15df325d4c69e9ea177efb046`.
  - [runtime-build-diagnostics](https://github.com/dnctf/ghost-town-recomp/actions/runs/44235028738/artifacts/14397167386), 425,126 bytes, artifact ZIP SHA-256 `493d2eac5c031e72c6c77d08e3bf4453f3f19ea8b043b62fdb3fe40c21b6f7ea`.
  - [runtime-wine-smoke-diagnostics](https://github.com/dnctf/ghost-town-recomp/actions/runs/44235028738/artifacts/14396738987), 189 bytes; no rung log was produced.
- Box64 compatibility log:
  ```
  rootfs_libc6_package=2.36-9+deb12u14
  rootfs_glibc=2.36
  box64_max_required_glibc=2.35
  ```
  The GLIBC ceiling check passed.
- Exact smoke log excerpt:
  ```
  ### active chroot mounts
  [ERROR] stage=06-smoke-wine-untrimmed-rootfs line=3 exit_code=1 command=bash runtime/scripts/ci-wine-smoke.sh "$ROOTFS" runtime-smoke
  ```
  The smoke script mounts `/dev`, `/dev/pts`, `/proc`, and `/sys`, then executes `findmnt -R "$ROOTFS"` in a pipeline under `set -euo pipefail`. The log stops immediately after the heading, before any mount inventory or rung output. This identifies the mount-inventory command/pipeline as the failing point; it does **not** show a Wine or Box64 rung failure.
- The artifact's `apt-get-loader-paths.txt` reports `/lib` as a real directory, not a symlink to `usr/lib`. Thus the required usr-merged layout was still absent after amd64 extraction. The run did not record a pre-extraction `/lib` assertion, so it cannot establish whether amd64 extraction changed the layout.
- Rung table for this run:

| Rung | Status | Duration | Log evidence |
|---|---|---:|---|
| a — `box64 --version` | **NOT RUN** | — | Smoke runner stopped before `run_rung a-box64-version` |
| b — x86_64 `wineserver --version` | **NOT RUN** | — | No `b-x86_64-utility.log` |
| c — `wine --version` | **NOT RUN** | — | No `c-wine-version.log` |
| d — `wineboot` | **NOT RUN** | — | No `d-wineboot.log` |
| e — `wine cmd /c ver` | **NOT RUN** | — | No `e-wine-cmd-ver.log` |

- Root cause category: smoke setup/diagnostic failure before rung a. It is distinct from the earlier trim and loader failures. No standalone Wine experiment is justified because rung c/d has not run.
- Corrective commit: [49c4833](https://github.com/dnctf/ghost-town-recomp/commit/49c48338298d170d7247e889128da8d3a0b2455b) adds explicit `usrmerge` installation and pre-/post-amd64-extraction assertions that `/lib -> usr/lib`; removes the ad-hoc loader symlink repair; asserts all four chroot mountpoints before treating the mount inventory as diagnostic; and adds both `/lib/x86_64-linux-gnu` and `/usr/lib/x86_64-linux-gnu` to `BOX64_LD_LIBRARY_PATH`.
- Validation of that correction is pending in [runtime run 44242702116](https://github.com/dnctf/ghost-town-recomp/actions/runs/44242702116). Do not count it as a fix until the workflow completes and logs prove the assertions and smoke ladder.


## Run #47: ekstraksi paket mengubah usr-merged /lib

- Run: [44242702116](https://github.com/dnctf/ghost-town-recomp/actions/runs/44242702116), commit `49c48338298d170d7247e889128da8d3a0b2455b`, completed **failure**.
- Box64 build job succeeded. Rootfs bootstrap, native ARM64 runner check, download Wine, dan download paket berhasil.
- Pemeriksaan sebelum ekstraksi berhasil:
  ```
  usrmerge_pre_amd64_extraction=passed
  lib_target=usr/lib
  lrwxrwxrwx ... rootfs/lib -> usr/lib
  ```
- Pemeriksaan sesudah ekstraksi gagal pada baris:
  ```
  sudo test -L "$ROOTFS/lib"
  [ERROR] stage=03-extract-runtime-payloads line=18 exit_code=1 command=sudo test -L "$ROOTFS/lib"
  ```
- Log trace menunjukkan semua paket `amd64` dan `all` diekstrak langsung ke rootfs dengan `sudo dpkg-deb -x "$deb" "$ROOTFS"`. Karena validasi sebelum ekstraksi lulus dan sesudah ekstraksi gagal, kerusakan terjadi di jalur ekstraksi langsung. Run ini tidak menandai paket tertentu karena pemeriksaan dilakukan setelah loop selesai.
- Tidak ada Wine rung yang dijalankan; smoke, trim, dan assemble dilewati. Rung a-e: **semua NOT RUN**, tanpa durasi/log rung.
- Koreksi berikutnya pada [commit b3e012d](https://github.com/dnctf/ghost-town-recomp/commit/b3e012daae07e8ffd1a811a5d78b935c7cf4b1f6): setiap paket diekstrak ke direktori staging terpisah; jika staging berisi direktori `/lib`, kontennya dipetakan ke `/usr/lib` sebelum sisa payload disalin ke rootfs. Symlink rootfs `/lib -> usr/lib` diperiksa setelah setiap paket. Nama paket/arsitektur/versi dan remapping dicatat untuk diagnosis.
- [Run #48](https://github.com/dnctf/ghost-town-recomp/actions/runs/44243158612) sedang memvalidasi strategi staging tersebut. Sampai workflow selesai, hasilnya belum dapat dianggap lolos.

| Rung | Status | Durasi | Bukti |
|---|---|---:|---|
| a — `box64 --version` | **NOT RUN** | — | Ekstraksi berhenti sebelum tahap smoke |
| b — x86_64 `wineserver --version` | **NOT RUN** | — | Tidak ada log rung b |
| c — `wine --version` | **NOT RUN** | — | Tidak ada log rung c |
| d — `wineboot` | **NOT RUN** | — | Tidak ada log rung d |
| e — `wine cmd /c ver` | **NOT RUN** | — | Tidak ada log rung e |


## Run #48: rung d wineboot timeout dengan bukti SIGSEGV dan EGL

- Run: [44243158612](https://github.com/dnctf/ghost-town-recomp/actions/runs/44243158612), commit `b3e012daae07e8ffd1a811a5d78b935c7cf4b1f6`, completed **failure**. Runtime rootfs job gagal pada rung d; assemble/trim dilewati.
- Artifact smoke [runtime-wine-smoke-diagnostics](https://github.com/dnctf/ghost-town-recomp/actions/runs/44243158612/artifacts/14401010508), 6,596 bytes, SHA-256 `f049af373f7fbc90428a03848b6a28543438a734a827e016d05945a04a9421e6`.
- Artifact build [runtime-build-diagnostics](https://github.com/dnctf/ghost-town-recomp/actions/runs/44243158612/artifacts/14400543231), 471,387 bytes, SHA-256 `b62e4f1636f9ea65c670d212d5e64fa28903ad857b18d85e98bf793e9b3ea5c2`.
- Versi artifact: Box64 `arm64 v0.4.5` (dibangun 18 Nov 2026); Wine `11.19`. Rung c output persis `wine-11.19`; rung b output `Wine 11.19`.
- File smoke artifact: `00-entrypoint.log`, `a-box64-version.log`, `b-x86_64-utility.log`, `c-wine-version.log`, `d-wineboot.log`, `mounts.log`, `PASSED.txt`, `STOPPED_AT.txt`.
- `d-wineboot.log` ada: 31.257 bytes / 434 lines. `BOX64_LOG=1` output `[BOX64]` tercampur dalam file ini; tidak ada file BOX64 terpisah. Ada jejak wineserver (`posix_spawn(...wineserver...)`, `Rename process to "wineserver"`), tetapi tidak ada stdout/stderr wineserver terpisah.
- Tidak tersimpan: snapshot proses berkala, `/proc/<pid>/status`, `wchan`, daftar isi `/tmp` saat proses hidup, log BOX64 terpisah, log wineserver terpisah, dan log rung e. Tidak ada direktori `d-snapshots/`.
- Environment rung d lama hanya mencatat `BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all` dan `WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix`. `HOME`, `USER`, `WINEARCH`, dan `TMPDIR` tidak diset eksplisit; tidak ada assertion prefix/home writable sebelum proses.
- Kutipan log penting (nomor baris di `d-wineboot.log`):
  ```
  [line 1] [RUN] rung=d-wineboot timeout_seconds=180
  [line 56] [BOX64] posix_spawn(... "/opt/wine/lib/wine/../../bin/wineserver" ...)
  [line 71] [BOX64] Rename process to "wineserver"
  [line 420] err:wgl:egldrv_init_pixel_formats Failed to get any configs from eglChooseConfig
  [line 421] err:wgl:internal_context_create Failed to create internal root context ...
  [line 422] [BOX64] 19242|SIGSEGV ... win32u.so/parse_extensions ... for accessing (nil)
  [line 430] [BOX64] Signal 11: si_addr=(nil) ...
  [line 432] exit_code=124
  [line 433] duration_seconds=180
  ```
- Klasifikasi bukti: **(d) SIGSEGV terlihat eksplisit** dan **(e) kegagalan inisialisasi EGL/display terlihat**. Kausalitas EGL → SIGSEGV belum terbukti. **(a) socket/izin wineserver** dan **(b) anak Wine macet** belum dapat diputuskan karena tidak ada snapshot/status/wchan. **(c) hanya lambat** tidak didukung karena SIGSEGV tercatat sebelum timeout; belum ada bukti progres hingga 180 detik.
- Error tambahan: simbol `strunvis`, `vis`, `strvis` tidak ditemukan saat memuat `libedit.so.2`; swrast gagal dibuka dari `/usr/lib/dri/swrast_dri.so`. Ini petunjuk dependency/driver, bukan akar masalah final.
- Rung a PASS 0s: `Box64 arm64 v0.4.5 with Dynarec`. Rung b PASS 0s: `Wine 11.19`. Rung c PASS 0s: `wine-11.19`. Rung d FAIL/timeout 180s dengan SIGSEGV/EGL. Rung e NOT RUN karena harness berhenti pada d.
- Harness baru menambahkan assertion output rung c `^wine-[0-9]`; rung d berjalan terpisah dengan log bertimestamp, HOME/USER/TMPDIR/WINEARCH/WINEPREFIX eksplisit, prefix writable dalam rootfs, snapshot t=15/45/90/180/600, status/wchan, dan cleanup wineserver sebelum kill process group.
- Belum ada variasi `BOX64_DYNAREC=0`, `BOX64_LOG=0`, `WINEDEBUG=-all`, atau Xvfb. Satu run diagnostik 600 detik harus mengumpulkan bukti snapshot terlebih dahulu sebelum memilih satu variasi.


## Run #49: extended wineboot diagnostic 600 seconds — pending dispatch

- Status: **belum dijalankan**. Konfigurasi workflow manual `workflow_dispatch` sekarang memilih timeout `600` sebagai default; push-triggered runtime run tetap memakai fallback `180`.
- Tidak ada hasil atau klasifikasi penyebab baru. Run #48 tetap menjadi bukti terakhir: rung d timeout 180 detik, SIGSEGV dan EGL init error terlihat, tetapi hubungan sebab-akibat belum terbukti.
- Harness saat ini mencatat stdout/stderr dengan timestamp per baris, `WINEDEBUG=err+all,+timestamp,+process`, `HOME`, `USER`, `WINEARCH=win64`, `WINEPREFIX`, dan `TMPDIR` eksplisit. Rung c memeriksa stdout dengan regex `^wine-[0-9]`.
- Snapshot dijadwalkan pada t=15/45/90/180/300/600 detik. Setiap snapshot berisi `ps --forest`, isi `/tmp` dan entri `.wine-*`, serta `/proc/<pid>/status` dan `wchan` untuk proses diagnostik.
- Jika timeout, harness menjalankan `wineserver -k` melalui Box64, mencatat exit code cleanup, lalu menghentikan process group yang tersisa. Artifact `runtime-wine-smoke-diagnostics` diunggah dengan `if: always()`.
- Setelah satu run dispatch 600 detik, klasifikasi harus dipilih hanya jika log mendukungnya:
  - **wineserver tidak hidup**: tidak ada proses wineserver pada snapshot, tidak ada startup sukses/socket evidence; bila hanya tidak terlihat, tulis belum diketahui.
  - **anak proses macet**: wineserver hidup, proses anak tetap ada, status/wchan menunjukkan menunggu tanpa progres.
  - **hanya lambat**: proses menunjukkan progres berkelanjutan dan akhirnya keluar normal; durasi panjang saja tidak cukup.
  - **hang dynarec**: stack/trace menunjukkan loop atau fault di jalur dynarec Box64; SIGSEGV tunggal tidak cukup.
  - **driver display**: error EGL/GL/Vulkan berkorelasi dengan kegagalan inisialisasi/presentasi; error EGL yang muncul bersamaan belum membuktikan kausalitas.
  - **lain/belum diketahui**: bukti tidak memenuhi kriteria di atas.
- Setelah hasil run ini dianalisis, hentikan eksperimen. Jangan jalankan variasi dynarec/Xvfb/driver paralel atau spekulatif sebelum laporan hasil satu run selesai.
