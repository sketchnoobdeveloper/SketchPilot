package snd.sketchpilot.project.model;

import org.json.JSONObject;

import snd.sketchpilot.util.JsonUtil;

/**
 * A single BlockBean JSON line from the encrypted "logic" file (evidence E15).
 * Fields: id, opCode, nextBlock, subStack1, subStack2, spec, parameters[],
 * color, type, typeName.
 */
public final class BlockNode {

    private final JSONObject raw;

    public BlockNode(JSONObject raw) {
        this.raw = raw == null ? new JSONObject() : raw;
    }

    public JSONObject getRaw() {
        return raw;
    }

    public String getId() {
        return JsonUtil.optString(raw, "id", "");
    }

    public String getOpCode() {
        return JsonUtil.optString(raw, "opCode", "");
    }

    public String getSpec() {
        return JsonUtil.optString(raw, "spec", "");
    }

    public int getNextBlock() {
        return JsonUtil.optInt(raw, "nextBlock", -1);
    }

    public int getSubStack1() {
        return JsonUtil.optInt(raw, "subStack1", -1);
    }

    public int getSubStack2() {
        return JsonUtil.optInt(raw, "subStack2", -1);
    }

    public String toJsonString() {
        return raw.toString();
    }
}
