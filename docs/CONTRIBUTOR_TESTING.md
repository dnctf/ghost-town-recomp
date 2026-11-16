# Contributor & Tester Guide

This is an experimental Android ARM64 launcher and Windows-game runtime research project. APK compilation, runtime installation, Wine startup, DXVK initialization, and playable gameplay are separate validation gates.

## What build 0.3.2 contains

- Android launcher, game-library UI, storage importer, touch/gamepad input scaffolding, and native C++/Vulkan scaffolding.
- Unity Windows installation inspection for PE architecture, adjacent UnityPlayer.dll, matching `*_Data`, and Mono/IL2CPP metadata hints.
- Wine/Box64 status diagnostics and a fixed headless `wine --version` probe.
- App-private runtime bundle installer for a gzip-compressed tar archive. It stages extraction, checks required files, validates SHA-256 digests for key binaries/DXVK DLLs, rejects path traversal and escaping symlinks, and preserves the previous runtime until the new one validates.
- A separate CI workflow to assemble Android PRoot, Debian ARM64/glibc rootfs, Box64, Wine 11.19, and DXVK 3.1.1.

The APK does not embed the large Wine runtime bundle, Unity Player, or proprietary game files. The X11/display server, Vulkan presentation bridge, audio bridge, and Wine game-launch lifecycle are not integrated yet.

## Obtain the Android APK

Use the latest successful **Ghost Town Recomp Android Build and Tester Release** workflow on the `main` branch. Download the `ghost-town-recomp-0.3.2-arm64` artifact ZIP and extract:
- `app-debug.apk` for debugging
- `app-release.apk` for release-mode smoke tests

The current release APK is CI/debug-signed and is a tester build, not a production-signed release.

## Install the optional Wine runtime bundle

1. Open the successful **Build Complete ARM64 Wine Runtime Bundle** workflow run.
2. Download artifact `ghost-town-wine-runtime-arm64-debian-bookworm`.
3. Extract the outer Actions artifact ZIP. Inside it, locate `ghost-town-wine-runtime-arm64.tar.gz` and its `.sha256` file.
4. Keep enough free internal storage for the expanded rootfs plus temporary staging space. The installer creates a new staging directory before replacing an existing runtime.
5. Install the APK, open Ghost Town Recomp, and choose **INSTALL WINE RUNTIME BUNDLE (.TAR.GZ)**.
6. Select `ghost-town-wine-runtime-arm64.tar.gz`. Wait for the app to report that installation and payload checksums completed.
7. Open **WINE / BOX64 STATUS**. If the runtime is complete, run **TEST WINE VERSION** and save the displayed output/log.

The archive is a separate runtime payload, not an APK expansion file. The installer does not download it automatically.

## What a successful Wine probe means

A successful probe means the app found the required runtime files and the fixed headless command `box64 wine --version` returned exit code 0 through PRoot. It does not verify:
- PRoot ptrace behavior under the target HarmonyOS kernel policy
- Wine prefix initialization or Windows PE execution
- DXVK DLL loading or Vulkan device creation
- X11/Wayland display and Android surface presentation
- audio, keyboard, mouse, touch, gamepad, or process lifecycle
- any Unity game's menu or gameplay

Do not mark a Windows game playable based on this probe.

## Prepare a legally obtained PC game installation

Copy the complete game installation from a source you are authorized to use. A typical Unity Windows build may contain:

```text
Game/
  Game.exe
  UnityPlayer.dll
  Game_Data/
    globalgamemanagers
    Managed/Assembly-CSharp.dll
    il2cpp_data/Metadata/global-metadata.dat
  GameAssembly.dll
```

The exact layout varies by Unity version and title. Mono and IL2CPP files are alternatives, not all required together. Do not upload game files, keys, saves, or copyrighted assets to this public repository.

In the game card, choose **PILIH FOLDER INSTALASI PC** and select the top-level folder containing the EXE, UnityPlayer.dll, and matching `*_Data` directory. Importing only `*_Data` is not a complete PC installation.

## Static installation probe (Linux/macOS/Windows with Python 3)

```sh
python3 tools/game_installation_probe.py /path/to/Game --json game-report.json
python3 -m unittest discover -s tools/tests -v
```

The probe reports executable architecture, adjacent UnityPlayer.dll, matching data folder, and Mono/IL2CPP hints. A complete layout is only a candidate for runtime testing.

## Android launcher smoke test

With USB debugging enabled and the device authorized by ADB:

```sh
tools/android_device_smoke_test.sh android/app/build/outputs/apk/debug/app-debug.apk
```

This installs and launches the APK and captures startup logcat. It does not run a Windows game.

## Device test report

For the target Huawei MatePad Air 2023 (Snapdragon 888 / Adreno 660 / HarmonyOS 4.2), record:

1. APK version, device model, OS build, ABI, GPU driver, and available storage.
2. Whether runtime bundle installation completed; include the bundle SHA-256.
3. Wine probe output, exit code, and `wine-version-probe.log`.
4. Any PRoot/Box64/Wine errors from `adb logcat`.
5. Separate outcomes for import, Wine startup, PE startup, first frame, menu, gameplay, input, audio, save/load, crash, FPS, temperature, memory, and at least 15 minutes of stability.

Attach the probe JSON, logs, screenshots/video, and exact reproduction steps to a GitHub issue.

## Current playable status

No Windows Unity game is currently claimed playable on Android. The graphics/display backend and Android audio/input integration remain outstanding. The next milestone after a successful Wine probe is a harmless Windows PE test, followed by a minimal Direct3D/Vulkan presentation test. Only then should a game be launched.
