package snd.sketchpilot.project.ir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.util.JsonUtil;

/** Converts the native "view"/"view_root" files into typed ViewIr. */
public final class ViewIrExtractor {

    public static List<ViewIr> extract(SectionedFile viewFile, String rootLayoutsJson) {
        List<ViewIr> result = new ArrayList<ViewIr>();
        JSONObject roots = JsonUtil.tryParseObject(rootLayoutsJson);
        List<SectionedFile.Section> sections = viewFile.getSections();
        for (int i = 0; i < sections.size(); i++) {
            SectionedFile.Section section = sections.get(i);
            String name = section.getName();
            if (!name.endsWith(".xml")) {
                continue;
            }
            ViewIr ir = new ViewIr();
            ir.setLayoutName(name);
            if (roots != null) {
                JSONObject root = roots.optJSONObject(name);
                if (root != null) {
                    JSONObject attrs = root.optJSONObject("attributes");
                    if (attrs != null) {
                        Iterator<String> keys = attrs.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            try {
                                ir.getRootAttributes().put(key, attrs.get(key));
                            } catch (Throwable ignored) {
                                // skip exotic value types
                            }
                        }
                    }
                }
            }
            List<String> lines = section.getLines();
            for (int j = 0; j < lines.size(); j++) {
                JSONObject object = JsonUtil.tryParseObject(lines.get(j));
                if (object == null) {
                    continue;
                }
                WidgetIr widget = new WidgetIr();
                widget.setId(JsonUtil.optString(object, "id", ""));
                widget.setTypeCode(JsonUtil.optInt(object, "type", -1));
                widget.setParent(JsonUtil.optString(object, "parent", ""));
                widget.setIndex(JsonUtil.optInt(object, "index", 0));
                Iterator<String> keys = object.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        widget.getProperties().put(key, object.get(key));
                    } catch (Throwable ignored) {
                        // skip exotic value types
                    }
                }
                ir.addWidget(widget);
            }
            result.add(ir);
        }
        return result;
    }
}
