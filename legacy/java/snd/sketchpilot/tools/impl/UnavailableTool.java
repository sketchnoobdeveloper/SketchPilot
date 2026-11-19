package snd.sketchpilot.tools.impl;

import org.json.JSONObject;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.tools.Schemas;

/** Placeholder for catalogued tools whose live backend is not implemented yet. */
public final class UnavailableTool implements Tool {

    private final String name;
    private final String description;
    private final String risk;
    private final String reason;

    public UnavailableTool(String name, String description, String risk, String reason) {
        this.name = name;
        this.description = description;
        this.risk = risk;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JSONObject getSchema() {
        return Schemas.object();
    }

    public String getRisk() {
        return risk;
    }

    public boolean supportsDryRun() {
        return false;
    }

    public boolean isAvailable() {
        return false;
    }

    public String getUnavailableReason() {
        return reason;
    }

    public JSONObject execute(JSONObject arguments) {
        JSONObject out = new JSONObject();
        try {
            out.put("ok", false);
            out.put("status", "unavailable");
            out.put("reason", reason);
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }
}
