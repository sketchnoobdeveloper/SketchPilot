package snd.sketchpilot.project.model;

import org.json.JSONObject;

import snd.sketchpilot.util.JsonUtil;

/**
 * A single ViewBean JSON line from the encrypted "view" file (evidence E10).
 * The raw JSONObject is retained so properties this module does not model are
 * preserved byte-for-byte on write-back.
 */
public final class ViewNode {

    private final JSONObject raw;

    public ViewNode(JSONObject raw) {
        this.raw = raw == null ? new JSONObject() : raw;
    }

    public JSONObject getRaw() {
        return raw;
    }

    public String getId() {
        return JsonUtil.optString(raw, "id", "");
    }

    public int getType() {
        return JsonUtil.optInt(raw, "type", -1);
    }

    public String getParent() {
        return JsonUtil.optString(raw, "parent", "");
    }

    public int getParentType() {
        return JsonUtil.optInt(raw, "parentType", -1);
    }

    public int getIndex() {
        return JsonUtil.optInt(raw, "index", 0);
    }

    public String getConvert() {
        return JsonUtil.optString(raw, "convert", "");
    }

    public void setId(String id) {
        try {
            raw.put("id", id);
        } catch (Throwable ignored) {
            // JSONObject.put only throws for non-finite numbers; String is safe.
        }
    }

    public String toJsonString() {
        return raw.toString();
    }
}
