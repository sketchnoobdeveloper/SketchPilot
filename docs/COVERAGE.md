# Coverage Audit — SketchPilot × Sketchware Pro

Phase 0 audit. Every Sketchware Pro user action is mapped to a tool, or listed as a documented gap with a
reason. Legend:

- **Live-bridge** = tool can be served by hooking the running Sketchware process (`core.bridge`).
- **Offline** = tool can be served by decrypting/parsing/writing project files directly (`OfflineRepository`).
- **MCP tool** = intended name in the `tools` catalog (§6b of the master prompt).
- **Status** — `mapped`: internals verified against source **and** a real project; `planned`: source-verified
  but not empirically exercised; `gap`: unresolved (reason in Notes).

All rows were mapped from `AndroidManifest.xml` (76 `<activity>` elements / 71 names ending in `Activity`; source-only [E17]), the source tree, and the real `sc_id=601/602` projects [E2]. No device UI walk was performed; rows describing dialogs are source-derived. Evidence tags refer to `docs/SKETCHWARE_INTERNALS.md` §0.

---

## 1. Project-level actions

| Feature | Screen / Menu | Live-bridge | Offline | MCP tool | Status | Notes / evidence |
|---|---|---|---|---|---|---|
| List projects | MainActivity project drawer | `lC.a()` | read `mysc/list/*/project` | `project.list` | mapped | E5,E8 |
| Open project | project card tap | `lC.b(id)`, `yq(ctx,id)` | parse `data/<id>` | `project.info` | mapped | E4,E12 |
| Create project | New project dialog | `lC.a(id,map)` | create dirs + encrypted metadata | `project.create` | planned | E5 |
| Clone / duplicate | project context menu | manager save chain | copy `data/<id>` + new metadata | `project.clone` | planned | E5,E9 |
| Rename project | MyProjectSettingActivity | `lC.b(id,map)` | patch metadata JSON | `project.rename` | mapped | E5,E8 |
| Export project (.sw / source / AAB) | ExportProjectActivity | `yq` export types | archive `data/<id>` | `project.export` | planned | E4; GAP-7 |
| Backup project | settings / backup menu | manager savers | copy to `bak/<id>/` | `project.backup` | mapped | E18 |
| Restore project | backup picker | `oB` copy | restore from `bak/<id>/` | `project.restore` | mapped | E18 |
| App name / package / version / icon | MyProjectSettingActivity | `lC.b(id,map)` | patch metadata | `project.settings` | mapped | E8 |
| SDK levels / theme / permissions | MyProjectSettingActivity, ManagePermissionActivity | `project_config` edit | patch `project_config`, `permission` | `project.settings` | mapped | E10b,E9 |
| Build system / Gradle version / IDE-only | ConfigActivity, gradle_project.json | settings edit | patch `gradle_project.json` | `project.settings` | mapped | E10b (contains secrets — redact) |
| ProGuard / R8 config | ManageProguardActivity | `proguard` edit | patch `proguard*` | `project.settings` | mapped | E9 |
| StringFog toggle | ConfigActivity | settings edit | patch `stringfog` | `project.settings` | planned | E9 |

## 2. Screens / files

| Feature | Screen / Menu | Live-bridge | Offline | MCP tool | Status | Notes / evidence |
|---|---|---|---|---|---|---|
| Add / remove / rename screen | `file` @activity editor | `hC` savers | patch `file` | `screen.add/remove/rename` | mapped | E10,E11 |
| Orientation / theme / keyboard | PropertyActivity for activity | `ProjectFileBean` fields | patch `file` JSON | `screen.settings` | mapped | E10 |
| Custom view definitions | AddCustomViewActivity, `file` @customview | `hC` | patch `file` | `screen.custom_views` | planned | E10 |
| Root layout attributes | view editor root | `eC` + `view_root` | patch plaintext `view_root` | `screen.settings` | mapped | E10b |
| Layout preview | LayoutPreviewActivity | n/a (read-only render) | parse `view` | `view.get_tree` | planned | E17 |

## 3. Views / UI

| Feature | Screen / Menu | Live-bridge | Offline | MCP tool | Status | Notes / evidence |
|---|---|---|---|---|---|---|
| Get view tree | DesignActivity canvas | `eC.c` map | parse `view` | `view.get_tree` | mapped | E10,E11 |
| Add / edit / move / delete widget | DesignActivity, PropertyActivity | `eC` mutators + refresh | patch `view` | `view.add/edit/move/delete` | planned | E11; GAP-2 |
| Set widget properties | PropertyActivity | `ViewBean` fields | patch `view` JSON | `view.set_properties` | mapped | E10 |
| Apply layout from IR | design canvas | `eC.a(ViewBean)` | rebuild `view` sections | `view.apply_ir` | planned | E11; GAP-2 |
| Set raw XML | ViewCodeEditorActivity, ManageXMLCommandActivity | source editor hook | write generated layout | `view.set_xml` | planned | E17 |
| Widget type registry | palette | `wq.b(int)` | static table | `kb.search` | mapped | E13 |
| Custom widgets | ManageViewActivity, `widgets.json` | registry read | read `resources/widgets` | `view.*` | planned | E19 (empty on device) |

## 4. Logic / blocks

| Feature | Screen / Menu | Live-bridge | Offline | MCP tool | Status | Notes / evidence |
|---|---|---|---|---|---|---|
| Read block chains | LogicEditorActivity | `eC.d` map | parse `logic` | `logic.get` | mapped | E10,E11 |
| Add / edit / delete block | block canvas | `eC` mutators | patch `logic` JSON | `logic.edit` | planned | GAP-2 |
| Events CRUD | AddEventActivity, `@*.java_events` | `eC.i` / `a(EventBean)` | patch `logic` | `logic.events` | mapped | E10,E11 |
| Variables / lists | LogicEditorActivity | `eC.e/f` pairs | patch `logic` | `logic.variables`, `logic.lists` | planned | E11 |
| More blocks | LogicEditorActivity | `eC.g` pairs | patch `logic` | `logic.more_blocks` | planned | E11 |
| Components CRUD | AddCustomComponentActivity, `@*.java_components` | `eC.h` | patch `logic` | `logic.components` | mapped | E10,E11 |
| Explain logic | AI quick action | read chain | read chain | `logic.explain` | mapped | E15 |
| Generate logic from spec | AI quick action | IR→Bean | IR→Bean | `logic.generate` | planned | E15; GAP-3 |
| Custom blocks / palettes | MakeBlockActivity, BlocksManagerCreatorActivity | `BlockLoader` | read `custom_blocks` | `logic.*`, `kb.search` | planned | E14; GAP-3 |
| Custom events | BlocksManagerDetailsActivity, ConfigActivity | `EventsHandler` | read `events.json` | `logic.events` | planned | E14; GAP-6 |

## 5. Code / resources / libraries

| Feature | Screen / Menu | Live-bridge | Offline | MCP tool | Status | Notes / evidence |
|---|---|---|---|---|---|---|
| Read/write custom Java | ManageJavaActivity, `data/<id>/java`, `files/java` | source manager | patch files | `code.read/write` | planned | E9; GAP-5 |
| Add-source-directly blocks | LogicEditorActivity | block op `addSourceDirectly` | patch `logic` | `code.add_source_blocks` | mapped | E15 |
| Generated source viewer | CodeViewerActivity, SrcViewerActivity | read `mysc/<id>/app/src` | read generated tree | `code.generated` | mapped | E2,E16 |
| Images / fonts / sounds | ManageImage/Font/SoundActivity | resource manager | copy into `resources/<type>/<id>` | `resource.*` | planned | E19 |
| Strings / colors / drawables | ResourcesEditorActivity, ManageResourceActivity | XML edit | patch `files/resource/values` | `resource.*` | mapped | E9 |
| Shape / vector drawables | VectorDrawableParser | n/a | write XML | `resource.generate` | planned | source: mod.bobur |
| Local libraries | ManageLocalLibraryActivity, `data/<id>/local_library` | `iC` + lib manager | patch index + `libs/local_libs` | `library.local` | planned | E9; GAP-5 |
| Maven dependencies | Firebase/Admob/Map/Library activities, `dependencies.json` | dependency resolver | patch JSON | `library.maven` | mapped | E9 |
| Built-in library toggles | ManageLibraryActivity, `@*.java` in `library` | `iC` | patch `library` | `library.toggle` | mapped | E10,E11 |
| Conflict / duplicate-class check | compile errors | R8/D8 diagnostics | static scan | `library.check` | planned | E16 |

## 6. Build / debug

| Feature | Screen / Menu | Live-bridge | Offline | MCP tool | Status | Notes / evidence |
|---|---|---|---|---|---|---|
| Compile project | build FAB / CompileLogActivity | `ProjectBuilder` | run toolchain locally | `build.compile` | mapped | E16 |
| Stream build log | CompileLogActivity | log writers + `debug.txt` | tail `debug.txt` | `build.log` | mapped | E16 |
| Parse diagnostics | CompileLogActivity, CollectErrorActivity | stage-keyed parse | stage-keyed parse | `build.diagnostics` | mapped | E16 |
| Compile-and-fix loop | AI quick action | bounded auto-fix | bounded auto-fix | `build.compile_and_fix` | planned | E16 |
| Install / run | build result actions | `pm install` via allowlist | n/a | `build.install` | planned | — |
| Logcat / crash analysis | LogReaderActivity, RemoteDebugActivity | logcat reader | read saved logs | `debug.logcat` | mapped | E17,E2 |

## 7. System / environment tools

| Feature | Screen / Menu | Live-bridge | Offline | MCP tool | Status | Notes / evidence |
|---|---|---|---|---|---|---|
| Sandboxed FS browse | companion app | n/a | allowed roots | `fs.*` | planned | hard constraint: sandbox |
| Shell (OFF by default) | companion app | n/a | ProcessBuilder allowlist | `shell.*` | planned | hard constraint §1 |
| Web docs fetch (opt-in) | companion app | n/a | HttpURLConnection | `web.*` | planned | — |
| Offline KB search | AI panel | block/component/error tables | static KB | `kb.search` | mapped | E13,E14,E16 |

## 8. UI entry points (agent surfaces)

| Surface | Host | Live-bridge | Offline | MCP tool | Status | Notes / evidence |
|---|---|---|---|---|---|---|
| Floating agent panel | DesignActivity, LogicEditorActivity, MainActivity | overlay hook | n/a | n/a | planned | E17 |
| "Fix this error" quick action | CompileLogActivity | build diag → agent | diag → agent | `build.compile_and_fix` | planned | E16 |
| "Explain this logic" | LogicEditorActivity | `eC.d` read | `logic` read | `logic.explain` | mapped | E15 |
| "Design this screen" | DesignActivity | `eC.c` read | `view` read | `view.apply_ir` | planned | E10 |
| Diff viewer (Apply/Reject/Undo) | overlay | transaction stack | transaction stack | n/a | planned | requirement §5 |
| Capability matrix | companion app + overlay | resolver results | n/a | n/a | planned | §4 |
| Companion app (provider setup, browser, chat, MCP controls) | `snd.sketchpilot` app | IPC | direct | n/a | planned | requirement §3 |
| MCP server (tools/resources/prompts/notifications) | companion foreground service | proxy via `core.bridge` | offline engine | MCP | planned | requirement §7 |
| MCP client (external servers) | in-app agent | n/a | n/a | n/a | planned | requirement §7 |

---

## Coverage summary (Phase 0)

- Total audit rows: **64**.
- `mapped` (internals verified against source + real project): **30**.
- `planned` (source-verified, needs empirical exercise or implementation): **34**.
- `gap` items are recorded inline (GAP-1…GAP-7) and in `SKETCHWARE_INTERNALS.md` §7: **7**.

**Acceptance for Phase 0:** every enumerated user action has a tool mapping or a documented gap with a
reason. No live hook is implemented yet, so no unresolved hook can crash the host. The 7 gaps must be closed
before the corresponding rows may be promoted from `planned` to `mapped` in later phases.
