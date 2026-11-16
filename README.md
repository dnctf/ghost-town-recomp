# 🎮 Ghost Town Recomp (Android)

[![Build Ghost Town Recomp Android NDK & APK](https://github.com/dnctf/ghost-town-recomp/actions/workflows/build-android.yml/badge.svg)](https://github.com/dnctf/ghost-town-recomp/actions)
[![Latest Release](https://img.shields.io/github/v/release/dnctf/ghost-town-recomp?include_prereleases&color=orange&label=Release)](https://github.com/dnctf/ghost-town-recomp/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20%7C%20HarmonyOS%204-brightgreen)](https://github.com/dnctf/ghost-town-recomp)
[![Architecture](https://img.shields.io/badge/Arch-arm64--v8a%20(Vulkan)-blue)](https://github.com/dnctf/ghost-town-recomp)
[![License](https://img.shields.io/badge/License-GPLv3-yellow.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/dnctf/ghost-town-recomp/pulls)

> **Experimental Android compatibility and porting workspace for PC Unity games. Game execution is not yet implemented.**

**Ghost Town Recomp** is an experimental Android project containing a game-data importer, diagnostic UI, and early native graphics/runtime scaffolding. It does **not** currently execute Windows .exe files or run the complete Unity player/gameplay loop.

> [!NOTE]
> **100% Legal & Clean-Room (Bring Your Own Game / BYOG):**
> This repository contains **NO copyrighted game assets, proprietary code, or binaries**. Users must supply their own legally purchased PC game files (Steam, Epic Games, GOG, etc.).

---

## 🕹️ Supported Games

| Game | Status | Required PC Folder | Features |
| :--- | :---: | :--- | :--- |
| **Overcooked!** | 🧪 Data detection only | `Overcooked_Data` | Unity data folder detection (execution not implemented) |
| **Overcooked! 2** | 🧪 Data detection only | `Overcooked2_Data` | Unity data folder detection (execution not implemented) |
| **Moving Out** | 🧪 Data detection only | `MovingOut_Data` | Unity data folder detection (execution not implemented) |
| **Moving Out 2** | 🧪 Data detection only | `MovingOut2_Data` | Unity data folder detection (execution not implemented) |

---

## ✨ Prototype Components

- ⚡ **Early C++20 / Vulkan scaffolding**: Android surface and Vulkan initialization experiments. The current frame loop clears the screen; it is not a Unity renderer.
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
- 🌡️ **Experimental thermal policy**: Applies conservative frame pacing/resolution policy to the current native prototype loop. This is not yet a measured game-runtime governor; device behavior must be validated on physical hardware.

---

## 📱 Snapdragon device targeting

The Android app uses a generic `arm64-v8a` build rather than one APK per Snapdragon generation. Vulkan capability detection reads the actual Adreno model, Vulkan API/driver versions, and device limits at runtime, then refreshes the initial graphics preset after GPU discovery.

The initial policy targets Snapdragon 845 / Adreno 630 and newer Qualcomm devices with conservative Low-to-Ultra presets. These are unverified starting profiles, not performance guarantees. See [Snapdragon compatibility](docs/SNAPDRAGON_COMPATIBILITY.md) and [Wine + Box64 integration plan](docs/WINE_BOX64_INTEGRATION.md).

---

## ⚙️ Current Prototype Setup (Not Playable Yet)

### 1. Download & Install the APK
Grab the latest APK from the [Releases](https://github.com/dnctf/ghost-town-recomp/releases) page. Install it on your Android 10+ phone or tablet.

### 2. Copy Your Complete PC Game Installation
Copy the **complete installed game folder** from your legitimate PC installation to your device. Include the main `.exe`, adjacent DLL files, and the matching Unity `*_Data` directory. Copying only `*_Data` is insufficient for the planned Windows compatibility runtime.

> 💡 You can zip the complete game folder on your PC, send it to your phone's **Download** folder, and use **Impor .ZIP PC** or **Pilih Folder** inside the relevant game card.

### 3. Inspect the imported game
Use **Periksa & Pilih Game** to scan PE executable architecture and Unity data markers. This is a read-only preflight check; it does not launch the PC game.

### 4. Windows execution status
**Wine + Box64 are not integrated in this build.** The current APK cannot execute the selected `.exe`. The next engineering milestone is a separately validated Android Wine/Box64 bootstrap based on the [Winlator WB64 reference](https://github.com/winebox64/winlator), followed by a per-game launch profile. See [Runtime Status](docs/RUNTIME_STATUS.md) and [Wine + Box64 Integration Plan](docs/WINE_BOX64_INTEGRATION.md).

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
./gradlew assembleDebug
```
The output APK will be generated at `android/app/build/outputs/apk/debug/app-debug.apk`.

---

## ⚖️ Legal Disclaimer

*Ghost Town Recomp* is an independent, non-commercial open-source project developed for interoperability and preservation purposes. It is **not** endorsed by, affiliated with, or associated with Ghost Town Games, SMG Studio, Team17 Digital, or Unity Technologies. All game trademarks, titles, and copyrighted materials belong to their respective owners. Users must own legitimate copies of the PC games to use this software.
