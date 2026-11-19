package snd.sketchpilot.project.ir;

import java.util.ArrayList;
import java.util.List;

/** Typed IR for one logic section (e.g. MainActivity.java_onCreate_initializeLogic). */
public final class LogicIr {

    private final String sectionName;
    private final List<BlockIr> blocks = new ArrayList<BlockIr>();

    public LogicIr(String sectionName) {
        this.sectionName = sectionName == null ? "" : sectionName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public List<BlockIr> getBlocks() {
        return blocks;
    }

    public void addBlock(BlockIr block) {
        blocks.add(block);
    }

    public org.json.JSONObject toJson() {
        org.json.JSONObject out = new org.json.JSONObject();
        org.json.JSONArray array = new org.json.JSONArray();
        for (int i = 0; i < blocks.size(); i++) {
            array.put(blocks.get(i).toJson());
        }
        try {
            out.put("section", sectionName);
            out.put("blocks", array);
        } catch (Throwable ignored) {
            // safe values
        }
        return out;
    }
}
