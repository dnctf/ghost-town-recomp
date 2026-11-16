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
