#!/usr/bin/env bash
# Applies p0..p8 into an EMPTY temp dir exactly as a user would, then compiles
# all Java and runs every pure-Java test suite there. Prints PASS/FAIL.
set -uo pipefail

SRC_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PATCH_DIR="$SRC_ROOT/patches"
WORK=$(mktemp -d /tmp/sketchpilot-verify.XXXXXX)
echo "verify_patches.sh workdir: $WORK"

fail=0
for p in "$PATCH_DIR"/p0-*.zip "$PATCH_DIR"/p1-*.zip "$PATCH_DIR"/p2-*.zip \
         "$PATCH_DIR"/p3-*.zip "$PATCH_DIR"/p4-*.zip "$PATCH_DIR"/p5-*.zip \
         "$PATCH_DIR"/p6-*.zip "$PATCH_DIR"/p7-*.zip "$PATCH_DIR"/p8-*.zip; do
  if [ ! -f "$p" ]; then
    echo "  FAIL - missing patch $(basename "$p")"
    fail=$((fail+1))
    continue
  fi
  unzip -o -q "$p" -d "$WORK" || fail=$((fail+1))
  echo "  applied $(basename "$p")"
done

JSON_JAR=""
for candidate in /root/.gradle/caches/modules-2/files-2.1/org.json/json/20240303/*/json-20240303.jar \
                 "$SRC_ROOT/libs/json-20240303.jar"; do
  if [ -f "$candidate" ]; then JSON_JAR="$candidate"; break; fi
done
ANDROID_JAR="${ANDROID_JAR:-/opt/android-sdk/platforms/android-35/android.jar}"
XPOSED_JAR="$WORK/app/libs/xposed-api-82.jar"

if [ -z "$JSON_JAR" ] || [ ! -f "$ANDROID_JAR" ] || [ ! -f "$XPOSED_JAR" ]; then
  echo "  FAIL - missing compile jars (JSON_JAR=$JSON_JAR ANDROID_JAR=$ANDROID_JAR XPOSED_JAR=$XPOSED_JAR)"
  fail=$((fail+1))
fi

if [ "$fail" -eq 0 ]; then
  OUT="$WORK/build"
  mkdir -p "$OUT"
  find "$WORK/app/src/main/java" -name '*.java' > "$WORK/main.srcs"
  javac -source 17 -target 17 -Xlint:none -cp "$JSON_JAR:$ANDROID_JAR:$XPOSED_JAR" \
    -d "$OUT" @"$WORK/main.srcs" > "$WORK/compile.log" 2>&1 || {
      echo "  FAIL - main compile"; tail -20 "$WORK/compile.log"; fail=$((fail+1)); }
  if [ "$fail" -eq 0 ]; then
    find "$WORK/app/src/test/java" -name '*.java' > "$WORK/test.srcs"
    javac -source 17 -target 17 -Xlint:none -cp "$JSON_JAR:$OUT" \
      -d "$OUT" @"$WORK/test.srcs" > "$WORK/testcompile.log" 2>&1 || {
        echo "  FAIL - test compile"; tail -20 "$WORK/testcompile.log"; fail=$((fail+1)); }
  fi
  # Stronger proof: the replayed tree must also build a real APK, not just pass javac.
  if [ "$fail" -eq 0 ]; then
    ANDROID_HOME_VALUE="${ANDROID_HOME:-/opt/android-sdk}"
    if [ -d "$ANDROID_HOME_VALUE" ]; then
      printf 'sdk.dir=%s\n' "$ANDROID_HOME_VALUE" > "$WORK/local.properties"
      GRADLE_BIN="${GRADLE_BIN:-gradle}"
      if "$GRADLE_BIN" -p "$WORK" :app:assembleDebug --offline --no-daemon > "$WORK/gradle.log" 2>&1; then
        APK=$(find "$WORK/app/build/outputs/apk/debug" -name '*.apk' -type f | head -1)
        if [ -n "$APK" ] && bash "$WORK/scripts/verify_apk.sh" "$APK" > "$WORK/apk.log" 2>&1; then
          echo "  ok - replayed tree builds APK: $(basename "$APK")"
        else
          echo "  FAIL - APK packaging check"; tail -20 "$WORK/apk.log" 2>/dev/null; fail=$((fail+1))
        fi
      else
        echo "  FAIL - Gradle build from replayed tree"; tail -25 "$WORK/gradle.log"; fail=$((fail+1))
      fi
    else
      echo "  NOTE - ANDROID_HOME missing; skipped Gradle APK build in replay"
    fi
  fi

  if [ "$fail" -eq 0 ]; then
    for t in CoreTest TransactionTest BridgeTest AgentTest ToolsTest McpTest HardeningTest ScenarioTest McpHardeningTest; do
      if [ -f "$WORK/app/src/test/java/snd/sketchpilot/test/$t.java" ]; then
        if java -ea -cp "$JSON_JAR:$OUT" "snd.sketchpilot.test.$t" > "$WORK/$t.log" 2>&1; then
          echo "  ok - $t: $(tail -1 "$WORK/$t.log")"
        else
          echo "  FAIL - $t"; tail -20 "$WORK/$t.log"; fail=$((fail+1))
        fi
      fi
    done
  fi
fi

if [ "$fail" -ne 0 ]; then
  echo "verify_patches.sh: FAILED ($fail) - logs in $WORK"
  exit 1
fi
echo "verify_patches.sh: PASS (applied patches from scratch, compiled, all tests passed)"
