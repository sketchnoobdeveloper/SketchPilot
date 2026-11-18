# SketchPilot MCP Server — setup

## Verified protocol facts (checked 2026-11-18)

- Latest stable MCP revision: **2026-07-28** (`modelcontextprotocol.io/specification/2026-07-28`).
- Older revisions still supported by the server: `2025-11-25`, `2025-06-18`, `2025-03-26`.
- Transport: Streamable HTTP at `http://127.0.0.1:17890/mcp`, plus a stdio bridge (`StdioBridge`).
- Auth: bearer token in `Authorization: Bearer <token>`; token generated at
  `/storage/emulated/0/.sketchware/sketchpilot/mcp_token` (QR/copy from the companion app in later UX work).
- OpenCode V2 config places servers under **`mcp.servers`**, not directly under `mcp`. Verified from
  `https://opencode.ai/docs/mcp-servers.md`. Remote servers use OAuth **by default**, so `oauth: false`
  is required for our bearer-token server. `protocol: "auto"` probes `server/discover` for the
  2026-07-28 revision and falls back to the classic `initialize` handshake.

## Register in OpenCode

Add to the project's `opencode.jsonc` (or global config):

```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "servers": {
      "sketchpilot": {
        "type": "remote",
        "url": "http://127.0.0.1:17890/mcp",
        "oauth": false,
        "protocol": "auto",
        "headers": {
          "Authorization": "Bearer {env:SKETCHPILOT_MCP_TOKEN}"
        }
      }
    }
  }
}
```

Then:

```sh
export SKETCHPILOT_MCP_TOKEN="$(cat /storage/emulated/0/.sketchware/sketchpilot/mcp_token)"
opencode mcp list
```

OpenCode names tools `<server>_<tool>` (e.g. `sketchpilot_project_list`). MCP prompts become commands
like `/sketchpilot:fix_build_error`.

## Capabilities exposed

- **Tools** — the full catalog from `docs/COVERAGE.md`, filtered by `mcp_tool_allowlist.txt`.
- **Resources** — `sketchware://project/{id}/{metadata|view|logic|file|library|config|buildlog}`.
- **Prompts** — `fix_build_error`, `create_screen`, `refactor_logic`.
- **Notifications** — SSE channel on `GET /mcp` (keepalive frames).
- **Audit log** — `/storage/emulated/0/.sketchware/sketchpilot/mcp-audit.log`.
- **Rate limit** — 120 calls/minute per bearer token.

## Companion app controls

Start/stop the foreground service (`McpService`), copy the pairing token, edit the port (constant in
`McpConfig`), and manage the tool allowlist. Live-state calls proxy through `core.bridge` when
Sketchware is running; otherwise the offline engine answers.
