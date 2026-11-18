package snd.sketchpilot.agent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.view.WidgetRegistry;

/**
 * Layered context snapshots with token-budget retrieval and rolling summaries.
 * Layers are ordered: project digest, screen, view tree, logic digest, errors.
 */
public final class ContextEngine {

    public static final String PROJECT = "project";
    public static final String SCREEN = "screen";
    public static final String VIEW_TREE = "view_tree";
    public static final String LOGIC = "logic";
    public static final String ERRORS = "errors";

    private final Map<String, String> layers = new LinkedHashMap<String, String>();
    private final StringBuilder summary = new StringBuilder();
    private String memoryDigest = "";

    public void setLayer(String name, String content) {
        layers.put(name, content == null ? "" : content);
    }

    public void setMemoryDigest(String digest) {
        this.memoryDigest = digest == null ? "" : digest;
    }

    public void addSummary(String text) {
        if (text == null || text.length() == 0) {
            return;
        }
        summary.append("\n- ").append(text.length() > 240 ? text.substring(0, 240) : text);
        if (summary.length() > 2000) {
            summary.delete(0, summary.length() - 2000);
        }
    }

    public String getSummary() {
        return summary.toString();
    }

    /** Retrieves layers in priority order until the approximate character budget is spent. */
    public String buildContext(int budgetChars) {
        StringBuilder out = new StringBuilder();
        int remaining = budgetChars;
        Iterator<String> keys = layers.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = layers.get(key);
            if (value.length() > remaining) {
                value = value.substring(0, Math.max(0, remaining));
            }
            out.append("## ").append(key).append('\n').append(value).append('\n');
            remaining -= value.length();
            if (remaining <= 0) {
                break;
            }
        }
        return out.toString();
    }

    /** System prompt embeds the live registries so the model cannot invent blocks. */
    public String buildSystemPrompt(BlockSpecRegistry specs, WidgetRegistry widgets) {
        StringBuilder out = new StringBuilder();
        out.append("You are SketchPilot, an agent controlling Sketchware Pro projects.\n");
        out.append("Use only the tools provided. Never invent opCodes or widget types.\n");
        out.append("Valid block opCodes:\n");
        java.util.Iterator<String> ops = specs.all().keySet().iterator();
        while (ops.hasNext()) {
            String op = ops.next();
            out.append("- ").append(op).append(" : ").append(specs.getSpec(op)).append('\n');
        }
        out.append("Valid widget types:\n");
        java.util.List<String> names = widgets.allNames();
        for (int i = 0; i < names.size(); i++) {
            out.append("- ").append(names.get(i)).append('\n');
        }
        if (memoryDigest.length() > 0) {
            out.append("Project memory:\n").append(memoryDigest).append('\n');
        }
        if (summary.length() > 0) {
            out.append("Conversation summary:\n").append(summary).append('\n');
        }
        return out.toString();
    }
}
