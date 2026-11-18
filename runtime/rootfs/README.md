# Complete Android Wine runtime pack

This workflow builds the Linux guest userspace as a separately versioned runtime pack. The base APK contains the Android/Bionic PRoot executable and its companion loader under `jniLibs/arm64-v8a`; the Wine guest payload is downloaded only when the user installs the runtime.

## Target layout after extraction

The archive contains:
- `rootfs/`: Debian Bookworm ARM64/glibc userspace plus the amd64 libraries needed by WineD3D and Mesa VirGL
- `rootfs/usr/local/bin/box64`: pinned ARM64 Box64 build using the Snapdragon 888 profile
- `rootfs/opt/wine`: pinned Wine 11.19 amd64 payload
- `rootfs/usr/lib/x86_64-linux-gnu/dri/virgl_dri.so`: guest Mesa VirGL driver
- `provenance/`: upstream commits, license notices, package versions, dependency reports, hashes, size measurements and Wine-under-Box64 smoke log

PRoot and its Android loader are deliberately not duplicated in this archive. DXVK is deliberately not included until guest Vulkan and a visible D3D11 frame are independently validated. The runtime pack is extracted into app-private `filesDir/runtime/wine/`; Box64 and Wine guest binaries are invoked through PRoot, not executed directly from writable app data.

## Build inputs and reproducibility

- Debian suite: Bookworm; the exact amd64 package versions are recorded in `provenance/rootfs-package-manifest.txt`
- Package list: `runtime/rootfs/packages-amd64.txt`; packages are downloaded without running foreign-architecture maintainer scripts, then unpacked with `dpkg-deb -x`
- Box64: pinned commit `7fab05334445948c918281b54f2a604ff63337f7`, built in a separate cached ARM64 job and reused as an artifact
- Wine: Kron4ek Wine 11.19 amd64, SHA-256 pinned in the workflow
- Mesa VirGL: Debian Bookworm amd64 packages, exact package versions recorded in provenance
- PRoot: pinned commit `6f8ebfd8e24887dfba64c3f2d7d5fe9dc059b60e`, built into the APK's read-only native library directory

The rootfs package step does not execute amd64 maintainer scripts under QEMU. This avoids the previous QEMU/binfmt configuration failure and avoids running foreign post-install hooks. It also means generated package caches such as GSettings schemas are not assumed to exist unless a runtime test proves they are needed.

## Optimization and size audit

The workflow removes apt caches/lists, non-English locales (retaining English), man/info pages, non-license documentation, headers, static archives, debug files and Python bytecode. It applies `strip --strip-unneeded` to ELF files where the matching architecture tool succeeds, and records before/after tree sizes plus any strip failures. The final archive is validated for required files and rootfs-confined symlinks.

The runtime remains gzip-compressed for compatibility with the current Android installer. zstd/xz and first-run extraction performance have not yet been benchmarked on the Snapdragon 888, so no device-speed claim is made.

## First-run manifest and publication gate

The app reads `runtime/manifest.json`; it does not hardcode a runtime asset URL or checksum. The downloader supports HTTPS redirects, Range resume, bounded retries, SHA-256 validation, storage preflight, progress reporting and staged/atomic installation. The manifest's `runtime_pack.status` remains `pending` until a validated runtime asset is published. A pending manifest is an intentional safe state, not a playable build.

Do not create a GitHub Release/tag or mark the pack published until the app/runtime compatibility checks and required display/gameplay gates pass.

## Scope and non-claims

The CI smoke test executes Wine's version probe through Box64 on a native ARM64 GitHub runner. It does not prove that PRoot executes under Android/HarmonyOS app sandbox rules, that X11/VirGL displays a visible frame on Adreno 660, or that Overcooked! 2 is playable. No physical HarmonyOS 4.2 test is claimed. Keep `GHOST_TOWN_DISPLAY_READY=1` mandatory until the X11 display session and VirGL presentation self-tests pass.
