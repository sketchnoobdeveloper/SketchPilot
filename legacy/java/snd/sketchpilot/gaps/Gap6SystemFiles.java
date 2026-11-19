package snd.sketchpilot.gaps;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.project.SketchwarePaths;

/** GAP-6: tolerant reader for system JSON files; fallback is an empty structure. */
public final class Gap6SystemFiles {

    private Gap6SystemFiles() {
    }

    public static JSONArray read(SketchwarePaths paths, String name) {
        JSONArray empty = new JSONArray();
        File file = paths.systemDataFile(name);
        if (!file.isFile()) {
            return empty;
        }
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (text.trim().startsWith("[")) {
                return new JSONArray(text);
            }
            JSONObject object = new JSONObject(text);
            JSONArray array = new JSONArray();
            java.util.Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject row = new JSONObject();
                row.put("name", key);
                row.put("value", object.get(key));
                array.put(row);
            }
            return array;
        } catch (Throwable t) {
            return empty;
        }
    }
}
