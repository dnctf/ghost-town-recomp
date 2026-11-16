#!/bin/sh
set -eu
: "${WINEPREFIX:?WINEPREFIX must point to this title's private prefix}"
: "${WINE:?WINE must point to the guest Wine executable}"
: "${GAME_EXE:?GAME_EXE must point to the user-owned Overcooked2.exe}"
[ -f "$GAME_EXE" ] || { echo "User game executable not found: $GAME_EXE" >&2; exit 2; }
[ -s "$WINEPREFIX/system.reg" ] || { echo "Wine prefix is not initialized" >&2; exit 3; }
# A real display/WSI bridge must be supplied by the host before enabling this script.
[ "${GHOST_TOWN_DISPLAY_READY:-0}" = 1 ] || {
  echo "Launch blocked: Wine display/Vulkan presentation backend has not passed device validation" >&2
  exit 4
}
export WINEPREFIX WINEDEBUG="${WINEDEBUG:--all}"
export DXVK_CONFIG_FILE="${DXVK_CONFIG_FILE:-$WINEPREFIX/dxvk.conf}"
exec "$WINE" "$GAME_EXE"
