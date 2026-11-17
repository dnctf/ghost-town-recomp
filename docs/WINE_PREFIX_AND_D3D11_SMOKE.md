# Wine prefix bootstrap and D3D11 smoke-test milestone

## Implemented in branch `feat/wine-prefix-supervisor-d3d11-smoke`

- Adds an app-private, per-title Wine prefix at `filesDir/wine-prefixes/overcooked2`.
- Mounts the prefix into the Debian guest at `/home/ghosttown/.wineprefixes/overcooked2` through PRoot; no root or external terminal is required.
- Runs the fixed `wineboot --init` command through PRoot + Box64 with a bounded timeout and a captured log.
- Installs the pinned DXVK x64 DLL payload into the prefix's `drive_c/windows/system32` and registers native overrides for `d3d11` and `dxgi`.
- Exposes an in-app **PREPARE WINE PREFIX + DXVK** action. This prepares runtime state only; it does not enable game launch.
- Adds a small, original Windows D3D11 smoke-test program. CI cross-compiles it with MinGW-w64 and embeds it as `assets/tests/d3d11-smoke.exe` in the APK.

## Current gate status

| Gate | Status | Evidence still required |
|---|---|---|
| Runtime archive integrity and APK packaging | CI-validated | Physical device install |
| Wine prefix creation | Implemented, not device-validated | Successful `wineboot --init` and non-empty `system.reg` on MatePad |
| DXVK per-prefix installation | Implemented, not device-validated | DLL override registry success and DXVK logs |
| D3D11 smoke executable | Built by CI | Run under Wine after a display backend is available |
| Guest Vulkan ICD | Not implemented | glibc loader + Adreno ICD enumeration from guest |
| Android display presentation | Not implemented | X11/Wayland or direct presentation bridge to Android surface |
| Visible D3D11 frame | Not tested | Device screenshot/video and DXVK log |
| Overcooked! 2 | Blocked | Do not enable until all preceding gates pass |

## Important implementation boundary

The prefix bootstrap deliberately removes inherited `DISPLAY` and `WAYLAND_DISPLAY` variables. It is a headless provisioning operation and cannot create a display server. The D3D11 smoke executable is a test payload, not proof that DXVK is functional. The launch gate must remain closed until a guest display backend and Vulkan path are integrated and a visible frame is captured on the target device.

## Device validation sequence

1. Install the APK and wait for built-in runtime provisioning.
2. Open **PREPARE WINE PREFIX + DXVK**.
3. Confirm `system.reg` exists under `filesDir/wine-prefixes/overcooked2`.
4. Inspect `filesDir/runtime/wine/logs/overcooked2/bootstrap.log` and the DXVK registry logs.
5. After the display backend lands, run the embedded `d3d11-smoke.exe` and require `D3D11_SMOKE_OK frames=300` plus a visible blue test window.
6. Only after that test passes may Overcooked! 2 be enabled for launch testing.
