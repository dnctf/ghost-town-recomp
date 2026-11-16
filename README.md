# ghost-town-recomp

**Multi-Game Launcher, Asset/Shader Recompilation Pipeline & Controller Hub for Overcooked 1 & 2 and Moving Out 1 & 2 (Ghost Town Games / Team17)** targeting Qualcomm Snapdragon SoCs (Snapdragon 845 – 8 Gen 3+) via Vulkan NDK, Embedded Mono ARM64 / IL2CPP Metadata Reinterpretation, and AAudio.

> **Legal Notice (BYOG):** This repository contains **zero** copyrighted assets, binaries, or game code from *Overcooked*, *Overcooked 2*, *Moving Out*, or *Moving Out 2*. Users must supply their own legally purchased PC installations (`Overcooked_Data/`, `Overcooked2_Data/`, `MovingOut_Data/`, or `MovingOut2_Data/`). Read [docs/LEGAL.md](docs/LEGAL.md) before contributing or building.

## Architecture Summary
- **Unified Developer Compiler & Launcher Pattern (Ghost Town Games / Team17):**
  - **Overcooked 1:** Unity 5 Mono CIL recompilation & asset pipeline.
  - **Overcooked 2:** Unity 2017+ Throw mechanic & recipe synchronization.
  - **Moving Out:** Physics joint furniture translation & truck delivery bridge (`MovingOutCompiler.kt`).
  - **Moving Out 2:** Multiverse delivery, arcade physics & joint chaos (`MovingOut2Compiler.kt`).
  - **Central Compiler Registry:** `CompilerRegistry.kt` mengelola dynamic discovery dan scanning data game secara modular.
  - **Moving Out UI Launcher:** `MovingOutLauncherActivity.kt` dinamis mendukung Moving Out 1 & 2.
- **Hardware Controller Support (DualShock 4 & Xbox):**
  - **Sony DualShock 4 / DualSense (DS4):** Auto-detection profil Sony (Vendor ID `0x054c`), mapping Cross/Circle/Square/Triangle, L1/R1 bumper, L2/R2 analog trigger threshold, and touchpad button.
  - **Xbox Wireless / Elite Controller:** Auto-detection profil Microsoft (Vendor ID `0x045e`), mapping A/B/X/Y, LB/RB bumper, LT/RT analog trigger threshold, and View/Menu keys.
  - **Digital D-Pad Normalization:** Dukungan komprehensif D-pad fisik baik melalui event `AXIS_HAT_X/Y` maupun keycode `DPAD_UP/DOWN/LEFT/RIGHT`.
- **In-App On-Screen Virtual Touch Controls:**
  - **Virtual Analog Joystick:** Joystick sentuh dinamis di kiri bawah dengan deadzone smoothing dan pembacaan -1.0 s/d 1.0.
  - **Virtual Action Buttons (Diamond):** Tombol aksi A (Grab/Pickup), B (Dash), X (Chop/Slap), Y (Throw/Jump) berpenampilan neon semi-transparan.
  - **Shoulder Buttons (L1, R1, L2, R2):** Tombol virtual cepat untuk switch chef, emote, dan grab.
  - **Multi-Touch Support:** Pelacakan multi-jari independen dan haptic feedback saat tombol ditekan.
  - **Toggleable Overlay:** Dapat diaktifkan atau dinonaktifkan kapan saja sesuai kebutuhan.
- **Android 10 & 12 (HarmonyOS 4) Robustness:**
  - **Vulkan Surface Recreation:** Re-create swapchain dan framebuffers secara atomik saat `surfaceChanged()` untuk mencegah crash Adreno di Android 12 / EMUI tablet.
  - **Deadlock-Free Synchronization:** Penanganan `VK_SUBOPTIMAL_KHR` pada presentasi frame tanpa mereset fence sebelum acquire.
  - **Android 10 Legacy Storage:** Dukungan `requestLegacyExternalStorage="true"` dan SAF stream handling tanpa `FileUriExposedException`.
  - **16 KB Memory Page Alignment:** Flag linker `-Wl,-z,max-page-size=16384` untuk kesiapan Android 15+.
- **Snapdragon / Adreno Optimizations:**
  - TBDR-aware Vulkan render passes (`LOAD_OP_CLEAR`, `STORE_OP_DONT_CARE` for depth).
  - Adreno 735/750 `128 MB` storage buffer range guard & triple-buffered swapchain anti-black-frame mitigation.
  - Real-time `AThermal` + `PowerManager` thermal governor with dynamic viewport resolution scaling (`0.75x`–`1.0x`).

## Repository Layout
```text
ghost-town-recomp/
├── .github/workflows/build-android.yml   # GitHub Actions NDK/APK CI & Pre-Release Publisher
├── android/                              # Android Gradle wrapper, JNI & C++20 Vulkan backend
│   ├── app/                              # Android Application module
│   │   ├── src/main/java/com/overcookedrecomp/app/
│   │   │   ├── compiler/core/            # Developer, GameCompiler, CompilerRegistry
│   │   │   ├── compiler/overcooked/      # OvercookedCompiler
│   │   │   ├── compiler/movingout/       # MovingOutCompiler, MovingOut2Compiler & MovingOutLauncherActivity
│   │   │   ├── input/                    # GamepadManager, GamepadProfile, VirtualGamepadOverlay
│   │   │   ├── GameStorageVerifier.java  # Storage scanner & SHA256 integrity verifier
│   │   │   ├── GameFileImporter.java     # SAF importer & zip slip protection
│   │   │   └── MainActivity.java         # Hub utama, virtual overlay, Vulkan surface
│   │   └── src/main/res/drawable/        # ic_overcooked.xml, ic_moving_out.xml
│   └── native/                           # C++20 Vulkan, Audio, Graphics Settings, Memory Arena
├── runtime/                              # Mono ARM64 host, IL2CPP metadata parser & Unity API shim
├── assets-pipeline/                      # Unity SerializedFile parser & ASTC/ETC2 transcoder
├── shader-bridge/                        # Unity ShaderLab/HLSL -> SPIR-V Vulkan bridge
├── input-mapper/                         # Gamepad HID, DS4/Xbox mapper, split-controller & D-pad
├── thermal/                              # Snapdragon AThermal governor & DRS controller
└── docs/                                 # ARCHITECTURE.md, SNAPDRAGON_MATRIX.md, LEGAL.md
```

## Quick Start (GitHub & Build)
1. Push repositori ini ke GitHub; `.github/workflows/build-android.yml` otomatis mengompilasi library NDK `arm64-v8a` dan memaketkan Release & Debug APK.
2. Salin folder game dari instalasi PC resmi Anda ke penyimpanan Android (`Android/data/com.overcookedrecomp.app/files/`):
   - **Overcooked 1:** salin sebagai folder `Overcooked_Data`
   - **Overcooked 2:** salin sebagai folder `Overcooked2_Data`
   - **Moving Out:** salin sebagai folder `MovingOut_Data`
   - **Moving Out 2:** salin sebagai folder `MovingOut2_Data`
3. Buka aplikasi `Ghost Town Recomp`, pilih game yang ingin dimainkan, gunakan kontrol virtual layar sentuh atau sambungkan gamepad DS4 / Xbox via Bluetooth, dan nikmati permainan!

## Pengaturan Grafik & Resolusi Layar Ponsel
Engine secara default membaca rasio dan dimensi layar ponsel saat startup:
- **Low (Snapdragon 845 / 4GB RAM):** Target 720p / 30 FPS, AA Off, Shadows Off, Textures Low, Anisotropic 1x.
- **Medium (Snapdragon 870/888 / 6-8GB RAM):** Adaptive resolution ~85% / 30-60 FPS, FXAA, Shadows Low, Textures Medium, Anisotropic 2x.
- **High (Snapdragon 8 Gen 1/2 / 8-12GB RAM):** Native Display / 60 FPS, FXAA, Shadows Medium, Textures High, Anisotropic 8x.
- **Ultra (Snapdragon 8 Gen 3+ / 12GB+ RAM):** Native Display / 60 FPS, MSAA 4x, Shadows High, Textures Ultra, Anisotropic 16x.
