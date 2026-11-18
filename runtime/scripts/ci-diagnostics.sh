#!/usr/bin/env bash
# Shared CI diagnostics preamble for runtime build stages.
set -Eeuo pipefail
RUNTIME_STAGE="${1:-runtime-stage}"
RUNTIME_LOG_DIR="${GITHUB_WORKSPACE:-$PWD}/logs"
mkdir -p "$RUNTIME_LOG_DIR"
RUNTIME_LOG_FILE="$RUNTIME_LOG_DIR/$RUNTIME_STAGE.log"
exec > >(tee -a "$RUNTIME_LOG_FILE") 2>&1
runtime_error_trap() {
  local status=$?
  local line="${BASH_LINENO[0]:-unknown}"
  local command="${BASH_COMMAND:-unknown}"
  trap - ERR
  set +e
  printf '\n[ERROR] stage=%s line=%s exit_code=%s command=%s\n' "$RUNTIME_STAGE" "$line" "$status" "$command" >&2
  printf '[ERROR] Last 50 lines from %s:\n' "$RUNTIME_LOG_FILE" >&2
  tail -n 50 "$RUNTIME_LOG_FILE" >&2
  exit "$status"
}
trap runtime_error_trap ERR
export PS4='+ ${BASH_SOURCE##*/}:${LINENO}:${FUNCNAME[0]:-main}: '
set -x
