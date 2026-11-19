package snd.sketchpilot.project.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser/serializer for Sketchware Pro's sectioned payloads (evidence E10).
 *
 * Grammar:
 *   payload := ( '@' name nl ) ( line nl )*
 * A section name such as "MainActivity.java_onResume_onResume" encodes the
 * activity and logic section. Empty sections (e.g. @images) are valid.
 *
 * This class is deliberately lossless: every line and section order is kept so
 * that a read-modify-write cycle does not corrupt files the module does not
 * understand yet.
 */
public final class SectionedFile {

    public static final class Section {
        private final String name;
        private final List<String> lines = new ArrayList<String>();

        public Section(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<String> getLines() {
            return lines;
        }

        public void addLine(String line) {
            lines.add(line);
        }
    }

    private final List<Section> sections = new ArrayList<Section>();

    public static SectionedFile parse(String text) {
        SectionedFile result = new SectionedFile();
        if (text == null || text.length() == 0) {
            return result;
        }
        String[] lines = text.split("\n", -1);
        Section current = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() == 0 && i == lines.length - 1) {
                // trailing newline produced by split(-1); ignore it
                continue;
            }
            if (line.startsWith("@")) {
                current = new Section(line.substring(1));
                result.sections.add(current);
            } else {
                if (current == null) {
                    current = new Section("");
                    result.sections.add(current);
                }
                current.addLine(line);
            }
        }
        return result;
    }

    public String serialize() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (section.name.length() > 0) {
                out.append('@').append(section.name).append('\n');
            }
            for (int j = 0; j < section.lines.size(); j++) {
                out.append(section.lines.get(j)).append('\n');
            }
        }
        return out.toString();
    }

    public List<Section> getSections() {
        return sections;
    }

    public Section first(String name) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).name.equals(name)) {
                return sections.get(i);
            }
        }
        return null;
    }

    public List<Section> all(String name) {
        List<Section> found = new ArrayList<Section>();
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).name.equals(name)) {
                found.add(sections.get(i));
            }
        }
        return found;
    }

    /** Replaces the first matching section, or appends a new one. */
    public void put(String name, List<String> lines) {
        Section target = first(name);
        if (target == null) {
            target = new Section(name);
            sections.add(target);
        }
        target.lines.clear();
        if (lines != null) {
            target.lines.addAll(lines);
        }
    }

    public boolean remove(String name) {
        Section target = first(name);
        if (target == null) {
            return false;
        }
        return sections.remove(target);
    }
}
