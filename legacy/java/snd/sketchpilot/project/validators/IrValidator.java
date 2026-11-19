package snd.sketchpilot.project.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.project.ir.BlockIr;
import snd.sketchpilot.project.ir.LogicIr;
import snd.sketchpilot.project.ir.ViewIr;
import snd.sketchpilot.project.ir.WidgetIr;
import snd.sketchpilot.view.WidgetRegistry;

/**
 * Validates typed IR against the runtime registries before any native write.
 * Returns a list of errors; an empty list means the IR may be compiled.
 */
public final class IrValidator {

    private final WidgetRegistry widgets;
    private final BlockSpecRegistry specs;

    public IrValidator(WidgetRegistry widgets, BlockSpecRegistry specs) {
        this.widgets = widgets;
        this.specs = specs;
    }

    public List<String> validate(ViewIr ir) {
        List<String> errors = new ArrayList<String>();
        if (ir == null) {
            errors.add("view IR is null");
            return errors;
        }
        if (!ir.getLayoutName().endsWith(".xml")) {
            errors.add("layout name must end with .xml: " + ir.getLayoutName());
        }
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < ir.getWidgets().size(); i++) {
            WidgetIr widget = ir.getWidgets().get(i);
            String id = widget.getId();
            // Some native layout entries legitimately have no id (root nodes).
            // Enforce uniqueness only for named widgets so real data round-trips.
            if (id != null && id.length() > 0 && !ids.add(id)) {
                errors.add("duplicate widget id: " + id);
            }
            boolean typeSpecified = widget.getTypeCode() >= 0 || widget.getTypeName().length() > 0;
            if (typeSpecified && widget.getTypeCode() < 0 && !widgets.isValidType(widget.getTypeName())) {
                errors.add("unknown widget type: " + widget.getTypeName());
            }
            if (widget.getTypeCode() >= 0 && !widgets.isValidType(widget.getTypeCode())) {
                errors.add("widget type out of range: " + widget.getTypeCode());
            }
            if (widget.getIndex() < 0) {
                errors.add("negative index for widget " + id);
            }
        }
        return errors;
    }

    public List<String> validate(LogicIr ir) {
        List<String> errors = new ArrayList<String>();
        if (ir == null) {
            errors.add("logic IR is null");
            return errors;
        }
        if (ir.getSectionName().indexOf(".java_") < 0) {
            errors.add("logic section name must be <Activity>.java_<section>: " + ir.getSectionName());
        }
        for (int i = 0; i < ir.getBlocks().size(); i++) {
            BlockIr block = ir.getBlocks().get(i);
            String op = block.getOpCode();
            if (op == null || op.length() == 0) {
                errors.add("block #" + i + " has empty opCode");
                continue;
            }
            if (specs.isLoadedFromHost() && !specs.has(op)) {
                errors.add("opCode not in runtime registry: " + op);
            }
            int expected = specs.getParameterCount(op);
            if (expected >= 0 && block.getParameters().size() != expected) {
                errors.add("block " + op + " expects " + expected + " parameters but got "
                        + block.getParameters().size());
            }
        }
        return errors;
    }
}
