# Final report — Phase 8 reality audit

## VERIFIED (executed, output observed)

- **Reality audit performed.** Grepped all 111 main Java sources for TODO/FIXME/stub/mock/placeholder and
  empty/hardcoded-success bodies. Found and fixed the real problem: 40+ planned tools were advertised as
  working (`UnavailableTool`). They are now hidden from `tools/list` and return an explicit unavailable
  error (`docs/TOOL_STATUS.md`).
- **Evidence recheck performed.** `SKETCHWARE_INTERNALS.md` E17 corrected to measured values (76
  `<activity>` elements / 71 names ending in `Activity`; no device UI walk), E19 marks `component.json`
  as source-only/absent on device, `COVERAGE.md` reworded accordingly. No other evidence tag was found
  without a concrete file path, decompiled symbol, or bytes from real projects 601/602.
- **API existence checked** against the exact jars used: Xposed API 82 (`findAndHookMethod`,
  `findAndHookConstructor`, `XC_MethodHook` callbacks), Android 35 platform `org.json`, AGP 8.2.2.
  The real Gradle build exposed two org.json incompatibilities, fixed.
- **Patch replay**: `scripts/verify_patches.sh` applies p0…p8 into an empty temp dir, recompiles main and
  tests with javac, and runs all 9 suites. Result: `verify_patches.sh: PASS`. A separate integrity scan
  confirmed all 142 tracked project files appear in at least one patch (no missing files). Later patches
  intentionally own the final version of files changed across phases; applying in order yields the
  correct tree.
- **Installable APK built**: `gradle :app:assembleDebug --offline --no-daemon` → `BUILD SUCCESSFUL`,
  130 KB debug APK. `scripts/verify_apk.sh` passes all 9 LSPosed packaging checks (metadata, scope,
  `<queries>`, `xposed_init`, entry class in multidex).
- **115 test assertions pass**, including S1–S5 end-to-end offline scenarios on a byte-hashed copy of a
  real project (undo returns SHA-256 `aa951b08ba01e5fe...`, identical to the start).
- **MCP hardening verified**: wrong token 401, missing token 401, bad Origin 403, local Origin 200,
  1.2 MB body 413, malformed JSON-RPC returns structured error, disallowed tool returns `-32602`, audit
  log contains neither API keys nor tokens.
- **GAP-1…GAP-7** each has a documented (a) unproven item, (b) implemented safe path, and (c) fallback
  (`docs/GAPS.md`). Live refresh degrades to "close and reopen the project"; offline writes to an open
  project are refused.
- **On-device kit built**: companion "Run diagnostics" button + real `debug.diagnostics` tool produce one
  zip with manifest, app/device info, capability matrix, `SP-HOOK` breadcrumbs, last 500 log lines, MCP
  status and four self-tests. `docs/DEVICE_TEST.md` has 8 numbered steps with expected output, likely
  failure and fix.

## UNVERIFIED (needs the user's device)

- LSPosed actually loading the module; hook firing; overlay injection; live capability resolution.
- GAP-1 manager holder classes and GAP-2 refresh method (only safe probes + fallbacks exist).
- Companion-app reading of the installed Sketchware versionCode through `<queries>` (code and manifest
  are built into the APK; no device here to observe the value).
- A positive duplicate-class conflict, a real failed build, and custom block palette content — the
  available sample data contained none of these.

## PLANNED (hidden, explicit unavailable error)

40+ tools listed in `docs/TOOL_STATUS.md`: project create/clone/rename/restore/settings, screen/view/logic
mutations beyond IR, code.write, resource generators, library Maven/local, build.compile/install,
logcat, fs/web tools, and full Gradle export.

## Five riskiest assumptions (ranked)

1. **Crypto key is version-pinned.** `sketchwaresecure` was verified only on v7.1.0/code150. If a fork
   changes it, decryption fails; `CryptoRegistry` then refuses writes. Mitigation: per-version probe.
2. **Manager field names (`c`, `d`, `h`, `i`) stay stable.** Live reads rely on them; shape verification
   reduces but does not remove the risk. Mitigation: offline fallback.
3. **`ConflictGuard` detects every open session.** It uses in-process registry + loopback bridge + lock
   files. A host where the bridge thread died but the project is still open could slip through.
   Mitigation: user-facing "reopen project" rule and backup-before-write.
4. **Platform `org.json` parity.** Desktop tests pass with `org.json` 20240303 while Android's platform
   implementation differs; the AGP build caught two cases, but other runtime differences may remain.
   Mitigation: real build in acceptance, avoid unchecked platform-specific methods.
5. **Diagnostics self-tests use project 602.** If that project is absent on the user's device,
   `offline_decrypt_copy` and `backup_restore_copy` report false; this is an honest "unverified", not a
   silent pass.
