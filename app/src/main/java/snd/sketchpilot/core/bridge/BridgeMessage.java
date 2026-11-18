package snd.sketchpilot.core.bridge;

import org.json.JSONObject;

/** Envelope for IPC between the hooked Sketchware process and SketchPilot. */
public final class BridgeMessage {

    private final String type;
    private final JSONObject payload;
    private final String requestId;

    public BridgeMessage(String type, JSONObject payload, String requestId) {
        this.type = type;
        this.payload = payload == null ? new JSONObject() : payload;
        this.requestId = requestId;
    }

    public String getType() {
        return type;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public String getRequestId() {
        return requestId;
    }

    public String toJsonString() {
        JSONObject root = new JSONObject();
        try {
            root.put("type", type);
            root.put("payload", payload);
            root.put("requestId", requestId);
        } catch (Throwable ignored) {
            // String/JSONObject values cannot fail here
        }
        return root.toString();
    }

    public static BridgeMessage fromJson(String json) {
        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (Throwable t) {
            root = new JSONObject();
        }
        return new BridgeMessage(root.optString("type"), root.optJSONObject("payload"),
                root.optString("requestId"));
    }
}
