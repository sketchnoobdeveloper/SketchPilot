package snd.sketchpilot.core.hook;

import snd.sketchpilot.util.Logger;

/** Central guard used by every hook callback body. Never rethrows into the host. */
public final class SafeHooks {

    private SafeHooks() {
    }

    public static void run(String hookName, Runnable work) {
        try {
            work.run();
        } catch (Throwable t) {
            Logger.hookFailure(hookName, t);
        }
    }
}
