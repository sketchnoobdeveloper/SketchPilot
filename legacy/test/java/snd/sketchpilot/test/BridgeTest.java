package snd.sketchpilot.test;

import org.json.JSONObject;

import snd.sketchpilot.core.bridge.BridgeConfig;
import snd.sketchpilot.core.bridge.BridgeException;
import snd.sketchpilot.core.bridge.BridgeHandler;
import snd.sketchpilot.core.bridge.BridgeMessage;
import snd.sketchpilot.core.bridge.LiveBridgeServer;
import snd.sketchpilot.core.bridge.SocketBridgeTransport;

/** Loopback test of the JSON-RPC-like bridge envelope and transport. */
public final class BridgeTest {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        final String echo = "hello-bridge";
        LiveBridgeServer server = new LiveBridgeServer(BridgeConfig.PORT + 99);
        server.register(new BridgeHandler() {
            public String getType() {
                return "echo";
            }

            public BridgeMessage handle(BridgeMessage message) throws BridgeException {
                JSONObject payload = new JSONObject();
                try {
                    payload.put("echo", message.getPayload().optString("value", ""));
                } catch (Throwable ignored) {
                    // safe
                }
                return new BridgeMessage("response:echo", payload, message.getRequestId());
            }
        });
        server.start();

        SocketBridgeTransport transport = new SocketBridgeTransport(BridgeConfig.HOST, BridgeConfig.PORT + 99);
        boolean available = false;
        for (int i = 0; i < 40; i++) {
            if (transport.isAvailable()) {
                available = true;
                break;
            }
            Thread.sleep(50);
        }
        check("bridge is reachable", available);

        JSONObject payload = new JSONObject();
        payload.put("value", echo);
        BridgeMessage reply = transport.call(new BridgeMessage("echo", payload, "req-1"));
        check("response type", "response:echo".equals(reply.getType()));
        check("request id echoed", "req-1".equals(reply.getRequestId()));
        check("payload echoed", echo.equals(reply.getPayload().optString("echo", "")));

        BridgeMessage unknown = transport.call(new BridgeMessage("no-such-handler", new JSONObject(), "req-2"));
        check("unknown handler returns error", "error".equals(unknown.getType()));

        server.stop();
        System.out.println("BridgeTest: " + passed + " assertions passed.");
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("  ok - " + name);
    }
}
