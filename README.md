# 🎮 Ghost Town Recomp (Android)

[![Android build](https://github.com/dnctf/ghost-town-recomp/actions/workflows/build-android.yml/badge.svg)](https://github.com/dnctf/ghost-town-recomp/actions)
[![Latest release](https://img.shields.io/github/v/release/dnctf/ghost-town-recomp?include_prereleases&color=orange&label=Release)](https://github.com/dnctf/ghost-town-recomp/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20%7C%20HarmonyOS%204-brightgreen)](https://github.com/dnctf/ghost-town-recomp)
[![Architecture](https://img.shields.io/badge/Arch-arm64--v8a-blue)](https://github.com/dnctf/ghost-town-recomp)
[![License](https://img.shields.io/badge/License-GPLv3-yellow.svg)](LICENSE)

> An experimental Android launcher and native runtime research project for selected PC co-op games.

Ghost Town Recomp is an open-source Android launcher and experimental Unity-oriented runtime/recompilation prototype. File inspection, metadata helpers, native rendering groundwork, and runtime provisioning do not establish that a Windows PC game is playable.

> [!NOTE]
> **Bring Your Own Game (BYOG).** This repository does not contain proprietary game assets, Unity Player files, or game binaries. Supply a legally obtained PC installation.

## Runtime support matrix

| Game / engine | Current evidence |
|---|---|
| Overcooked! | Data detection and Mono/CIL inspection scaffold; no execution |
| Overcooked! 2 | Data detection and IL2CPP metadata inspection scaffold; no execution |
| Moving Out | Installation inspection scaffold; no verified execution |
| Moving Out 2 | Installation inspection scaffold; no verified execution |
| Wine / Box64 | ARM64 runtime bundle pipeline and headless Wine version probe; Windows PE execution not verified |
| Source Engine | Separate feasibility research |

See [Runtime Audit](RUNTIME_AUDIT.md), [Multi-Runtime Architecture](docs/MULTI_RUNTIME_ARCHITECTURE.md), [Wine/Box64 Evaluation](docs/WINE_BOX64_EVALUATION.md), and [Graphics Backend Audit](docs/GRAPHICS_BACKEND_AUDIT.md).

## Features and groundwork

- C++20 / Android NDK ARM64 native integration and Vulkan capability scaffolding.
- Controller and virtual touch input scaffolding.
- Scoped-storage/document-picker game import.
- Unity Windows installation inspection for EXE architecture, adjacent UnityPlayer.dll, matching `*_Data`, and Mono/IL2CPP metadata hints.
- App-private Wine runtime installation with archive validation and rollback.
- Version-matched HTTPS runtime download from GitHub Releases (v0.3.3).
- Fixed-command, headless Wine version probe.

## Current status — 0.3.3 tester candidate

### 1. Install the launcher

Download `app-debug.apk` or `app-release.apk` from [GitHub Releases](https://github.com/dnctf/ghost-town-recomp/releases). The release APK is CI/debug-signed for testing, not a production-signed distribution.

### 2. Install the optional Windows runtime

Open Ghost Town Recomp and select **DOWNLOAD & INSTALL WINE RUNTIME v0.3.3**. The app downloads a version-matched manifest and archive over HTTPS, validates the declared size and SHA-256, then stages the runtime in app-private storage. Manual `.tar.gz` import remains available.

### 3. Run the headless probe

Open **WINE / BOX64 STATUS** and select **TEST WINE VERSION**. A successful result means the fixed `box64 wine --version` command returned exit code 0 through PRoot. It does not prove PE execution, Wine prefix initialization, DXVK/Vulkan rendering, audio/input, or gameplay.

### 4. Import game files for inspection

Select the complete Windows PC installation containing the game EXE, adjacent `UnityPlayer.dll`, and matching `*_Data` folder. The app reports data detection separately from execution readiness.

**Game launch remains disabled.** No X11/Wayland display backend, glibc-to-Android Vulkan presentation bridge, Android audio bridge, or complete Wine game lifecycle is integrated. Do not claim a game is playable until it reaches gameplay on the target physical device with graphics, input, audio, and stability verified.

## Build from source

Prerequisites: JDK 17, Android NDK r26d (`26.3.11579264`), CMake 3.22.1+, Gradle 8.5, and target ABI `arm64-v8a`.

```sh
git clone --recursive https://github.com/dnctf/ghost-town-recomp.git
cd ghost-town-recomp/android
gradle assembleDebug
gradle assembleRelease
gradle lint
```

The repository does not currently include the Gradle wrapper.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [Contributor & Tester Guide](docs/CONTRIBUTOR_TESTING.md). Priority milestones include a harmless Windows PE test, a real display backend, glibc-visible Vulkan/Adreno driver integration, audio/input bridging, and physical-device validation.

## Legal disclaimer

Ghost Town Recomp is independent and is not endorsed by Ghost Town Games, SMG Studio, Team17 Digital, or Unity Technologies. All trademarks and copyrighted materials belong to their respective owners. Users must own legitimate copies of PC games.
