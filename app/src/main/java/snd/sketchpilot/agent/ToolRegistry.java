package snd.sketchpilot.agent;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/** Registry of tools, filtered per client by permission in MCP (P6). */
public final class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<String, Tool>();

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    public JSONArray schemas() {
        JSONArray array = new JSONArray();
        java.util.Iterator<String> keys = tools.keySet().iterator();
        while (keys.hasNext()) {
            Tool tool = tools.get(keys.next());
            if (!tool.isAvailable()) {
                continue;
            }
            JSONObject entry = new JSONObject();
            try {
                entry.put("type", "function");
                JSONObject function = new JSONObject();
                function.put("name", tool.getName());
                function.put("description", tool.getDescription());
                function.put("parameters", tool.getSchema());
                function.put("risk", tool.getRisk());
                entry.put("function", function);
            } catch (Throwable ignored) {
                // safe
            }
            array.put(entry);
        }
        return array;
    }

    public JSONObject execute(String name, JSONObject arguments) throws Exception {
        Tool tool = tools.get(name);
        if (tool == null) {
            JSONObject error = new JSONObject();
            error.put("error", "unknown tool: " + name);
            return error;
        }
        if (!tool.isAvailable()) {
            JSONObject error = new JSONObject();
            error.put("ok", false);
            error.put("status", "unavailable");
            error.put("error", "not available yet: " + name);
            return error;
        }
        return tool.execute(arguments);
    }
}
