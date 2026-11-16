# Contributor & Tester Guide

This is an experimental Android ARM64 launcher and Windows-game runtime research project. APK compilation, runtime installation, Wine startup, DXVK initialization, and playable gameplay are separate validation gates.

## What build 0.3.3 contains

- Android launcher, game-library UI, storage importer, touch/gamepad input scaffolding, and native C++/Vulkan scaffolding.
- Unity Windows installation inspection for PE architecture, adjacent UnityPlayer.dll, matching `*_Data`, and Mono/IL2CPP metadata hints.
- Wine/Box64 status diagnostics and a fixed headless `wine --version` probe.
- Version-matched HTTPS GitHub Release manifest downloader with retry/resume, archive-size validation, and SHA-256 verification before installation.
- App-private runtime installer with staging, required-file validation, traversal and escaping-link checks, guest-rooted absolute-symlink normalization, core-file checksum validation, and rollback.
- CI workflow assembling Android PRoot, Debian Bookworm ARM64/glibc rootfs, Box64, Wine 11.19, and DXVK 3.1.1.

The APK does not embed the large Wine runtime bundle, Unity Player, or proprietary game files. The X11/display server, Vulkan presentation bridge, audio bridge, and Wine game-launch lifecycle are not integrated yet.

## Obtain the Android APK

Open [GitHub Releases](https://github.com/dnctf/ghost-town-recomp/releases) and select **Ghost Town Recomp 0.3.3 - Contributor Tester Candidate** (`v0.3.3`). Download `app-debug.apk` for debugging or `app-release.apk` for release-mode smoke tests. The release APK is CI/debug-signed and is not production-signed.

If the release assets are not yet available, open the successful **Ghost Town Recomp Android Build and Tester Release** workflow on `main` and download artifact `ghost-town-recomp-0.3.3-arm64`.

## Install the optional Wine runtime

1. Install the APK and connect to the internet.
2. Open Ghost Town Recomp and choose **DOWNLOAD & INSTALL WINE RUNTIME v0.3.3**.
3. The app retrieves `runtime-manifest.json` from the matching GitHub Release over HTTPS.
4. It checks the runtime version, ARM64 architecture, HTTPS GitHub asset URL, declared archive size, and SHA-256.
5. Interrupted transfers are retried and resumed when the release server supports HTTP Range. The old runtime is not changed during download.
6. After the archive digest matches, the app extracts into an app-private staging directory, validates required files and their SHA-256 entries, then activates the runtime while retaining rollback capability.
7. Open **WINE / BOX64 STATUS** and choose **TEST WINE VERSION**. Save the output and exit code for device testing.

Manual installation remains available through **IMPORT WINE RUNTIME BUNDLE (.TAR.GZ)**. Download `ghost-town-wine-runtime-arm64.tar.gz` from the same release and select it through Android's document picker.

Keep sufficient internal storage for the downloaded archive, expanded rootfs, and staging copy. The downloader stores its partial archive in app cache. A failed download or installation should leave the previously active runtime intact.

## What a successful Wine probe means

A successful probe means the fixed headless command `box64 wine --version` returned exit code 0 through PRoot. It does not verify:
- PRoot ptrace behavior under the target HarmonyOS kernel policy
- Wine prefix initialization or Windows PE execution
- DXVK DLL loading or Vulkan device creation
- X11/Wayland display and Android surface presentation
- audio, keyboard, mouse, touch, gamepad, or process lifecycle
- any Unity game's menu or gameplay

Do not mark a Windows game playable based on this probe.

## Prepare a legally obtained PC game installation

A typical Unity Windows installation may contain:

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

The exact layout varies by Unity version and title. Mono and IL2CPP files are alternatives. Do not upload game files, keys, saves, or copyrighted assets to this public repository.

Select **PILIH FOLDER INSTALASI PC** and choose the top-level folder containing the EXE, UnityPlayer.dll, and matching `*_Data` directory. Importing only `*_Data` is not a complete PC installation.

## Static installation probe

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
2. Runtime download/install result and the bundle SHA-256.
3. Wine probe output, exit code, and `wine-version-probe.log`.
4. PRoot/Box64/Wine errors from `adb logcat`.
5. Separate outcomes for import, Wine startup, PE startup, first frame, menu, gameplay, input, audio, save/load, crash, FPS, temperature, memory, and at least 15 minutes of stability.

Attach probe JSON, logs, screenshots/video, and exact reproduction steps to a GitHub issue.

## Current playable status

No Windows Unity game is currently claimed playable on Android. Graphics/display and Android audio/input integration remain outstanding. The next milestone after a successful Wine probe is a harmless Windows PE test, followed by a minimal Direct3D/Vulkan presentation test. Only then should a game be launched.
