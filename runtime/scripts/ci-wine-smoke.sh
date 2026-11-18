#!/usr/bin/env bash
set -Eeuo pipefail
ROOTFS="$1"
LOG_DIR="$2"
mkdir -p "$LOG_DIR"
ROOTFS="$(realpath "$ROOTFS")"
MOUNTS_READY=1
cleanup_mounts() {
  set +e
  {
    echo "### mounts before cleanup"
    findmnt -R "$ROOTFS" || true
  } >> "$LOG_DIR/mounts.log" 2>&1
  sudo umount "$ROOTFS/sys" 2>/dev/null || true
  sudo umount "$ROOTFS/proc" 2>/dev/null || true
  sudo umount "$ROOTFS/dev/pts" 2>/dev/null || true
  sudo umount "$ROOTFS/dev" 2>/dev/null || true
  {
    echo "### mounts after cleanup"
    findmnt -R "$ROOTFS" || true
  } >> "$LOG_DIR/mounts.log" 2>&1
}
trap cleanup_mounts EXIT
test -x "$ROOTFS/usr/local/bin/box64"
sudo mkdir -p "$ROOTFS/proc" "$ROOTFS/dev/pts" "$ROOTFS/sys"
sudo mount --bind /dev "$ROOTFS/dev"
sudo mount --bind /dev/pts "$ROOTFS/dev/pts"
sudo mount -t proc proc "$ROOTFS/proc"
sudo mount -t sysfs sysfs "$ROOTFS/sys"
{
  echo "### active chroot mounts"
  findmnt -R "$ROOTFS"
} | tee "$LOG_DIR/mounts.log"
find_entry() {
  local name="$1" candidate
  candidate="$(find "$ROOTFS/opt/wine/bin" -maxdepth 1 \( -type f -o -type l \) -name "$name" -print -quit)"
  if [ -n "$candidate" ] && [ -x "$candidate" ]; then printf '/opt/wine/bin/%s' "$name"; return 0; fi
  return 1
}
WINE_ENTRY="$(find_entry wine || find_entry wine64 || true)"
if [ -z "$WINE_ENTRY" ]; then
  echo "[FAIL] No executable wine or wine64 entry point found." | tee "$LOG_DIR/00-entrypoint.log"
  sudo ls -la "$ROOTFS/opt/wine/bin" >> "$LOG_DIR/00-entrypoint.log" 2>&1 || true
  exit 1
fi
WINEBOOT_ENTRY="$(find_entry wineboot || true)"
{
  echo "Selected Wine entry: $WINE_ENTRY"
  echo "Selected wineboot entry: $WINEBOOT_ENTRY"
  sudo ls -la "$ROOTFS/opt/wine/bin"
} > "$LOG_DIR/00-entrypoint.log"
run_rung() {
  local rung="$1" limit="$2"; shift 2
  local log="$LOG_DIR/$rung.log" rc=0 start end elapsed
  start="$(date +%s)"
  {
    echo "[RUN] rung=$rung timeout_seconds=$limit"
    printf 'command='; printf '%q ' "$@"; echo
  } | tee "$log"
  set +e
  timeout --signal=TERM --kill-after=10s "$limit"s sudo chroot "$ROOTFS" env \
    BOX64_LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:/opt/wine/lib64:/opt/wine/lib \
    BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all "$@" >> "$log" 2>&1
  rc=$?
  set -e
  end="$(date +%s)"
  elapsed=$((end-start))
  {
    echo "exit_code=$rc"
    echo "duration_seconds=$elapsed"
    if [ "$rc" -eq 124 ] || [ "$rc" -eq 137 ]; then echo "[TIMEOUT] rung=$rung limit_seconds=$limit"; fi
  } | tee -a "$log"
  if [ "$rc" -ne 0 ]; then
    echo "[FAIL] $rung exit_code=$rc duration_seconds=$elapsed" | tee "$LOG_DIR/STOPPED_AT.txt"
    exit "$rc"
  fi
  echo "[PASS] $rung duration_seconds=$elapsed" | tee -a "$LOG_DIR/PASSED.txt"
}
run_rung a-box64-version 30 /usr/local/bin/box64 --version
run_rung b-x86_64-utility 30 /usr/local/bin/box64 /opt/wine/bin/wineserver --version
run_rung c-wine-version 60 /usr/local/bin/box64 "$WINE_ENTRY" --version
if [ -z "$WINEBOOT_ENTRY" ]; then
  echo "[FAIL] d-wineboot: no executable wineboot entry." | tee "$LOG_DIR/STOPPED_AT.txt"
  exit 1
fi
run_rung d-wineboot 180 env WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix WINEDLLOVERRIDES=mscoree,mshtml= /usr/local/bin/box64 "$WINEBOOT_ENTRY" -u
run_rung e-wine-cmd-ver 180 /usr/local/bin/box64 "$WINE_ENTRY" cmd /c ver
echo "[PASS] all Wine smoke rungs a-e" | tee "$LOG_DIR/ALL-PASSED.txt"
