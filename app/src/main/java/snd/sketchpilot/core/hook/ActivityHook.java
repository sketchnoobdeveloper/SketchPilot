package snd.sketchpilot.core.hook;

import android.app.Activity;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import snd.sketchpilot.ui.OverlayPanel;
import snd.sketchpilot.util.Logger;

/** Hooks Sketchware activities to inject the agent panel. */
public final class ActivityHook {

    private static final String[] TARGETS = {
            "ui.main", "ui.design", "ui.logic", "ui.settings", "ui.buildlog"
    };

    private ActivityHook() {
    }

    public static void installAll(HookContext context) {
        for (int i = 0; i < TARGETS.length; i++) {
            install(context, TARGETS[i]);
        }
    }

    private static void install(final HookContext context, String capabilityId) {
        Fingerprint fingerprint = CapabilityMatrix.getFingerprint(capabilityId);
        Class<?> target = context.getResolver().resolve(fingerprint);
        if (target == null) {
            Logger.d("Hook", "activity not resolved: " + capabilityId + " (offline mode)");
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(target, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.thisObject instanceof Activity) {
                            OverlayPanel.attach((Activity) param.thisObject, context);
                            HookBreadcrumbs.ok("Activity." + capabilityId);
                        }
                    } catch (Throwable t) {
                        HookBreadcrumbs.fail("Activity." + capabilityId, t);
                        Logger.hookFailure("ActivityHook." + capabilityId, t);
                    }
                }
            });
            Logger.d("Hook", "overlay installed on " + target.getName());
        } catch (Throwable t) {
            Logger.hookFailure("ActivityHook.install." + capabilityId, t);
        }
    }
}
