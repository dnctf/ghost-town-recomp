# Changelog

## [0.3.3] - 2026-11-16 (tester candidate)

### Added
- Version-matched HTTPS GitHub Release manifest discovery and automatic Wine runtime download from the Android dashboard.
- Bounded retry/resume download, archive-size validation, and SHA-256 verification before installation.
- App-private staging and rollback installation remain shared by automatic download and manual document-picker import.
- Guest-rooted normalization of absolute symlinks from the Debian rootfs.

### Fixed
- Fixed ARM64 rootfs workflow artifact upload failing when the uploader traversed root-only Debian paths such as `/etc/ssl/private`.
- Rootfs is now transferred as a tar archive, preserving symlinks and permission metadata without exposing private directories to recursive artifact upload.
- Aligned APK version to 0.3.3 (versionCode 8) and documented the matching runtime release manifest.

### Validation boundary
- Python probe tests, ARM64 native build, APK packaging, Android lint, PRoot/rootfs assembly, and runtime release packaging are independent CI gates.
- Static archive validation does not prove PRoot ptrace compatibility on HarmonyOS, Wine/Box64 execution, Windows PE execution, DXVK/Vulkan presentation, audio/input, or gameplay on the MatePad Air.
- No Windows Unity game is claimed playable.

## [0.3.2] - 2026-11-16 (tester candidate)

### Added
- Complete ARM64 glibc runtime bundle workflow assembling Android PRoot, Debian Bookworm ARM64 userspace, amd64 Wine dependencies, Box64, Wine 11.19, and DXVK 3.1.1.
- App-private runtime installer for gzip tar archives, with staging, required-file validation, traversal/symlink checks, and rollback.
- Dashboard action to import the runtime bundle through Android's document picker.
- Explicit PRoot companion-loader environment paths for the Wine probe.

### Validation boundary
- CI bundle creation, APK compilation, runtime archive import, PRoot execution on HarmonyOS, Wine startup, DXVK/Vulkan initialization, and actual game compatibility are separate gates.
- Graphics/display bridge, Wine prefix bootstrap, Android audio/input bridge, and physical-device gameplay tests remain pending.

## [0.3.1] - 2026-11-16

### Added
- Host-side Unity Windows installation probe for PE architecture, adjacent UnityPlayer.dll, matching Unity data folder, and Mono/IL2CPP metadata hints.
- Optional explicit host-runner launch attempt for testers.
- Unit tests for data-only, Mono, and IL2CPP installation layouts.
- Contributor guide, Android import improvements, and ADB launcher smoke-test tooling.

### Limitations
- No Unity Player, Wine, Box64, DXVK, VKD3D-Proton, Linux userspace, or proprietary game data is bundled in the APK.
- Host runner testing is not Android runtime testing.
- No title is claimed playable until gameplay is demonstrated on target Android hardware.
