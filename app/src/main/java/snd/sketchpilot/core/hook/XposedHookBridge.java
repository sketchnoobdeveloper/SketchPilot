package snd.sketchpilot.core.hook;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import snd.sketchpilot.core.bridge.BridgeConfig;
import snd.sketchpilot.core.bridge.BridgeHandlers;
import snd.sketchpilot.core.bridge.LiveBridgeServer;
import snd.sketchpilot.project.LiveRepository;
import snd.sketchpilot.project.OfflineRepository;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.util.Logger;

/** Xposed implementation of HookBridge. Every callback is wrapped in try/catch(Throwable). */
public final class XposedHookBridge implements HookBridge {

    private boolean enabled = true;
    private final CapabilityMatrix matrix = new CapabilityMatrix();
    private static LiveBridgeServer bridgeServer;

    public String getName() {
        return "xposed";
    }

    public List<Capability> getCapabilities() {
        return matrix.getCapabilities();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void activate(HookContext context) throws Throwable {
        if (!enabled) {
            Logger.d("Hook", "global kill switch active; not registering hooks");
            return;
        }
        try {
            matrix.evaluate(context.getResolver());
            matrix.saveSnapshot(snapshotFile());

            captureManagers(context);
            startBridge(context);
            ActivityHook.installAll(context);

            XposedHelpers.findAndHookMethod(
                    "android.app.Application",
                    context.getClassLoader(),
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                Logger.d("Hook", "host application started: " + context.getPackageName()
                                        + " versionKey=" + context.getVersionKey());
                                HookBreadcrumbs.ok("Application.onCreate");
                            } catch (Throwable t) {
                                HookBreadcrumbs.fail("Application.onCreate", t);
                                Logger.hookFailure("Application.onCreate", t);
                            }
                        }
                    });
            Logger.d("Hook", "Xposed hooks registered; live capabilities="
                    + countLive() + "/" + matrix.getCapabilities().size());
        } catch (Throwable t) {
            Logger.hookFailure("XposedHookBridge.activate", t);
        }
    }

    private void captureManagers(HookContext context) {
        String[] logical = {"project.data", "project.files", "project.libraries"};
        for (int i = 0; i < logical.length; i++) {
            Class<?> cls = context.getResolver().resolve(CapabilityMatrix.getFingerprint(logical[i]));
            if (cls != null) {
                ManagerCaptureHook.install(logical[i], cls);
            }
        }
    }

    private void startBridge(HookContext context) {
        try {
            if (bridgeServer != null) {
                bridgeServer.stop();
            }
            OfflineRepository offline = new OfflineRepository(
                    new SketchwarePaths(new java.io.File("/storage/emulated/0")),
                    new SketchwareCrypto());
            LiveRepository live = new LiveRepository(offline);
            bridgeServer = new LiveBridgeServer(BridgeConfig.PORT);
            BridgeHandlers.registerAll(bridgeServer, live, matrix);
            bridgeServer.start();
            HookBreadcrumbs.ok("Bridge.start");
        } catch (Throwable t) {
            HookBreadcrumbs.fail("Bridge.start", t);
            Logger.hookFailure("XposedHookBridge.startBridge", t);
        }
    }

    private int countLive() {
        int count = 0;
        List<Capability> list = matrix.getCapabilities();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isLive()) {
                count++;
            }
        }
        return count;
    }

    private static java.io.File snapshotFile() {
        return new java.io.File("/storage/emulated/0/.sketchware/sketchpilot/capabilities.json");
    }
}
