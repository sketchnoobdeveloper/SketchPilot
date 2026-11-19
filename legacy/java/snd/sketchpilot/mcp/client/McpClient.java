package snd.sketchpilot.mcp.client;

import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import snd.sketchpilot.mcp.protocol.McpProtocol;

/** MCP client so the in-app agent can consume external MCP servers. */
public final class McpClient {

    private final String endpoint;
    private final String token;
    private String protocolVersion = McpProtocol.LATEST;

    public McpClient(String endpoint, String token) {
        this.endpoint = endpoint;
        this.token = token;
    }

    public JSONObject initialize() throws Exception {
        JSONObject params = new JSONObject();
        params.put("protocolVersion", McpProtocol.LATEST);
        params.put("capabilities", new JSONObject());
        JSONObject result = rpc(McpProtocol.METHOD_INITIALIZE, params);
        String negotiated = result.optJSONObject("result") == null ? McpProtocol.LATEST
                : result.getJSONObject("result").optString("protocolVersion", McpProtocol.LATEST);
        if (McpProtocol.isSupported(negotiated)) {
            protocolVersion = negotiated;
        }
        rpc(McpProtocol.METHOD_INITIALIZED, new JSONObject());
        return result;
    }

    public JSONObject listTools() throws Exception {
        return rpc(McpProtocol.METHOD_TOOLS_LIST, new JSONObject());
    }

    public JSONObject callTool(String name, JSONObject arguments) throws Exception {
        JSONObject params = new JSONObject();
        params.put("name", name);
        params.put("arguments", arguments == null ? new JSONObject() : arguments);
        return rpc(McpProtocol.METHOD_TOOLS_CALL, params);
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    private JSONObject rpc(String method, JSONObject params) throws Exception {
        HttpURLConnection connection = null;
        try {
            JSONObject request = new JSONObject();
            request.put("jsonrpc", "2.0");
            request.put("id", System.currentTimeMillis());
            request.put("method", method);
            request.put("params", params);
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json, text/event-stream");
            if (token != null && token.length() > 0) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            try {
                writer.write(request.toString());
                writer.flush();
            } finally {
                writer.close();
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder();
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        body.append(line.substring(6));
                    } else if (!line.startsWith("event:") && !line.startsWith("id:")) {
                        body.append(line);
                    }
                }
            } finally {
                reader.close();
            }
            try {
                return new JSONObject(body.toString());
            } catch (Throwable t) {
                throw new IllegalStateException("invalid JSON-RPC response", t);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
