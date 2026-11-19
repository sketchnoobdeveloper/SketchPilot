package snd.sketchpilot.project;

import java.io.File;

/**
 * Verified on-device storage layout (docs/SKETCHWARE_INTERNALS.md E2, E4, E5).
 * All paths are relative to the external storage root, normally /storage/emulated/0.
 */
public final class SketchwarePaths {

    public static final String ROOT_RELATIVE = ".sketchware";
    public static final String DATA = ROOT_RELATIVE + "/data";
    public static final String MYSC = ROOT_RELATIVE + "/mysc";
    public static final String LIST = MYSC + "/list";
    public static final String BAK = ROOT_RELATIVE + "/bak";
    public static final String LIBS = ROOT_RELATIVE + "/libs";
    public static final String RESOURCES = ROOT_RELATIVE + "/resources";
    public static final String SYSTEM_DATA = DATA + "/system";

    private final File externalRoot;

    public SketchwarePaths(File externalRoot) {
        this.externalRoot = externalRoot;
    }

    public File root() {
        return new File(externalRoot, ROOT_RELATIVE);
    }

    public File data(String scId) {
        return new File(externalRoot, DATA + "/" + scId);
    }

    public File mysc(String scId) {
        return new File(externalRoot, MYSC + "/" + scId);
    }

    public File list() {
        return new File(externalRoot, LIST);
    }

    public File projectMetadata(String scId) {
        return new File(externalRoot, LIST + "/" + scId + "/project");
    }

    public File dataFile(String scId, String name) {
        return new File(data(scId), name);
    }

    public File backup(String scId) {
        return new File(externalRoot, BAK + "/" + scId);
    }

    public File systemDataFile(String name) {
        return new File(externalRoot, SYSTEM_DATA + "/" + name);
    }
}
