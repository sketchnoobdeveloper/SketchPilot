package snd.sketchpilot.gaps;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import snd.sketchpilot.core.hook.HookBreadcrumbs;
import snd.sketchpilot.core.hook.ManagerCapture;
import snd.sketchpilot.util.JsonUtil;

/**
 * GAP-1: locate live manager instances. Safest implementation is constructor
 * capture (already installed) plus reflective static-field probing. Fallback:
 * null instance means LiveRepository degrades to offline reads.
 */
public final class Gap1ManagerLocator {

    private static final String[] HOLDER_CANDIDATES = {
            "a.a.a.DA", "a.a.a.MB", "a.a.a.NB", "a.a.a.PB", "a.a.a.QB", "a.a.a.RB", "a.a.a.SB",
            "a.a.a.VB", "a.a.a.YB", "a.a.a.ZB"
    };

    private Gap1ManagerLocator() {
    }

    public static Map<String, Object> locate(ClassLoader loader) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIfPresent(result, "project.data", ManagerCapture.getDataManager());
        putIfPresent(result, "project.files", ManagerCapture.getFileManager());
        putIfPresent(result, "project.libraries", ManagerCapture.getLibraryManager());
        for (int i = 0; i < HOLDER_CANDIDATES.length; i++) {
            Class<?> holder = load(loader, HOLDER_CANDIDATES[i]);
            if (holder == null) {
                continue;
            }
            Field[] fields = holder.getDeclaredFields();
            for (int j = 0; j < fields.length; j++) {
                Field field = fields[j];
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String type = field.getType().getName();
                String logical = null;
                if ("a.a.a.eC".equals(type)) {
                    logical = "project.data";
                } else if ("a.a.a.hC".equals(type)) {
                    logical = "project.files";
                } else if ("a.a.a.iC".equals(type)) {
                    logical = "project.libraries";
                }
                if (logical == null || result.containsKey(logical)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null) {
                        result.put(logical, value);
                        HookBreadcrumbs.ok("Gap1.static." + logical);
                    }
                } catch (Throwable ignored) {
                    // fallback: keep captured value
                }
            }
        }
        return result;
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static Class<?> load(ClassLoader loader, String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable t) {
            return null;
        }
    }
}
