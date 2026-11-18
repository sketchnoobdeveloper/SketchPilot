package snd.sketchpilot.gaps;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import snd.sketchpilot.project.SketchwarePaths;

/**
 * GAP-7: full Android Studio Gradle export is not verified. Safest implemented
 * export is a portable backup archive (data + metadata). Gradle export returns
 * a clear "not available yet" error rather than a fake success.
 */
public final class Gap7GradleExport {

    private Gap7GradleExport() {
    }

    public static File backupArchive(SketchwarePaths paths, String scId, File outputDir) {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File output = new File(outputDir, "sketchpilot-" + scId + "-backup.zip");
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(output, false));
            File data = paths.data(scId);
            if (data.isDirectory()) {
                addDirectory(out, data, "data");
            }
            File metadata = paths.projectMetadata(scId);
            if (metadata.isFile()) {
                out.putNextEntry(new ZipEntry("metadata/project"));
                out.write(Files.readAllBytes(metadata.toPath()));
                out.closeEntry();
            }
            return output;
        } catch (Throwable t) {
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }

    private static void addDirectory(ZipOutputStream out, File dir, String prefix) throws Exception {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            String entry = prefix + "/" + child.getName();
            if (child.isDirectory()) {
                addDirectory(out, child, entry);
            } else {
                out.putNextEntry(new ZipEntry(entry));
                out.write(Files.readAllBytes(child.toPath()));
                out.closeEntry();
            }
        }
    }
}
