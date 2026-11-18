package snd.sketchpilot.project.transactions;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** A set of file writes that are applied atomically with rollback on failure. */
public final class TransactionPlan {

    private final String label;
    private final Map<String, byte[]> changes = new LinkedHashMap<String, byte[]>();

    public TransactionPlan(String label) {
        this.label = label == null ? "write" : label;
    }

    public String getLabel() {
        return label;
    }

    public void put(File file, byte[] bytes) {
        changes.put(file.getAbsolutePath(), bytes);
    }

    public Map<String, byte[]> getChanges() {
        return changes;
    }
}
