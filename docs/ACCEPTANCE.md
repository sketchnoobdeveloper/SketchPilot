# Acceptance report (P0–P8, reality audit)

## What was actually executed on 2026-11-18

| Check | Command | Observed result |
|---|---|---|
| Constraint guard | `bash scripts/check_constraints.sh` | `PASS` |
| Patch replay | `bash scripts/verify_patches.sh` | see final report; applies p0…p8 into an empty dir, recompiles, runs all suites |
| Gradle APK build | `gradle :app:assembleDebug --offline --no-daemon` | `BUILD SUCCESSFUL in 40s` |
| APK produced | `ls -lh app/build/outputs/apk/debug/app-debug.apk` | `130K ... app-debug.apk` |
| LSPosed packaging | `bash scripts/verify_apk.sh app/build/outputs/apk/debug/app-debug.apk` | `verify_apk.sh: PASS` (9 checks, including entry class in multidex, `<queries>`, metadata) |
| Java compile | `javac 17` with Android 35 + Xposed API 82 + org.json 20240303 | all 111 main sources compile |
| Tests | 9 JVM suites | **115 assertions passed** |

Suite totals: CoreTest 11, TransactionTest 14, BridgeTest 5, AgentTest 17, ToolsTest 13,
McpTest 11, HardeningTest 12, ScenarioTest 22, McpHardeningTest 10.

## Reality audit findings (P8)

1. **False advertisement of planned tools** — fixed: `Tool.isAvailable()` added; hidden tools are absent
   from `tools/list` and return a clear unavailable error. `docs/TOOL_STATUS.md` lists every tool.
2. **"P1 stub" string** in the MCP service — replaced.
3. **Evidence overstatement** — `SKETCHWARE_INTERNALS.md` E17 now states the measured 76 `<activity>`
   elements / 71 names ending in `Activity`, and explicitly says no device UI walk was performed.
   E19 now marks `data/system/component.json` as source-only (absent on device).
4. **API mismatch** — the real AGP build found `JSONObject.clear()` and checked `JSONException`
   differences; both fixed (`docs/BUILD.md`). This is why a build is part of acceptance, not just javac.
5. **MCP replay protection** — added local-Origin rejection, constant-time bearer comparison, 1 MB body
   limit, and audit-log redaction; verified by `McpHardeningTest` (10 assertions).

## End-to-end scenarios (offline repo, temp COPY of real project 602)

Real output captured from `ScenarioTest`:

```
SCENARIO S0: prepare temp copy of real project 602
  fixture sha256=aa951b08ba01e5fe...
SCENARIO S1: create Login screen from natural-language spec via IR
  result=true / committed: view:login.xml
SCENARIO S2: add button onClick, validate then toast logic
  result=true / committed: logic:MainActivity.java_onClick_btn_login
SCENARIO S3: build-error fixture, diagnostics parser and fix proposal
  diagnostics=[{"severity":"error","stage":"java","line":2,"message":"error: cannot find symbol"},
               {"severity":"error","stage":"resources","line":3,"message":"aapt2: error: resource not found"}]
SCENARIO S4: add library + duplicate-class conflict check
  result=true / committed: library:compat
  duplicate conflicts=[]
SCENARIO S5: undo/redo returns byte-identical project
  after undo sha256=aa951b08ba01e5fe...
ScenarioTest: 22 assertions passed.
```

S1 asserts `login.xml` in `view`, all four widget ids, and `view_root` updated in the same transaction.
S2 asserts the new logic section and the toast source text. S3 uses a synthetic build-error fixture (not
a real failed build). S4 asserts zero duplicate classes in `libs/local_libs` (which is empty on this
device — the checker itself was executed, but a positive conflict was not observed). S5 compares SHA-256
of every file before and after undo and again after a final undo; both matched.

## Coverage acceptance against `docs/COVERAGE.md`

- 64 audit rows: 30 `mapped`, 34 `planned`, 7 documented gaps. No row was silently promoted.
- 19 tools are real and advertised; 40+ are planned, hidden from MCP, and return an explicit
  `not available yet` error (`docs/TOOL_STATUS.md`).
- Every hook body is wrapped and degrades to OFFLINE; no unresolved hook can crash the host.

## UNVERIFIED (requires the user's device)

- Actual LSPosed activation, hook firing, overlay injection, capability resolution, companion-app
  versionCode read, and diagnostics zip contents. `docs/DEVICE_TEST.md` is the exact procedure.
- GAP-1 (live manager instance holders) and GAP-2 (editor refresh method) cannot be proven without a
  running Sketchware process; safe implementations plus explicit fallbacks are in `snd.sketchpilot.gaps`.
- Positive duplicate-class conflict, real build failure, and custom block palette content were not
  present in the available sample data.
