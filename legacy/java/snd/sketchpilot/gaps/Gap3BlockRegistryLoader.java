package snd.sketchpilot.gaps;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.project.SketchwarePaths;

/** GAP-3: load custom block palette/events from runtime files; fall back to builtin specs. */
public final class Gap3BlockRegistryLoader {

    private Gap3BlockRegistryLoader() {
    }

    public static int load(SketchwarePaths paths, String scId, BlockSpecRegistry registry) {
        JSONArray specs = new JSONArray();
        collect(readArray(new File(paths.root(), "resources/block/My Block/palette.json")), specs);
        File customDir = paths.dataFile(scId, "custom_blocks");
        if (customDir.isDirectory()) {
            File[] files = customDir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile() && files[i].getName().endsWith(".json")) {
                        collect(readArray(files[i]), specs);
                    }
                }
            }
        }
        collect(readArray(paths.systemDataFile("events.json")), specs);
        if (specs.length() > 0) {
            registry.loadFromJson(specs);
        }
        return specs.length();
    }

    private static JSONArray readArray(File file) {
        JSONArray empty = new JSONArray();
        if (!file.isFile()) {
            return empty;
        }
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (text.trim().startsWith("[")) {
                return new JSONArray(text);
            }
            JSONObject object = new JSONObject(text);
            JSONArray nested = object.optJSONArray("blocks");
            return nested == null ? empty : nested;
        } catch (Throwable t) {
            return empty;
        }
    }

    private static void collect(JSONArray source, JSONArray target) {
        for (int i = 0; i < source.length(); i++) {
            Object item = source.opt(i);
            if (item instanceof JSONObject) {
                JSONObject row = (JSONObject) item;
                String op = row.optString("opCode", row.optString("name", ""));
                if (op.length() > 0) {
                    JSONObject normalized = new JSONObject();
                    try {
                        normalized.put("opCode", op);
                        normalized.put("spec", row.optString("spec", ""));
                    } catch (Throwable ignored) {
                        // safe
                    }
                    target.put(normalized);
                }
            }
        }
    }
}
