#!/bin/sh
set -eu
: "${WINEPREFIX:?WINEPREFIX must point to this title's private prefix}"
: "${WINE:?WINE must point to the guest Wine executable}"
mkdir -p "$WINEPREFIX"
export WINEPREFIX WINEARCH=win64
export WINEDEBUG="${WINEDEBUG:--all}"
"$WINE" wineboot --init
# Wait for Wine's registry to become durable before reporting success.
for attempt in $(seq 1 60); do
  [ -s "$WINEPREFIX/system.reg" ] && exit 0
  sleep 1
done
echo "Wine prefix initialization timed out: system.reg was not created" >&2
exit 1
