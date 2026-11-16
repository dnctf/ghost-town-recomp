# overcooked-recomp

**Android Native Runtime & Asset/Shader Recompilation Pipeline for Overcooked 1 & Overcooked 2 (PC)** targeting Qualcomm Snapdragon SoCs (Snapdragon 845 – 8 Gen 3+) via Vulkan NDK, Embedded Mono ARM64 / IL2CPP Metadata Reinterpretation, and AAudio.

> **Legal Notice (BYOG):** This repository contains **zero** copyrighted assets, binaries, or game code from *Overcooked* or *Overcooked 2*. Users must supply their own legally purchased PC installation (`Overcooked_Data/` or `Overcooked2_Data/`). Read [docs/LEGAL.md](docs/LEGAL.md) before contributing or building.

## Architecture Summary
- **Dual-Title Support (Overcooked 1 & Overcooked 2):**
  - **Overcooked 1:** Unity 5.x engine runtime, Mono CIL / IL2CPP metadata interpretation, classic kitchen mechanics (pot, pan, plate, chop, wash, serve).
  - **Overcooked 2:** Unity 2017+ engine runtime, enhanced recipe state-machine, dynamic platforms/conveyors, dan mekanik lempar bahan (**Throw mechanic / Button Y / Triangle**).
  - **Auto-Detection & Game Selector:** Memindai folder `Overcooked_Data` dan `Overcooked2_Data`. Jika kedua game terpasang, aplikasi akan memunculkan menu pemilihan game.
- **Hybrid Execution Model:** Mengadopsi arsitektur clean-room **C++20 NDK Unity Engine API Shim** yang berjalan langsung di atas Vulkan dan AAudio.
- **Snapdragon / Adreno Optimizations:**
  - TBDR-aware Vulkan render passes (`LOAD_OP_CLEAR`, `STORE_OP_DONT_CARE` for depth).
  - Adreno 735/750 `128 MB` storage buffer range guard & triple-buffered swapchain anti-black-frame mitigation.
  - Real-time `AThermal` + `PowerManager` thermal governor with dynamic viewport resolution scaling (`0.75x`–`1.0x`) and adaptive spin-wait pacing (`<= 100 µs`).
- **Dynamic Mobile Display & Graphics Settings:**
  - **Auto-Detection Resolution:** Mendeteksi resolusi fisik layar HP asli (FHD+ `1080x2400`, QHD+ `1440x3200`, dsb.), refresh rate (60Hz / 90Hz / 120Hz), dan rasio aspek layar ponsel.
  - **Resolution Modes:** Native Layar HP, Adaptive Thermal Scaling, Fixed 1080p, Fixed 720p, Fixed 540p, serta Custom Scaling (50% - 150%).
  - **Graphics Presets:** Low, Medium, High, Ultra, dan Custom dengan kontrol Anisotropic Filtering, Anti-Aliasing (FXAA, MSAA), Shadows, Bloom, dan Ambient Occlusion.
- **Local Co-op Input:** NDK `AInputEvent` HID Gamepad mapping with **Split-Controller Mode** (2 players on 1 controller), tombol Throw (Y/Triangle) untuk Overcooked 2, dan touch overlay.

## Repository Layout
```text
overcooked-recomp/
├── .github/workflows/build-android.yml   # GitHub Actions NDK/APK CI & Pre-Release Publisher
├── android/                              # Android Gradle wrapper, JNI & C++20 Vulkan backend
│   ├── app/                              # Android Application module (MainActivity, GameStorageVerifier)
│   └── native/                           # C++20 Vulkan, Audio, Graphics Settings, Memory Arena
├── runtime/                              # Mono ARM64 host, IL2CPP metadata parser & Unity API shim
├── assets-pipeline/                      # Unity SerializedFile parser & ASTC/ETC2 transcoder
├── shader-bridge/                        # Unity ShaderLab/HLSL -> SPIR-V Vulkan bridge (Water/Conveyor)
├── input-mapper/                         # Gamepad HID, split-controller & throw mapper
├── thermal/                              # Snapdragon AThermal governor & DRS controller
└── docs/                                 # ARCHITECTURE.md, SNAPDRAGON_MATRIX.md, LEGAL.md
```

## Quick Start (GitHub & Build)
1. Push this repository to GitHub; `.github/workflows/build-android.yml` otomatis mengompilasi library NDK `arm64-v8a` dan memaketkan Release & Debug APK.
2. Salin folder game dari instalasi PC resmi Anda ke penyimpanan Android (`Android/data/com.overcookedrecomp.app/files/`):
   - **Overcooked 1:** salin sebagai folder `Overcooked_Data`
   - **Overcooked 2:** salin sebagai folder `Overcooked2_Data`
3. Buka aplikasi `Overcooked Recomp`, pilih game yang ingin dimainkan (jika keduanya ada), dan nikmati gameplay dengan performa Snapdragon yang optimal!

## Pengaturan Grafik & Resolusi Layar Ponsel
Engine secara default membaca rasio dan dimensi layar ponsel saat startup:
- **Low (Snapdragon 845 / 4GB RAM):** Target 720p / 30 FPS, AA Off, Shadows Off, Textures Low, Anisotropic 1x.
- **Medium (Snapdragon 870/888 / 6-8GB RAM):** Adaptive resolution ~85% / 30-60 FPS, FXAA, Shadows Low, Textures Medium, Anisotropic 2x.
- **High (Snapdragon 8 Gen 1/2 / 8-12GB RAM):** Native Display / 60 FPS, FXAA, Shadows Medium, Textures High, Anisotropic 8x.
- **Ultra (Snapdragon 8 Gen 3+ / 12GB+ RAM):** Native Display / 60 FPS, MSAA 4x, Shadows High, Textures Ultra, Anisotropic 16x.
