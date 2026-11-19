package snd.sketchpilot.core.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.json.JSONArray;
import org.json.JSONObject;

/** Converts Sketchware bean objects to JSON using reflective field access. */
public final class ReflectBeans {

    private ReflectBeans() {
    }

    public static JSONObject toJson(Object bean) {
        JSONObject out = new JSONObject();
        if (bean == null) {
            return out;
        }
        writeFields(bean, out, 0);
        return out;
    }

    private static void writeFields(Object bean, JSONObject out, int depth) {
        if (bean == null || depth > 2) {
            return;
        }
        Field[] fields = bean.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(bean);
                out.put(field.getName(), convert(value, depth));
            } catch (Throwable ignored) {
                // fields that cannot be read are simply omitted
            }
        }
    }

    private static Object convert(Object value, int depth) {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof java.util.Collection<?>) {
            JSONArray array = new JSONArray();
            java.util.Iterator<?> it = ((java.util.Collection<?>) value).iterator();
            while (it.hasNext()) {
                array.put(convert(it.next(), depth + 1));
            }
            return array;
        }
        if (value.getClass().isArray()) {
            JSONArray array = new JSONArray();
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                array.put(convert(java.lang.reflect.Array.get(value, i), depth + 1));
            }
            return array;
        }
        if (value instanceof java.util.Map<?, ?>) {
            JSONObject object = new JSONObject();
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
            java.util.Iterator<?> keys = map.keySet().iterator();
            while (keys.hasNext()) {
                Object key = keys.next();
                try {
                    object.put(String.valueOf(key), convert(map.get(key), depth + 1));
                } catch (Throwable ignored) {
                    // skip exotic keys
                }
            }
            return object;
        }
        if (depth < 2 && value.getClass().getName().startsWith("com.besome.sketch.beans")) {
            return toJson(value);
        }
        return String.valueOf(value);
    }
}
