package snd.sketchpilot.project.ir;

import java.util.ArrayList;
import java.util.List;

/** Typed IR of one block: opCode, parameters, and two optional sub-stacks. */
public final class BlockIr {

    private String id = "";
    private String opCode = "";
    private final List<String> parameters = new ArrayList<String>();
    private final List<BlockIr> subStack1 = new ArrayList<BlockIr>();
    private final List<BlockIr> subStack2 = new ArrayList<BlockIr>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOpCode() {
        return opCode;
    }

    public void setOpCode(String opCode) {
        this.opCode = opCode;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<BlockIr> getSubStack1() {
        return subStack1;
    }

    public List<BlockIr> getSubStack2() {
        return subStack2;
    }

    public org.json.JSONObject toJson() {
        org.json.JSONObject out = new org.json.JSONObject();
        org.json.JSONArray params = new org.json.JSONArray();
        for (int i = 0; i < parameters.size(); i++) {
            params.put(parameters.get(i));
        }
        try {
            out.put("id", id);
            out.put("opCode", opCode);
            out.put("parameters", params);
        } catch (Throwable ignored) {
            // safe values
        }
        return out;
    }
}
