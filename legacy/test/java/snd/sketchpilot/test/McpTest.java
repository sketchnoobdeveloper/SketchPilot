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
import snd.sketchpilot.mcp.protocol.McpProtocol;
import snd.sketchpilot.mcp.server.McpServer;
import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.tools.ToolCatalog;

/** Tests the MCP dispatcher and the Streamable HTTP transport. */
public final class McpTest {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        SketchwarePaths paths = new SketchwarePaths(new File("/sdcard"));
        ProjectEditor editor = new ProjectEditor(paths, new SketchwareCrypto());
        McpDispatcher dispatcher = new McpDispatcher(ToolCatalog.build(editor, paths),
                editor.getRepository(), new File("/tmp/opencode/mcp-audit-test.log"));

        JSONObject init = new JSONObject(dispatcher.dispatch(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2026-07-28\"}}"));
        check("initialize negotiates latest", McpProtocol.LATEST.equals(
                init.getJSONObject("result").optString("protocolVersion")));
        check("capabilities present", init.getJSONObject("result").getJSONObject("capabilities").has("tools"));

        JSONObject discover = new JSONObject(dispatcher.dispatch(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"server/discover\",\"params\":{}}"));
        check("discover reports latest", McpProtocol.LATEST.equals(
                discover.getJSONObject("result").optString("protocolVersion")));

        JSONObject tools = new JSONObject(dispatcher.dispatch(
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\",\"params\":{}}"));
        check("tools list non-empty", tools.getJSONObject("result").getJSONArray("tools").length() >= 15);

        List<String> restricted = new ArrayList<String>();
        restricted.add("project.list");
        dispatcher.setAllowedTools(restricted);
        JSONObject limited = new JSONObject(dispatcher.dispatch(
                "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/list\",\"params\":{}}"));
        check("per-client allowlist applied",
                limited.getJSONObject("result").getJSONArray("tools").length() == 1);
        dispatcher.setAllowedTools(null);

        JSONObject call = new JSONObject(dispatcher.dispatch(
                "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"project.list\",\"arguments\":{}}}"));
        check("tool call succeeds", !call.getJSONObject("result").optBoolean("isError"));

        JSONObject resources = new JSONObject(dispatcher.dispatch(
                "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"resources/list\",\"params\":{}}"));
        check("resources listed", resources.getJSONObject("result").getJSONArray("resources").length() > 0);

        JSONObject read = new JSONObject(dispatcher.dispatch(
                "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"resources/read\",\"params\":{\"uri\":\"sketchware://project/602/view\"}}"));
        check("resource read", read.getJSONObject("result").getJSONArray("contents")
                .getJSONObject(0).optString("text").contains("@"));

        String notification = dispatcher.dispatch(
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}");
        check("notification returns null", notification == null);

        // HTTP transport with bearer auth
        int port = 18890;
        String token = "sp_test_token";
        McpServer server = new McpServer(port, token, dispatcher);
        server.start();
        Thread.sleep(300);

        String body = "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"initialize\",\"params\":{}}";
        JSONObject authed = new JSONObject(httpPost("http://127.0.0.1:" + port + "/mcp", body, token));
        check("HTTP initialize ok", authed.getJSONObject("result").has("protocolVersion"));

        int unauthorizedStatus = httpStatus("http://127.0.0.1:" + port + "/mcp", body, "wrong");
        check("HTTP rejects bad token", unauthorizedStatus == 401);

        server.stop();
        System.out.println("McpTest: " + passed + " assertions passed.");
    }

    private static String httpPost(String url, String body, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
        try {
            writer.write(body);
            writer.flush();
        } finally {
            writer.close();
        }
        byte[] bytes = readAll(connection.getInputStream());
        connection.disconnect();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int httpStatus(String url, String body, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
        try {
            writer.write(body);
            writer.flush();
        } finally {
            writer.close();
        }
        int status = connection.getResponseCode();
        connection.disconnect();
        return status;
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
