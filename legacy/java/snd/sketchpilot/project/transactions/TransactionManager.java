package snd.sketchpilot.project.transactions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import snd.sketchpilot.project.LiveSessionRegistry;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.backup.BackupManager;
import snd.sketchpilot.project.io.ProjectFileIO;
import snd.sketchpilot.util.FsSandbox;

/**
 * snapshot, validate, backup, apply, verify, commit,
 * or rollback on any failure. Maintains per-project undo/redo stacks.
 */
public final class TransactionManager {

    public static final class Result {
        public final boolean success;
        public final String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static final class UndoRecord {
        final Map<String, byte[]> before = new LinkedHashMap<String, byte[]>();
        final Map<String, byte[]> after = new LinkedHashMap<String, byte[]>();
    }

    private final SketchwarePaths paths;
    private final ProjectFileIO io;
    private final BackupManager backups;
    private final FsSandbox sandbox;
    private snd.sketchpilot.project.ConflictGuard conflictGuard;
    private final Map<String, List<UndoRecord>> undoStacks = new HashMap<String, List<UndoRecord>>();
    private final Map<String, List<UndoRecord>> redoStacks = new HashMap<String, List<UndoRecord>>();

    public TransactionManager(SketchwarePaths paths, ProjectFileIO io, BackupManager backups) {
        this.paths = paths;
        this.io = io;
        this.backups = backups;
        this.sandbox = new FsSandbox(paths);
    }

    public void setConflictGuard(snd.sketchpilot.project.ConflictGuard guard) {
        this.conflictGuard = guard;
    }

    public Result commit(String scId, TransactionPlan plan) {
        if (conflictGuard != null && !conflictGuard.canWrite(scId)) {
            return new Result(false, "project " + scId + " is open in Sketchware (or locked). "
                    + "Close and reopen the project before offline edits; live editor refresh is unavailable.");
        }
        if (LiveSessionRegistry.isOpen(scId)) {
            return new Result(false, "project " + scId + " is open in a live Sketchware session. "
                    + "Close and reopen the project before offline edits.");
        }
        Iterator<String> keys = plan.getChanges().keySet().iterator();
        while (keys.hasNext()) {
            File file = new File(keys.next());
            if (!sandbox.isAllowed(file, scId)) {
                return new Result(false, "path outside project sandbox: " + file.getAbsolutePath());
            }
        }

        UndoRecord record = new UndoRecord();
        Iterator<String> beforeKeys = plan.getChanges().keySet().iterator();
        while (beforeKeys.hasNext()) {
            String path = beforeKeys.next();
            File file = new File(path);
            try {
                record.before.put(path, file.isFile() ? io.read(file) : null);
            } catch (Throwable t) {
                return new Result(false, "snapshot failed for " + path + ": " + t.getMessage());
            }
        }
        record.after.putAll(plan.getChanges());

        File backup = backups.backup(scId, plan.getLabel());
        if (backup == null) {
            return new Result(false, "backup failed; write aborted");
        }

        Iterator<String> writeKeys = plan.getChanges().keySet().iterator();
        while (writeKeys.hasNext()) {
            String path = writeKeys.next();
            try {
                io.write(new File(path), plan.getChanges().get(path));
            } catch (Throwable t) {
                rollback(record);
                return new Result(false, "write failed for " + path + ": " + t.getMessage());
            }
        }

        Iterator<String> verifyKeys = plan.getChanges().keySet().iterator();
        while (verifyKeys.hasNext()) {
            String path = verifyKeys.next();
            try {
                byte[] actual = io.read(new File(path));
                byte[] expected = plan.getChanges().get(path);
                if (actual.length != expected.length) {
                    rollback(record);
                    return new Result(false, "verification failed for " + path);
                }
                for (int i = 0; i < actual.length; i++) {
                    if (actual[i] != expected[i]) {
                        rollback(record);
                        return new Result(false, "verification failed for " + path);
                    }
                }
            } catch (Throwable t) {
                rollback(record);
                return new Result(false, "verification error for " + path + ": " + t.getMessage());
            }
        }

        pushUndo(scId, record);
        List<UndoRecord> redo = redoStacks.get(scId);
        if (redo != null) {
            redo.clear();
        }
        return new Result(true, "committed: " + plan.getLabel());
    }

    private void rollback(UndoRecord record) {
        Iterator<String> keys = record.before.keySet().iterator();
        while (keys.hasNext()) {
            String path = keys.next();
            byte[] bytes = record.before.get(path);
            try {
                if (bytes == null) {
                    new File(path).delete();
                } else {
                    io.write(new File(path), bytes);
                }
            } catch (Throwable t) {
                java.util.logging.Logger.getLogger("SketchPilot").log(Level.SEVERE, "rollback failed", t);
            }
        }
    }

    private void pushUndo(String scId, UndoRecord record) {
        List<UndoRecord> stack = undoStacks.get(scId);
        if (stack == null) {
            stack = new ArrayList<UndoRecord>();
            undoStacks.put(scId, stack);
        }
        stack.add(record);
    }

    public Result undo(String scId) {
        List<UndoRecord> stack = undoStacks.get(scId);
        if (stack == null || stack.isEmpty()) {
            return new Result(false, "nothing to undo");
        }
        UndoRecord record = stack.remove(stack.size() - 1);
        rollback(record);
        List<UndoRecord> redo = redoStacks.get(scId);
        if (redo == null) {
            redo = new ArrayList<UndoRecord>();
            redoStacks.put(scId, redo);
        }
        redo.add(record);
        return new Result(true, "undone");
    }

    public Result redo(String scId) {
        List<UndoRecord> stack = redoStacks.get(scId);
        if (stack == null || stack.isEmpty()) {
            return new Result(false, "nothing to redo");
        }
        UndoRecord record = stack.remove(stack.size() - 1);
        Iterator<String> keys = record.after.keySet().iterator();
        while (keys.hasNext()) {
            String path = keys.next();
            try {
                io.write(new File(path), record.after.get(path));
            } catch (Throwable t) {
                return new Result(false, "redo failed: " + t.getMessage());
            }
        }
        pushUndo(scId, record);
        return new Result(true, "redone");
    }
}
