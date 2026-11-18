# Assumptions Log — SketchPilot

Every assumption is recorded with a status and a falsification/verification path. Verified items are still
recorded because they are version-pinned and must be re-checked per Sketchware Pro release.

| ID | Assumption | Status | Basis / verification path |
|---|---|---|---|
| A1 | Scope package is `pro.sketchware`; forks/clones are configurable additional scopes. | **Verified** | `app/build.gradle` `applicationId = "pro.sketchware"` [E1] |
| A2 | Source of truth for project data is `data/<id>/`; `mysc/<id>/` is a generated build tree, not interchangeable. | **Verified** | both trees observed on device; `yq` constructed from `wq.d(sc_id)` [E2,E4] |
| A3 | Project metadata lives in `mysc/list/<id>/project`, and `lC.a()` enumerates it by matching `sc_id`. | **Verified** | `lC.java:14-35` + real decrypted files [E5,E8] |
| A4 | Project data files are whole-file AES/CBC/PKCS5 blobs with key/IV `"sketchwaresecure"`. | **Verified for v7.1.0 / code 150** | `oB` bytecode + empirical decryption [E6,E8]. Re-verify per version; never hardcode without resolver fallback. |
| A5 | Not every project file is encrypted: config/XML/JSON files are plaintext. | **Verified** | classification of all files in two real projects [E9] |
| A6 | Encrypted payloads use a section grammar `@section` + one JSON object per line. | **Verified** | real `file`, `view`, `logic`, `library`, `resource` [E10] |
| A7 | Live managers are `eC` (data), `hC` (files), `iC` (libraries), with type-level fingerprints recorded. | **Verified (type-level)** | `javap` + `yq.a(iC,hC,eC)` [E11,E12]. Instance holders/refresh hooks NOT verified (GAP-1/2). |
| A8 | Obfuscated names are version-specific; DexKit resolution must use signatures, field types, call relations, and runtime-decoded strings, with a `versionCode + dex hash` cache. | **Decision** | source is heavily obfuscated and StringFog/XOR is applied at build time [E1,E3] |
| A9 | String literals from decompiled source cannot be matched directly against the shipped DEX. | **Decision** | StringFog deps + `stringfog.zip` in build [E1]; use type/graph-first matching |
| A10 | The block registry must be read at runtime from the app, not baked into SketchPilot. | **Decision** | custom blocks/palettes/events are user-extensible at runtime [E14,E19] |
| A11 | `gradle_project.json` contains keystore passwords; treat as secret, encrypt at rest, redact before LLM. | **Verified** | real file contains `debugKeyPass`, `releaseStorePass`, etc. [E10b] |
| A12 | A real device with installed Sketchware Pro is available in this environment for verification. | **Verified** | `/sdcard/.sketchware` with projects 601/602 and `debug.txt` [E2] |
| A13 | Offline writes must be blocked while a Sketchware session has the same `sc_id` open (conflict safety). | **Decision** | requirement §5; no lock file found on device, so use bridge liveness + in-memory open-set |
| A14 | Backups precede every write; `bak/<id>/` is the host's own backup convention. | **Verified (host) / Decision (module)** | `bak/602/{logic,view}` present and byte-identical to live files [E18] |
| A15 | Installed release versionCode equals source's 150. | **Unverified** | no `/data/app` read access; project metadata says `sketchware_ver:193` [E9]. Reconcile at runtime (GAP-4). |
| A16 | `java`/`local_library` encrypted payload grammar, `events.json`/`component.json` shapes, and Android Studio export grammar. | **Unverified** | samples empty/absent on device (GAP-5/6/7). Source constants only. |

## Phase 0 plan and assumptions (delivery record)

- **Plan:** static recon of upstream source + LFS jars, then empirical verification against the on-device
  projects, producing `SKETCHWARE_INTERNALS.md`, `COVERAGE.md`, and this log. No feature code was written.
- **Assumptions governing later phases:** A2, A4, A6, A8, A9, A10, A11, A13, A14 are binding design inputs.
  A15/A16 must not be relied upon until verified.
- **Next phase (P1):** module skeleton, DexKit resolver + capability matrix, file logging, offline read-only
  repository implementing exactly the formats in §1–2, and `scripts/check_constraints.sh` enforcement in CI.
