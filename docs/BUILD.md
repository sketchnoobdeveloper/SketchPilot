# Build guide (verified in this environment)

## What was actually executed

On 2026-11-18 the following succeeded in this container:

```
$ gradle :app:assembleDebug --offline --no-daemon
BUILD SUCCESSFUL in 40s
$ ls -lh app/build/outputs/apk/debug/app-debug.apk
... 130K ... app-debug.apk
$ bash scripts/verify_apk.sh app/build/outputs/apk/debug/app-debug.apk
verify_apk.sh: PASS (app/build/outputs/apk/debug/app-debug.apk)
```

Environment detected: JDK 17 (`openjdk 17.0.21`), Android SDK at `/opt/android-sdk`
(platforms 34/35, build-tools 34/35/36), Gradle 8.4 CLI, AGP **8.2.2** from the local Gradle cache.
The build is fully offline-capable because the project has **no external runtime dependencies**:
- `org.json` comes from the Android platform (this caused two real compile errors against Android's
  checked-exception API, both fixed in P8 — see below);
- LSPosed/Xposed API is `compileOnly files('libs/xposed-api-82.jar')` (25 KB, shipped in p1).
- No Kotlin plugin, no annotation processors, no AppCompat/Material/constraintlayout.

## One-time setup on a Linux laptop

1. Install JDK 17 and Android SDK Platform 34 + Build-Tools 34.
2. Install Gradle 8.4 (or set `GRADLE_BIN=/path/to/gradle`).
3. Export `ANDROID_HOME=/path/to/android-sdk`.
4. The committed `gradle.properties` already pins low-memory settings:
   ```
   org.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8
   org.gradle.daemon=false
   org.gradle.parallel=false
   android.useAndroidX=false
   ```
5. `scripts/build.sh` writes `local.properties` with `sdk.dir=$ANDROID_HOME` if it is absent.

## Exact commands

```sh
export ANDROID_HOME=/opt/android-sdk        # adjust to your SDK path
bash scripts/build.sh
# outputs app/build/outputs/apk/debug/app-debug.apk
bash scripts/verify_apk.sh app/build/outputs/apk/debug/app-debug.apk
```

`verify_apk.sh` asserts: `xposedmodule`, `xposeddescription`, `xposedminversion`, `xposedscope`
metadata; `<queries>` with `pro.sketchware`; `assets/xposed_init` pointing at
`snd.sketchpilot.core.hook.SketchPilotEntry`; the entry class exists in **all** `classes*.dex`
(multidex is enabled); and the launcher activity is declared.

## Compatibility fixes discovered by the real build

| Problem | Cause | Fix |
|---|---|---|
| `JSONObject.clear()` not found | Android platform `org.json` lacks `clear()` (present in desktop `org.json` 20240303) | iterate `keys()` and `remove()` |
| `unreported exception JSONException` | Android's `JSONObject(String)`, `getJSONObject`, chained `put` declare checked `JSONException`; desktop jar does not | use `optJSONObject`/`opt`, wrap parsing in `try/catch(Throwable)`, helper `wrap()` |

These are the kind of errors that only a real AGP build reveals; the javac-only test loop in P0–P7
could not detect them.
