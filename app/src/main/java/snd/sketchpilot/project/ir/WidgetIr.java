package snd.sketchpilot.project.ir;

import org.json.JSONObject;

/** Typed intermediate representation of one widget. */
public final class WidgetIr {

    private String id = "";
    private String typeName = "";
    private int typeCode = -1;
    private String parent = "";
    private int index = 0;
    private final JSONObject properties = new JSONObject();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(int typeCode) {
        this.typeCode = typeCode;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public JSONObject getProperties() {
        return properties;
    }

    public JSONObject toJson() {
        JSONObject out = new JSONObject();
        try {
            out.put("id", id);
            out.put("type", typeName);
            out.put("typeCode", typeCode);
            out.put("parent", parent);
            out.put("index", index);
            out.put("properties", properties);
        } catch (Throwable ignored) {
            // String/long/int/JSONObject values are safe
        }
        return out;
    }
}
