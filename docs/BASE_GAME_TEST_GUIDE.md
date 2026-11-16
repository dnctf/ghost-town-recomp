# Base game import and preflight test

## What this build is for

This APK is a **base-game preflight build**, not a playable Wine/Box64 build. It imports a user-owned PC installation and inspects Windows executable headers so the next Wine/Box64 proof of compatibility can be selected from real evidence.

It does not execute `.exe` files. It does not contain Wine, Box64, DXVK, VKD3D-Proton, or a Unity Windows player port.

## Prepare the source on Windows

For each game, locate the actual installed game directory, not the installer setup file. The selected directory should contain the Windows executable and the Unity data folder, for example:

```text
GameInstall/
├── GameName.exe
├── UnityPlayer.dll
├── GameName_Data/
│   ├── globalgamemanagers
│   ├── Managed/                 # Mono builds, if present
│   └── ...
├── other-game-dlls/
└── ...
```

This is an illustrative layout only. The actual game may have additional folders or a different Unity backend. Do not rename executables or data folders just to match this example.

If you have an archive, create a ZIP containing the installation contents. Avoid nesting several unrelated games in one archive. Keep the original PC installation unchanged.

## Import on Android

1. Install the debug APK built by GitHub Actions.
2. Open Ghost Town Recomp.
3. On the correct game card, choose **Folder Instalasi PC**.
4. In Android's folder picker, select the top-level installed game directory containing the `.exe` and its adjacent game files. Do not select only `*_Data` if you have the full installation.
5. Confirm the folder access request. The app copies the selected contents into its private app storage. Large installations need sufficient free space; this creates a second copy.
6. Alternatively, choose **Impor .ZIP PC** and select an archive containing that game's installation.
7. Choose **Periksa Base Game**. The app lists discovered `.exe` files, their PE architecture, and whether Unity data markers were found.

## Interpret the PE result

- `x86-64 / 64-bit`: candidate for a Wine + Box64 proof. It is not a compatibility guarantee.
- `x86 / 32-bit`: requires a suitable 32-bit Wine/WoW64 strategy and supporting libraries; Box64 alone does not solve this.
- `ARM64`: the executable is not a conventional x86-64 Windows binary; assess its platform/runtime separately.
- `Unknown`: malformed, packed, non-PE, or unsupported header; inspect on PC before choosing a runtime.

The current preflight scans up to six directory levels and reads PE headers only. It does not inspect every imported DLL, determine DirectX version, identify all DRM, or validate actual gameplay.

## What to send back for the PoC

For the first test, start with Overcooked! and send:

- Screenshot or copied text from **Periksa Base Game**.
- Exact executable name and reported architecture.
- Whether the imported folder contains `UnityPlayer.dll`, `GameAssembly.dll`, and/or `*_Data/Managed/Assembly-CSharp.dll`.
- The game's display/version information if available.
- MatePad's Android/HarmonyOS version and free storage.

Do not upload copyrighted game files to GitHub or include them in the APK. The game remains on the user's device. A successful APK build or successful PE scan does not mean the game is playable.

## Storage and rollback

Imported game copies live in the app-specific external files directory. Removing the app may remove those private copies. Keep the original PC installation or a backup ZIP. If the import fails because of storage space, delete the app's imported copy through Android app storage settings before retrying; do not delete the original game folder.
