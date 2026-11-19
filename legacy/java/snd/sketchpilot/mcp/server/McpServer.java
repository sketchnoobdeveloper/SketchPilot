package snd.sketchpilot.mcp.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import snd.sketchpilot.mcp.protocol.McpDispatcher;

/**
 * Streamable HTTP MCP server on 127.0.0.1 using raw ServerSocket (Android-safe;
 * com.sun.net.httpserver is not available on Android). Thread-only concurrency.
 */
public final class McpServer {

    private final int port;
    private final String token;
    private final McpDispatcher dispatcher;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    private final Map<String, long[]> rateLimits = new HashMap<String, long[]>();
    private static final int MAX_CALLS_PER_MINUTE = 120;
    public static volatile boolean RUNNING = false;

    public McpServer(int port, String token, McpDispatcher dispatcher) {
        this.port = port;
        this.token = token;
        this.dispatcher = dispatcher;
    }

    public void start() {
        if (running) {
            return;
        }
        try {
            serverSocket = new ServerSocket(port, 20, InetAddress.getByName("127.0.0.1"));
            running = true;
            RUNNING = true;
            acceptThread = new Thread(new Runnable() {
                public void run() {
                    acceptLoop();
                }
            }, "SketchPilotMcp");
            acceptThread.setDaemon(true);
            acceptThread.start();
        } catch (Throwable t) {
            java.util.logging.Logger.getLogger("SketchPilot").warning("MCP start failed: " + t);
        }
    }

    public void stop() {
        running = false;
        RUNNING = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                final Socket client = serverSocket.accept();
                Thread worker = new Thread(new Runnable() {
                    public void run() {
                        serve(client);
                    }
                }, "SketchPilotMcpWorker");
                worker.setDaemon(true);
                worker.start();
            } catch (Throwable t) {
                if (running) {
                    java.util.logging.Logger.getLogger("SketchPilot").warning("MCP accept: " + t);
                }
            }
        }
    }

    private void serve(Socket client) {
        BufferedReader reader = null;
        try {
            client.setSoTimeout(10000);
            reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }
            Map<String, String> headers = new HashMap<String, String>();
            String line;
            while ((line = reader.readLine()) != null && line.length() > 0) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    headers.put(line.substring(0, colon).trim().toLowerCase(),
                            line.substring(colon + 1).trim());
                }
            }
            String origin = headers.get("origin");
            if (origin != null && !isLocalOrigin(origin)) {
                write(client, 403, "Forbidden", "application/json",
                        "{\"error\":\"non-local origin rejected\"}");
                return;
            }
            int length = 0;
            try {
                length = Integer.parseInt(headers.get("content-length"));
            } catch (Throwable ignored) {
                // zero-length body
            }
            if (length > 1000000) {
                write(client, 413, "Payload Too Large", "application/json",
                        "{\"error\":\"request body too large\"}");
                return;
            }
            char[] body = new char[Math.max(0, length)];
            int offset = 0;
            while (offset < body.length) {
                int read = reader.read(body, offset, body.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }

            String authorization = headers.get("authorization");
            String expected = "Bearer " + token;
            if (!constantTimeEquals(expected, authorization)) {
                write(client, 401, "Unauthorized", "application/json",
                        "{\"error\":\"invalid bearer token\"}");
                return;
            }

            String[] parts = requestLine.split(" ");
            String verb = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "/";

            if ("GET".equals(verb) && "/health".equals(path)) {
                write(client, 200, "OK", "application/json", "{\"status\":\"ok\"}");
                return;
            }
            if (!rateAllowed(authorization)) {
                write(client, 429, "Too Many Requests", "application/json",
                        "{\"error\":\"rate limit exceeded\"}");
                return;
            }
            if ("POST".equals(verb) && "/mcp".equals(path)) {
                String requestBody = new String(body, 0, offset);
                String response = dispatcher.dispatch(requestBody);
                if (response == null) {
                    write(client, 202, "Accepted", "application/json", "");
                } else {
                    write(client, 200, "OK", "application/json", response);
                }
                return;
            }
            if ("GET".equals(verb) && "/mcp".equals(path)) {
                // SSE channel for server notifications; heartbeat keeps it alive.
                writeSseHeader(client);
                OutputStream out = client.getOutputStream();
                for (int i = 0; i < 20 && !client.isClosed(); i++) {
                    out.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Thread.sleep(1500);
                }
                return;
            }
            write(client, 404, "Not Found", "application/json", "{\"error\":\"not found\"}");
        } catch (Throwable t) {
            java.util.logging.Logger.getLogger("SketchPilot").warning("MCP serve: " + t);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
            try {
                client.close();
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }

    private static boolean isLocalOrigin(String origin) {
        String value = origin.trim().toLowerCase();
        return "null".equals(value) || value.startsWith("http://127.0.0.1")
                || value.startsWith("http://localhost") || value.startsWith("http://[:" + ":1]");
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            // Still perform a comparison to avoid leaking length via early return.
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    new byte[expected.length()]) && false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private synchronized boolean rateAllowed(String key) {
        long now = System.currentTimeMillis();
        long[] state = rateLimits.get(key);
        if (state == null || now - state[0] > 60000) {
            state = new long[]{now, 1};
            rateLimits.put(key, state);
            return true;
        }
        state[1]++;
        return state[1] <= MAX_CALLS_PER_MINUTE;
    }

    private static void writeSseHeader(Socket client) throws Exception {
        String headers = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/event-stream\r\n"
                + "Cache-Control: no-cache\r\n"
                + "Connection: close\r\n\r\n";
        client.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));
        client.getOutputStream().flush();
    }

    private static void write(Socket client, int status, String reason, String contentType, String body)
            throws Exception {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        OutputStream out = client.getOutputStream();
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }
}
