# GAP-1…GAP-7 closure record (Phase 8)

For each gap: (a) what remains unproven, (b) safest implementation now, (c) fallback if the hook/registry
cannot be resolved. None of these are marked "working" without device evidence.

| Gap | (a) Unproven | (b) Implemented now | (c) Fallback |
|---|---|---|---|
| GAP-1 | Whether manager instances are singletons and which static holder owns them on this version | `Gap1ManagerLocator`: constructor capture (`ManagerCaptureHook`) + reflective static field probing by exact field type; results recorded as hook breadcrumbs | If no instance found, `LiveRepository` delegates every read to `OfflineRepository`; live writes are never attempted |
| GAP-2 | Exact UI refresh method after a manager save | `Gap2EditorRefresher`: probes only descriptive zero-arg methods (`invalidateViews`, `refreshView`, `refresh`, `notifyDataSetChanged`, `loadViews`, `reloadViews`, `updateUI`), logs `SP-HOOK Gap2.refresh.*` | If none resolves, returns a message telling the user to close/reopen the project; `ConflictGuard` blocks offline writes while the project is open, so no silent corruption |
| GAP-3 | Real shape of custom block palette/events (on-device `palette.json` was empty) | `Gap3BlockRegistryLoader` reads `resources/block/My Block/palette.json`, `<id>/custom_blocks/*.json` and `data/system/events.json` into `BlockSpecRegistry` | If empty/unparseable, builtin spec table (from real `logic` evidence E15) is used and the validator does not reject unknown opCodes unless a host registry was actually loaded |
| GAP-4 | Installed Sketchware Pro versionCode (no `/data/app` read) | `<queries>` in manifest for `pro.sketchware`, `pro.sketchware.debug`, `com.besome.sketch`; `InstalledApps.getSketchwareInfo()` uses `PackageManager`; result included in diagnostics | If no package visible, diagnostics says "not visible"; `CompatMatrix.forVersion` falls back to unknown profile and `CryptoRegistry.allowWrite` refuses writes without override |
| GAP-5 | Grammar of `java` / `local_library` (empty in both samples) | `Gap5OpaquePayloads`: decrypt generically, run the lossless `SectionedFile` parser, report `sectioned` flag and raw text via `code.read_opaque` | If no sections, treat payload as opaque and never guess a schema; raw text still returned for inspection |
| GAP-6 | Shapes of `data/system/{events,listeners,component}.json` (absent on device) | `Gap6SystemFiles`: tolerant reader accepting a JSON array or an object (converted to name/value rows) via `kb.system` | Missing or malformed files yield empty arrays; no tool claims data it did not read |
| GAP-7 | Full Gradle/Android Studio export grammar | `Gap7GradleExport`: real portable backup archive (data + metadata) exposed as `project.export` with `format=backup`; `verify_apk.sh`/`BUILD.md` document the build route | `format=gradle` returns `status=unavailable` with an explicit error instead of a fake exported project |

## Live refresh safety rule (implemented)

`TransactionManager.commit()` first checks `ConflictGuard`: live bridge reachable, live session registered,
or lock file present all block the offline write with the message "project ... is open in Sketchware ...
Close and reopen the project before offline edits". This is the required degradation path when GAP-2
cannot be resolved. No code path writes offline to an open project.
