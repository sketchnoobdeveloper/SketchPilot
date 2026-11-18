package snd.sketchpilot.util;

import java.io.File;
import java.io.FileWriter;

/** Append-only audit log for writes, approvals and MCP calls. */
public final class AuditLog {

    private final File file;

    public AuditLog(File file) {
        this.file = file;
    }

    public void record(String action, String detail) {
        FileWriter writer = null;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            writer = new FileWriter(file, true);
            writer.write(System.currentTimeMillis() + " " + action + " "
                    + (detail == null ? "" : Redactor.redactText(detail).replace('\n', ' ')) + "\n");
            writer.flush();
        } catch (Throwable ignored) {
            // audit is best-effort
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
}
