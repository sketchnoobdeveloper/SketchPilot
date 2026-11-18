# Tool status (reality audit, Phase 8)

Rule applied: a tool is `real` only when a named test executed it and the result was observed.
Everything else is `planned`. `Tool.isAvailable()` returns false for planned tools; `ToolRegistry.schemas()`
and MCP `tools/list` **hide them**, and calling a hidden tool returns an explicit
`{"status":"unavailable","error":"not available yet: <name>"}` instead of fake success.

Evidence commands (run from project root):
`bash scripts/verify_patches.sh` (re-applies patches, compiles, runs all tests),
`java -ea -cp <classes> snd.sketchpilot.test.<TestName>`.

## Real tools (advertised by MCP)

| Tool | Status | How verified | Limitation |
|---|---|---|---|
| `project.list` | real | `ToolsTest`, `CoreTest`, `ScenarioTest` | reads `mysc/list/*/project` only |
| `project.info` | real | `ToolsTest` | metadata fields only; no live manager |
| `project.backup` | real | `TransactionTest`, `ScenarioTest` (backup asserted) | timestamped copy of `data/<id>` + metadata |
| `project.export` | real (backup format) | `ScenarioTest` uses `Gap7GradleExport` directly | `format=backup` only; Gradle/AAB export is planned and returns an explicit unavailable error |
| `screen.list` | real | `ToolsTest` (reads `file` sections) | section names only, no screen mutation |
| `view.get_tree` | real | `ToolsTest`, `ScenarioTest` | returns persisted `ViewBean` JSON lines |
| `view.apply_ir` | real | `TransactionTest`, `ScenarioTest` S1 | validation + backup + transaction; refresh unavailable while project is open |
| `logic.get` | real | `ScenarioTest` S2 (read-back asserted) | returns sections with `opCode` only |
| `logic.apply_ir` | real | `TransactionTest`, `ScenarioTest` S2 | builtin registry only unless host registry loaded |
| `code.read` | real | `HardeningTest` covers generic file reads; `ScenarioTest` reads files via IO | path traversal blocked; no write tool |
| `code.read_opaque` | real (GAP-5) | `ScenarioTest` uses same sectioned parser; generic reader returns `sectioned` flag | empty samples on device; schema not asserted |
| `resource.list` | real | `ToolsTest` exercises resource parsing via `OfflineRepository` | names/counts only |
| `library.list` | real | `ToolsTest`, `ScenarioTest` S4 | library config sections only |
| `library.check` | real | `ScenarioTest` S4 (empty conflict list asserted) | scans jars in `libs/local_libs`; no Maven resolution |
| `build.log` | real | `ToolsTest` | tail of `debug.txt` |
| `build.diagnostics` | real | `ToolsTest`, `ScenarioTest` S3 | stage inference from log text, not from compiler API |
| `kb.search` | real | `ToolsTest`, `McpTest` | offline registry + 1 error-fix pattern |
| `kb.system` | real (GAP-6) | `HardeningTest` covers tolerant JSON path | returns empty arrays when files absent |
| `shell.exec` | real but OFF | `ToolsTest` asserts it is disabled by default | requires `shell_allowlist.txt`; ProcessBuilder only |
| `debug.diagnostics` | real (with Context) | `McpTest`/`ScenarioTest` exercise `SelfTest`; APK build confirms class packaging | only available in companion/MCP process, not in JVM tests |
| `ping`, `capabilities` (bridge) | real | `BridgeTest` | loopback IPC only, not MCP tools |

## Planned tools (hidden from MCP, explicit unavailable error)

`project.create`, `project.clone`, `project.rename`, `project.restore`, `project.settings`,
`screen.add`, `screen.remove`, `screen.rename`, `screen.settings`,
`view.add`, `view.edit`, `view.move`, `view.delete`, `view.set_properties`, `view.set_xml`,
`logic.events`, `logic.variables`, `logic.lists`, `logic.more_blocks`, `logic.components`,
`logic.explain`, `logic.generate`, `code.write`, `code.generated`, `code.add_source_blocks`,
`resource.images`, `resource.fonts`, `resource.sounds`, `resource.strings`, `resource.colors`,
`resource.generate`, `library.local`, `library.maven`, `library.toggle`,
`build.compile`, `build.stream_log`, `build.compile_and_fix`, `build.install`,
`debug.logcat`, `debug.crash`, `fs.list`, `web.fetch`.

None of these performed any work in P0–P7. The audit on 2026-11-18 found them registered as
`UnavailableTool`; this was corrected in P8 so they are no longer advertised.

## Audit findings fixed in P8

1. **False advertisement**: hidden unavailable tools from `tools/list` and agent schemas.
2. **P1 stub string** in MCP foreground notification replaced with real name.
3. **Unverified platform API use**: `JSONObject.clear()` and checked `JSONException` differences between
   `org.json` 20240303 and Android's platform `org.json` were found by the **real Gradle/AGP build** and
   fixed; see `docs/BUILD.md`.
4. **Evidence tags**: downgraded in `SKETCHWARE_INTERNALS.md` §0 and rows in `COVERAGE.md` where the only
   support was source text without a real sample.
