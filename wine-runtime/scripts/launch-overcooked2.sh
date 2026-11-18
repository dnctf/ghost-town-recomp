#!/bin/sh
set -eu
: "${WINEPREFIX:?WINEPREFIX must point to this title's private prefix}"
: "${WINE:?WINE must point to the guest Wine executable}"
: "${GAME_EXE:?GAME_EXE must point to the user-owned Overcooked2.exe}"
[ -f "$GAME_EXE" ] || { echo "User game executable not found: $GAME_EXE" >&2; exit 2; }
[ -s "$WINEPREFIX/system.reg" ] || { echo "Wine prefix is not initialized" >&2; exit 3; }
# Always record why a launch is refused; the graphics gate remains closed until
# guest Vulkan + DXVK + presentation have passed device-level tests.
LOG_DIR="${GHOST_TOWN_LOG_DIR:-$WINEPREFIX/logs}"
mkdir -p "$LOG_DIR"
export WINEPREFIX WINEDEBUG="${WINEDEBUG:--all}"
export BOX64_LD_LIBRARY_PATH="${BOX64_LD_LIBRARY_PATH:-/usr/lib/x86_64-linux-gnu:/lib/x86_64-linux-gnu:/opt/wine/lib/wine/x86_64-unix:/opt/wine/lib64:/opt/wine/lib}"
export BOX64_PRELOAD="${BOX64_PRELOAD:-libmd.so.0:libbsd.so.0}"
export DXVK_CONFIG_FILE="${DXVK_CONFIG_FILE:-$WINEPREFIX/dxvk.conf}"
export DXVK_LOG_LEVEL="${DXVK_LOG_LEVEL:-info}"
export DXVK_LOG_PATH="${DXVK_LOG_PATH:-$LOG_DIR/dxvk}"
export VK_LOADER_DEBUG="${VK_LOADER_DEBUG:-error,warn}"
{
  echo "=== Ghost Town Wine/DXVK launch request ==="
  date -Iseconds 2>/dev/null || date
  echo "WINE=$WINE"
  echo "GAME_EXE=$GAME_EXE"
  echo "WINEPREFIX=$WINEPREFIX"
  echo "DXVK_LOG_PATH=$DXVK_LOG_PATH"
  echo "VK_ICD_FILENAMES=${VK_ICD_FILENAMES:-<unset>}"
  echo "VK_DRIVER_FILES=${VK_DRIVER_FILES:-<unset>}"
  echo "GHOST_TOWN_DISPLAY_READY=${GHOST_TOWN_DISPLAY_READY:-0}"
} >> "$LOG_DIR/graphics-session.log" 2>&1
[ "${GHOST_TOWN_DISPLAY_READY:-0}" = 1 ] || {
  echo "Launch blocked: Wine guest Vulkan + DXVK + Android presentation bridge not validated." >> "$LOG_DIR/graphics-session.log"
  exit 4
}
exec "$WINE" "$GAME_EXE" >> "$LOG_DIR/wine-game.log" 2>&1
