# Complete Android Wine runtime bundle

This workflow creates a single app-private runtime payload for the experimental Android launcher. The build is split across an x86_64 host for Android NDK/PRoot and a native ARM64 host for the Debian rootfs/Box64 build, then merged in a final packaging job.

## Target layout after extraction

The archive contains:
- `bin/proot`: static Android ARM64/Bionic executable
- `libexec/proot/loader` and `loader32`: PRoot companion loaders
- `rootfs/`: Debian ARM64/glibc userspace with amd64 runtime libraries
- `rootfs/usr/local/bin/box64`: Linux ARM64 Box64 built with the Snapdragon 888 profile
- `rootfs/opt/wine`: pinned Wine 11.19 amd64 payload
- `rootfs/opt/dxvk`: pinned DXVK 3.1.1 DLL payload
- `provenance/`: upstream commits, license, dependency report, hashes and build information

The tar.gz archive is intended to be extracted into the app's private `filesDir/runtime/wine/` directory. The archive is a large external runtime payload; it is not embedded in the APK.

## Build inputs

- Debian suite: Bookworm by default; manually selectable Bookworm or Trixie.
- Box64: upstream ref configurable at dispatch; exact commit recorded.
- PRoot: `berilevi/proot-static`; exact commit recorded.
- Wine: Kron4ek Wine 11.19 amd64, SHA-256 pinned.
- DXVK: 3.1.1, SHA-256 pinned.
- Build host: Ubuntu 24.04 ARM64 runner.

## Scope and validation

The workflow assembles the userspace and validates files, ELF architecture, and source archive checksums. It does not execute the Wine x86_64 binary through Box64, run PRoot on Android, install DXVK into a Wine prefix, create a Vulkan device, or validate game rendering. DXVK needs a compatible graphics/display bridge that is not bundled here.

Debian packages are fetched from the selected Debian suite at build time; the exact package versions are represented in the rootfs dpkg database. For byte-identical rebuilds, use a dated Debian snapshot and pin both upstream Git refs to immutable commit IDs.

## Important compatibility boundary

This runtime is a foundation, not a playable Windows emulator. A successful artifact build is not proof of PRoot ptrace compatibility on HarmonyOS or of Wine/Box64 execution on the MatePad Air. Do not enable game launch until the headless probe, a harmless PE test, DXVK/Vulkan initialization, input/audio, and gameplay tests pass on device.


## App installation

Use the dashboard action **INSTALL WINE RUNTIME BUNDLE (.TAR.GZ)** and select the workflow's `ghost-town-wine-runtime-arm64.tar.gz` artifact after downloading it. The installer extracts into app-private storage and validates the required runtime files before activation. Keep at least the archive's expanded size plus temporary staging space free; installation stages a second copy before replacing an existing runtime.

The installer does not make the runtime executable on a particular Android kernel/GPU. Run the headless Wine version probe after import and record its log and exit code.
