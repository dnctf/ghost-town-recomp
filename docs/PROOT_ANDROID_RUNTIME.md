# Android PRoot runtime component

## Source and build

The PRoot layer is built from [berilevi/proot-static](https://github.com/berilevi/proot-static), a Termux-oriented PRoot fork with a static Android ARM64 build script and bundled talloc source. The build uses Android NDK 28.2.13676358, a version listed by that project as tested. The workflow records the resolved upstream commit instead of silently treating a moving branch as immutable.

Workflow: `.github/workflows/build-proot-android.yml`

Artifact: `proot-android-arm64-bionic-static`

The artifact contains:
- `root/bin/proot`
- `root/libexec/proot/loader`
- `root/libexec/proot/loader32`
- upstream license and SHA-256 provenance

## App-private installation target

Copy the artifact's `root/` contents into:

```text
filesDir/runtime/wine/
  bin/proot
  libexec/proot/loader
  libexec/proot/loader32
  rootfs/
    etc/os-release
    ...
```

The companion loader files are part of the PRoot runtime and must not be discarded. The current Wine adapter additionally checks for `bin/proot` and `libexec/proot/loader`.

## Scope and limitations

This artifact provides only an Android ARM64/Bionic PRoot executable and companion loaders. It is not a Linux root filesystem and cannot run Wine on its own. The separate Box64 artifact is Linux ARM64/glibc and must live inside a compatible ARM64 glibc rootfs. Wine is Linux x86_64 and needs its x86_64 libraries; DXVK DLLs need a Wine prefix and a functioning Vulkan/display backend.

The CI job validates file presence, AArch64 ELF headers, and static linkage metadata. It does not execute the Android binary, validate ptrace behavior on HarmonyOS, or prove that a rootfs can be entered on the MatePad Air. Device-level PRoot tests remain required.

## Licensing

The upstream PRoot project is GPL-2.0-or-later. The artifact includes the upstream COPYING file. Preserve license and attribution material when distributing the binary.
