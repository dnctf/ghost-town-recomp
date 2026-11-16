# Complete Android Wine runtime bundle

This workflow creates a single app-private runtime payload for the experimental Android launcher. The build is split across an x86_64 host for Android NDK/PRoot and a native ARM64 host for the Debian rootfs/Box64 build, then merged in a final packaging job.

## Target layout

- `bin/proot`: static Android ARM64/Bionic executable
- `libexec/proot/loader` and `loader32`: PRoot companion loaders
- `rootfs/`: Debian Bookworm ARM64/glibc userspace with amd64 runtime libraries
- `rootfs/usr/local/bin/box64`: Linux ARM64 Box64 built with the Snapdragon 888 profile
- `rootfs/opt/wine`: pinned Wine 11.19 amd64 payload
- `rootfs/opt/dxvk`: pinned DXVK 3.1.1 DLL payload
- `provenance/`: upstream commits, licenses, dependency report, hashes, and build information

The archive is installed into app-private `filesDir/runtime/wine/`; it is not embedded in the APK. The app downloads the matching `runtime-manifest.json` from the corresponding GitHub Release, verifies the archive size and SHA-256, and then uses the same safe staging installer as manual import.

## Packaging and extraction safety

The ARM64 CI job archives the rootfs before uploading it. This avoids recursively scanning root-only Debian directories such as `/etc/ssl/private` and preserves symlinks, modes, ownership metadata, ACLs, and extended attributes. The Android installer rejects traversal and escaping links, enforces entry and expanded-size limits, validates required files and runtime SHA-256 entries, and activates the staged directory while retaining rollback capability.

Absolute symlinks inside the guest rootfs are rewritten as relative links rooted within `rootfs/`. This preserves guest chroot/PRoot meaning without creating absolute links into Android host storage.

## Inputs and provenance

- Debian suite: Bookworm.
- Box64 and PRoot refs are configurable at workflow dispatch; exact commits are recorded.
- Wine: Kron4ek Wine 11.19 amd64, archive SHA-256 pinned.
- DXVK: 3.1.1, archive SHA-256 pinned.
- Build host: Ubuntu 24.04 ARM64 for rootfs and Box64.

Debian packages are fetched from Bookworm at build time. Exact package versions are recorded in the dpkg database and provenance report. For byte-identical rebuilds, use a dated Debian snapshot and immutable upstream commit refs.

## Validation boundary

The workflow validates archive checksums, ELF architecture, rootfs package metadata, and required runtime layout. It does not execute Wine x86_64 through Box64, run PRoot on Android, initialize a Wine prefix, install DXVK into that prefix, create a Vulkan device, or validate game rendering.

A successful artifact build is not proof of PRoot ptrace compatibility on HarmonyOS or Wine/Box64 execution on the MatePad Air. The X11/display server, glibc-visible Vulkan/Adreno presentation bridge, Android audio/input bridge, and game process lifecycle remain unimplemented.

## License and attribution

The runtime artifact includes notices for PRoot (GPL-2.0-or-later), Box64 (MIT), Wine 11.19 (LGPL-2.1-or-later plus third-party notices), and DXVK 3.1.1 (zlib/libpng). Debian package copyright files are retained under `rootfs/usr/share/doc/*/copyright`. Preserve `provenance/licenses/` when redistributing the runtime.
