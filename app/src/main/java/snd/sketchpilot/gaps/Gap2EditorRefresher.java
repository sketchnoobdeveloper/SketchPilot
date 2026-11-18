package snd.sketchpilot.gaps;

import java.lang.reflect.Method;

import org.json.JSONObject;

import snd.sketchpilot.core.hook.HookBreadcrumbs;

/**
 * GAP-2: refresh the live editor after a manager write. Probe known refresh
 * method names by signature; if none resolves, DO NOT write offline while the
 * project is open - tell the user to close and reopen the project.
 */
public final class Gap2EditorRefresher {

    private static final String[] METHOD_NAMES = {
            "invalidateViews", "refreshView", "refresh", "notifyDataSetChanged",
            "loadViews", "reloadViews", "updateUI"
    };

    private Gap2EditorRefresher() {
    }

    public static JSONObject refreshOrExplain(Object activity, String scId) {
        JSONObject out = new JSONObject();
        boolean refreshed = activity != null && invoke(activity);
        try {
            out.put("ok", refreshed);
            if (refreshed) {
                out.put("message", "live editor refreshed");
            } else {
                out.put("message", "editor refresh is unavailable for this version. "
                        + "Close and reopen project " + scId + " in Sketchware Pro to see changes. "
                        + "Offline writes are blocked while the project is open.");
            }
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }

    private static boolean invoke(Object target) {
        Class<?> cls = target.getClass();
        for (int i = 0; i < METHOD_NAMES.length; i++) {
            try {
                Method method = findNoArg(cls, METHOD_NAMES[i]);
                if (method == null) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(target);
                HookBreadcrumbs.ok("Gap2.refresh." + METHOD_NAMES[i]);
                return true;
            } catch (Throwable t) {
                HookBreadcrumbs.fail("Gap2.refresh." + METHOD_NAMES[i], t);
            }
        }
        return false;
    }

    private static Method findNoArg(Class<?> cls, String name) {
        Class<?> current = cls;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name);
                if (method.getParameterTypes().length == 0) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
