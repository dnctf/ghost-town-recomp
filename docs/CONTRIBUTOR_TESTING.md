# Contributor & Tester Guide

This project is an experimental Android ARM64 launcher and runtime research project. A successful APK build is not proof that a Windows Unity game runs.

## What this repository contains
- Android launcher, storage importer, input overlay, and native C++/Vulkan scaffolding.
- Unity data/metadata inspection (not Mono hosting, IL2CPP recompilation, or a Unity Player).
- An inactive Wine/Box64 bridge. No Wine, Box64, DXVK, VKD3D-Proton, Linux root filesystem, or proprietary game binaries are bundled.

## Prepare a legally obtained PC game installation
Copy the complete game installation from a source you are authorized to use. For a typical Unity Windows build, the folder may contain:
```
Game/
  Game.exe
  UnityPlayer.dll
  Game_Data/
    globalgamemanagers
    Managed/Assembly-CSharp.dll       # Mono, if applicable
    il2cpp_data/Metadata/global-metadata.dat  # IL2CPP, if applicable
  GameAssembly.dll                   # IL2CPP, if applicable
```
The exact layout varies by Unity version and title. Do not upload game files, keys, saves, or copyrighted assets to this public repository.

## Static installation probe (Linux/macOS/Windows with Python 3)
```sh
python3 tools/game_installation_probe.py /path/to/Game --json game-report.json
python3 -m unittest discover -s tools/tests -v
```
The probe reports executable architecture, adjacent UnityPlayer.dll, matching _Data folder, and Mono/IL2CPP hints. A complete layout is only a candidate for runtime testing; it is not proof of compatibility.

## Actual launch test (opt-in)
Only run this on a machine where the required runtime and graphics environment are already installed:
```sh
python3 tools/game_installation_probe.py /path/to/Game \
  --run --runner box64 /path/to/wine --exe Game.exe --timeout 90
```
The runner prefix is passed as an argument array (no shell expansion). This is a host-side diagnostic, not an Android launch integration. A process exit code alone does not prove playable rendering. Record logs, renderer, first rendered scene, input, audio, and a 15-minute stability test separately.

## Android tester checklist
1. Install the debug APK on the target device; record model, Android/HarmonyOS version, ABI, GPU and driver.
2. Import the complete PC installation into app-private storage only if the app's importer supports the required size and folder structure. Do not import only `*_Data` and label it runnable.
3. Capture `adb logcat -c`, launch the app, reproduce the test, then capture `adb logcat -d -v threadtime`.
4. Attach the game probe JSON, APK version, logs, screenshots/video, and exact steps to a GitHub issue.
5. Record separate outcomes: import, process startup, first frame, menu, gameplay, input, audio, saves, crash, FPS, temperature, and memory.

## Current runtime gate
The current APK does not ship a Unity Player or a validated Wine/Box64 runtime. The tester tool can inspect a full PC installation and optionally launch it through an explicitly supplied host runner. It cannot make the Android APK execute the game. Do not mark a game playable until the target Android device renders and accepts input in actual gameplay.
