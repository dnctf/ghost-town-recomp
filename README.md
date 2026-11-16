# 🎮 Ghost Town Recomp (Android)

[![Build Ghost Town Recomp Android NDK & APK](https://github.com/dnctf/ghost-town-recomp/actions/workflows/build-android.yml/badge.svg)](https://github.com/dnctf/ghost-town-recomp/actions)
[![Latest Release](https://img.shields.io/github/v/release/dnctf/ghost-town-recomp?include_prereleases&color=orange&label=Release)](https://github.com/dnctf/ghost-town-recomp/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20%7C%20HarmonyOS%204-brightgreen)](https://github.com/dnctf/ghost-town-recomp)
[![Architecture](https://img.shields.io/badge/Arch-arm64--v8a%20(Vulkan)-blue)](https://github.com/dnctf/ghost-town-recomp)
[![License](https://img.shields.io/badge/License-GPLv3-yellow.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/dnctf/ghost-town-recomp/pulls)

> **An experimental Android launcher and native runtime research project for selected PC co-op games.**

**Ghost Town Recomp** is an open-source Android launcher and experimental Unity-oriented native runtime/recompilation prototype. The current repository includes file inspection, metadata helpers, Vulkan integration points, and title-specific scaffolding; it does not yet establish that the listed PC games are playable.

> [!NOTE]
> **100% Legal & Clean-Room (Bring Your Own Game / BYOG):**
> This repository contains **NO copyrighted game assets, proprietary code, or binaries**. Users must supply their own legally purchased PC game files (Steam, Epic Games, GOG, etc.).

---

## Runtime Support Matrix

| Game / Engine | Runtime Path | Status |
| :--- | :--- | :--- |
| Overcooked! | Data verifier + Mono/CIL inspection scaffold | 🧪 Data detection only; no execution |
| Overcooked! 2 | IL2CPP metadata inspection scaffold | 🧪 Metadata detection only; no execution |
| Moving Out | Title-specific inspection scaffold | 🧪 No verified execution |
| Moving Out 2 | Title-specific inspection scaffold | 🧪 No verified execution |
| Wine / Box64 (.exe) | Required candidate path for Windows PC builds | 📄 Research only; not integrated or tested |
| Source Engine (HL2 / mods) | Separate SDK-based fork/port | 📄 Feasibility research; separate project |

See [Runtime Audit](RUNTIME_AUDIT.md), [Multi-Runtime Architecture](docs/MULTI_RUNTIME_ARCHITECTURE.md), [Wine/Box64 Evaluation](docs/WINE_BOX64_EVALUATION.md), and [Source Engine Feasibility](docs/SOURCE_ENGINE_FEASIBILITY.md) for evidence and limitations.

---

## ✨ Features

- ⚡ **C++20 & Vulkan 1.0 integration point**: Android native rendering and device capability groundwork; this is not yet a complete Unity renderer.
- 🎮 **Native Controller Support (Plug & Play)**:
  - **Sony DualShock 4 & DualSense (PS4 / PS5)**: Native button glyphs (✕, ◯, ▢, △), analog triggers, and touchpad support.
  - **Xbox Wireless & Elite Controllers**: Auto-detected profiles, standard A/B/X/Y mappings, and LT/RT analog thresholds.
  - **Split-Controller Co-op**: Play with a friend on a single controller (Left Stick & L1/L2 for Player 1, Right Stick & R1/R2 for Player 2).
- 📱 **On-Screen Touch Controls (Virtual Gamepad)**:
  - Responsive analog touch joystick on the bottom-left with deadzone smoothing.
  - Diamond action buttons (A, B, X, Y) + shoulder buttons (L1, L2, R1, R2).
  - Multi-touch tracking and haptic feedback. Can be toggled on/off anytime.
- 🛡️ **Android 10 to 15+ Compatibility**:
  - Full support for Android 10 (API 29), Android 12 (HarmonyOS 4), and modern Android 14/15.
  - **16 KB page-size aligned** (`-Wl,-z,max-page-size=16384`) for next-gen Android kernels.
  - Scoped storage & SAF file importer with zip extraction safeguards.
- 🌡️ **Smart Thermal Governor**: Automatically adjusts resolution scale (`0.75x` – `1.0x`) during long gaming sessions to keep your device cool and battery healthy.

---

## 🚧 Current Status (0.3.0)

### 1. Install the experimental launcher
Install the APK from the [Releases](https://github.com/dnctf/ghost-town-recomp/releases) page on an Android 10+ device. This release is a launcher and runtime-readiness preview, not a playable Unity runtime.

### 2. Import game data for inspection
Copy the `_Data` folder from your legitimate PC game installation to your device:

```text
/sdcard/Android/data/com.overcookedrecomp.app/files/
  ├── Overcooked_Data/     # for Overcooked 1
  ├── Overcooked2_Data/    # for Overcooked 2
  ├── MovingOut_Data/      # for Moving Out
  └── MovingOut2_Data/     # for Moving Out 2
```

> 💡 **Tip:** You can also simply zip the folder on your PC, send it to your phone's **Download** folder, and tap **"⚡ Deteksi Otomatis dari Folder Download"** or **"📁 Impor .ZIP PC"** inside the app!

### 3. Inspect execution requirements
Open **Ghost Town Recomp** and inspect the imported game data. The app reports detected data separately from execution readiness. Press **EXECUTION STATUS** on a game card to see the missing runtime components. The orange clear-only renderer has been removed. Mono hosting, IL2CPP recompilation, Unity rendering, and Wine/Box64 integration are not yet implemented; therefore no PC game is claimed playable.

---

## 🤝 We Love Contributors!

Ghost Town Recomp is an open-source project and we welcome contributions of all skill levels! Whether you want to add a feature, optimize performance, or fix a bug, we'd love to have you on board.

### 🔥 Great Areas to Help With:
- 🎮 **New Controller Profiles**: Add support for Nintendo Switch Pro Controller, 8BitDo, Joy-Cons, Razer Kishi, etc.
- 🎨 **UI / UX Polish**: Modernize the setup launcher, add themes, customizable button layouts, or game art scrapers.
- ⚡ **Graphics & Shaders**: Unity ShaderLab-to-SPIRV translations, Mali/PowerVR/Xclipse GPU compatibility.
- 🌐 **Translations & Docs**: Help translate the app and documentation into more languages.
- 🧪 **Device Testing**: Test on different chips (Snapdragon 8 Gen 1/2/3, Dimensity 9000+, Tensor G3, etc.) and submit test reports.

### 🛠️ How to Contribute:
1. **Fork** the repository.
2. Create your feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m "feat: add support for Nintendo Switch Pro Controller"`).
4. Push to your branch (`git push origin feature/amazing-feature`).
5. Open a **Pull Request** and describe what you built!

---

## 🏗️ Building from Source

### Prerequisites
- **Android Studio** (Hedgehog or newer) / JDK 17
- **Android NDK**: `r26d` (26.3.11579264)
- **CMake**: `3.22.1+`
- **Target ABI**: `arm64-v8a`

### Build Command
```bash
git clone --recursive https://github.com/dnctf/ghost-town-recomp.git
cd ghost-town-recomp/android
gradle assembleDebug
gradle lint
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`. This repository currently does not include `gradlew` or the Gradle wrapper JAR/properties; use installed Gradle 8.5+ as CI does, or add a verified wrapper before using `./gradlew`.

---

## ⚖️ Legal Disclaimer

*Ghost Town Recomp* is an independent, non-commercial open-source project developed for interoperability and preservation purposes. It is **not** endorsed by, affiliated with, or associated with Ghost Town Games, SMG Studio, Team17 Digital, or Unity Technologies. All game trademarks, titles, and copyrighted materials belong to their respective owners. Users must own legitimate copies of the PC games to use this software.
