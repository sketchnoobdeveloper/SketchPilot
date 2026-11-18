package snd.sketchpilot.core.hook;

import java.util.HashSet;
import java.util.Set;

import snd.sketchpilot.util.Logger;

/** Logs "SP-HOOK <name> OK/FAIL" the first time each hook fires. */
public final class HookBreadcrumbs {

    private static final Set<String> LOGGED = new HashSet<String>();

    private HookBreadcrumbs() {
    }

    public static synchronized void ok(String hookName) {
        if (LOGGED.add(hookName)) {
            Logger.d("Hook", "SP-HOOK " + hookName + " OK");
        }
    }

    public static synchronized void fail(String hookName, Throwable error) {
        if (LOGGED.add(hookName + ":fail")) {
            Logger.e("Hook", "SP-HOOK " + hookName + " FAIL " + error, error);
        }
    }
}
