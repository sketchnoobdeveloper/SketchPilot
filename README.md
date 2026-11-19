# SketchPilot

SketchPilot turns Sketchware Pro into an AI-agent-driven IDE: an LSPosed module, a companion app, and an MCP server.

## Architecture (current rewrite stage)
- `:core` — **pure Kotlin** domain/data layer: crypto, sectioned file parser, offline repository,
  typed IR, validators, backed-up transactions with undo/redo. Unit tests run in CI.
- `:app` — **Kotlin Jetpack Compose Material 3** UI, consuming `:core` directly.
- Compose BOM 2024.12.01, Material3 1.3.1, Kotlin 2.0.21, AGP 8.2.2, Gradle 8.4 wrapper.
- The shipping app is Kotlin-only: `app/src/main/kotlin` contains the Material 3 UI, Xposed entry,
  manager-capture hooks, overlay, live bridge and diagnostics. Legacy Java sources are archived under
  `legacy/` and are not compiled.

## Build
Every push triggers GitHub Actions (`.github/workflows/build.yml`), which builds and uploads the debug APK as an artifact named `sketchpilot-debug-apk`.

Local build (if you have JDK 17 + Android SDK 34/build-tools 34):
```sh
./gradlew :app:assembleDebug --no-daemon
```

## Docs
See `docs/` for the reality audit, tool status, coverage, MCP setup, build, device test, gaps and completion state.
