package snd.sketchpilot.library;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.JSONArray;
import org.json.JSONObject;

/** Detects duplicate class entries across local library jars. */
public final class DuplicateClassChecker {

    private DuplicateClassChecker() {
    }

    public static JSONArray check(File localLibsDir) {
        JSONArray conflicts = new JSONArray();
        if (localLibsDir == null || !localLibsDir.isDirectory()) {
            return conflicts;
        }
        File[] files = localLibsDir.listFiles();
        if (files == null) {
            return conflicts;
        }
        Map<String, String> owners = new HashMap<String, String>();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }
            ZipFile zip = null;
            try {
                zip = new ZipFile(file);
                java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    String previous = owners.get(name);
                    if (previous != null && !previous.equals(file.getName())) {
                        JSONObject conflict = new JSONObject();
                        conflict.put("class", name);
                        conflict.put("first", previous);
                        conflict.put("second", file.getName());
                        conflicts.put(conflict);
                    } else {
                        owners.put(name, file.getName());
                    }
                }
            } catch (Throwable ignored) {
                // skip unreadable jar
            } finally {
                if (zip != null) {
                    try {
                        zip.close();
                    } catch (Throwable ignored) {
                        // ignore
                    }
                }
            }
        }
        return conflicts;
    }
}
