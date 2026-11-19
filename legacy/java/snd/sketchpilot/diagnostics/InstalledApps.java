package snd.sketchpilot.diagnostics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.core.hook.HookScope;

/** Reads installed Sketchware Pro version using Android 11+ package visibility queries. */
public final class InstalledApps {

    private InstalledApps() {
    }

    public static JSONObject getSketchwareInfo(Context context) {
        JSONObject out = new JSONObject();
        try {
            JSONArray candidates = new JSONArray();
            java.util.List<String> scopes = HookScope.getScopes();
            for (int i = 0; i < scopes.size(); i++) {
                candidates.put(scopes.get(i));
            }
            out.put("candidates", candidates);
            PackageManager manager = context.getPackageManager();
            for (int i = 0; i < scopes.size(); i++) {
                String pkg = scopes.get(i);
                try {
                    PackageInfo info = manager.getPackageInfo(pkg, 0);
                    JSONObject found = new JSONObject();
                    found.put("package", pkg);
                    found.put("versionCode", info.versionCode);
                    found.put("versionName", info.versionName);
                    out.put("installed", found);
                    return out;
                } catch (PackageManager.NameNotFoundException ignored) {
                    // try next configured package
                }
            }
            out.put("installed", JSONObject.NULL);
            out.put("note", "no configured package installed or not visible; check <queries> and scope list");
        } catch (Throwable t) {
            try {
                out.put("error", String.valueOf(t.getMessage()));
            } catch (Throwable ignored) {
                // safe
            }
        }
        return out;
    }
}
