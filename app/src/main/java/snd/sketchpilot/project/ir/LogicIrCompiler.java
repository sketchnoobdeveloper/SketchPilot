package snd.sketchpilot.project.ir;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.project.io.SectionedFile;

/** Compiles LogicIr into the native BlockBean JSON lines of the "logic" file. */
public final class LogicIrCompiler {

    private final BlockSpecRegistry specs;

    public LogicIrCompiler(BlockSpecRegistry specs) {
        this.specs = specs;
    }

    public SectionedFile compile(SectionedFile original, LogicIr ir) {
        SectionedFile result = new SectionedFile();
        List<SectionedFile.Section> sections = original.getSections();
        for (int i = 0; i < sections.size(); i++) {
            SectionedFile.Section source = sections.get(i);
            if (!source.getName().equals(ir.getSectionName())) {
                SectionedFile.Section copy = new SectionedFile.Section(source.getName());
                copy.getLines().addAll(source.getLines());
                result.getSections().add(copy);
                continue;
            }
            result.getSections().add(buildSection(ir));
        }
        if (result.first(ir.getSectionName()) == null) {
            result.getSections().add(buildSection(ir));
        }
        return result;
    }

    private SectionedFile.Section buildSection(LogicIr ir) {
        SectionedFile.Section section = new SectionedFile.Section(ir.getSectionName());
        List<BlockIr> blocks = ir.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            BlockIr block = blocks.get(i);
            section.addLine(toBlockJson(block, i + 1, i + 2 < blocks.size() ? i + 2 : -1));
        }
        return section;
    }

    private String toBlockJson(BlockIr block, int numericId, int nextId) {
        JSONObject out = new JSONObject();
        JSONArray params = new JSONArray();
        for (int i = 0; i < block.getParameters().size(); i++) {
            params.put(block.getParameters().get(i));
        }
        try {
            out.put("color", -10701022);
            out.put("id", block.getId().length() > 0 ? block.getId() : String.valueOf(numericId));
            out.put("nextBlock", nextId);
            out.put("opCode", block.getOpCode());
            out.put("parameters", params);
            out.put("spec", specs.getSpec(block.getOpCode()));
            out.put("subStack1", block.getSubStack1().isEmpty() ? -1 : 0);
            out.put("subStack2", block.getSubStack2().isEmpty() ? -1 : 0);
            out.put("type", " ");
            out.put("typeName", "");
        } catch (Throwable ignored) {
            // safe values
        }
        return out.toString();
    }
}
