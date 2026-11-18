package snd.sketchpilot.agent;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Maintains the per-project SKETCHPILOT.md memory file. */
public final class AgentMemory {

    public static final String FILE_NAME = "SKETCHPILOT.md";
    private static final int MAX_CHARS = 8000;

    private AgentMemory() {
    }

    public static File fileFor(File projectRoot) {
        return new File(projectRoot, FILE_NAME);
    }

    public static String load(File projectRoot) {
        File file = fileFor(projectRoot);
        if (!file.isFile()) {
            return "";
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String text = new String(bytes, StandardCharsets.UTF_8);
            return text.length() > MAX_CHARS ? text.substring(text.length() - MAX_CHARS) : text;
        } catch (Throwable t) {
            return "";
        }
    }

    public static void append(File projectRoot, String note) {
        if (note == null || note.length() == 0) {
            return;
        }
        File file = fileFor(projectRoot);
        FileWriter writer = null;
        try {
            String existing = load(projectRoot);
            String next = existing + "\n- " + note;
            if (next.length() > MAX_CHARS) {
                next = next.substring(next.length() - MAX_CHARS);
            }
            writer = new FileWriter(file, false);
            writer.write("# SketchPilot project memory\n" + next + "\n");
            writer.flush();
        } catch (Throwable ignored) {
            // memory is best-effort
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }

    public static String digest(File projectRoot) {
        String text = load(projectRoot);
        return text.length() > 1200 ? text.substring(0, 1200) : text;
    }
}
