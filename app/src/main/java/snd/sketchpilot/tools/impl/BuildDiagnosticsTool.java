package snd.sketchpilot.tools.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.build.BuildDiagnosticsParser;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.tools.Schemas;

/** Parses the real build log into structured diagnostics. */
public final class BuildDiagnosticsTool implements Tool {

    private final SketchwarePaths paths;

    public BuildDiagnosticsTool(SketchwarePaths paths) {
        this.paths = paths;
    }

    public String getName() {
        return "build.diagnostics";
    }

    public String getDescription() {
        return "Parse build log into structured diagnostics";
    }

    public JSONObject getSchema() {
        return Schemas.object();
    }

    public String getRisk() {
        return RISK_READ;
    }

    public boolean supportsDryRun() {
        return false;
    }

    public JSONObject execute(JSONObject arguments) {
        JSONObject out = new JSONObject();
        try {
            File log = new File(paths.root(), "debug.txt");
            if (!log.isFile()) {
                out.put("ok", false);
                out.put("error", "build log not found");
                return out;
            }
            List<String> lines = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
            JSONArray diagnostics = BuildDiagnosticsParser.parse(lines);
            out.put("ok", true);
            out.put("diagnostics", diagnostics);
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
