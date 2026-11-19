package snd.sketchpilot.core.hook;

import java.io.File;

/** Global module kill switch, toggleable from the companion app. */
public final class KillSwitch {

    public static final File FILE =
            new File("/storage/emulated/0/.sketchware/sketchpilot/killswitch");

    private KillSwitch() {
    }

    public static boolean isActive() {
        return FILE.exists();
    }

    public static void set(boolean active) {
        try {
            if (active) {
                File parent = FILE.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                FILE.createNewFile();
            } else {
                FILE.delete();
            }
        } catch (Throwable ignored) {
            // best effort
        }
    }
}
