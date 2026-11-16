#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then echo "Usage: $0 <dxvk-release-directory> <wine-prefix>" >&2; exit 2; fi
DXVK_DIR="$(cd "$1" && pwd)"
WINEPREFIX="$(mkdir -p "$2" && cd "$2" && pwd)"
export WINEPREFIX
[[ -d "$DXVK_DIR/x64" && -d "$DXVK_DIR/x32" ]] || { echo "DXVK archive must contain x64/ and x32/." >&2; exit 1; }
[[ -d "$WINEPREFIX/drive_c/windows/system32" ]] || { echo "Wine prefix is not initialized." >&2; exit 1; }
[[ -d "$WINEPREFIX/drive_c/windows/syswow64" ]] || { echo "Wine prefix lacks syswow64; use a 64-bit WoW64 prefix." >&2; exit 1; }
install -m 0644 "$DXVK_DIR"/x64/*.dll "$WINEPREFIX/drive_c/windows/system32/"
install -m 0644 "$DXVK_DIR"/x32/*.dll "$WINEPREFIX/drive_c/windows/syswow64/"
if [[ -x "$DXVK_DIR/setup_dxvk.sh" ]]; then "$DXVK_DIR/setup_dxvk.sh" install; fi
echo "DXVK DLLs copied. Verify native overrides for d3d8,d3d9,d3d10core,d3d11,dxgi with winecfg."
echo "This does not install Wine, Box64, a Linux rootfs, or graphics drivers."
