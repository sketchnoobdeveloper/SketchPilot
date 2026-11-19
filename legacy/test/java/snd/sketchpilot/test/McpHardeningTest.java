package snd.sketchpilot.test;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import snd.sketchpilot.mcp.protocol.McpDispatcher;
import snd.sketchpilot.mcp.server.McpServer;
import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.tools.ToolCatalog;
import snd.sketchpilot.util.AuditLog;

/** MCP hardening tests: auth, Origin, size, malformed JSON, allowlist, audit redaction. */
public final class McpHardeningTest {

    private static int passed = 0;
    private static final int PORT = 18991;
    private static final String TOKEN = "sp_hardening_test";

    public static void main(String[] args) throws Exception {
        SketchwarePaths paths = new SketchwarePaths(new File("/sdcard"));
        ProjectEditor editor = new ProjectEditor(paths, new SketchwareCrypto());
        File audit = new File("/tmp/opencode/mcp-hardening-audit.log");
        audit.delete();
        McpDispatcher dispatcher = new McpDispatcher(
                ToolCatalog.build(editor, paths), editor.getRepository(), audit);
        McpServer server = new McpServer(PORT, TOKEN, dispatcher);
        server.start();
        Thread.sleep(300);

        String valid = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        check("valid token accepted", post(valid, TOKEN, null, 0) == 200);
        check("wrong token rejected 401", post(valid, "wrong", null, 0) == 401);
        check("missing token rejected 401", post(valid, null, null, 0) == 401);
        int badOriginStatus = post(valid, TOKEN, "https://evil.example", 0);
        check("bad Origin rejected 403", badOriginStatus == 403);
        check("local Origin accepted", post(valid, TOKEN, "http://127.0.0.1:1234", 0) == 200);
        check("oversized body rejected 413", post(valid, TOKEN, null, 1200000) == 413);
        check("malformed JSON still 200 with JSON-RPC error", post("{not json", TOKEN, null, 0) == 200);

        String notAllowed = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"project.list\",\"arguments\":{}}}";
        List<String> restricted = new ArrayList<String>();
        restricted.add("kb.search");
        dispatcher.setAllowedTools(restricted);
        String response = postString(notAllowed, TOKEN);
        check("tool not allowed returns JSON-RPC error",
                response.contains("tool not allowed") || response.contains("-32602"));

        String leak = "apiKey=sk-abcdefghijklmnop token=sp_hardening_test";
        new AuditLog(audit).record("test.write", leak);
        String auditText = new String(java.nio.file.Files.readAllBytes(audit.toPath()), StandardCharsets.UTF_8);
        check("audit contains no API key", !auditText.contains("sk-abcdefghijklmnop"));
        check("audit contains no token", !auditText.contains("sp_hardening_test"));

        server.stop();
        System.out.println("McpHardeningTest: " + passed + " assertions passed.");
    }

    private static int post(String body, String token, String origin, int padding) throws Exception {
        return Integer.parseInt(postFull(body, token, origin, padding)[0]);
    }

    private static String postString(String body, String token) throws Exception {
        return postFull(body, token, null, 0)[1];
    }

    private static String[] postFull(String body, String token, String origin, int padding) throws Exception {
        String payload = body;
        if (padding > 0) {
            StringBuilder sb = new StringBuilder(body);
            while (sb.length() < padding) {
                sb.append(' ');
            }
            payload = sb.toString();
        }
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        java.net.Socket socket = new java.net.Socket("127.0.0.1", PORT);
        try {
            StringBuilder request = new StringBuilder();
            request.append("POST /mcp HTTP/1.1\r\n");
            request.append("Host: 127.0.0.1:").append(PORT).append("\r\n");
            request.append("Content-Type: application/json\r\n");
            request.append("Content-Length: ").append(bytes.length).append("\r\n");
            if (token != null) {
                request.append("Authorization: Bearer ").append(token).append("\r\n");
            }
            if (origin != null) {
                request.append("Origin: ").append(origin).append("\r\n");
            }
            request.append("Connection: close\r\n\r\n");
            java.io.OutputStream out = socket.getOutputStream();
            out.write(request.toString().getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.flush();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String statusLine = reader.readLine();
            int status = 0;
            if (statusLine != null) {
                String[] parts = statusLine.split(" ");
                status = Integer.parseInt(parts[1]);
            }
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return new String[]{String.valueOf(status), response.toString()};
        } finally {
            socket.close();
        }
    }

    private static byte[] readAll(java.io.InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int n;
        try {
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
            }
        } finally {
            in.close();
        }
        return out.toByteArray();
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("  ok - " + name);
    }
}
