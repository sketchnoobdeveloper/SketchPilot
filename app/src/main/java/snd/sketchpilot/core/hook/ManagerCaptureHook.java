package snd.sketchpilot.core.hook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import snd.sketchpilot.util.Logger;

/** Captures live manager instances as Sketchware constructs them. */
public final class ManagerCaptureHook {

    private ManagerCaptureHook() {
    }

    public static void install(final String logicalName, Class<?> managerClass) {
        if (managerClass == null) {
            return;
        }
        try {
            XposedHelpers.findAndHookConstructor(managerClass, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        ManagerCapture.capture(logicalName, param.thisObject);
                        Logger.d("Hook", "captured live manager " + logicalName);
                        HookBreadcrumbs.ok("ManagerCapture." + logicalName);
                    } catch (Throwable t) {
                        HookBreadcrumbs.fail("ManagerCapture." + logicalName, t);
                        Logger.hookFailure("ManagerCaptureHook.after", t);
                    }
                }
            });
        } catch (Throwable t) {
            Logger.hookFailure("ManagerCaptureHook.install." + logicalName, t);
        }
    }
}
