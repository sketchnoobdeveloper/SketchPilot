package snd.sketchpilot.core.hook;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import snd.sketchpilot.util.Logger;

/** Configurable scope list for Sketchware Pro forks/clones. */
public final class HookScope {

    private static final String[] DEFAULTS = {"pro.sketchware"};
    private static final File CONFIG = new File(
            "/storage/emulated/0/.sketchware/sketchpilot/scopes.txt");

    private HookScope() {
    }

    public static boolean contains(String packageName) {
        if (packageName == null) {
            return false;
        }
        List<String> scopes = read();
        for (int i = 0; i < scopes.size(); i++) {
            if (scopes.get(i).equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /** Configured scope packages (defaults plus user-configured forks). */
    public static List<String> getScopes() {
        return read();
    }

    private static List<String> read() {
        List<String> result = new ArrayList<String>();
        try {
            if (CONFIG.isFile()) {
                String content = new String(snd.sketchpilot.project.crypto.SketchwareCrypto.readAll(
                        new java.io.FileInputStream(CONFIG)), "UTF-8");
                String[] lines = content.split("\\r?\\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.length() > 0 && !line.startsWith("#")) {
                        result.add(line);
                    }
                }
            }
        } catch (Throwable t) {
            Logger.hookFailure("HookScope.read", t);
        }
        if (result.isEmpty()) {
            for (int i = 0; i < DEFAULTS.length; i++) {
                result.add(DEFAULTS[i]);
            }
        }
        return result;
    }
}
