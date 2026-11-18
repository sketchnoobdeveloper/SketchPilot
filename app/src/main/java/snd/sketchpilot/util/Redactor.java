package snd.sketchpilot.util;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Removes secrets from anything destined for an LLM: API keys, keystore
 * passwords, google-services data (master prompt section 1).
 */
public final class Redactor {

    public static final String MASK = "[REDACTED]";

    private static final String[] SENSITIVE_KEYS = {
            "apiKey", "api_key", "apikey", "password", "pass", "storePass",
            "keyPass", "keystorePassword", "secret", "token", "authorization",
            "google-services", "google_services", "client_secret", "private_key"
    };

    private Redactor() {
    }

    public static String redactText(String text) {
        if (text == null) {
            return "";
        }
        String result = text;
        result = result.replaceAll("(?i)(sk-[A-Za-z0-9_-]{12,})", MASK);
        result = result.replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._-]{12,}", "$1" + MASK);
        result = result.replaceAll("(?i)(token|api[_-]?key|password|secret|authorization)(\\s*[=:]\\s*)[^\\s,;]+",
                "$1$2" + MASK);
        return result;
    }

    public static JSONObject redactJson(JSONObject source) {
        JSONObject out = new JSONObject();
        if (source == null) {
            return out;
        }
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = source.get(key);
                if (isSensitive(key)) {
                    out.put(key, MASK);
                } else if (value instanceof JSONObject) {
                    out.put(key, redactJson((JSONObject) value));
                } else if (value instanceof JSONArray) {
                    out.put(key, redactArray((JSONArray) value));
                } else if (value instanceof String) {
                    out.put(key, redactText((String) value));
                } else {
                    out.put(key, value);
                }
            } catch (Throwable ignored) {
                // skip unreadable field
            }
        }
        return out;
    }

    private static JSONArray redactArray(JSONArray source) {
        JSONArray out = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            Object value = source.opt(i);
            if (value instanceof JSONObject) {
                out.put(redactJson((JSONObject) value));
            } else if (value instanceof String) {
                out.put(redactText((String) value));
            } else {
                out.put(value);
            }
        }
        return out;
    }

    private static boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        for (int i = 0; i < SENSITIVE_KEYS.length; i++) {
            if (lower.contains(SENSITIVE_KEYS[i].toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
