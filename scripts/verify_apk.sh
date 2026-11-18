#!/usr/bin/env bash
# Verify LSPosed packaging of a built APK. Fails if metadata/assets are missing.
set -uo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
if [ ! -f "$APK" ]; then
  echo "FAIL: APK not found: $APK" >&2
  exit 1
fi

AAPT="${AAPT:-}"
if [ -z "$AAPT" ]; then
  for candidate in /opt/android-sdk/build-tools/34.0.0/aapt /opt/android-sdk/build-tools/35.0.0/aapt /opt/android-sdk/build-tools/36.0.0/aapt; do
    if [ -x "$candidate" ]; then AAPT="$candidate"; break; fi
  done
fi
if [ -z "$AAPT" ]; then
  echo "FAIL: aapt not found; set AAPT=/path/to/aapt" >&2
  exit 1
fi

fail=0
record() {
  local name="$1"
  local ok="$2"
  if [ "$ok" -eq 0 ]; then
    echo "  ok - $name"
  else
    echo "  FAIL - $name"
    fail=$((fail+1))
  fi
}

MANIFEST="$("$AAPT" dump xmltree "$APK" AndroidManifest.xml 2>/dev/null)"
INIT="$(unzip -p "$APK" assets/xposed_init 2>/dev/null || echo "")"

DEXFILE=$(mktemp /tmp/sketchpilot-dex.XXXXXX)
: > "$DEXFILE"
for dex in $(unzip -Z1 "$APK" 2>/dev/null | grep -E '^classes[0-9]*\.dex$'); do
  unzip -p "$APK" "$dex" 2>/dev/null | strings >> "$DEXFILE"
done

printf '%s' "$MANIFEST" | grep -q 'xposedmodule'; record "xposedmodule metadata" $?
printf '%s' "$MANIFEST" | grep -q 'xposeddescription'; record "xposeddescription metadata" $?
printf '%s' "$MANIFEST" | grep -q 'xposedminversion'; record "xposedminversion metadata" $?
printf '%s' "$MANIFEST" | grep -q 'xposedscope'; record "xposedscope metadata" $?
printf '%s' "$MANIFEST" | grep -q 'E: queries'; record "package visibility queries" $?
printf '%s' "$MANIFEST" | grep -q 'pro.sketchware'; record "pro.sketchware query" $?
printf '%s' "$INIT" | grep -q 'snd.sketchpilot.core.hook.SketchPilotEntry'; record "xposed_init asset" $?
grep -q 'SketchPilotEntry' "$DEXFILE"; record "entry class in dex" $?
printf '%s' "$MANIFEST" | grep -q 'snd.sketchpilot.ui.ComposeMainActivity'; record "launcher activity declared" $?
rm -f "$DEXFILE"

if [ "$fail" -ne 0 ]; then
  echo "verify_apk.sh: FAILED ($fail)" >&2
  exit 1
fi
echo "verify_apk.sh: PASS ($APK)"
