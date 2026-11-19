package snd.sketchpilot.project.ir;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/** Typed IR for one screen layout: root attributes + ordered widgets. */
public final class ViewIr {

    private String layoutName = "";
    private final JSONObject rootAttributes = new JSONObject();
    private final List<WidgetIr> widgets = new ArrayList<WidgetIr>();

    public String getLayoutName() {
        return layoutName;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public JSONObject getRootAttributes() {
        return rootAttributes;
    }

    public void setRootAttributes(JSONObject source) {
        java.util.Iterator<String> old = rootAttributes.keys();
        java.util.List<String> toRemove = new java.util.ArrayList<String>();
        while (old.hasNext()) {
            toRemove.add(old.next());
        }
        for (int i = 0; i < toRemove.size(); i++) {
            rootAttributes.remove(toRemove.get(i));
        }
        if (source == null) {
            return;
        }
        java.util.Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                rootAttributes.put(key, source.get(key));
            } catch (Throwable ignored) {
                // skip exotic values
            }
        }
    }

    public List<WidgetIr> getWidgets() {
        return widgets;
    }

    public void addWidget(WidgetIr widget) {
        widgets.add(widget);
    }

    public JSONObject toJson() {
        JSONObject out = new JSONObject();
        org.json.JSONArray array = new org.json.JSONArray();
        for (int i = 0; i < widgets.size(); i++) {
            array.put(widgets.get(i).toJson());
        }
        try {
            out.put("layout", layoutName);
            out.put("rootAttributes", rootAttributes);
            out.put("widgets", array);
        } catch (Throwable ignored) {
            // safe values
        }
        return out;
    }
}
