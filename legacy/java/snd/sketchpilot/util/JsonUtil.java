package snd.sketchpilot.util;

import org.json.JSONException;
import org.json.JSONObject;

/** Small org.json helpers. Kept Android-free so pure-core tests can run on the JVM. */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String optString(JSONObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return fallback;
        }
        return String.valueOf(object.opt(key));
    }

    public static int optInt(JSONObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return fallback;
        }
        Object value = object.opt(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean optBoolean(JSONObject object, String key, boolean fallback) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return fallback;
        }
        Object value = object.opt(key);
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static JSONObject parseObject(String text) throws JSONException {
        if (text == null || text.trim().length() == 0) {
            return new JSONObject();
        }
        return new JSONObject(text);
    }

    /** Returns null instead of throwing when the text is not an object. */
    public static JSONObject tryParseObject(String text) {
        try {
            return parseObject(text);
        } catch (Throwable t) {
            return null;
        }
    }
}
