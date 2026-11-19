package snd.sketchpilot.core.hook;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import snd.sketchpilot.util.Logger;

/**
 * Xposed entry point. The whole body is wrapped in try/catch(Throwable): the
 * module must never crash the host, whatever resolution fails.
 */
public final class SketchPilotEntry implements IXposedHookLoadPackage {

    /** ApplicationInfo.versionCode is hidden in recent SDK stubs; read it reflectively. */
    private static int readVersionCode(android.content.pm.ApplicationInfo info) {
        if (info == null) {
            return 0;
        }
        try {
            java.lang.reflect.Field field = android.content.pm.ApplicationInfo.class.getField("versionCode");
            return field.getInt(info);
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            if (lpparam == null || !HookScope.contains(lpparam.packageName)) {
                return;
            }
            Logger.init();
            Logger.d("Entry", "loading in " + lpparam.packageName);

            if (KillSwitch.isActive()) {
                Logger.w("Entry", "kill switch present; module disabled");
                return;
            }

            String apkPath = lpparam.appInfo == null ? null : lpparam.appInfo.sourceDir;
            int versionCode = readVersionCode(lpparam.appInfo);
            String versionKey = VersionKey.of(versionCode, apkPath);

            ShapeMatchingResolver shape = new ShapeMatchingResolver(lpparam.classLoader);
            shape.addCandidate("a.a.a.eC");
            shape.addCandidate("a.a.a.hC");
            shape.addCandidate("a.a.a.iC");
            shape.addCandidate("a.a.a.oB");
            shape.addCandidate("a.a.a.lC");
            shape.addCandidate("a.a.a.yq");
            shape.addCandidate("a.a.a.ProjectBuilder");
            DexKitResolver resolver = new DexKitResolver(lpparam.classLoader, apkPath, shape);
            Logger.d("Entry", "resolver: " + resolver.describe());

            HookContext context = new HookContext(lpparam.classLoader, lpparam.packageName,
                    versionCode, versionKey, apkPath, resolver);

            XposedHookBridge bridge = new XposedHookBridge();
            bridge.activate(context);
        } catch (Throwable t) {
            Logger.hookFailure("SketchPilotEntry.handleLoadPackage", t);
        }
    }
}
