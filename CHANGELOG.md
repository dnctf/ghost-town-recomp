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
