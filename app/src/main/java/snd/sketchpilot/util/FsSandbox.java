package snd.sketchpilot.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import snd.sketchpilot.project.SketchwarePaths;

/** Restricts writes to project-owned paths. Denylist for everything else. */
public final class FsSandbox {

    private final SketchwarePaths paths;

    public FsSandbox(SketchwarePaths paths) {
        this.paths = paths;
    }

    public boolean isAllowed(File file, String scId) {
        if (file == null || scId == null) {
            return false;
        }
        try {
            String canonical = file.getCanonicalPath();
            List<File> allowed = new ArrayList<File>();
            allowed.add(paths.data(scId));
            allowed.add(paths.mysc(scId));
            allowed.add(new File(paths.list(), scId));
            allowed.add(paths.backup(scId));
            for (int i = 0; i < allowed.size(); i++) {
                String root = allowed.get(i).getCanonicalPath();
                if (canonical.equals(root) || canonical.startsWith(root + File.separator)) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }
}
