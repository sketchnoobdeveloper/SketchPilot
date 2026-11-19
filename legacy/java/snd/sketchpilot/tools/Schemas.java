package snd.sketchpilot.tools;

import org.json.JSONObject;

/** JSON Schema helpers for tool definitions. */
public final class Schemas {

    private Schemas() {
    }

    public static JSONObject object(String... propertyNames) {
        JSONObject schema = new JSONObject();
        JSONObject properties = new JSONObject();
        try {
            schema.put("type", "object");
            for (int i = 0; i < propertyNames.length; i++) {
                JSONObject property = new JSONObject();
                property.put("type", "string");
                properties.put(propertyNames[i], property);
            }
            schema.put("properties", properties);
        } catch (Throwable ignored) {
            // safe
        }
        return schema;
    }
}
