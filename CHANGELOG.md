## [0.4.0-alpha.1] - 2026-11-17 (runtime foundation pre-release)

### Added
- Managed HTTPS downloader in the Android app for the project-owned runtime release, including SHA-256 sidecar verification and transactional app-private installation.
- CI publication path for the v0.4.0-alpha.1 APKs and native ARM64 library, aligned with the runtime bundle release tag.
- Runtime bundle contains Android ARM64 PRoot, Debian Bookworm ARM64/amd64 userspace, Box64 ARM64, Wine 11.19 amd64, and DXVK 3.1.1.

### Not yet implemented / not verified
- Turnip and libadrenotools integration.
- A glibc-visible Vulkan loader/ICD and Android Surface presentation bridge for Wine/DXVK.
- Automated first-run Wine prefix creation in the Android app.
- In-app Wine/Box64 process launcher for Overcooked2.exe.
- Wine audio and gamepad/touch input bridge.
- Physical MatePad Air 2023 tests, main-menu screenshot, gameplay screenshot/FPS, and five-minute stability evidence.

This is a foundation pre-release only. It is not a playable Overcooked 2 build. Stable v0.4.0 remains blocked on the device gates in docs/V0.4.0_EXECUTION_GATES.md.

## [0.3.3] - 2026-11-16 (tester candidate)

### Fixed
- Changed the runtime workflow to upload the privileged Debian rootfs as a checksummed tar archive, avoiding upload-artifact directory traversal failures on restricted paths such as `/lib/ssl/private`.
- Verify the rootfs component archive checksum before extracting it for final runtime assembly.
- Exclude rootfs `/dev` device nodes from the portable archive; PRoot bind-mounts the Android `/dev` tree at runtime, so privileged `mknod` entries are unnecessary.
- Align Android and runtime assets with the `v0.3.3` tester release.

### Added
- Added a dashboard **BAGIKAN LOG DIAGNOSTIK** action that shares device/build/runtime information and recent app-process logcat as plain text.
- Updated physical-device tester instructions with in-app report export and an ADB fallback.

### Validation boundary
- APK and runtime CI success do not prove PRoot, Wine, DXVK/Vulkan, display, audio, input, or game execution on a physical MatePad. These remain separate device-test gates.

## [0.3.2] - 2026-11-16 (tester candidate)

## Unreleased — Runtime provisioning

### Added
- Added a full ARM64 glibc runtime bundle workflow assembling Android PRoot, Debian Bookworm ARM64 userspace, amd64 Wine dependencies, Box64, Wine 11.19, and DXVK 3.1.1.
- Added an app-private runtime bundle installer for gzip-compressed tar archives, with staging, required-file validation, traversal/symlink checks, and rollback to the previous runtime.
- Split runtime CI across an x86_64 NDK/PRoot build and a native ARM64 Box64/rootfs build; corrected the 32-bit ARM PRoot loader validation.
- Added dashboard action to import the runtime bundle through Android's document picker.
- Added explicit PRoot companion loader environment paths to the Wine probe.

### Validation boundary
- CI bundle creation, Android APK compilation, runtime archive import, PRoot execution on HarmonyOS, Wine startup, DXVK/Vulkan initialization, and actual game compatibility are separate gates.
- The graphics/display bridge, Wine prefix bootstrap, Android audio/input bridge, and physical-device gameplay tests are still pending.

## Unreleased — Runtime component groundwork

## Unreleased

### Added
- Added an Android launcher Wine/Box64 runtime diagnostics panel.
- Added a guarded, fixed-command headless Wine version probe through PRoot and Box64 when a complete runtime is provisioned.
- Added an Android ARM64/Bionic PRoot build workflow with companion loaders and provenance.
- Documented the PRoot build artifact and the updated app-private runtime layout.
- Documented the required ARM64 glibc rootfs layout and remaining runtime integration gates in `runtime/ANDROID_RUNTIME_LAYOUT.md`.

### Limitations
- The APK does not yet provision PRoot, a glibc rootfs, Wine, Box64, or DXVK automatically.
- PRoot's Android ELF build is not yet validated on the MatePad's HarmonyOS kernel/ptrace policy.
- The headless probe does not validate DXVK, Vulkan rendering, Windows game execution, audio, input, or gameplay.
- Game launch remains disabled pending rootfs provisioning, graphics/display integration, and physical-device validation.


- Corrected Box64 target from Android Bionic to Linux ARM64/glibc for use inside a managed Linux userspace; native Termux/Bionic Box64 is not suitable for executing Linux Wine binaries.
- Added an ARM64 Linux Box64 build workflow for the Snapdragon 888 profile, with ELF and dynamic dependency reports.
- Added a verified contributor bundle job for Kron4ek Wine 11.19 amd64 and DXVK 3.1.1, including pinned SHA-256 checks and artifact provenance.
- Added a DXVK Wine-prefix installer helper and runtime architecture documentation.
- Wine/Box64/DXVK are not yet integrated into the APK; Android glibc userspace, process bridge, Vulkan presentation, audio/input, and physical-device execution tests remain outstanding.

# Changelog

## [0.3.1] - 2026-11-16

Release candidate commit: CI publishes tag `v0.3.1` only after probe tests, ARM64 native build, APK packaging, and lint pass.

### Added
- Host-side Unity Windows installation probe: detects PE executable architecture, adjacent UnityPlayer.dll, matching Unity data folder, and Mono/IL2CPP metadata hints.
- Optional explicit host-runner launch attempt for testers. It is opt-in, uses argument arrays without shell expansion, and records timeout/exit status.
- Automated unit tests for data-only, Mono, and IL2CPP installation layouts.
- Contributor guide and Android/game runtime tester checklist.
- CI now runs the installation-probe unit tests before native/APK packaging.\n- Android folder import preserves the full PC installation under a dedicated per-game `_PC` directory instead of flattening it into the Unity data directory.\n- Added an ADB install/launcher smoke-test script for physical Android testers.

### Changed
- Android package version is 0.3.1 (versionCode 6).
- Release notes distinguish a complete Windows installation layout from a verified runnable game.

### Limitations
- No Unity Player, Wine, Box64, DXVK, VKD3D-Proton, Linux userspace, or proprietary game data is bundled.
- Host runner testing is not Android runtime testing.
- No title is claimed playable until actual gameplay is demonstrated on the target Android device.


## 0.4.0-alpha.1 — runtime architecture foundation

- Start the transition toward a self-contained, app-managed Wine/Box64 runtime for BYOG Overcooked 2.
- Add prefix, DXVK installation, and game launch scripts as isolated guest-side entry points.
- Add component provenance and explicit device-verification gates.
- Mark Turnip/adrenotools, Wine display/Vulkan bridge, audio/input integration, and physical gameplay validation as incomplete. This pre-release does not claim game execution or playability.

## Unreleased

- Added a dedicated game-session lifecycle model with validated transitions, failure-stage tracking, stop/crash handling, and retry support. It remains additive and is not yet connected to the runtime launch pipeline.

## Unreleased — game-library audit
- Re-audited existing Android game verification/import components and documented the live catalog integration gaps. Existing verifiers/importers are to be reused rather than duplicated.
- Verified GitHub Actions run #319 succeeded for the dedicated-session branch: probe tests, Android ARM64 APK build, lint, size audit, and artifacts. This does not constitute physical-device or gameplay validation.
