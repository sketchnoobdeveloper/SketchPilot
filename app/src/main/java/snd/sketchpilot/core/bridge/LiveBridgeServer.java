package snd.sketchpilot.core.bridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loopback IPC server hosted in the hooked Sketchware process. Uses Thread
 * only (hard constraint). Every accept and dispatch is wrapped so the host
 * can never crash because of the bridge.
 */
public final class LiveBridgeServer {

    private final int port;
    private final Map<String, BridgeHandler> handlers = new LinkedHashMap<String, BridgeHandler>();
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    public LiveBridgeServer(int port) {
        this.port = port;
    }

    public void register(BridgeHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    public void start() {
        if (running) {
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            acceptThread = new Thread(new Runnable() {
                public void run() {
                    acceptLoop();
                }
            }, "SketchPilotBridge");
            acceptThread.setDaemon(true);
            acceptThread.start();
            Logger.getLogger("SketchPilot").info("listening on 127.0.0.1:" + port);
        } catch (Throwable t) {
            logFailure("LiveBridgeServer.start", t);
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Throwable t) {
            logFailure("LiveBridgeServer.stop", t);
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
                }, "SketchPilotBridgeWorker");
                worker.setDaemon(true);
                worker.start();
            } catch (Throwable t) {
                if (running) {
                    logFailure("LiveBridgeServer.accept", t);
                }
            }
        }
    }

    private static void logFailure(String where, Throwable t) {
        Logger.getLogger("SketchPilot").log(Level.WARNING, where, t);
    }

    private void serve(Socket client) {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            client.setSoTimeout(5000);
            reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new java.io.OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            BridgeMessage response;
            if (line == null || line.length() == 0) {
                response = error("empty request", null);
            } else {
                try {
                    BridgeMessage request = BridgeMessage.fromJson(line);
                    BridgeHandler handler = handlers.get(request.getType());
                    response = handler == null ? error("unknown request type: " + request.getType(),
                            request.getRequestId()) : safeHandle(handler, request);
                } catch (Throwable t) {
                    response = error("malformed request", null);
                }
            }
            writer.println(response.toJsonString());
            writer.flush();
        } catch (Throwable t) {
            logFailure("LiveBridgeServer.serve", t);
        } finally {
            if (writer != null) {
                writer.close();
            }
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

    private BridgeMessage safeHandle(BridgeHandler handler, BridgeMessage request) {
        try {
            return handler.handle(request);
        } catch (Throwable t) {
            logFailure("BridgeHandler." + handler.getType(), t);
            return error("handler failed: " + t.getClass().getSimpleName(), request.getRequestId());
        }
    }

    private static BridgeMessage error(String message, String requestId) {
        org.json.JSONObject payload = new org.json.JSONObject();
        try {
            payload.put("error", message);
        } catch (Throwable ignored) {
            // safe
        }
        return new BridgeMessage("error", payload, requestId);
    }
}
