package snd.sketchpilot.blocks;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Runtime registry of valid opCodes and their spec strings. The P3 live bridge
 * replaces the builtin table with the registry read from the running app so the
 * AI model cannot invent blocks (master prompt sections 3 and 6).
 */
public final class BlockSpecRegistry {

    private final Map<String, String> specs = new LinkedHashMap<String, String>();
    private boolean loadedFromHost = false;

    public BlockSpecRegistry() {
        // Only opCodes observed in real project data (evidence E15).
        specs.put("addSourceDirectly", "add source directly %s.inputOnly");
    }

    public void loadFromJson(JSONArray array) {
        if (array == null) {
            return;
        }
        Map<String, String> loaded = new LinkedHashMap<String, String>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject row = array.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String op = row.optString("opCode", "");
            String spec = row.optString("spec", "");
            if (op.length() > 0) {
                loaded.put(op, spec);
            }
        }
        if (!loaded.isEmpty()) {
            specs.clear();
            specs.putAll(loaded);
            loadedFromHost = true;
        }
    }

    public boolean has(String opCode) {
        return specs.containsKey(opCode);
    }

    public String getSpec(String opCode) {
        String spec = specs.get(opCode);
        return spec == null ? "" : spec;
    }

    public int getParameterCount(String opCode) {
        String spec = getSpec(opCode);
        if (spec.length() == 0) {
            return -1;
        }
        int count = 0;
        for (int i = 0; i < spec.length(); i++) {
            if (spec.charAt(i) == '%' && i + 1 < spec.length()) {
                char next = spec.charAt(i + 1);
                if (next == 's' || next == 'd' || next == 'b' || next == 'm' || next == 'i') {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isLoadedFromHost() {
        return loadedFromHost;
    }

    public Map<String, String> all() {
        return specs;
    }
}
