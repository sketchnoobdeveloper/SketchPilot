package snd.sketchpilot.project.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import snd.sketchpilot.project.SketchwarePaths;
import java.util.logging.Level;

/** Creates a timestamped backup before every mutation (hard constraint). */
public final class BackupManager {

    private final SketchwarePaths paths;

    public BackupManager(SketchwarePaths paths) {
        this.paths = paths;
    }

    public File backup(String scId, String label) {
        File destination = new File(paths.backup(scId),
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date())
                        + "_" + safe(label));
        File source = paths.data(scId);
        if (!source.isDirectory()) {
            return null;
        }
        try {
            copyDirectory(source, new File(destination, "data"));
            File metadata = paths.projectMetadata(scId);
            if (metadata.isFile()) {
                File metaDir = new File(destination, "metadata");
                metaDir.mkdirs();
                Files.copy(metadata.toPath(), new File(metaDir, "project").toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return destination;
        } catch (Throwable t) {
            java.util.logging.Logger.getLogger("SketchPilot").log(Level.WARNING, "backup failed", t);
            return null;
        }
    }

    /** Restores a backup created by backup(). Returns true on success. */
    public boolean restore(String scId, File backupDir) {
        if (backupDir == null || !backupDir.isDirectory()) {
            return false;
        }
        try {
            File archivedData = new File(backupDir, "data");
            if (archivedData.isDirectory()) {
                copyDirectory(archivedData, paths.data(scId));
            }
            File archivedMeta = new File(backupDir, "metadata/project");
            if (archivedMeta.isFile()) {
                File target = paths.projectMetadata(scId);
                File parent = target.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                Files.copy(archivedMeta.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Throwable t) {
            java.util.logging.Logger.getLogger("SketchPilot").log(Level.WARNING, "restore failed", t);
            return false;
        }
    }

    private static void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdirs();
        }
        File[] children = source.listFiles();
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            File copy = new File(target, child.getName());
            if (child.isDirectory()) {
                copyDirectory(child, copy);
            } else {
                Files.copy(child.toPath(), copy.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static String safe(String label) {
        if (label == null) {
            return "write";
        }
        return label.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
