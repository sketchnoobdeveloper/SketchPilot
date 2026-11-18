package snd.sketchpilot.tools.impl;

import android.content.Context;

import org.json.JSONObject;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.diagnostics.DiagnosticsKit;
import snd.sketchpilot.tools.Schemas;

/** Real tool: builds the on-device diagnostics zip. */
public final class DiagnosticsTool implements Tool {

    private final Context context;

    public DiagnosticsTool(Context context) {
        this.context = context;
    }

    public String getName() {
        return "debug.diagnostics";
    }

    public String getDescription() {
        return "Build a diagnostics zip with logs, capability results and self-tests";
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
        java.io.File zip = DiagnosticsKit.buildZip(context);
        try {
            out.put("ok", zip != null);
            if (zip != null) {
                out.put("path", zip.getAbsolutePath());
                out.put("bytes", zip.length());
            } else {
                out.put("error", "diagnostics zip could not be created");
            }
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }
}
