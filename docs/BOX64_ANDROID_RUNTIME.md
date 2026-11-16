# Runtime integration status: Box64, Wine, DXVK

## Correct execution model
Android app → managed Linux ARM64/glibc userspace → Box64 ARM64 Linux binary → Wine x86_64 → Windows game → DXVK → Vulkan bridge/driver.

Box64 translates Linux x86_64 ELF programs; it is not a Windows loader. Wine provides Windows API compatibility. DXVK supplies D3D8/9/10/11-to-Vulkan DLLs loaded inside Wine.

## Why the original Android/Bionic build was wrong
Upstream Box64 documents native Termux/Bionic as experimental and explicitly warns that it will not run Linux binaries. The TERMUX build is therefore not the correct Box64 artifact for an x86_64 Linux Wine build. The repository workflow now builds Box64 natively on an ARM64 Linux runner for glibc and the Snapdragon 888 profile. That artifact is intended to run inside a compatible ARM64 glibc userspace on Android, not directly as a Bionic app executable.

## Wine + DXVK bundle
The separate package workflow downloads Kron4ek Wine 11.19 amd64 and DXVK 3.1.1 with pinned SHA-256 digests. Wine 11.20 is not selected because its release notes report a startup segmentation-fault regression on some systems. DXVK is packaged as DLL payload for Wine prefixes; it is not a driver.

The bundle is an auditable contributor artifact, not yet an APK-integrated runtime. Full Android integration still needs an app-managed ARM64 glibc rootfs, x86_64 Wine libraries, process/loader integration, display server or graphics bridge, audio, input, and lifecycle management.

## Test sequence
1. Build Box64 with .github/workflows/build-box64-android.yml; inspect ELF, ldd, and upstream commit provenance.
2. Run the Wine/DXVK package workflow and verify artifact provenance.
3. Provision an ARM64 glibc userspace and verify Box64 starts there.
4. Verify Box64 can load Wine x86_64 and initialize a Wine prefix.
5. Run a harmless Windows PE test app before a game.
6. Install DXVK into the prefix and verify Vulkan/D3D device creation.
7. Test Overcooked 2 on MatePad Air: menu, gameplay, input, audio, and at least 15 minutes stability.

## No premature compatibility claims
A successful compile or archive hash check is not a Wine startup test. A Wine startup is not a DXVK/Vulkan test. A DXVK test is not proof that Overcooked 2 is playable.
