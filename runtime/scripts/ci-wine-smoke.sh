#!/usr/bin/env bash
set -Eeuo pipefail
ROOTFS="${1:?usage: ci-wine-smoke.sh ROOTFS LOG_DIR}"
LOG_DIR="${2:?usage: ci-wine-smoke.sh ROOTFS LOG_DIR}"
mkdir -p "$LOG_DIR"
ROOTFS="$(realpath "$ROOTFS")"
test -x "$ROOTFS/usr/local/bin/box64"
find_entry() {
  local name="$1" candidate
  candidate="$(find "$ROOTFS/opt/wine/bin" -maxdepth 1 \( -type f -o -type l \) -name "$name" -print -quit)"
  if [ -n "$candidate" ] && [ -x "$candidate" ]; then printf '/opt/wine/bin/%s' "$name"; return 0; fi
  return 1
}
WINE_ENTRY="$(find_entry wine || find_entry wine64 || true)"
if [ -z "$WINE_ENTRY" ]; then
  echo "[FAIL] No executable wine or wine64 entry point found." | tee "$LOG_DIR/00-entrypoint.log"
  ls -la "$ROOTFS/opt/wine/bin" >> "$LOG_DIR/00-entrypoint.log" 2>&1 || true
  exit 1
fi
WINEBOOT_ENTRY="$(find_entry wineboot || true)"
{
  echo "Selected Wine entry: $WINE_ENTRY"
  echo "Selected wineboot entry: ${WINEBOOT_ENTRY:-missing}"
  ls -la "$ROOTFS/opt/wine/bin"
} > "$LOG_DIR/00-entrypoint.log"
run_rung() {
  local rung="$1"; shift
  local log="$LOG_DIR/$rung.log" rc=0
  echo "[RUN] $rung: $*"
  set +e
  sudo chroot "$ROOTFS" env \
    BOX64_LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:/opt/wine/lib64:/opt/wine/lib \
    BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all \
    "$@" > "$log" 2>&1
  rc=$?
  set -e
  cat "$log"
  printf 'exit_code=%s\n' "$rc" >> "$log"
  if [ "$rc" -ne 0 ]; then echo "[FAIL] $rung exit_code=$rc" | tee "$LOG_DIR/STOPPED_AT.txt"; exit "$rc"; fi
  echo "[PASS] $rung" | tee -a "$LOG_DIR/PASSED.txt"
}
run_rung a-box64-version /usr/local/bin/box64 --version
run_rung b-x86_64-utility /usr/local/bin/box64 /opt/wine/bin/wineserver --version
run_rung c-wine-version /usr/local/bin/box64 "$WINE_ENTRY" --version
if [ -z "$WINEBOOT_ENTRY" ]; then
  echo "[FAIL] d-wineboot: no executable wineboot entry." | tee "$LOG_DIR/STOPPED_AT.txt"
  exit 1
fi
run_rung d-wineboot env WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix WINEDLLOVERRIDES=mscoree,mshtml= /usr/local/bin/box64 "$WINEBOOT_ENTRY" -u
run_rung e-wine-cmd-ver /usr/local/bin/box64 "$WINE_ENTRY" cmd /c ver
echo "[PASS] all Wine smoke rungs a-e" | tee "$LOG_DIR/ALL-PASSED.txt"
