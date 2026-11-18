# Completion state

## Done (closed)
- P0–P8 delivered as patches `p0`…`p8` (plus `p9-final` for verification/install helpers).
- Reality audit, evidence downgrades, tool-status truth table, MCP hardening, build guide, device test
  guide, GAP-1…GAP-7 implementation/fallback record.
- A **real installable debug APK** is produced by `bash scripts/build.sh`; `verify_apk.sh` passes.
- `verify_patches.sh` applies every patch into an empty directory, recompiles, runs 9 suites (115
  assertions), **builds the APK from the replayed tree**, and verifies its LSPosed packaging.
- APK staged at `/sdcard/Download/sketchpilot-debug.apk` for on-device install.

## Cannot be closed without the user's device
- LSPosed activation, hook firing, capability resolution, overlay injection, versionCode read via
  `<queries>`, and the actual zip contents from "Run diagnostics".
- GAP-1/GAP-2 positive confirmation (live manager holder and UI refresh method). Safe implementations
  and explicit fallbacks exist; no unverified claim is marked working.

## Intentionally planned (not defects)
40+ tools listed in `docs/TOOL_STATUS.md` are hidden from MCP and return an explicit unavailable error.

## Final commands
```sh
bash scripts/check_constraints.sh
bash scripts/verify_patches.sh
bash scripts/build.sh
bash scripts/install.sh     # prints the pm install -r command for the device
```
