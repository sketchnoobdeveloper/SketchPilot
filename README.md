# SketchPilot

SketchPilot turns Sketchware Pro into an AI-agent-driven IDE: an LSPosed module, a companion app, and an MCP server.

## Current UI stack
- **Pure Kotlin + Jetpack Compose + Material 3** (`app/src/main/kotlin/.../ComposeMainActivity.kt`)
- Compose BOM 2024.12.01, Material3 1.3.1, Kotlin 2.0.21, AGP 8.2.2, Gradle 8.4 wrapper
- The verified Java engine remains as the interop layer; porting it to Kotlin is tracked as a follow-up, not claimed complete.

## Build
Every push triggers GitHub Actions (`.github/workflows/build.yml`), which builds and uploads the debug APK as an artifact named `sketchpilot-debug-apk`.

Local build (if you have JDK 17 + Android SDK 34/build-tools 34):
```sh
./gradlew :app:assembleDebug --no-daemon
```

## Docs
See `docs/` for the reality audit, tool status, coverage, MCP setup, build, device test, gaps and completion state.
