# Device bring-up — gated test guide

> **Status: NOT READY.** This is a preparation document only. Gerbang R has not passed, no runtime archive exists, and `runtime/manifest.json` remains `pending`. Do not attempt Wine, Wineboot, X11, or game tests using the current UI Preview APK.

## Target

- Huawei MatePad Air 2023
- Qualcomm Snapdragon 888 / Adreno 660
- ARM64
- HarmonyOS 4.2

## Current evidence boundary

| Layer | CI | Emulator | MatePad |
|---|---|---|---|
| UI Preview APK | Build/lint/unit tests passed | Not tested | Not tested |
| PRoot under Android sandbox | Static APK packaging checks only | Not tested | Not tested |
| Runtime pack download/install | No validated archive | Not tested | Not tested |
| Wine / wineboot | Smoke ladder not yet passed | Not tested | Not tested |
| X11 / VirGL visible output | Not tested | Not tested | Not tested |
| Game/audio/input | Not tested | Not tested | Not tested |

## Do not proceed until P4

P4 is complete only after:
1. Gerbang R has all smoke rungs a-e green with logs and a real runtime archive has been generated, checksummed, size-audited and published.
2. Manifest status is activated from that verified archive, not manually guessed.
3. Downloader/installer tests cover the real archive.
4. A test APK and a completed bring-up procedure are attached to the repository.

At that point, update this document with exact artifact names, SHA-256, APK package/version, install steps, and the diagnostic export procedure. The user should then perform the device test and send the generated diagnostic ZIP. Stop at P4; do not begin display/game work until those results are reviewed.

## Device-specific risks to inspect at P4

- W^X / executable mapping restrictions in HarmonyOS app-private storage.
- seccomp restrictions and PRoot/ptrace behavior.
- Availability and bind-mount semantics for `/proc`, `/dev`, and `/dev/pts`.
- No dependency on SysV shared memory.
- Runtime extraction free-space requirements and interrupted-install recovery.
- Background process limits, battery optimization, thermal throttling, and RAM pressure.

## Diagnostics

The in-app **Ekspor diagnostik** feature is a P3 requirement and is not implemented yet. Do not treat manually collecting partial logs as an equivalent. When P3 is implemented, the ZIP must include app/runtime versions, device/OS information, installer state, Wine/Box64 stdout/stderr, self-test results, and sanitized system diagnostics. It must not include game files, credentials, or personal data.
