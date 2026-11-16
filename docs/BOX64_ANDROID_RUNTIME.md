# Android runtime integration status

## What is being built now

The repository now provides an on-demand GitHub Actions workflow to compile
upstream Box64 for Android Bionic ARM64 using the Android NDK. The workflow
records the exact upstream commit used, verifies the produced executable is
AArch64 ELF, and publishes it as a short-lived workflow artifact for
contributors.

This is the first real runtime binary build step. It is intentionally separate
from the normal APK workflow: a successful Box64 build does not mean the
launcher can execute Windows games.

## Why Wine is a separate milestone

Box64 executes Linux x86-64 ELF binaries. A Windows game executable is PE,
not ELF. Wine supplies Windows API compatibility, but x86-64 Wine itself
requires a compatible Linux userspace and libraries. Android Bionic is not a
drop-in glibc distribution. The project must choose and validate a supported
runtime layout (for example, an app-managed userspace or a separately
provisioned compatible environment) before launching Wine.

Do not treat the following as equivalent:

- Android ARM64 Box64 binary exists.
- Wine can start on Android.
- A Windows game starts.
- A Unity game renders and is playable.

Each is a separate test gate.

## Test sequence

1. Run the Box64 workflow with a pinned upstream commit SHA.
2. Download the artifact and inspect `file`, `readelf -h`, and upstream
   commit provenance.
3. Install/test the binary on a supported Android test environment and capture
   stdout/stderr and exit status.
4. Select and build a Wine path that matches the Android/Bionic or managed
   userspace strategy. Do not bundle random Wine builds.
5. Start a harmless Windows PE test application before using a game.
6. Test the imported Overcooked 2 installation only after Wine starts reliably.
7. Verify first frame, menu, gameplay, input, audio, process lifecycle, and a
   15-minute stability run on the MatePad Air.

## Current limitations

- Box64 is not yet linked into the APK or called by `WineRuntimeBridge`.
- Wine, Linux userspace, DXVK, and VKD3D-Proton are not included.
- No Unity Editor is required; the game installation supplies its matching
  Unity Player files.
- No Android game execution or physical-device compatibility is claimed.
