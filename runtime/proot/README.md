# Android ARM64 PRoot bundle

This workflow builds a static Android ARM64/Bionic PRoot executable and its companion loaders from the public `berilevi/proot-static` source tree.

The upstream ref is configurable for manual workflow dispatch. Every artifact records the exact resolved upstream commit, NDK version, ELF validation, SHA-256 checksums, and upstream COPYING file.

## Artifact layout

```text
root/
  bin/proot
  libexec/proot/loader
  libexec/proot/loader32
provenance/
  PROOT-COPYING
  BUILD-INFO.txt
  proot-upstream-commit.txt
  SHA256SUMS
```

Copy the contents of `root/` into the app-private runtime root. This is only the PRoot executable layer; it does not contain a Linux rootfs, Box64, Wine, or DXVK.

## Important

- The binary is compiled for Android ARM64/Bionic, not Linux/glibc.
- The companion loader files must remain at the relative runtime paths.
- CI validates ELF architecture and static linkage metadata; it does not execute PRoot on an Android device.
- PRoot is GPL-2.0-or-later; preserve the supplied license notice when redistributing.
- This is a build artifact, not yet an installable Android runtime by itself.
