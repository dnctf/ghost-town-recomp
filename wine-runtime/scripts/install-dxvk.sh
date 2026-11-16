#!/bin/sh
set -eu
: "${WINEPREFIX:?WINEPREFIX must point to this title's private prefix}"
: "${WINE:?WINE must point to the guest Wine executable}"
: "${DXVK_DIR:?DXVK_DIR must point to the pinned DXVK release directory}"
[ -s "$DXVK_DIR/x64/d3d11.dll" ] || { echo "DXVK x64 d3d11.dll missing" >&2; exit 1; }
[ -s "$DXVK_DIR/x64/dxgi.dll" ] || { echo "DXVK x64 dxgi.dll missing" >&2; exit 1; }
# DXVK setup is per-prefix: copy x64 DLLs and register native overrides.
# The x86_64-only game profile deliberately does not install x32 DLLs.
SYSTEM32="$WINEPREFIX/drive_c/windows/system32"
mkdir -p "$SYSTEM32"
install -m 0644 "$DXVK_DIR/x64/d3d11.dll" "$SYSTEM32/d3d11.dll"
install -m 0644 "$DXVK_DIR/x64/dxgi.dll" "$SYSTEM32/dxgi.dll"
"$WINE" reg add 'HKCU\\Software\\Wine\\DllOverrides' /v d3d11 /d native /f
"$WINE" reg add 'HKCU\\Software\\Wine\\DllOverrides' /v dxgi /d native /f
