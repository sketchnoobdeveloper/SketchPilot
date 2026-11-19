package snd.sketchpilot.tools.impl;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.tools.Schemas;
import snd.sketchpilot.view.WidgetRegistry;

/** Offline knowledge base over the runtime registries and known error patterns. */
public final class KbSearchTool implements Tool {

    private final BlockSpecRegistry specs;
    private final WidgetRegistry widgets;

    public KbSearchTool(BlockSpecRegistry specs, WidgetRegistry widgets) {
        this.specs = specs;
        this.widgets = widgets;
    }

    public String getName() {
        return "kb.search";
    }

    public String getDescription() {
        return "Search offline block/widget/error knowledge base";
    }

    public JSONObject getSchema() {
        return Schemas.object("query");
    }

    public String getRisk() {
        return RISK_READ;
    }

    public boolean supportsDryRun() {
        return false;
    }

    public JSONObject execute(JSONObject arguments) {
        String query = arguments.optString("query", "").toLowerCase();
        JSONArray results = new JSONArray();
        java.util.Iterator<String> ops = specs.all().keySet().iterator();
        while (ops.hasNext()) {
            String op = ops.next();
            if (op.toLowerCase().contains(query) || specs.getSpec(op).toLowerCase().contains(query)) {
                results.put("block: " + op + " — " + specs.getSpec(op));
            }
        }
        java.util.List<String> names = widgets.allNames();
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).contains(query)) {
                results.put("widget: " + names.get(i));
            }
        }
        if (query.contains("cannot find symbol") || query.contains("error")) {
            results.put("fix: generated Java cannot resolve a symbol - check component/block parameters "
                    + "and that the library is enabled for this project.");
        }
        JSONObject out = new JSONObject();
        try {
            out.put("ok", true);
            out.put("results", results);
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }
}
