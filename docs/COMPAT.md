# Compatibility matrix

| Sketchware Pro version | versionCode | Status | Crypto key/IV | Manager hints | Basis |
|---|---|---|---|---|---|
| v7.1.0 | 150 | **Verified** | `sketchwaresecure` (AES-128/CBC, key=IV) | `a.a.a.eC/hC/iC/oB/lC/yq` | upstream commit `2c721f0` + on-device projects 601/602 + `javap` + decryption |
| v7.0.0-beta | unknown | Hypothesis | `sketchwaresecure` (unverified) | same hints, shape-verified | upstream tag only; re-verify key by decrypting a real `mysc/list/<id>/project` before any write |
| forks / clones | unknown | Unverified | must probe | fingerprints only | scope list in `sketchpilot/scopes.txt`; shape matching is mandatory |

## Runtime procedure for an unknown version

1. Read `versionCode` and APK path from the hooked process; compute `versionCode:SHA-256(apk)` (`VersionKey`).
2. Run the DexKit resolver with shape fingerprints (`CapabilityMatrix`). Obfuscated names are hints only.
3. Probe the crypto key against `mysc/list/<id>/project`: decryption must yield JSON containing `sc_id`.
4. If probing fails, mark the capability `OFFLINE` and **refuse writes** (`CryptoRegistry.allowWrite`).
5. Record the result in the capability snapshot for the Capability Matrix screen.

## Hardening enforced in P7

- Every hook body wrapped in `try/catch(Throwable)` with file logging (`SafeHooks`, `Logger`).
- Global kill switch (`KillSwitch`) read at entry and toggleable from the companion app.
- Conflict safety: `ConflictGuard` rejects offline writes when the live bridge is reachable, a live session
  is registered, or a lock file exists; wired into `TransactionManager.commit`.
- Per-version crypto gating: unverified profiles cannot write without an explicit override.
- Filesystem sandbox restricts writes to `data/<id>`, `mysc/<id>`, `mysc/list/<id>` and backups.
- Secrets redacted before any LLM payload (`Redactor`); keystore-backed storage (`SecretsStore`).
- MCP server: bearer token, rate limit, per-client tool allowlist, audit log.
