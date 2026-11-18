# On-device validation kit (user runs; developer cannot)

The developer cannot install or enable an LSPosed module from this environment. These steps are written
so the result is reproducible and diagnosable. Send back the diagnostics zip and the numbered step
where something diverged.

## Before you start

- Root/LSPosed device with Sketchware Pro installed.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- The module is **not signed with a release key**; debug signing is fine for LSPosed.

## Steps

1. **Install the APK**
   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Expected: `Success`.
   Most likely failure: `INSTALL_FAILED_UPDATE_INCOMPATIBLE` from an older signature.
   Fix: `adb uninstall snd.sketchpilot` then reinstall.

2. **Enable the module in LSPosed**
   Open LSPosed → Modules → SketchPilot → enable.
   Expected: switch on, no "incompatible" warning.
   Failure: module not listed. Fix: reboot once; confirm APK passed `scripts/verify_apk.sh`.

3. **Set the scope**
   In the module's scope list tick **Sketchware Pro** (`pro.sketchware`). For a fork, add its package
   to `scopes.txt` in `/sdcard/.sketchware/sketchpilot/` and also to `<queries>` in the manifest before
   rebuilding.
   Expected: `pro.sketchware` checked.
   Failure: force-stopped apps not applying scope → reboot.

4. **Force-stop Sketchware Pro**
   ```sh
   adb shell am force-stop pro.sketchware
   ```
   Expected: no output, app fully closed. This matters: hooks only apply on process start.

5. **Open Sketchware Pro and open a real project**
   Open a project with at least one screen (e.g. the existing `QuickNotes`, sc_id 602).
   Expected: app launches normally; no crash/toast from SketchPilot.
   Failure: crash → capture `/sdcard/.sketchware/sketchpilot/sketchpilot.log` immediately.
   The global kill switch file `/sdcard/.sketchware/sketchpilot/killswitch` disables all hooks if needed.

6. **Tap the floating "SP" button** in the designer or project list, then tap **Capabilities**.
   Expected: a list with rows such as `Project data manager [LIVE] ...`; if a hook failed, the row says
   `[OFFLINE]` with a reason.
   Failure: overlay not visible → check `SP-HOOK Activity.*` breadcrumbs in the log.

7. **Run diagnostics** in the SketchPilot companion app (button **Run diagnostics**), or call MCP tool
   `debug.diagnostics`.
   Expected status text: `Diagnostics zip: /storage/emulated/0/Android/data/snd.sketchpilot/files/diagnostics/sketchpilot-diagnostics.zip`.
   Failure: "Diagnostics failed" → check logcat filtered to `SketchPilot`.

8. **Send back the zip**. It contains:
   - `manifest.txt` — module version and generation time
   - `app_info.json` — detected Sketchware Pro versionCode/versionName (GAP-4), candidate packages,
     LSPosed manager presence, whether XposedBridge is in the companion process
   - `device_info.json` — Android/device/ABI
   - `capability_matrix.json` — per-hook resolved/failed reason
   - `hook_breadcrumbs.txt` — first-fire `SP-HOOK <name> OK/FAIL` lines
   - `last500.log` — last 500 log lines
   - `mcp_status.json` — server running, port, token present (never the token itself)
   - `self_test.json` — crypto round-trip, offline decrypt on a temp copy, backup/restore round-trip,
     IR validate+compile
   - `gaps.json` — GAP-1…GAP-7 implementations and fallbacks

## Interpreting results

| Symptom | Most likely cause | Fix |
|---|---|---|
| all capabilities OFFLINE | module not enabled or wrong scope | steps 2–4 |
| `SP-HOOK Application.onCreate OK` but no manager breadcrumbs | manager constructor hooks not hit (version mismatch) | offline fallback is active; send capability matrix |
| diagnostics says Sketchware "not visible" | package visibility missing | confirm `<queries>` in APK via `verify_apk.sh` |
| self-test `offline_decrypt_copy` false | project 602 absent or crypto key differs on this version | send the failure detail; do not enable writes |
| overlay visible but bridge shows "offline mode" | hook process not started or port in use | force-stop and reopen Sketchware Pro |

**Safety rule:** if a project is open in Sketchware, offline writes are refused and the UI says to close
and reopen the project. Do not bypass this to avoid corrupting project files.
