#!/usr/bin/env bash
# Stage the built APK where the device can install it and print the exact command.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
  echo "FAIL: APK not built. Run: bash scripts/build.sh" >&2
  exit 1
fi

DEST="/sdcard/Download/sketchpilot-debug.apk"
if [ -d /sdcard/Download ]; then
  cp "$APK" "$DEST"
  echo "Staged: $DEST"
  echo "On the device (root shell):"
  echo "  pm install -r $DEST"
  echo "If not already root:"
  echo "  su -c 'pm install -r $DEST'"
else
  echo "No /sdcard/Download in this environment; copy $APK to the device and run:"
  echo "  pm install -r /sdcard/Download/sketchpilot-debug.apk"
fi

if command -v adb >/dev/null 2>&1; then
  DEVICES=$(adb devices 2>/dev/null | grep -c 'device$')
  if [ "$DEVICES" -gt 0 ]; then
    echo "adb device detected; you can also run:"
    echo "  adb install -r $APK"
  fi
fi
