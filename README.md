# overcooked-recomp

**Android Native Runtime & Asset/Shader Recompilation Pipeline for Overcooked (PC)** targeting Qualcomm Snapdragon SoCs (Snapdragon 845 – 8 Gen 3+) via Vulkan NDK, Embedded Mono ARM64 / IL2CPP Metadata Reinterpretation, and AAudio.

> **Legal Notice (BYOG):** This repository contains **zero** copyrighted assets, binaries, or game code from *Overcooked*. Users must supply their own legally purchased PC installation (`Overcooked_Data/`). Read [docs/LEGAL.md](docs/LEGAL.md) before contributing or building.

## Architecture Summary
- **Hybrid Execution Model:** Unlike pure ISA static recompilers (`pes2017-recomp`), Overcooked is built on Unity Engine. `overcooked-recomp` pairs an **Embedded Mono ARM64 VM** (MVP path) and **IL2CPP Metadata Reinterpretation + NativeAOT** (long-term path) with a clean-room **C++20 NDK Unity Engine API Shim** over Vulkan and AAudio.
- **Snapdragon / Adreno Optimizations:**
  - TBDR-aware Vulkan render passes (`LOAD_OP_CLEAR`, `STORE_OP_DONT_CARE` for depth).
  - Adreno 735/750 `128 MB` storage buffer range guard & triple-buffered swapchain anti-black-frame mitigation.
  - Real-time `AThermal` + `PowerManager` thermal governor with dynamic viewport resolution scaling (`0.75x`–`1.0x`) and adaptive spin-wait pacing (`<= 100 µs`).
- **Dynamic Mobile Display & Graphics Settings:**
  - **Auto-Detection Resolution:** Secara otomatis mendeteksi resolusi fisik layar HP (misal: 1080x2400 FHD+, 1440x3200 QHD+), refresh rate (60Hz / 90Hz / 120Hz), dan DPI.
  - **Resolution Modes:** Native Layar HP, Adaptive Thermal Scaling, Fixed 1080p, Fixed 720p, Fixed 540p, serta Custom Scaling (50% - 150%).
  - **Graphics Presets:** Low, Medium, High, Ultra, dan Custom dengan kontrol Anisotropic Filtering, Anti-Aliasing (FXAA, MSAA), Shadows, Bloom, dan Ambient Occlusion.
- **Local Co-op Input:** NDK `AInputEvent` HID Gamepad mapping with **Split-Controller Mode** (2 players on 1 controller) and customizable touch overlay.

## Repository Layout
```text
overcooked-recomp/
├── .github/workflows/build-android.yml   # GitHub Actions NDK/APK CI
├── android/                              # Android Gradle wrapper, JNI & C++20 Vulkan backend
│   ├── app/                              # Android Application module (MainActivity, GameStorageVerifier)
│   └── native/                           # C++20 Vulkan, Audio, Graphics Settings, Memory Arena
├── runtime/                              # Mono ARM64 host, IL2CPP metadata parser & Unity API shim
├── assets-pipeline/                      # Unity SerializedFile parser & ASTC/ETC2 transcoder
├── shader-bridge/                        # Unity ShaderLab/HLSL -> SPIR-V Vulkan bridge
├── input-mapper/                         # Gamepad HID, split-controller & touch mapper
├── thermal/                              # Snapdragon AThermal governor & DRS controller
└── docs/                                 # ARCHITECTURE.md, SNAPDRAGON_MATRIX.md, LEGAL.md
```

## Quick Start (GitHub & Build)
1. Push this repository to GitHub; `.github/workflows/build-android.yml` will automatically compile the `arm64-v8a` NDK libraries and package a debug APK.
2. Copy `Overcooked_Data/` from your legitimate PC installation onto your Android device storage.
3. Launch `overcooked-recomp`, select your `Overcooked_Data/` folder to verify checksums, convert assets to ASTC 6x6 + SPIR-V, and start playing.

## Pengaturan Grafik & Resolusi Layar Ponsel
Engine secara default membaca rasio dan dimensi layar ponsel saat startup:
- **Low (Snapdragon 845 / 4GB RAM):** Target 720p / 30 FPS, AA Off, Shadows Off, Textures Low, Anisotropic 1x.
- **Medium (Snapdragon 870/888 / 6-8GB RAM):** Adaptive resolution ~85% / 30-60 FPS, FXAA, Shadows Low, Textures Medium, Anisotropic 2x.
- **High (Snapdragon 8 Gen 1/2 / 8-12GB RAM):** Native Display / 60 FPS, FXAA, Shadows Medium, Textures High, Anisotropic 8x.
- **Ultra (Snapdragon 8 Gen 3+ / 12GB+ RAM):** Native Display / 60 FPS, MSAA 4x, Shadows High, Textures Ultra, Anisotropic 16x.
