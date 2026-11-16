# Changelog

## [0.3.0] - 2026-11-16

### Added
- Per-game execution-readiness details from the selected library tile.
- Explicit explanation of the missing Unity Player / Windows compatibility runtime.
- Automated GitHub pre-release publication for tag `v0.3.0`, gated behind successful APK build and Android lint.

### Changed
- Game folder scanning is now reported as **data detected**, not as a playable or runnable installation.
- Mono and IL2CPP verifier messages now describe inspection only.
- App version is `0.3.0-alpha` (versionCode 5).

### Fixed
- Removed the hard-coded orange Vulkan clear-only loop from the visible surface lifecycle.
- Selecting a detected game no longer enters a placeholder render screen or launches a misleading title-specific stub.
- Surface resize no longer attempts to resize an uninitialized Vulkan swapchain.

### Known limitations
- No complete Unity Player is included.
- Mono runtime hosting and managed Unity execution are not implemented.
- IL2CPP metadata parsing is not native recompilation.
- Wine/Box64/DXVK integration is not included or validated on Android ARM64.
- No title is currently verified as playable. A complete legal PC installation and a compatible execution runtime are still required.
