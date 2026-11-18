package snd.sketchpilot.mcp;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/** Local MCP server configuration and pairing token management. */
public final class McpConfig {

    public static final int DEFAULT_PORT = 17890;
    public static final String ENDPOINT = "/mcp";

    private static final File DIR = new File("/storage/emulated/0/.sketchware/sketchpilot");
    public static final File TOKEN_FILE = new File(DIR, "mcp_token");
    public static final File AUDIT_FILE = new File(DIR, "mcp-audit.log");
    public static final File ALLOWLIST_FILE = new File(DIR, "mcp_tool_allowlist.txt");

    private McpConfig() {
    }

    public static String getOrCreateToken() {
        if (TOKEN_FILE.isFile()) {
            try {
                String token = new String(Files.readAllBytes(TOKEN_FILE.toPath()), StandardCharsets.UTF_8).trim();
                if (token.length() > 0) {
                    return token;
                }
            } catch (Throwable ignored) {
                // regenerate
            }
        }
        String generated = "sp_" + UUID.randomUUID().toString().replace("-", "");
        FileWriter writer = null;
        try {
            if (!DIR.exists()) {
                DIR.mkdirs();
            }
            writer = new FileWriter(TOKEN_FILE, false);
            writer.write(generated);
            writer.flush();
        } catch (Throwable ignored) {
            // token will be ephemeral
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
        return generated;
    }
}
