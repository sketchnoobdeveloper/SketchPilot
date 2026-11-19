package snd.sketchpilot.project;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which projects are open in a live Sketchware session. Offline writes
 * to an open project are rejected (master prompt section 5, conflict safety).
 * The P3 live bridge populates this registry; offline-only runs keep it empty.
 */
public final class LiveSessionRegistry {

    private static final Set<String> OPEN = new HashSet<String>();

    private LiveSessionRegistry() {
    }

    public static synchronized void open(String scId) {
        if (scId != null) {
            OPEN.add(scId);
        }
    }

    public static synchronized void close(String scId) {
        if (scId != null) {
            OPEN.remove(scId);
        }
    }

    public static synchronized boolean isOpen(String scId) {
        return scId != null && OPEN.contains(scId);
    }

    public static synchronized void clear() {
        OPEN.clear();
    }
}
