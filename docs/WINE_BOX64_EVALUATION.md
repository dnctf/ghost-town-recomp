# Wine / Box64 Feasibility Evaluation

**Status: external-runtime evaluation only. No Wine/Box64 code or binary is integrated by this document.**

## Feasibility

Wine combined with Box64 can provide a route for running supported **x86-64 Windows applications** on ARM64 Linux-based environments: Wine implements Windows APIs, while Box64 translates x86-64 Linux user-mode code needed by the Wine stack. On Android, this generally requires a compatible Android runtime/userspace arrangement (for example, a PRoot-based Linux root filesystem), an ARM64 Box64 build, a compatible Wine build, filesystem/process setup, and a graphics stack.

This is not equivalent to dropping `box64` into an Android APK. Box64 is not itself a Windows PE loader and does not supply Wine, Windows APIs, a Linux root filesystem, or DirectX translation. Compatibility varies by application, CPU features, Wine version, libc/userspace, GPU driver, graphics API, DRM, and dependencies.

## Why this is separate from the Unity path

The current repository has a Unity-oriented native shim and asset metadata helpers. Wine/Box64 instead executes a Windows program in a compatibility environment. It does not convert Unity CIL or IL2CPP game code into this repository's native object model, nor does it make the current Vulkan surface a Unity renderer.

The screenshot supplied for the Overcooked 2 installation preflight reports PE machine `0x014C` (x86 / 32-bit), twice. That is **not** an x86-64 executable. A Box64-only x86-64 configuration cannot be assumed to run it. A compatible 32-bit path (for example, a validated WoW64 setup or Box86-based stack) must be selected and tested. The preflight's reported zero-byte sizes also mean file inventory/metadata should be rechecked against the complete installation folder before attempting execution.

## Integration options

1. **Separate runtime app/module (preferred for evaluation):** maintain a standalone `wine-runtime/` project or companion Android app that owns the rootfs, Wine prefix, Box64/Box86, graphics translation, and runtime updates. The Ghost Town launcher can later expose an explicit "Open in external runtime" hand-off if a stable and permission-safe interface exists.
2. **Termux/PRoot-assisted workflow:** use a user-installed Termux-compatible Linux userspace with Box64 and Wine. This can be useful for a proof of concept, but process hand-off, storage permissions, graphics/Vulkan access, and device-specific behavior must be validated. Do not assume that an arbitrary Termux installation exposes a supported public API for another app.
3. **Embedded runtime bundle:** only consider after a reproducible build, legal review, ABI/userspace validation, storage sizing, update strategy, and security review. Do not embed opaque binaries or extract another app's private data.

## Required proof-of-concept gates

- Select and pin an upstream Android Wine/Box64 implementation; record exact commit, licenses, notices, patches, and source obligations.
- Confirm 32-bit and 64-bit architecture strategy separately. The supplied Overcooked 2 preflight currently identifies x86 32-bit PE files.
- Build or obtain a reproducible ARM64 userspace and verify loader/libc compatibility.
- Start Box64/Box86 and the corresponding Wine loader in the intended Android process model.
- Run a small, redistributable Windows test program and capture exit code/stdout/stderr.
- Test Vulkan availability and the chosen DXVK/VKD3D path independently on target devices.
- Only then attempt one user-owned game installation; verify DRM, dependencies, audio, controller, save path, menu, and actual gameplay.

## Recommendation

Keep Wine/Box64 out of the Unity native build and APK for now. Prototype it as a separately maintained runtime module/app. The supplied x86 32-bit executable result makes the 32-bit compatibility decision the first blocker—not simply adding Box64.
