# Android Wine / Box64 runtime adapter

## Current integration

The Android launcher exposes **WINE / BOX64 STATUS** in its dashboard. The adapter checks a provisioned runtime directory and can run a fixed, headless `wine --version` probe when the required files are present. It never invokes a shell and does not accept arbitrary command strings.

This is a diagnostic/process-integration milestone, not a game-launcher claim. The current APK does not download, unpack, or bundle a Linux root filesystem. Game launch remains disabled until the graphics/display backend and lifecycle integration are implemented.

## Expected app-private layout

Provision under `Context.getFilesDir()/runtime/wine/`:

```text
runtime/wine/
  bin/
    proot                 # executable Android ARM64/Bionic PRoot
  rootfs/                 # ARM64 Linux userspace with glibc
    etc/os-release
    usr/local/bin/
      box64               # Linux ARM64/glibc Box64 build
    opt/
      wine/
        bin/wine          # Linux x86_64 Wine binary
        lib/              # Wine runtime libraries
        lib64/
      dxvk/
        x64/
          d3d11.dll
          dxgi.dll
        x32/
          ...
```

Wine and its x86_64 dependencies must be installed inside the rootfs. Box64 and its ARM64/glibc dependencies must be available in the same rootfs. The Wine bundle artifact alone is not a rootfs and cannot be copied directly into the APK as a runnable Android executable.

## Adapter checks

- PRoot exists and is executable.
- `rootfs/etc/os-release` exists.
- Box64 exists at `rootfs/usr/local/bin/box64` or `rootfs/usr/bin/box64`.
- Wine exists at `rootfs/opt/wine/bin/wine`.
- DXVK x64 `d3d11.dll` and `dxgi.dll` exist under `rootfs/opt/dxvk/x64`.
- The optional headless probe invokes only `box64 wine --version` through PRoot, with a 45-second timeout and a log file.

The probe does **not** verify PE game execution, a Wine prefix, Direct3D initialization, DXVK Vulkan device creation, rendering, audio, input, or sustained gameplay.

## Remaining engineering gates

1. **Runtime provisioning:** build/package an Android ARM64/Bionic PRoot executable; create a reproducible ARM64 glibc rootfs with required ARM64 and x86_64 libraries; install Box64 and pinned Wine; verify licenses and notices.
2. **Prefix bootstrap:** initialize a per-game Wine prefix, install DXVK into the prefix, configure native DLL overrides, and preserve per-game settings.
3. **Graphics/display:** integrate a compatible Android Vulkan driver/translation layer and a display backend. DXVK only translates Direct3D calls to Vulkan; it is not the Android Vulkan driver or window system.
4. **Audio/input:** bridge Wine audio to Android audio and map touch, keyboard, mouse, and gamepad events.
5. **Lifecycle/security:** manage process groups, pause/resume, force-stop, storage binds, permissions, logs, and cleanup without exposing arbitrary shell execution.
6. **Compatibility validation:** first run Wine self-tests and a minimal Direct3D test; then test one 64-bit game on physical Snapdragon 888 hardware. Record menu, gameplay, frame pacing, audio, controller, memory, temperature, crashes, and long-session stability.

## Target device validation

The target is Huawei MatePad Air 2023 (Snapdragon 888 / Adreno 660 / HarmonyOS 4.2). CI compilation on an ARM64 runner is not a substitute for testing on this device. Do not mark a game playable until all gameplay gates pass.
