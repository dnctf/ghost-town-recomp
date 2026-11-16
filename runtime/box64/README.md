# Box64 Android ARM64 build

This directory tracks the integration plan; upstream Box64 source is fetched by
`.github/workflows/build-box64-android.yml` and is not copied or vendored here.

## Build

Open **Actions → Build Box64 Android ARM64 (experimental) → Run workflow**.
Choose `sd888` for the Snapdragon 888 profile or `generic-arm64` for a generic
ARM64 target. The workflow accepts a branch, tag, or immutable commit in
`box64_ref`, records the resolved upstream commit, builds with Android NDK
26.3.11579264/API 29, verifies the output ELF is AArch64, and uploads the
experimental binary as a workflow artifact.

For reproducible contributor builds, supply a full 40-character upstream commit
SHA instead of `main`.

## Important runtime boundary

Box64 translates **Linux x86-64 ELF** programs to ARM64. It is not a Windows
loader and does not run `.exe` files by itself. Windows execution requires a
compatible Wine implementation plus a Linux userspace and the matching x86-64
libraries. The upstream Box64 documentation describes the Wine variants and
their dependency requirements.

The Android/Bionic Box64 build artifact is an isolated research deliverable. It
is not yet copied into the APK, invoked by the app, or proven to run Wine on
HarmonyOS. Do not wire it to the per-game Play button until the following gates
pass on the target device:

1. The binary starts under the app's Android process constraints.
2. A controlled ARM64/Linux test rootfs and required x86-64 libraries are
   available without relying on undocumented host paths.
3. A compatible Wine build starts through Box64.
4. Wine initializes a prefix and loads a harmless Windows test executable.
5. Graphics, audio, input, storage permissions, and process shutdown are tested.

## Unity files

Do not bundle Unity Editor or redistribute proprietary Unity Player/game files.
A Windows Unity game installation normally supplies its matching
`UnityPlayer.dll`, game executable, and `*_Data` directory. The game owner
imports those files. The Unity Player is title/build-specific; there is no
universal Unity runtime binary that can replace it.

## Upstream references

- Box64 source: https://github.com/ptitSeb/box64
- Box64 compile guide: https://github.com/ptitSeb/box64/blob/main/docs/COMPILE.md
- Box64 Wine guide: https://github.com/ptitSeb/box64/blob/main/docs/WINE.md
