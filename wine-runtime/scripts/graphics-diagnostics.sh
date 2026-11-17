#!/bin/sh
set -eu
: "${WINEPREFIX:?WINEPREFIX must point to this title's private prefix}"
: "${ROOTFS:?ROOTFS must point to the guest rootfs}"
LOG_DIR="${GHOST_TOWN_LOG_DIR:-$WINEPREFIX/logs}"
mkdir -p "$LOG_DIR"
OUT="$LOG_DIR/guest-vulkan-diagnostics.log"
{
  echo "=== Guest Vulkan / DXVK diagnostics ==="
  date -Iseconds 2>/dev/null || date
  echo "uname: $(uname -a)"
  echo "VK_ICD_FILENAMES: ${VK_ICD_FILENAMES:-<unset>}"
  echo "VK_DRIVER_FILES: ${VK_DRIVER_FILES:-<unset>}"
  echo "VK_LOADER_DEBUG: ${VK_LOADER_DEBUG:-<unset>}"
  echo "--- ICD manifests ---"
  find "$ROOTFS/usr/share/vulkan/icd.d" "$ROOTFS/etc/vulkan/icd.d" -maxdepth 1 -type f -name '*.json' -print 2>/dev/null || true
  echo "--- Vulkan loader / Turnip libraries ---"
  find "$ROOTFS/usr/lib" "$ROOTFS/usr/local/lib" -type f \\( -name 'libvulkan.so*' -o -name '*freedreno*.so' \\) -print 2>/dev/null | head -80 || true
  echo "--- DXVK payload ---"
  ls -l "$ROOTFS/opt/dxvk/x64/d3d11.dll" "$ROOTFS/opt/dxvk/x64/dxgi.dll" 2>&1 || true
  if command -v vulkaninfo >/dev/null 2>&1; then vulkaninfo --summary 2>&1 || true
  else echo "vulkaninfo not installed in guest; no Vulkan device enumeration performed."; fi
} > "$OUT"
cat "$OUT"
