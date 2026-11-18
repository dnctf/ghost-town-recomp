# Ghost Town Recomp — UI Preview

## Download and install

1. Open the [UI Preview workflow runs](https://github.com/dnctf/ghost-town-recomp/actions/workflows/ui-preview.yml).
2. Select the successful run for the commit you want to test.
3. In **Artifacts**, download `ghost-town-ui-preview-0.5.0-alpha.2-<full-sha>`.
4. Extract the ZIP and install the included debug-signed APK:
   - USB/ADB: `adb install -r app-debug.apk` (package suffix `.uipreview`, so it installs beside the normal app). Remove the previous UI Preview first if Android reports a debug-signature mismatch.
   - Device: copy the APK to the tablet, allow installation from that file manager, then sideload it.
5. Launch **Ghost Town Recomp** (the installed debug build is the UI Preview). The dashboard is the app's launcher Activity; the legacy native setup Activity remains internal and is not the UI Preview entry point.

The APK is signed with the standard Android debug signing configuration and is intended for sideload testing, not Play Store distribution. It is ARM64-only, matching the current application ABI.

## Included screens

- **Games:** four clearly labelled DEMO entries, abstract generated card artwork, focused-card treatment, storm-grey procedural background, profile/runtime status, battery/thermal/time indicators, stats and controller hint bar.
- **Game detail:** bottom sheet with the three Play states. Play is disabled unless the runtime is installed and the display self-test marker is present.
- **Runtime:** actual runtime installation check, display marker check, and a disabled install action while the runtime manifest is pending.
- **Library:** Android Storage Access Framework folder selection with persisted URI permission.
- **Settings:** reduce motion, UI scale, UI sound preference, and control-map selection.

## Controls and accessibility

- Touch: tap tabs/cards/actions; long-press a game card for its context menu; swipe/scroll the game row horizontally.
- Keyboard/gamepad: D-pad left/right changes focus/selection; A/Enter opens game details; B/Escape returns; Y opens the DEMO game search; Menu opens the context menu.
- Touch controls use at least 48dp action height where applicable. The Android system font family and font scaling are retained. Focus selection uses the teal accent and a visible outline.
- **Reduce motion** stops the animated storm backdrop. No real-time image blur is used: cards are original abstract gradients, so the backdrop uses its procedural fallback.

## CI validation

The isolated `.github/workflows/ui-preview.yml` workflow is triggered only by UI/app source paths, UI docs, and itself, or manually with `workflow_dispatch`. It does not depend on, download from, or wait for the runtime workflow. It runs:

- `:app:testDebugUnitTest` — Play launch policy (Ready / Blocked / Processing); JUnit XML summary is included in the artifact.
- `:app:lintDebug`.
- `:app:assembleDebug`.
- APK ZIP inventory, size comparison, and `EMBEDDED_RUNTIME_PARTS=0` assertion.

The size delta is compared with the pre-preview debug APK from [baseline Run 44224359054](https://github.com/dnctf/ghost-town-recomp/actions/runs/44224359054), artifact `apk-size-audit`, `app-debug.apk.txt` (13,135,290 bytes). The CI report includes the new APK size and signed byte/percentage delta.

## Not implemented in this preview

- Launching a game process is deliberately disabled. Even when the UI state reports the gate as ready in a future build, this preview Activity does not start a game.
- Runtime installation/download is unavailable while `runtime_pack.status=pending`.
- No successful Wine runtime, X11/GLX, VirGL, D3D11 visible-frame or game-display self-test is claimed.
- Audio process routing and game audio are not implemented here.
- Game data detection is not connected to the DEMO cards; Library currently stores the SAF folder permission only.
- No copyrighted game art, Microsoft/Xbox/Game Pass branding, proprietary logos or licensed font files are bundled.

## Display readiness gate

The Play policy uses `RuntimeLaunchGate`. Runtime readiness delegates to the existing `RuntimeBundleInstaller.isInstalled()` check. Display readiness requires the app-private marker:

`filesDir/runtime/display/GHOST_TOWN_DISPLAY_READY`

whose exact content must be `GHOST_TOWN_DISPLAY_READY=1`. This preview does not create the marker. It may only be written by a future successful end-to-end display self-test. Until both checks pass, Play stays blocked with the relevant reason. The runtime manifest and release gate are unchanged.

## Screenshot and device validation

The CI job builds the ARM64 APK and runs JVM unit tests/lint, but it does not launch an ARM64 emulator or capture an emulator screenshot. No screenshot is represented as a device capture. Capture one on the target tablet with:

```bash
adb shell monkey -p com.overcookedrecomp.app.uipreview 1
adb shell screencap -p /sdcard/ghost-town-ui-preview.png
adb pull /sdcard/ghost-town-ui-preview.png .
```

Still to verify on Huawei MatePad Air 2023: actual display scaling, focus traversal with the physical controller, SAF behavior on HarmonyOS, contrast under the device's display settings, rotation/resize behavior, and thermal/battery indicator behavior. CI success is not a substitute for this physical-device validation.


### CI compile correction

The first preview attempt, [Run 44236665584](https://github.com/dnctf/ghost-town-recomp/actions/runs/44236665584), stopped at Kotlin compilation with:

```
DashboardActivity.kt:55:38 Unresolved reference: nativeKeyEvent
Task :app:compileDebugKotlin FAILED
```

The input handler now maps Compose `Key` values directly (D-pad, A/B/Y, Enter/Escape, Menu), avoiding the unavailable Android-specific `nativeKeyEvent` extension and supporting explicit up/down focus-zone movement.


### Size note from preview build

The validated UI Preview APK measured 27,157,234 bytes (about 25.90 MiB), compared with the pre-UI debug APK baseline of 13,135,290 bytes. The measured increase is 14,021,944 bytes (+106.75%). The APK remains free of embedded runtime parts (`EMBEDDED_RUNTIME_PARTS=0`); the increase is primarily the added Compose UI stack. Compose tooling-only dependencies are excluded from the preview build.
