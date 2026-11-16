#!/usr/bin/env bash
set -euo pipefail
if [[ $# -lt 1 ]]; then
  echo "Usage: $0 /path/to/app-debug.apk [output-log.txt]" >&2
  exit 2
fi
APK="$1"
OUT="${2:-ghost-town-device-smoke.log}"
PACKAGE="com.overcookedrecomp.app"
[[ -f "$APK" ]] || { echo "APK not found: $APK" >&2; exit 2; }
command -v adb >/dev/null || { echo "adb is required (Android platform-tools)." >&2; exit 2; }
adb get-state >/dev/null
echo "[1/4] Installing APK"
adb install -r "$APK"
echo "[2/4] Clearing old logcat"
adb logcat -c
echo "[3/4] Launching launcher activity"
adb shell monkey -p "$PACKAGE" 1
echo "[4/4] Capturing startup logcat"
sleep 8
adb logcat -d -v threadtime > "$OUT"
echo "Saved $OUT"
echo "Relevant runtime lines:"
grep -Ei 'OvercookedRecomp|GameStorageVerifier|AndroidRuntime|FATAL EXCEPTION|linker|vulkan' "$OUT" | tail -n 120 || true
echo "NOTE: This verifies APK install/launcher startup only, not Windows game execution or gameplay."
