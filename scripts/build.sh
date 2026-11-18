#!/usr/bin/env bash
# Build the installable debug APK. JDK 17 + Android SDK required.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [ -z "${ANDROID_HOME:-}" ] && [ -d /opt/android-sdk ]; then
  export ANDROID_HOME=/opt/android-sdk
fi
if [ -z "${ANDROID_HOME:-}" ]; then
  echo "FAIL: set ANDROID_HOME to your Android SDK directory" >&2
  exit 1
fi
if [ ! -f local.properties ]; then
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
fi

java -version 2>&1 | head -1

GRADLE_BIN="${GRADLE_BIN:-gradle}"
"$GRADLE_BIN" :app:assembleDebug --offline --no-daemon

APK=$(find app/build/outputs/apk/debug -name '*.apk' -type f | head -1)
if [ -z "$APK" ]; then
  echo "FAIL: no APK produced" >&2
  exit 1
fi
echo "APK: $ROOT/$APK"
bash scripts/verify_apk.sh "$APK"
