#!/usr/bin/env bash
set -Eeuo pipefail
ROOTFS="$1"; LOG_DIR="$2"; D_TIMEOUT="${3:-180}"
mkdir -p "$LOG_DIR/d-snapshots"
ROOTFS="$(realpath "$ROOTFS")"
case "$D_TIMEOUT" in 180|600) ;; *) echo "Unsupported wineboot timeout: $D_TIMEOUT" >&2; exit 64 ;; esac
cleanup_mounts() {
  set +e
  { echo "### mounts before cleanup"; findmnt -R "$ROOTFS" || true; } >> "$LOG_DIR/mounts.log" 2>&1
  sudo umount "$ROOTFS/sys" 2>/dev/null || true
  sudo umount "$ROOTFS/proc" 2>/dev/null || true
  sudo umount "$ROOTFS/dev/pts" 2>/dev/null || true
  sudo umount "$ROOTFS/dev" 2>/dev/null || true
  { echo "### mounts after cleanup"; findmnt -R "$ROOTFS" || true; } >> "$LOG_DIR/mounts.log" 2>&1
}
trap cleanup_mounts EXIT
test -x "$ROOTFS/usr/local/bin/box64"
sudo mkdir -p "$ROOTFS/proc" "$ROOTFS/dev/pts" "$ROOTFS/sys"
sudo mount --bind /dev "$ROOTFS/dev"
sudo mount --bind /dev/pts "$ROOTFS/dev/pts"
sudo mount -t proc proc "$ROOTFS/proc"
sudo mount -t sysfs sysfs "$ROOTFS/sys"
for mount_path in "$ROOTFS/dev" "$ROOTFS/dev/pts" "$ROOTFS/proc" "$ROOTFS/sys"; do
  mountpoint -q "$mount_path" || { echo "[FAIL] required chroot mount is missing: $mount_path" | tee "$LOG_DIR/mounts.log"; exit 1; }
done
{ echo "### active chroot mounts"; findmnt -R "$ROOTFS" || true; echo "### required mountpoint assertions"; for mount_path in "$ROOTFS/dev" "$ROOTFS/dev/pts" "$ROOTFS/proc" "$ROOTFS/sys"; do mountpoint "$mount_path"; done; } | tee "$LOG_DIR/mounts.log"
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
  { echo "[RUN] rung=$rung timeout_seconds=$limit"; echo "environment: BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all"; printf 'command='; printf '%q ' "$@"; echo; } | tee "$log"
  set +e
  timeout --signal=TERM --kill-after=10s "$limit"s sudo chroot "$ROOTFS" env \
    BOX64_LD_LIBRARY_PATH=/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu:/opt/wine/lib64:/opt/wine/lib \
    BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all "$@" >> "$log" 2>&1
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
  local label="$1" file="$LOG_DIR/d-snapshots/${label}.txt"
  {
    echo "snapshot=$label"; echo "timestamp_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "wineboot_log_bytes=$(stat -c '%s' "$LOG_DIR/d-wineboot.log" 2>/dev/null || echo 0)"
    echo "box64_log_bytes=$(stat -c '%s' "$LOG_DIR/d-box64.log" 2>/dev/null || echo 0)"
    echo "### ps -eo pid,ppid,stat,pcpu,pmem,etime,args --forest"
    ps -eo pid,ppid,stat,pcpu,pmem,etime,args --forest
    echo "### ls -la rootfs/tmp"; sudo ls -la "$ROOTFS/tmp" 2>&1 || true
    echo "### ls -la rootfs/tmp/.wine-*"; sudo find "$ROOTFS/tmp" -maxdepth 1 -mindepth 1 -name '.wine-*' -exec ls -lad {} \; 2>&1 || true
    echo "### process status and wchan"
    local pids pid
    pids="$(ps -eo pid=,pgid=,comm= | awk -v group="$D_PGID" '$2 == group || $3 ~ /^(box64|wine|wineserver)$/ {print $1}' | sort -nu)"
    for pid in $pids; do echo "### PID $pid"; cat "/proc/$pid/status" 2>&1 || true; printf 'wchan='; cat "/proc/$pid/wchan" 2>&1 || true; echo; done
  } > "$file"
  grep -E 'wineserver|box64|wineboot|wine ' "$file" > "$LOG_DIR/d-snapshots/${label}-wine-processes.txt" || true
}

run_wineboot_d() {
  local limit="$1" log="$LOG_DIR/d-wineboot.log" start elapsed rc=0 timed_out=0
  if [ -z "$WINEBOOT_ENTRY" ] || [ -z "$WINESERVER_ENTRY" ]; then echo "[FAIL] d-wineboot: missing executable wineboot or wineserver entry." | tee "$LOG_DIR/STOPPED_AT.txt"; exit 1; fi
  sudo chmod 1777 "$ROOTFS/tmp"
  sudo rm -rf "$ROOTFS/tmp/ghost-town-wine-smoke-prefix" "$ROOTFS/tmp/ghost-town-wine-smoke-home"
  sudo mkdir -p "$ROOTFS/tmp/ghost-town-wine-smoke-home"
  sudo chroot "$ROOTFS" env HOME=/tmp/ghost-town-wine-smoke-home USER=wine-smoke WINEARCH=win64 WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix sh -c 'test -w /tmp && mkdir -p "$HOME" && test -w "$HOME"'
  {
    echo "[RUN] rung=d-wineboot timeout_seconds=$limit"
    echo "timestamp_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "environment: BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all,+timestamp,+process HOME=/tmp/ghost-town-wine-smoke-home USER=wine-smoke WINEARCH=win64 WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix TMPDIR=/tmp DISPLAY=<unset> WAYLAND_DISPLAY=<unset>"
    echo "command=sudo chroot ROOTFS env ... box64 $WINEBOOT_ENTRY"
  } | tee "$log"
  setsid --wait bash -o pipefail -c '
    rootfs="$1"; entry="$2"; logfile="$3"; pidfile="$4"
    printf "%s\\n" "$" > "$pidfile"
    sudo chroot "$rootfs" env \
      BOX64_LD_LIBRARY_PATH=/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu:/opt/wine/lib64:/opt/wine/lib \
      BOX64_LOG=1 BOX64_SHOWSEGV=1 WINEDEBUG=err+all,+timestamp,+process \
      HOME=/tmp/ghost-town-wine-smoke-home USER=wine-smoke TMPDIR=/tmp \
      WINEARCH=win64 WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix WINEDLLOVERRIDES=mscoree,mshtml= \
      DISPLAY= WAYLAND_DISPLAY= /usr/local/bin/box64 "$entry" 2>&1 |
      while IFS= read -r line; do printf "[%s] %s\n" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$line"; done >> "$logfile"
  ' _ "$ROOTFS" "$WINEBOOT_ENTRY" "$log" "$LOG_DIR/d-process.pid" &
  D_SUPERVISOR_PID=$!
  for attempt in 1 2 3 4 5 6 7 8 9 10; do [ -s "$LOG_DIR/d-process.pid" ] && break; sleep 0.1; done
  if [ ! -s "$LOG_DIR/d-process.pid" ]; then echo "[FAIL] wineboot diagnostic worker did not publish PID" | tee "$LOG_DIR/STOPPED_AT.txt"; exit 1; fi
  D_PID="$(cat "$LOG_DIR/d-process.pid")"
  D_PGID="$(ps -o pgid= -p "$D_PID" | tr -d ' ')"
  if [ "$D_PGID" != "$D_PID" ]; then echo "[FAIL] isolated wineboot process group mismatch pid=$D_PID pgid=$D_PGID" | tee "$LOG_DIR/STOPPED_AT.txt"; exit 1; fi
  echo "host_pid=$D_PID process_group=$D_PGID" | tee -a "$log"
  start="$(date +%s)"
  for checkpoint in 15 45 90 180 600; do
    [ "$checkpoint" -le "$limit" ] || continue
    while kill -0 "$D_PID" 2>/dev/null; do elapsed=$(( $(date +%s) - start )); [ "$elapsed" -ge "$checkpoint" ] && break; sleep 1; done
    if kill -0 "$D_PID" 2>/dev/null; then snapshot_d "t${checkpoint}s"; else break; fi
  done
  if kill -0 "$D_PID" 2>/dev/null; then
    timed_out=1; echo "[TIMEOUT] rung=d-wineboot elapsed_seconds=$limit" | tee -a "$log"
    {
      echo "[CLEANUP] wineserver -k via Box64 timestamp_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
      timeout --signal=TERM --kill-after=3s 15s sudo chroot "$ROOTFS" env \
        BOX64_LD_LIBRARY_PATH=/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu:/opt/wine/lib64:/opt/wine/lib \
        BOX64_LOG=1 BOX64_SHOWSEGV=1 HOME=/tmp/ghost-town-wine-smoke-home USER=wine-smoke \
        WINEARCH=win64 WINEPREFIX=/tmp/ghost-town-wine-smoke-prefix /usr/local/bin/box64 "$WINESERVER_ENTRY" -k
      echo "wineserver_kill_exit_code=$?"
    } > "$LOG_DIR/d-wineserver-kill.log" 2>&1 || true
    echo "[CLEANUP] terminate remaining diagnostic process group=$D_PGID" | tee -a "$log"
    kill -TERM -- "-$D_PGID" 2>/dev/null || true; sleep 3; kill -KILL -- "-$D_PGID" 2>/dev/null || true
  fi
  set +e; wait "$D_SUPERVISOR_PID"; rc=$?; set -e
  [ "$timed_out" -eq 0 ] || rc=124
  snapshot_d final
  grep -E '\[BOX64\]' "$log" > "$LOG_DIR/d-box64.log" || true
  grep -E 'wineserver|wineboot|wine ' "$LOG_DIR/d-snapshots"/*-wine-processes.txt > "$LOG_DIR/d-wineserver-processes.log" || true
  elapsed=$(( $(date +%s) - start ))
  { echo "exit_code=$rc"; echo "duration_seconds=$elapsed"; echo "timeout_seconds=$limit"; echo "box64_log_lines=$(wc -l < "$LOG_DIR/d-box64.log")"; echo "process_snapshot_files=$(find "$LOG_DIR/d-snapshots" -maxdepth 1 -name '*.txt' | wc -l)"; } | tee -a "$log"
  if [ "$rc" -ne 0 ]; then echo "[FAIL] d-wineboot exit_code=$rc duration_seconds=$elapsed" | tee "$LOG_DIR/STOPPED_AT.txt"; exit "$rc"; fi
  echo "[PASS] d-wineboot duration_seconds=$elapsed" | tee -a "$LOG_DIR/PASSED.txt"
}
ASSERT_OUTPUT_REGEX=''
run_rung a-box64-version 30 /usr/local/bin/box64 --version
run_rung b-x86_64-utility 30 /usr/local/bin/box64 /opt/wine/bin/wineserver --version
ASSERT_OUTPUT_REGEX='^wine-[0-9]'
run_rung c-wine-version 60 /usr/local/bin/box64 "$WINE_ENTRY" --version
ASSERT_OUTPUT_REGEX=''
run_wineboot_d "$D_TIMEOUT"
run_rung e-wine-cmd-ver 180 /usr/local/bin/box64 "$WINE_ENTRY" cmd /c ver
echo "[PASS] all Wine smoke rungs a-e" | tee "$LOG_DIR/ALL-PASSED.txt"
