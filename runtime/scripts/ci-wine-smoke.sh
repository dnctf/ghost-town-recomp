#!/usr/bin/env bash
set -Eeuo pipefail
ROOTFS="$1"; LOG_DIR="$2"; D_TIMEOUT="${3:-180}"
mkdir -p "$LOG_DIR/d-snapshots"
ROOTFS="$(realpath "$ROOTFS")"
case "$D_TIMEOUT" in 180|600) ;; *) echo "Unsupported wineboot timeout: $D_TIMEOUT" >&2; exit 64 ;; esac
cleanup_mounts() {
  set +e
  if [ -n "${LOG_TAIL_PID:-}" ]; then kill -TERM "$LOG_TAIL_PID" 2>/dev/null || true; wait "$LOG_TAIL_PID" 2>/dev/null || true; fi
  if [ -n "${WINESERVER_ENTRY:-}" ] && [ -x "$ROOTFS/usr/local/bin/box64" ]; then
    timeout --signal=TERM --kill-after=3s 8s sudo chroot "$ROOTFS" /usr/bin/env \
      BOX64_LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:/lib/x86_64-linux-gnu:/opt/wine/lib/wine/x86_64-unix:/opt/wine/lib64:/opt/wine/lib \
      BOX64_EMULATED_LIBS=libbsd.so.0:libmd.so.0 BOX64_PRELOAD=libmd.so.0:libbsd.so.0 \
      HOME=/tmp/ghost-town-wine-smoke-home USER=wine-smoke \
      WINEARCH=win64 WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix \
      /usr/local/bin/box64 "$WINESERVER_ENTRY" -k >> "$LOG_DIR/cleanup-wineserver.log" 2>&1
  fi
  sudo rm -rf "$ROOTFS/tmp/ghost-town-wine-smoke-prefix" "$ROOTFS/tmp/ghost-town-wine-smoke-home"
  { echo "### mounts before cleanup"; findmnt -R "$ROOTFS" || true; } >> "$LOG_DIR/mounts.log" 2>&1
  sudo umount -lf "$ROOTFS/tmp/.X11-unix" 2>/dev/null || true
  sudo umount -lf "$ROOTFS/sys" 2>/dev/null || true
  sudo umount -lf "$ROOTFS/proc" 2>/dev/null || true
  sudo umount -lf "$ROOTFS/dev/pts" 2>/dev/null || true
  sudo umount -lf "$ROOTFS/dev" 2>/dev/null || true
  if [ -n "${XVFB_PID:-}" ]; then kill -TERM "$XVFB_PID" 2>/dev/null || true; wait "$XVFB_PID" 2>/dev/null || true; fi
  { echo "### mounts after cleanup"; findmnt -R "$ROOTFS" || true; } >> "$LOG_DIR/mounts.log" 2>&1
}
trap cleanup_mounts EXIT
XVFB_PID=""
LOG_TAIL_PID=""
test -x "$ROOTFS/usr/local/bin/box64"
sudo test -e "$ROOTFS/usr/lib/x86_64-linux-gnu/libbsd.so.0"
sudo test -e "$ROOTFS/usr/lib/x86_64-linux-gnu/libmd.so.0"
command -v Xvfb >/dev/null 2>&1 || { echo "[FAIL] Xvfb missing on CI host" >&2; exit 1; }
Xvfb :99 -screen 0 1024x768x24 -nolisten tcp -ac > "$LOG_DIR/xvfb.log" 2>&1 &
XVFB_PID=$!
for attempt in $(seq 1 50); do [ -S /tmp/.X11-unix/X99 ] && break; kill -0 "$XVFB_PID" 2>/dev/null || { cat "$LOG_DIR/xvfb.log" >&2; exit 1; }; sleep 0.2; done
test -S /tmp/.X11-unix/X99 || { echo "[FAIL] Xvfb socket :99 missing" >&2; exit 1; }
sudo mkdir -p "$ROOTFS/proc" "$ROOTFS/dev/pts" "$ROOTFS/sys" "$ROOTFS/tmp/.X11-unix"
sudo mount --bind /dev "$ROOTFS/dev"
sudo mount --bind /dev/pts "$ROOTFS/dev/pts"
sudo mount -t proc proc "$ROOTFS/proc"
sudo mount -t sysfs sysfs "$ROOTFS/sys"
sudo mount --bind /tmp/.X11-unix "$ROOTFS/tmp/.X11-unix"
for mount_path in "$ROOTFS/dev" "$ROOTFS/dev/pts" "$ROOTFS/proc" "$ROOTFS/sys" "$ROOTFS/tmp/.X11-unix"; do
  mountpoint -q "$mount_path" || { echo "[FAIL] required chroot mount is missing: $mount_path" | tee "$LOG_DIR/mounts.log"; exit 1; }
done
{ echo "### active chroot mounts"; findmnt -R "$ROOTFS" || true; echo "### required mountpoint assertions"; for mount_path in "$ROOTFS/dev" "$ROOTFS/dev/pts" "$ROOTFS/proc" "$ROOTFS/sys" "$ROOTFS/tmp/.X11-unix"; do mountpoint "$mount_path"; done; echo "DISPLAY=:99"; ps -p "$XVFB_PID" -o pid,pgid,stat,args; } | tee "$LOG_DIR/mounts.log"
# Keep the guest dynamic-loader search configuration explicit for multiarch x86_64 payloads.
sudo install -d "$ROOTFS/etc/ld.so.conf.d"
sudo tee "$ROOTFS/etc/ld.so.conf.d/ghost-town-x86_64.conf" >/dev/null <<'EOF'
/lib/x86_64-linux-gnu
/usr/lib/x86_64-linux-gnu
/opt/wine/lib
/opt/wine/lib64
/opt/wine/lib/wine/x86_64-unix
EOF
if [ -x "$ROOTFS/sbin/ldconfig" ]; then sudo chroot "$ROOTFS" /sbin/ldconfig; fi

find_entry() {
  local name="$1" candidate
  candidate="$(find "$ROOTFS/opt/wine/bin" -maxdepth 1 \( -type f -o -type l \) -name "$name" -print -quit)"
  if [ -n "$candidate" ] && [ -x "$candidate" ]; then printf '/opt/wine/bin/%s' "$name"; return 0; fi
  return 1
}
WINE_ENTRY="$(find_entry wine || find_entry wine64 || true)"
WINEBOOT_ENTRY="$(find_entry wineboot || true)"
WINESERVER_ENTRY="$(find_entry wineserver || true)"
if [ -z "$WINE_ENTRY" ]; then echo "[FAIL] No executable wine or wine64 entry point found." | tee "$LOG_DIR/00-entrypoint.log"; sudo ls -la "$ROOTFS/opt/wine/bin" >> "$LOG_DIR/00-entrypoint.log" 2>&1 || true; exit 1; fi
{ echo "Selected Wine entry: $WINE_ENTRY"; echo "Selected wineboot entry: $WINEBOOT_ENTRY"; echo "Selected wineserver entry: $WINESERVER_ENTRY"; sudo ls -la "$ROOTFS/opt/wine/bin"; } > "$LOG_DIR/00-entrypoint.log"

run_rung() {
  local rung="$1" limit="$2"; shift 2
  local log="$LOG_DIR/$rung.log" rc=0 start end elapsed
  start="$(date +%s)"
  {
    echo "[RUN] rung=$rung timeout_seconds=$limit"
    echo "environment: DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe FONTCONFIG_FILE=/etc/fonts/fonts.conf BOX64_LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:/lib/x86_64-linux-gnu:/opt/wine/lib/wine/x86_64-unix:/opt/wine/lib64:/opt/wine/lib BOX64_EMULATED_LIBS=libbsd.so.0:libmd.so.0 BOX64_PRELOAD=libmd.so.0:libbsd.so.0"
    printf 'command='; printf '%q ' "$@"; echo
  } | tee "$log"
  set +e
  timeout --signal=TERM --kill-after=10s "$limit"s sudo chroot "$ROOTFS" /usr/bin/env \
    BOX64_LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:/lib/x86_64-linux-gnu:/opt/wine/lib/wine/x86_64-unix:/opt/wine/lib64:/opt/wine/lib \
    BOX64_EMULATED_LIBS=libbsd.so.0:libmd.so.0 BOX64_PRELOAD=libmd.so.0:libbsd.so.0 \
    BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all \
    DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe \
    FONTCONFIG_FILE=/etc/fonts/fonts.conf WAYLAND_DISPLAY= \
    WINEDLLOVERRIDES=mscoree,mshtml= \
    HOME=/tmp/ghost-town-wine-smoke-home USER=wine-smoke TMPDIR=/tmp \
    WINEARCH=win64 WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix "$@" >> "$log" 2>&1
  rc=$?
  set -e
  end="$(date +%s)"; elapsed=$((end-start))
  if [ "$rc" -eq 0 ] && [ -n "$ASSERT_OUTPUT_REGEX" ] && ! grep -Eq "$ASSERT_OUTPUT_REGEX" "$log"; then
    echo "[FAIL] rung=$rung expected_output_regex=$ASSERT_OUTPUT_REGEX not_found" | tee -a "$log"; rc=65
  fi
  { echo "exit_code=$rc"; echo "duration_seconds=$elapsed"; if [ "$rc" -eq 124 ] || [ "$rc" -eq 137 ]; then echo "[TIMEOUT] rung=$rung limit_seconds=$limit"; fi; } | tee -a "$log"
  if [ "$rc" -ne 0 ]; then echo "[FAIL] $rung exit_code=$rc duration_seconds=$elapsed" | tee "$LOG_DIR/STOPPED_AT.txt"; exit "$rc"; fi
  echo "[PASS] $rung duration_seconds=$elapsed" | tee -a "$LOG_DIR/PASSED.txt"
}
snapshot_d() {
  local label="$1"
  local file="$LOG_DIR/d-snapshots/${label}.txt"
  {
    echo "snapshot=$label"; echo "timestamp_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "wineboot_log_bytes=$(stat -c '%s' "$LOG_DIR/d-wineboot.log" 2>/dev/null || echo 0)"
    echo "box64_log_bytes=$(stat -c '%s' "$LOG_DIR/d-box64.log" 2>/dev/null || echo 0)"
    echo "### ps -eo pid,ppid,stat,pcpu,pmem,etime,args --forest"
    ps -eo pid,ppid,stat,pcpu,pmem,etime,args --forest
    echo "### ls -la rootfs/tmp"; sudo ls -la "$ROOTFS/tmp" 2>&1 || true
    echo "### ls -la rootfs/tmp/.wine-*"; sudo find "$ROOTFS/tmp" -maxdepth 1 -mindepth 1 -name '.wine-*' -exec ls -la {} \; 2>&1 || true
    echo "### process status and wchan"
    local pids pid
    pids="$(ps -eo pid=,pgid=,comm= | awk -v group="$D_PGID" '$2 == group || $3 ~ /^(box64|wine|wineserver)$/ {print $1}' | sort -nu)"
    for pid in $pids; do echo "### PID $pid"; cat "/proc/$pid/status" 2>&1 || true; printf 'wchan='; cat "/proc/$pid/wchan" 2>&1 || true; echo; done
  } > "$file"
  grep -E 'wineserver|box64|wineboot|wine ' "$file" > "$LOG_DIR/d-snapshots/${label}-wine-processes.txt" || true
}

run_wineboot_d() {
  local limit="$1" log="$LOG_DIR/d-wineboot.log" start elapsed rc=0
  if [ -z "$WINEBOOT_ENTRY" ] || [ -z "$WINESERVER_ENTRY" ]; then echo "[FAIL] d-wineboot: missing executable wineboot or wineserver entry." | tee "$LOG_DIR/STOPPED_AT.txt"; exit 1; fi
  sudo chmod 1777 "$ROOTFS/tmp"
  sudo rm -rf "$ROOTFS/tmp/ghost-town-wine-smoke-prefix" "$ROOTFS/tmp/ghost-town-wine-smoke-home"
  sudo mkdir -p "$ROOTFS/tmp/ghost-town-wine-smoke-home"
  local -a guest_env=(
    BOX64_LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:/lib/x86_64-linux-gnu:/opt/wine/lib/wine/x86_64-unix:/opt/wine/lib64:/opt/wine/lib
    BOX64_EMULATED_LIBS=libbsd.so.0:libmd.so.0 BOX64_PRELOAD=libmd.so.0:libbsd.so.0
    BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all,+timestamp,+process
    DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe
    FONTCONFIG_FILE=/etc/fonts/fonts.conf WAYLAND_DISPLAY=
    WINEDLLOVERRIDES=mscoree,mshtml=
    HOME=/tmp/ghost-town-wine-smoke-home USER=wine-smoke TMPDIR=/tmp
    WINEARCH=win64 WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix
  )
  {
    echo "[RUN] rung=d-wineboot timeout_seconds=$limit"
    echo "timestamp_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "environment: DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe FONTCONFIG_FILE=/etc/fonts/fonts.conf BOX64_LD_LIBRARY_PATH=${guest_env[0]#BOX64_LD_LIBRARY_PATH=} BOX64_EMULATED_LIBS=libbsd.so.0:libmd.so.0 BOX64_PRELOAD=libmd.so.0:libbsd.so.0 WINEDLLOVERRIDES=mscoree,mshtml= WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix"
    echo "command=sudo chroot ROOTFS /usr/bin/env ... box64 wineboot --init; box64 wineserver -w"
  } | tee "$log"
  sudo chroot "$ROOTFS" /usr/bin/env "${guest_env[@]}" /bin/sh -c '
    printf "CHROOT_ENV DISPLAY=%s\\n" "$DISPLAY"
    printf "CHROOT_ENV BOX64_LD_LIBRARY_PATH=%s\\n" "$BOX64_LD_LIBRARY_PATH"
    printf "CHROOT_ENV BOX64_PRELOAD=%s\\n" "$BOX64_PRELOAD"
    printf "CHROOT_ENV GALLIUM_DRIVER=%s\\n" "$GALLIUM_DRIVER"
    test "$DISPLAY" = ":99"
    test -n "$BOX64_LD_LIBRARY_PATH"
    test -S /tmp/.X11-unix/X99
  ' | tee -a "$log"
  echo "WINEBOOT_LIMIT_SECONDS=90 WINESERVER_WAIT_LIMIT_SECONDS=45" | tee -a "$log"
  start="$(date +%s)"
  tail -n 80 -F "$log" &
  LOG_TAIL_PID=$!
  set +e
  timeout --signal=TERM --kill-after=10s "${limit}s" sudo chroot "$ROOTFS" /usr/bin/env "${guest_env[@]}" /bin/sh -c '
    box64="$1"; wineboot_entry="$2"; wineserver_entry="$3"
    echo "[GUEST] wineboot --init starting"
    /usr/bin/timeout --signal=TERM --kill-after=5s 90s "$box64" "$wineboot_entry" --init
    wineboot_rc=$?
    echo "[GUEST] wineboot --init exit_code=$wineboot_rc"
    [ "$wineboot_rc" -eq 0 ] || exit "$wineboot_rc"
    echo "[GUEST] wineserver -w starting"
    /usr/bin/timeout --signal=TERM --kill-after=5s 45s "$box64" "$wineserver_entry" -w
    wait_rc=$?
    echo "[GUEST] wineserver -w exit_code=$wait_rc"
    [ "$wait_rc" -eq 0 ] || exit "$wait_rc"
    test -s "$WINEPREFIX/system.reg"
    test -s "$WINEPREFIX/user.reg"
  ' _ /usr/local/bin/box64 "$WINEBOOT_ENTRY" "$WINESERVER_ENTRY" >> "$log" 2>&1
  rc=$?
  set -e
  if [ -n "$LOG_TAIL_PID" ]; then kill -TERM "$LOG_TAIL_PID" 2>/dev/null || true; wait "$LOG_TAIL_PID" 2>/dev/null || true; LOG_TAIL_PID=""; fi
  elapsed=$(( $(date +%s) - start ))
  if [ "$rc" -eq 0 ]; then
    for registry in system.reg user.reg; do
      if ! sudo test -s "$ROOTFS/tmp/ghost-town-wine-smoke-prefix/$registry"; then
        echo "[FAIL] missing or empty registry: $registry" | tee -a "$log"; rc=66
      else
        bytes="$(sudo stat -c %s "$ROOTFS/tmp/ghost-town-wine-smoke-prefix/$registry")"
        echo "[PASS] registry=$registry bytes=$bytes" | tee -a "$log"
      fi
    done
  fi
  timeout --signal=TERM --kill-after=3s 8s sudo chroot "$ROOTFS" /usr/bin/env "${guest_env[@]}" /usr/local/bin/box64 "$WINESERVER_ENTRY" -k >> "$LOG_DIR/d-wineserver-kill.log" 2>&1 || true
  grep -F '[BOX64]' "$log" > "$LOG_DIR/d-box64.log" || true
  if grep -Eq 'Symbol (strunvis|vis|strvis) not found' "$log"; then
    echo "[FAIL] libedit BSD symbols remain unresolved after Box64 preload" | tee -a "$log"
    rc=67
  fi
  { echo "exit_code=$rc"; echo "duration_seconds=$elapsed"; echo "timeout_seconds=$limit"; echo "box64_log_lines=$(wc -l < "$LOG_DIR/d-box64.log")"; } | tee -a "$log"
  if [ "$rc" -ne 0 ]; then
    if [ "$rc" -eq 124 ] || [ "$rc" -eq 137 ]; then echo "[TIMEOUT] rung=d-wineboot elapsed_seconds=$elapsed" | tee -a "$log"; fi
    echo "[FAIL] d-wineboot exit_code=$rc duration_seconds=$elapsed" | tee "$LOG_DIR/STOPPED_AT.txt"; exit "$rc"
  fi
  echo "[PASS] d-wineboot duration_seconds=$elapsed" | tee -a "$LOG_DIR/PASSED.txt"
}
ASSERT_OUTPUT_REGEX=''
run_rung a-box64-version 30 /usr/local/bin/box64 --version
run_rung b-x86_64-utility 30 /usr/local/bin/box64 /opt/wine/bin/wineserver --version
ASSERT_OUTPUT_REGEX='^wine-[0-9]'
run_rung c-wine-version 60 /usr/local/bin/box64 "$WINE_ENTRY" --version
ASSERT_OUTPUT_REGEX=''
run_wineboot_d "$D_TIMEOUT"
ASSERT_OUTPUT_REGEX='Microsoft Windows|Windows [0-9]'
run_rung e-wine-cmd-ver 180 /usr/local/bin/box64 "$WINE_ENTRY" cmd /c ver
echo "[PASS] all Wine smoke rungs a-e" | tee "$LOG_DIR/ALL-PASSED.txt"
