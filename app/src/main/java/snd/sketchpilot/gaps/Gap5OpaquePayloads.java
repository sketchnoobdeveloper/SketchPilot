package snd.sketchpilot.gaps;

import org.json.JSONObject;

import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.project.io.ProjectFileIO;
import snd.sketchpilot.project.io.SectionedFile;

/**
 * GAP-5: java/local_library payloads were empty in samples. Safest handling is
 * the generic sectioned parser; fallback is to return the opaque plaintext with
 * a flag, never to guess a schema.
 */
public final class Gap5OpaquePayloads {

    private Gap5OpaquePayloads() {
    }

    public static JSONObject read(SketchwarePaths paths, SketchwareCrypto crypto,
                                  String scId, String fileName) {
        JSONObject out = new JSONObject();
        try {
            ProjectFileIO io = new ProjectFileIO(crypto);
            java.io.File file = paths.dataFile(scId, fileName);
            if (!file.isFile()) {
                out.put("ok", false);
                out.put("error", "file not present");
                return out;
            }
            byte[] bytes = io.read(file);
            if (bytes.length == 0) {
                out.put("ok", true);
                out.put("empty", true);
                return out;
            }
            String text = crypto.looksEncrypted(bytes) ? crypto.decrypt(bytes)
                    : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            SectionedFile parsed = SectionedFile.parse(text);
            out.put("ok", true);
            out.put("sectioned", parsed.getSections().size() > 0);
            out.put("text", text);
        } catch (Throwable t) {
            try {
                out.put("ok", false);
                out.put("error", String.valueOf(t.getMessage()));
            } catch (Throwable ignored) {
                // safe
            }
        }
        return out;
    }
}
