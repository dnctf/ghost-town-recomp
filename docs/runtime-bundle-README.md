# Wine + DXVK component bundle

This bundle contains upstream Wine x86_64 and DXVK Windows DLLs. It is a component payload for contributors, not a self-contained Android runtime.

## Pinned baseline
- Wine: Kron4ek Wine 11.19 amd64 (SHA-256 verified in CI).
- DXVK: upstream DXVK 3.1.1 (SHA-256 verified in CI).
- Wine 11.20 is intentionally not the baseline because its release notes report a startup segmentation-fault regression on some systems.

## Required execution architecture
The Wine archive contains Linux x86_64 executables/libraries. It cannot run directly on Android ARM64. It requires Box64 built for Linux ARM64/glibc, an ARM64 glibc Linux userspace/root filesystem, Wine x86_64 and matching libraries, plus a working display/audio/input/Vulkan bridge.

DXVK is not a standalone renderer or Android Vulkan driver. It provides D3D8/9/10/11-to-Vulkan DLLs loaded by Wine. The host must expose a working Vulkan implementation.

## Build bundle
Run Actions → Package Wine and DXVK runtime components. The workflow downloads pinned release assets, verifies SHA-256 digests, extracts them, records provenance, and uploads a workflow artifact. It does not commit third-party binaries into Git history or embed Wine into the launcher APK.

## Install DXVK into a Wine prefix
Within the prepared Linux userspace, after a Wine prefix exists:

    export WINEPREFIX=/path/to/prefix
    cd /path/to/dxvk
    ./setup_dxvk.sh install

The upstream manual alternative copies x64 DLLs to drive_c/windows/system32, x32 DLLs to drive_c/windows/syswow64, then sets native overrides for d3d8, d3d9, d3d10core, d3d11, and dxgi. See https://github.com/doitsujin/DXVK#how-to-use.

## Validation gates
The bundle workflow validates download hashes and archive layout only. It does not prove Wine starts under Box64, DXVK initializes Vulkan, or any game runs. Do not expose this bundle as a working Play backend until a real ARM64/glibc userspace test and Android-device graphics test pass.
