package snd.sketchpilot.build;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Converts raw build log lines into structured diagnostics keyed by pipeline
 * stage (resources, java, dex, shrink, package).
 */
public final class BuildDiagnosticsParser {

    private BuildDiagnosticsParser() {
    }

    public static JSONArray parse(List<String> lines) {
        JSONArray result = new JSONArray();
        if (lines == null) {
            return result;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!looksLikeError(line)) {
                continue;
            }
            JSONObject diagnostic = new JSONObject();
            try {
                diagnostic.put("severity", severity(line));
                diagnostic.put("stage", stage(line));
                diagnostic.put("message", line.length() > 600 ? line.substring(0, 600) : line);
                diagnostic.put("line", i + 1);
            } catch (Throwable ignored) {
                // safe
            }
            result.put(diagnostic);
        }
        return result;
    }

    private static boolean looksLikeError(String line) {
        String lower = line.toLowerCase();
        return lower.contains("error:") || lower.contains(" error ") || lower.contains("failed")
                || lower.contains("exception") || lower.contains("cannot find symbol")
                || lower.contains("no resource found") || lower.contains("duplicate class");
    }

    private static String severity(String line) {
        String lower = line.toLowerCase();
        if (lower.contains("fatal") || lower.contains("exception")) {
            return "fatal";
        }
        return "error";
    }

    private static String stage(String line) {
        String lower = line.toLowerCase();
        if (lower.contains("aapt") || lower.contains("resource")) {
            return "resources";
        }
        if (lower.contains("eclipse") || lower.contains("java") || lower.contains("cannot find symbol")) {
            return "java";
        }
        if (lower.contains("dex") || lower.contains("d8")) {
            return "dex";
        }
        if (lower.contains("r8") || lower.contains("proguard")) {
            return "shrink";
        }
        if (lower.contains("zipalign") || lower.contains("sign")) {
            return "package";
        }
        return "unknown";
    }
}
