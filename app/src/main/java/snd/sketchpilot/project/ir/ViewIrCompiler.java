package snd.sketchpilot.project.ir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.view.WidgetRegistry;

/** Compiles ViewIr back into the native sectioned "view" payload. */
public final class ViewIrCompiler {

    private final WidgetRegistry widgets;

    public ViewIrCompiler(WidgetRegistry widgets) {
        this.widgets = widgets;
    }

    public SectionedFile compile(SectionedFile original, ViewIr ir) {
        SectionedFile result = new SectionedFile();
        List<SectionedFile.Section> sections = original.getSections();
        for (int i = 0; i < sections.size(); i++) {
            SectionedFile.Section source = sections.get(i);
            if (!source.getName().equals(ir.getLayoutName())) {
                result.getSections().add(copy(source));
                continue;
            }
            SectionedFile.Section replacement = new SectionedFile.Section(ir.getLayoutName());
            for (int j = 0; j < ir.getWidgets().size(); j++) {
                WidgetIr widget = ir.getWidgets().get(j);
                JSONObject line = new JSONObject();
                Iterator<String> keys = widget.getProperties().keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        line.put(key, widget.getProperties().get(key));
                    } catch (Throwable ignored) {
                        // skip exotic values
                    }
                }
                try {
                    line.put("id", widget.getId());
                    int code = widget.getTypeCode();
                    if (code < 0 && widget.getTypeName().length() > 0) {
                        code = widgets.typeOf(widget.getTypeName());
                    }
                    if (code >= 0) {
                        line.put("type", code);
                    }
                    if (widget.getParent().length() > 0) {
                        line.put("parent", widget.getParent());
                    }
                    line.put("index", widget.getIndex());
                } catch (Throwable ignored) {
                    // safe values
                }
                replacement.addLine(line.toString());
            }
            result.getSections().add(replacement);
        }
        if (result.first(ir.getLayoutName()) == null) {
            SectionedFile.Section added = new SectionedFile.Section(ir.getLayoutName());
            for (int j = 0; j < ir.getWidgets().size(); j++) {
                added.addLine(ir.getWidgets().get(j).toJson().toString());
            }
            result.getSections().add(added);
        }
        return result;
    }

    private static SectionedFile.Section copy(SectionedFile.Section source) {
        SectionedFile.Section copy = new SectionedFile.Section(source.getName());
        copy.getLines().addAll(source.getLines());
        return copy;
    }
}
