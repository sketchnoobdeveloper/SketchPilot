# Sketchware Pro Internals — Phase 0 Recon

**Status:** recon complete for Phase 0 scope. Every factual claim below carries an evidence tag `[E#]` resolved in §0.

**Subject under study**
- Upstream: `Sketchware-Pro/Sketchware-Pro`, cloned at commit `2c721f04580c74fd42197130b7c1becb6c00ca53` (2026-11-18), `versionName v7.1.0`, `versionCode 150`, `applicationId pro.sketchware` [E1].
- On-device release: installed in `/data/user/0/pro.sketchware`, real projects `sc_id=601` and `sc_id=602` present under `/sdcard/.sketchware/` [E2].
- Precompiled core classes recovered from Git-LFS artifacts `a.a.a-important-classes.jar`, `a.a.a-notimportant-classes.jar`, `com.besome.sketch-classes.jar` [E3].

**Method note.** The user requirement "treat any layout I assume as a hypothesis; verify against a real project" was satisfied: the on-device projects were read and their files decrypted with the key recovered from bytecode (§2). Claims that remain unverified are explicitly listed in §7 and are NOT used as a basis for code.

---

## 0. Evidence index

| Tag | Evidence | How to reproduce |
|---|---|---|
| E1 | Upstream README source map; `app/build.gradle` (`applicationId`, `versionCode 150`, `versionName "v7.1.0"`, Java 17, StringFog deps) | `grep -nE 'applicationId\|versionCode\|versionName\|stringfog' app/build.gradle` |
| E2 | Real on-device projects and storage tree | `find /sdcard/.sketchware -maxdepth 3 -type f` |
| E3 | LFS jars; `oB`,`vB`,`yB`,`eC`,`hC`,`iC` class files | `curl -L https://media.githubusercontent.com/media/Sketchware-Pro/Sketchware-Pro/main/app/libs/a.a.a-notimportant-classes.jar` then `javap -p -c a/a/a/oB.class` |
| E4 | Path constants and build-path construction | `a/a/a/wq.java:12-33,39-49,125-150`; `a/a/a/yq.java:193-244` |
| E5 | Project metadata read/write and section layout | `a/a/a/lC.java:14-35,100-114,124-181` |
| E6 | `oB` bytecode: `AES/CBC/PKCS5Padding`, key/IV literal `"sketchwaresecure"`, modes 1/2 | `javap -p -c a/a/a/oB.class` — methods `b([B)`, `c([B)`, `d(String)`, `h(String)`, `a(String,[B)` |
| E7 | `vB`/`yB` Gson map conversion | `javap -p -c a/a/a/vB.class`, `javap -p a/a/a/yB.class` |
| E8 | Empirical decryption of real files | `java Decrypt /sdcard/.sketchware/mysc/list/602/project` (decryptor: key/IV `"sketchwaresecure"`, AES/CBC/PKCS5Padding) — plaintext JSON recovered |
| E9 | Empirical file-format classification of every file in `data/601`, `data/602`, `mysc/list` | `java Classify $(find /sdcard/.sketchware/data /sdcard/.sketchware/mysc/list -type f)` |
| E10 | Real section grammars for `file`, `view`, `logic`, `library`, `resource` | `java Decrypt /sdcard/.sketchware/data/602/{file,view,logic,library,resource}` |
| E10b | `view_root` grammar; `project_config`; `gradle_project.json` secrets | `java Dump /sdcard/.sketchware/data/602/view_root /sdcard/.sketchware/data/601/project_config` |
| E11 | Manager field/method signatures | `javap -p -classpath <jar> a.a.a.eC / a.a.a.hC / a.a.a.iC` |
| E12 | Manager usage in codegen | `a/a/a/yq.java:479-483` — `a(iC projectLibraryManager, hC projectFileManager, eC projectDataManager)` |
| E13 | Widget type table | `a/a/a/wq.java:70-123` |
| E14 | Block/palette/custom-block/events loaders | `mod/hey/studios/editor/manage/block/v2/BlockLoader.java:31-197`; `mod/hilal/saif/blocks/BlocksHandler.java`; `mod/hilal/saif/events/EventsHandler.java:38,386-459`; `mod/hey/studios/project/custom_blocks/CustomBlocksManager.java:108-240`; `dev/aldi/sayuti/block/ExtraBlockFile.java:17-19` |
| E15 | Block bean as persisted in real `logic` file | E10, `@MainActivity.java_onCreate_initializeLogic` section |
| E16 | Compile stages and commands | `/sdcard/.sketchware/debug.txt:25,28,31,36` (tag `AppBuilder`); `a/a/a/ProjectBuilder.java:188,240,453,539,605,767,950,994,1067,1078` |
| E17 | Activity map | `app/src/main/AndroidManifest.xml`: 76 `<activity>` elements, of which 71 declare a name ending in `Activity`. Verified by `grep -c` — not a claim about 71 distinct screens |
| E18 | Backup dir | `/sdcard/.sketchware/bak/602/{logic,view}` — byte-identical to live `view` (E10) |
| E19 | Resource roots and custom-block config | `/sdcard/.sketchware/resources/{block,fonts,icons,images,sounds,widgets}` verified present on device. `data/system/component.json` is a **source-declared path only** (`wq.java:35-36`); it was absent on this device (GAP-6) |

---

## 1. Data layout and file formats

### 1.1 Three distinct roots (all under `Environment.getExternalStorageDirectory()`, i.e. `/storage/emulated/0` → `/sdcard`) [E2,E4]

```
/sdcard/.sketchware/
├── mysc/
│   ├── list/<sc_id>/project          # encrypted project metadata (ONE blob)
│   └── <sc_id>/                      # generated Gradle build working tree
│       ├── app/src/main/{java,res,res/layout,res/raw,assets,assets/fonts}
│       ├── app/src/main/AndroidManifest.xml
│       ├── app/build.gradle, build.gradle, settings.gradle, gradle.properties
│       ├── gen/                      # generated R.java / databinding
│       └── bin/                      # classes/, classes.dex, *.apk.res, *.apk.unsigned[.aligned], *.apk,
│                                     # aapt_rules.pro, classes_proguard.jar, mapping.txt, seeds.txt, usage.txt
├── data/<sc_id>/                     # AUTHORITATIVE project data (see 1.3)
├── bak/<sc_id>/                      # backups of view/logic (byte-identical snapshots) [E18]
├── libs/local_libs/                  # local library AAR/JAR storage
├── resources/{block,fonts,icons,images,sounds,widgets}/
├── temp/{images,sounds,fonts,proj,data}/
├── collection/, download/, SKX/, daydreamtemp/
├── data/system/component.json        # custom component registry [E4]
└── debug.txt                         # build log (tag AppBuilder) [E16]
```

**Important resolution of an earlier hypothesis:** both `data/<id>/` and `mysc/<id>/` really exist. `yq` is constructed with `wq.d(sc_id)` = `data/<id>/` as its "myscFolderPath" in `DesignActivity`/`ExportProjectActivity` [E4,E12], while the *generated build tree* is physically under `mysc/<id>/` [E2]. Sketchware Pro copies/regenerates the build tree from `data/<id>/` at build time. **Do not assume they are interchangeable.** The offline repository reads `data/<id>/`; the live bridge must observe which directory the running activity is using.

### 1.2 Project metadata — `mysc/list/<id>/project` [E5,E8]

One AES blob decrypting to a single flat JSON object. Real keys (project 601):
```
custom_icon, sc_ver_code, my_ws_name, build_system, color_accent, my_app_name,
sc_ver_name, sc_id, color_primary, color_control_highlight, color_control_normal,
my_sc_reg_dt, sketchware_ver, isIconAdaptive, my_sc_pkg_name, gradle_ide_only, color_primary_dark
```
Numbers such as colors are JSON doubles (`-5218972.0`). This file is the project index: `lC.a()` enumerates `mysc/list/*/project` and keeps entries whose `sc_id` matches the directory name [E5].

### 1.3 Project data directory — `data/<id>/` [E2,E9,E10,E10b]

Classified against real files (`AES` = decrypts with the §2 key; `PLAIN` = plain text/JSON/XML):

| File | Format | Content / grammar |
|---|---|---|
| `view` | AES | Sectioned: `@<layout.xml>` then one `ViewBean` JSON per line [E10] |
| `view_root` | PLAIN JSON | `{ "<layout>.xml": { "attributes": {...}, "class_name": "..." } }` root layout attributes per screen [E10b] |
| `logic` | AES | Sectioned: `@<Activity>.java_<section>` then lines [E10] |
| `file` | AES | Sections `@activity`, `@customview`; `ProjectFileBean` lines (`fileName`, `fileType`, `orientation`, `theme`, `options`, `qualifier`, `keyboardSetting`) [E10] |
| `library` | AES | Sections per built-in library (`@firebaseDB`, `@compat`, `@admob`, `@googleMap`, …), one `ProjectLibraryBean` JSON each [E10] |
| `resource` | AES | Sections `@images`, `@sounds`, `@fonts` [E10] |
| `java` | AES | Custom Java source index (empty in samples) [E9] |
| `local_library` | AES | Local library index (empty in samples) [E9] |
| `permission` | PLAIN/AES | JSON array; sample 601 `[]`, sample 602 empty file [E9] |
| `project_config` | PLAIN JSON | `project_template`, `min_sdk`, `target_sdk`, `enable_viewbinding`, `build_system`, `xml_command`, `manifest_manual_edit_enabled`, `app_class`, `gradle_version`, `enable_native`, … [E10b] |
| `build_config` | PLAIN JSON | `dexer`, `java_ver`, optionally `compile_sdk`, `enable_logcat`, `no_http_legacy`, `classpath` [E9,E10b] |
| `proguard` | PLAIN JSON | `debug`, `enabled`, `r8`/`optimize`, `shrink_resources` [E9] |
| `proguard-rules.pro` | PLAIN | raw ProGuard rules [E9] |
| `AndroidManifest.xml` | PLAIN XML | project manifest [E9] |
| `Injection/androidmanifest/manual_manifest.xml` | PLAIN XML | manual manifest injection [E9] |
| `dependencies.json` | PLAIN JSON | Maven coordinates + sync status [E9] |
| `gradle_project.json` | PLAIN JSON | full Gradle project settings **including `debugKeyPass`, `debugStorePass`, `releaseKeyPass`, `releaseStorePass`, keystore paths/aliases** → SECRET, must be redacted before LLM [E10b] |
| `app.gradle`, `custom_gradle/**`, `gradle_project.json` | PLAIN | user gradle customizations [E9] |
| `DataDayDream.json`, `stringfog`, `proguard`, … | PLAIN JSON | Pro feature flags [E9] |
| `files/{aidl,java,native,resource}`, `files/resource/values/strings.xml` | PLAIN | user custom source/resource files (ManageJava/ManageAssets/ManageResource) [E2,E9] |
| `custom_blocks/` | directory | Pro custom block definitions when present [E14] |

### 1.4 Sectioned-file grammar (encrypted payloads) [E10]

```
payload        := ( section_header nl )* 
section_header := '@' section_name
section_name   := 'activity' | 'customview' | 'images' | 'sounds' | 'fonts'
                | <layout_file_name>                         (view)
                | <Activity>.java_<logic_section>            (logic)
                | <built_in_library_id>                       (library)
body lines     := one Gson-JSON object per line (or `name:value` for `*_list`/`*_func`)
```

Real `logic` sections (project 602):
`@MainActivity.java_list`, `@MainActivity.java_func`, `@MainActivity.java_components`,
`@EditorActivity.java_components`, `@MainActivity.java_events`,
`@MainActivity.java_onCreate_initializeLogic`, `@MainActivity.java_onResume_onResume`.

`*_list` uses `3:notes` (count:name); `*_func` uses `loadData:loadData`; `*_components`, `*_events`,
`*_<event>` use JSON lines. The section name encodes `<Activity>.java_<sectionKind>_<eventName>`.

### 1.5 Build working tree — `mysc/<id>/` [E2,E16]

Generated by `yq` (`generateGradleFiles`, `generateDebugFiles`, `getFileSrc`) from `ViewBean`/`BlockBean`/`ProjectFileBean`/library managers, then compiled by `ProjectBuilder`. Physical outputs observed: `bin/QuickNotes.apk.res`, `bin/QuickNotes.apk.unsigned`, `bin/QuickNotes.apk`, `bin/classes/`, `bin/dex/classes*.dex`, `gen/`, `bin/aapt_rules.pro`.

---

## 2. Encryption and serialization

### 2.1 Exact cipher (recovered from bytecode, then empirically verified) [E6,E8]

```
Cipher.getInstance("AES/CBC/PKCS5Padding")
byte[] key = "sketchwaresecure".getBytes();     // 16 bytes -> AES-128
SecretKeySpec(key, "AES")
IvParameterSpec(key)                            // SAME 16 bytes used as IV
Cipher.ENCRYPT_MODE (1) / DECRYPT_MODE (2)
```

- There is **no Base64 and no salt**; the file on disk is raw ciphertext, length a multiple of 16.
- `oB.c(byte[])` = encrypt, `oB.d(String)` = `c(s.getBytes("UTF-8"))`, `oB.b(byte[])` = decrypt,
  `oB.a(byte[])` = `new String(b(bytes), "UTF-8")`, `oB.h(path)` = read all raw bytes,
  `oB.a(path, bytes)` = write raw bytes [E6].
- Read chain used by the app: `vB.a(oB.a(oB.h(path)))` → `HashMap<String,Object>` [E5,E7].
- Write chain: `oB.a(path, oB.d(vB.a(map)))` [E5,E7].
- `vB` is `new Gson().toJson(map)` / `Gson.fromJson(json, TypeToken<HashMap>)`; `yB` provides typed getters [E7].

### 2.2 Empirical verification [E8,E9]

Decrypting real files with the above parameters produced valid JSON/sectioned text for:
`mysc/list/601/project`, `mysc/list/602/project`, `data/601|602/{file,view,logic,library,resource}`,
`bak/602/view`. This confirms the algorithm, key, IV, and that each encrypted file is **one whole-file
AES blob** containing the sectioned payload of §1.4.

Non-encrypted config files were also identified (§1.3) — the module must branch on file type, not assume
everything is AES.

### 2.3 Edge cases and cautions

- 0-byte files exist (`java`, `local_library`, `permission`) — treat as empty, not as cipher failure [E9].
- A 16-byte `data/601/logic` did not yield UTF-8 plaintext; handle empty/degenerate logic defensively [E9].
- **Version pinning:** the key literal was recovered from this specific release's jar. It is a *verified fact for this version*, but must be re-verified per Sketchware Pro version (resolver cache key: `versionCode + dex hash`) rather than assumed universal. See `docs/ASSUMPTIONS.md` A5.
- **Secrets:** `gradle_project.json` holds keystore passwords in plaintext [E10b]. The crypto module must encrypt these at rest in SketchPilot and redact them from all LLM payloads (requirement §1 hard constraints).

---

## 3. In-memory managers (live state)

### 3.1 Verified manager classes [E11,E12]

Named by local variable types in `yq.a(iC projectLibraryManager, hC projectFileManager, eC projectDataManager)`:

| Class | Role | Fingerprint (fields, from javap) |
|---|---|---|
| `a.a.a.eC` | **project data manager** — views, logic, components, events | `String a`; `oB b`; `HashMap<String,ArrayList<ViewBean>> c`; `HashMap<String,HashMap<String,ArrayList<BlockBean>>> d`; `HashMap<String,ArrayList<Pair<Integer,String>>> e,f`; `HashMap<String,ArrayList<Pair<String,String>>> g`; `HashMap<String,ArrayList<ComponentBean>> h`; `HashMap<String,ArrayList<EventBean>> i`; `ViewBean j`; `Gson k`; `jq l`. Serializers `a(StringBuffer)`, `b(StringBuffer)`; savers `a()`…`l()`. |
| `a.a.a.hC` | **project file manager** — screens/activities | `ArrayList<String> a,b`; `ArrayList<ProjectFileBean> c,d`; `String e`; `oB f`; `Gson g`. Loaders `a(BufferedReader)`, `e(StringBuffer)`; savers `a()`…`m()`. |
| `a.a.a.iC` | **project library manager** | `String a`; `oB b`; `ProjectLibraryBean c,d,e,f`; `Gson g`. Methods `a(StringBuffer)`, `a(BufferedReader)`, savers `a()`…`l()`. |
| `a.a.a.oB` | encrypted file I/O (§2) | `boolean a`; ctor `oB(boolean)`; `d(String)`, `a(byte[])`, `h(String)`, `a(String,byte[])`, `b/c(byte[])`, `f(String)`, `g(String)` |
| `a.a.a.vB`, `a.a.a.yB` | Gson map (de)serialization and typed getters | static `a(HashMap)`, `a(String)`; `a/b/c(Map,String,…)` |
| `a.a.a.lC` | project-metadata registry | static `a()`, `a(String)`, `a(String,HashMap)`, `b(String)`, `b()` |

Bean types are in the `com.besome.sketch.beans` package: `ViewBean`, `BlockBean`, `ComponentBean`, `EventBean`, `ProjectFileBean`, `ProjectLibraryBean`, `ProjectResourceBean` [E11].

### 3.2 How editors read and refresh [E4,E12]

- Editors construct `new yq(context, sc_id)` → `yq(context, wq.d(sc_id), lC.b(sc_id))`. The metadata map
  from `lC.b(sc_id)` supplies `sc_id`, package, name, version, colors; `yq` derives all build paths.
- `oB` instance inside each manager (`eC.b`, `hC.f`, `iC.b`) performs the encrypted read of `view`, `logic`,
  `file`, `library` from `data/<id>/`.
- `eC.a(StringBuffer)`/`b(StringBuffer)` round-trip the sectioned payloads of §1.4; `hC`/`iC` do the same
  for `file`/`library`.
- UI refresh therefore requires: mutate manager → call its saver (`a()`, `h()`, etc.) → ask the activity/fragment
  to reload from the manager (the concrete refresh hook is **not yet resolved**; see §7 GAP-3).

### 3.3 Resolver strategy (constraint: never hardcode obfuscated names without fallback) [E1,E3]

- Names above are **version-specific**. Resolve with DexKit using a composite fingerprint:
  1. class declares field of type `HashMap<String,ArrayList<com.besome.sketch.beans.ViewBean>>` (identifies `eC`);
  2. method signature `a(StringBuffer)` + `a(BufferedReader)` + constructor `(String)`;
  3. call relation: referenced from method with descriptor `(La/a/a/iC;La/a/a/hC;La/a/a/eC;)V`;
  4. string constants recovered at runtime (`view`, `logic`, `root`) — but note **StringFog/XOR obfuscation**
     is applied at build time [E1], so raw source-string matching is unreliable; prefer type/signature and
     call-graph matching, with decoded-string matching as a secondary signal.
- Cache key: `versionCode + dex hash` [E1]. Any unresolved live hook degrades to offline mode (never crash).

---

## 4. Blocks, palette, widgets, components, events

### 4.1 Block/opcode model (persisted form) [E15]

Real `BlockBean` JSON from `data/602/logic`:
```json
{"color":-10701022,"id":"10","nextBlock":-1,"opCode":"addSourceDirectly",
 "parameters":["getSupportActionBar().setDisplayHomeAsUpEnabled(false); ..."],
 "spec":"add source directly %s.inputOnly","subStack1":-1,"subStack2":-1,
 "type":" ","typeName":""}
```
Fields: `color`, `id`, `nextBlock` (linked-list chain, `-1` = end), `opCode`, `parameters[]`, `spec`,
`subStack1`, `subStack2` (nested chains), `type`, `typeName`.
This is a native IR already: `opCode` + typed parameters + spec + sub-stacks. Our `blocks` module should
compile IR→Bean by preserving these fields verbatim and validating `opCode` against the runtime registry.

### 4.2 Palette / block registries [E14,E19]

- Built-in block definitions: `mod.hilal.saif.blocks.BlocksHandler` (static `HashMap` per block with keys
  `id`, `opCode`, `spec`, `parameters`, `palette`, etc.); Pro "extra" blocks in `mod.hilal.saif.blocks`.
- Custom blocks: `.sketchware/data/<id>/custom_blocks` (`BlockLoader`, `CustomBlocksManager`),
  palette colors from `.sketchware/resources/block/My Block/palette.json` [E19], extra block files declared
  in `dev.aldi.sayuti.block.ExtraBlockFile`.
- Custom events: `.sketchware/data/system/events.json` (`EventsHandler.getBlocks`, `getPalette`) [E14].
- More-blocks/variables/lists: sectioned into `logic` via `Pair<Integer,String>`/`Pair<String,String>` maps in `eC.e/f/g` [E11].
- The registry must be **read at runtime from the app** (requirement §3 `blocks`) — these paths and bean
  shapes are the evidence for that runtime read.

### 4.3 Widget / property model [E13,E10,E10b]

- `wq.b(int type)` maps type ordinals 0–48: `linear`, `relativelayout`, `hscroll`, `button`, `textview`,
  `edittext`, `imageview`, `webview`, `progressbar`, `listview`, `spinner`, `checkbox`, `vscroll`, `switch`,
  `seekbar`, `calendarview`, `fab`, `adview`, `mapview`, `radiobutton`, `ratingbar`, `videoview`, `searchview`,
  `autocomplete`, `multiautocomplete`, `gridview`, `analogclock`, `datepicker`, `timepicker`, `digitalclock`,
  `tablayout`, `viewpager`, `bottomnavigation`, `badgeview`, `patternlockview`, `wavesidebar`, `cardview`,
  `collapsingtoolbar`, `textinputlayout`, `swiperefreshlayout`, `radiogroup`, `materialbutton`, `signinbutton`,
  `circleimageview`, `lottie`, `youtube`, `otpview`, `codeview`, `recyclerview`; default → `widget`.
- Real `ViewBean` JSON keys: `id`, `type`, `parentType`, `parent`, `index`, `preIndex`, `preParentType`,
  `convert`, `customView`, `injected`, `layout{width,height,weight,weightSum,margin*,padding*,gravity,layoutGravity,orientation,backgroundColor,borderColor}`,
  `text{text,hint,textColor,hintColor,textSize,textFont,textType,inputType,imeOption,singleLine,line}`,
  `image{scaleType,rotate}`, `alpha`, `scaleX/Y`, `translationX/Y`, `enabled`, `clickable`, `checked`,
  `choiceMode`, `dividerHeight`, `firstDayOfWeek`, `progress`, `progressStyle`, `max`, `spinnerMode`,
  `indeterminate`, `inject`, `parentAttributes` [E10].
- Root layout attributes per screen come from plaintext `view_root` (`attributes` + `class_name`) [E10b].
- `.sketchware/resources/widgets/widgets.json` is `[]` on this device — custom widget definitions are optional [E19].

### 4.4 Component registry [E10,E11,E4]

- Real `ComponentBean` JSON: `{"componentId":"prefs","param1":"data","param2":"","param3":"","type":2}`,
  `{"componentId":"intent",...,"type":1}` — stored in `@<Activity>.java_components` sections.
- Custom component definitions live at `.sketchware/data/system/component.json` (`wq.getCustomComponent()`) [E4].

### 4.5 Event model [E10,E14]

- `EventBean` JSON: `{"eventName":"onResume","eventType":3,"targetId":"onResume","targetType":0}`,
  `{"eventName":"onItemClicked","eventType":1,"targetId":"lv_notes","targetType":9}` — stored in
  `@<Activity>.java_events`.
- Event logic lives in `@<Activity>.java_<eventName>_<eventName>` sections; `onCreate_initializeLogic` is
  the initialization chain.
- Custom events/palettes: `EventsHandler` with `events.json`/`listeners.json` under
  `.sketchware/data/system/` [E14].

---

## 5. Compile pipeline [E16]

Stages observed in the real `debug.txt` (`D/AppBuilder:` tag) and source:

1. **Generate sources** — `yq` writes `app/src/main/{java,res,layout,AndroidManifest.xml}`, `gen/`, gradle files from managers (`Ix` manifest, `Jx` activity source, `Lx` components/listeners, `Ox` layout XML).
2. **Resource compile** — `/data/user/0/pro.sketchware/cache/aapt2 compile --dir .../app/src/main/res -o .../bin/res/project.zip`, then `aapt2 link ... -I .../files/libs/android.jar -A assets -R <compiled libs>.zip ... --java .../gen --proguard .../bin/aapt_rules.pro --manifest ... -o .../bin/<project>.apk.res`.
3. **Java compile** — Eclipse compiler (ECJ) invoked with `-1.8`/`-11`, `-d .../bin/classes`, huge `-cp` of `files/libs/libs/*/classes.jar`, sources `.../app/src/main/java` + `.../gen`. `ProjectBuilder.compileJavaCode()`.
4. **Dex** — D8 if enabled (`ProjectBuilder.isD8Enabled`, `createDexFilesFromClasses`); 56 DEX files merged into `classes.dex`; outputs `bin/dex/classes*.dex`.
5. **Shrink/obfuscate (optional)** — `runR8()` / `runProguard()` → `bin/classes_proguard.jar`, `mapping.txt`, `seeds.txt`, `usage.txt`; `runStringfog()`.
6. **Package & sign** — `buildApk()` → `*.apk.unsigned`, `zipalign` → `*.apk.unsigned.aligned`, `signDebugApk()` → `*.apk`; AAB path also supported (`buildAppBundle`).
7. **Logs/diagnostics** — `debug.txt` (tag `AppBuilder`) plus `CompileLogActivity` / `CollectErrorActivity` UI. Error strings originate from aapt2 (XML/resource), ECJ (Java), D8/R8 (dex/shrink), zipalign/apksig (packaging/signing).

**Implications for `build.*` tools:** trigger via `ProjectBuilder` with a `BuildProgressReceiver`; capture stdout/stderr by wiring the existing writers (`ProjectBuilder.compileJavaCode` installs `OutputStream` writers at lines 453–479); stream logs from `debug.txt` and parse into structured diagnostics keyed by stage. A dry compile for transaction verification can reuse stages 1–3 without signing.

---

## 6. Activity map for UI injection [E17]

| Area | Activities (manifest names) | Injection target |
|---|---|---|
| Launcher / project list | `pro.sketchware.activities.main.activities.MainActivity`, `...fragments.projects_store.ProjectPreviewActivity` | FAB + project card context menu |
| Designer | `com.besome.sketch.design.DesignActivity`, `com.besome.sketch.editor.PropertyActivity`, `a.a.a.ViewEditorFragment` | canvas context, property sheet, selected view |
| Logic editor | `com.besome.sketch.editor.LogicEditorActivity`, `com.besome.sketch.editor.event.AddEventActivity`, `com.besome.sketch.editor.makeblock.MakeBlockActivity` | block context menu, palette |
| Views / custom views | `...editor.manage.view.{AddViewActivity,ManageViewActivity,AddCustomViewActivity,PresetSettingActivity,ViewSelectorActivity}` | library dialogs |
| Library manager | `...editor.manage.library.{ManageLibraryActivity,AdmobActivity,FirebaseActivity,GoogleMapActivity,compat.ManageCompatActivity,material3.Material3LibraryActivity}`, `dev.aldi.sayuti.editor.manage.ManageLocalLibraryActivity`, `mod.jbk.editor.manage.library.ExcludeBuiltInLibrariesActivity` | library dialogs |
| Resources | `...editor.manage.{image,font,sound}.{Add*,Manage*}Activity`, `mod.agus.jcoderz.editor.manage.resource.ManageResourceActivity`, `pro.sketchware.activities.resourceseditor.ResourcesEditorActivity` | resource pickers |
| Code / XML | `...editor.view.{CodeViewerActivity,ViewCodeEditorActivity}`, `...editor.command.ManageXMLCommandActivity`, `mod.hey.studios.activity.managers.{java.ManageJavaActivity,assets.ManageAssetsActivity,nativelib.ManageNativelibsActivity}` | source editor menu |
| Components | `...component.{AddCustomComponentActivity,ManageCustomComponentActivity}`, `...appcompat.ManageAppCompatActivity`, `mod.agus.jcoderz.editor.manage.permission.ManagePermissionActivity` | component dialogs |
| Project settings | `com.besome.sketch.projects.MyProjectSettingActivity`, `mod.hilal.saif.activities.tools.ConfigActivity`, `mod.hey.studios.project.proguard.ManageProguardActivity`, `mod.hilal.saif.activities.tools.BlocksManagerCreatorActivity`/`DetailsActivity` | settings rows |
| Build / debug | `com.besome.sketch.tools.{CompileLogActivity,CollectErrorActivity,NewKeyStoreActivity}`, `mod.khaled.logcat.LogReaderActivity`, `pro.sketchware.activities.debug.RemoteDebugActivity` | build log toolbar ("Ask AI to fix") |
| Export / other | `com.besome.sketch.export.ExportProjectActivity`, `...help.*`, `...iconcreator.IconCreatorActivity`, `...preview.LayoutPreviewActivity` | export flow |

76 `<activity>` elements / 71 activity names ending in `Activity` (E17, source-verified only; no device UI walk was performed). Overlay injection should hook `onCreate`/`onResume` of these entry activities, with
the panel context derived from the current `sc_id`, `ProjectFileBean`, and selected `ViewBean`/`BlockBean`.

---

## 7. Open gaps / hypotheses (NOT to be treated as verified)

- **GAP-1** Live manager *instances* (singletons vs per-activity) and their static holder class names are not yet resolved. Fingerprints in §3 are type-level only. Needs DexKit call-graph pass against installed APK.
- **GAP-2** The exact UI-refresh method called after a manager save (e.g. `DesignActivity.notifyDataSetChanged`-equivalent) is not resolved.
- **GAP-3** Built-in block palette JSON location: source constants found (`My Block/menu.json`, `ExtraBlockFile`), but the on-device `palette.json` was empty; need a project with custom blocks to verify content shape.
- **GAP-4** Installed APK's `versionCode` could not be read directly (no `/data/app` read access); source says 150, project metadata says `sketchware_ver:193` — reconcile per-version at runtime, never hardcode.
- **GAP-5** Exact format of `java`/`local_library` encrypted payloads (samples were empty). Needs a project with custom Java/local libs.
- **GAP-6** `.sketchware/data/system/{events.json,listeners.json,component.json}` were not present on this device; shapes taken from source constants only.
- **GAP-7** Gradle export path (`gradle_project.json` → Android Studio project) was not exhaustively traced; relevant for `project.export`.

These gaps are tracked in `docs/COVERAGE.md` with status `gap` and must be closed in later phases before the
corresponding live tools are marked supported.
