# Changelog

## [0.3.1] - 2026-11-16

### Added
- Host-side Unity Windows installation probe: detects PE executable architecture, adjacent UnityPlayer.dll, matching Unity data folder, and Mono/IL2CPP metadata hints.
- Optional explicit host-runner launch attempt for testers. It is opt-in, uses argument arrays without shell expansion, and records timeout/exit status.
- Automated unit tests for data-only, Mono, and IL2CPP installation layouts.
- Contributor guide and Android/game runtime tester checklist.
- CI now runs the installation-probe unit tests before native/APK packaging.

### Changed
- Android package version is 0.3.1 (versionCode 6).
- Release notes distinguish a complete Windows installation layout from a verified runnable game.

### Limitations
- No Unity Player, Wine, Box64, DXVK, VKD3D-Proton, Linux userspace, or proprietary game data is bundled.
- Host runner testing is not Android runtime testing.
- No title is claimed playable until actual gameplay is demonstrated on the target Android device.
