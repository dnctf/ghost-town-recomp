# Box64 Linux ARM64 build for Android-hosted glibc userspace

The Box64 artifact is built for AArch64 Linux/glibc, not Android Bionic. It is
intended to run inside a compatible ARM64 glibc userspace/rootfs hosted by the
Android app. It is not a standalone Android executable and is not yet invoked
by the launcher.

## Build

Open Actions → Build Box64 Android ARM64 (experimental). The workflow now runs
on an ARM64 Linux runner, builds upstream Box64 with the Snapdragon 888 profile
and ARM dynarec, records the upstream commit, and uploads the executable plus
ELF and ldd reports.

For reproducibility, run workflow_dispatch with a full 40-character upstream
commit SHA instead of main.

## Runtime boundary

Box64 translates Linux x86-64 ELF programs. It does not load Windows PE files.
Windows execution requires Wine x86_64, an ARM64 glibc userspace with the
matching x86-64 libraries, and a graphics/audio/input bridge.

The separate Wine/DXVK workflow packages pinned upstream components. Neither
workflow currently installs a full rootfs or integrates the runtime into the
APK. Do not enable Play until Box64 starts inside the target userspace, Wine
runs a harmless Windows test application, DXVK initializes Vulkan, and the
game passes device tests.

## Upstream

- https://github.com/ptitSeb/box64
- https://github.com/ptitSeb/box64/blob/main/docs/COMPILE.md
- https://github.com/ptitSeb/box64/blob/main/docs/WINE.md
