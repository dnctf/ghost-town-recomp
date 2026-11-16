# Android Wine / Box64 runtime adapter and provisioning

## Current integration

The 0.4.0-alpha.1 launcher still exposes **WINE / BOX64 STATUS** and **INSTALL WINE RUNTIME BUNDLE (.TAR.GZ)**. The single-APK managed first-run downloader is not yet implemented. A user-selected runtime archive is extracted into app-private storage at `Context.getFilesDir()/runtime/wine/`. Extraction stages the new runtime, rejects absolute/traversal paths and symlinks escaping the staging directory, validates required files, and only then swaps it into place.

The optional Wine probe runs only the fixed headless command `box64 wine --version` through PRoot. It is not a game launch path. Wine prefix creation, per-title DXVK installation, and a guarded game launch entry point are now scaffolded under `wine-runtime/scripts/`, but are not yet wired to a device-tested graphics session.

## Full bundle build

Workflow: `.github/workflows/build-wine-runtime-bundle.yml`

Default artifact: `ghost-town-wine-runtime-arm64-debian-bookworm`; 0.4.0-alpha.1 pre-release asset target.

The build assembles:
- Static Android ARM64/Bionic PRoot plus `loader` and `loader32`
- Debian Bookworm ARM64/glibc rootfs
- Debian amd64 runtime libraries for x86_64 Wine
- Box64 built for ARM64/glibc with the Snapdragon 888 profile
- Pinned Wine 11.19 amd64 payload
- Pinned DXVK 3.1.1 DLL payload
- Upstream commit IDs, package dependency report, checksums and license notice

The workflow checks the Wine ELF dependency tree before packaging. Debian package versions are recorded in the dpkg database; for byte-identical rebuilds, use a dated Debian snapshot and immutable upstream commit refs.

## Expected extracted layout

```text
filesDir/runtime/wine/
  bin/proot
  libexec/proot/
    loader
    loader32
  rootfs/
    etc/os-release
    usr/local/bin/box64
    opt/wine/bin/wine
    opt/wine/lib/
    opt/wine/lib64/
    opt/dxvk/x64/d3d11.dll
    opt/dxvk/x64/dxgi.dll
    opt/dxvk/x32/...
  provenance/
```

The PRoot process must receive explicit `PROOT_LOADER` and `PROOT_LOADER_32` paths because the executable is installed under the app-private files directory rather than the build-time Termux prefix.

## Validation boundary

A complete archive and successful import only prove that the payload was assembled and copied. They do not prove:
- PRoot ptrace support under the MatePad's HarmonyOS kernel policy
- Box64 can load the Wine x86_64 ELF and all dependencies on device
- Wine can initialize a prefix
- DXVK can create a Vulkan device or present a window
- audio, touch, keyboard, mouse, gamepad, pause/resume, and process cleanup work
- a Unity Windows game reaches a playable state

## Remaining gates

1. Build and inspect the complete bundle artifact; fix missing x86_64 dependencies.
2. Install the archive on the MatePad Air and run the headless Wine version probe.
3. Run a minimal Windows PE executable and initialize a per-game Wine prefix.
4. Install DXVK into the prefix and configure native DLL overrides.
5. Integrate a compatible Android Vulkan/display backend. DXVK itself is not a Vulkan driver.
6. Bridge audio and input; implement process lifecycle, storage mapping, logs, cancellation and cleanup.
7. Test one user-supplied 64-bit Unity title on the physical Snapdragon 888 / Adreno 660 device.

Do not mark a title playable until actual gameplay, rendering, input, audio, frame pacing, thermal behavior and stability have been tested on device.


## 0.4.0-alpha.1 CI correction

The runtime assembly job now copies the license directory from the downloaded `rootfs-unpacked/provenance` artifact (not the unavailable build-job workspace path) and pins the default Box64 and PRoot source refs to immutable commits. The resulting archive is attached to the `v0.4.0-alpha.1` pre-release only. This remains a separately downloaded archive until the managed in-app downloader is implemented.
