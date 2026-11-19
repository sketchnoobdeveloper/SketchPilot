package snd.sketchpilot.diagnostics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import snd.sketchpilot.mcp.McpConfig;
import snd.sketchpilot.mcp.server.McpServer;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;

/** Builds ONE diagnostic zip in the app's external files directory. */
public final class DiagnosticsKit {

    private static final File CAPABILITY_SNAPSHOT =
            new File("/storage/emulated/0/.sketchware/sketchpilot/capabilities.json");

    private DiagnosticsKit() {
    }

    public static File buildZip(Context context) {
        File outputDir = new File(context.getExternalFilesDir(null), "diagnostics");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File zip = new File(outputDir, "sketchpilot-diagnostics.zip");
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(zip, false));
            SelfTest selfTest = new SelfTest();
            selfTest.runAll(new SketchwarePaths(Environment.getExternalStorageDirectory()));
            write(out, "manifest.txt", manifest(context));
            write(out, "app_info.json", appInfo(context).toString(2));
            write(out, "device_info.json", deviceInfo().toString(2));
            write(out, "capability_matrix.json", readTextOrEmpty(CAPABILITY_SNAPSHOT));
            write(out, "hook_breadcrumbs.txt", breadcrumbs(logFile()));
            write(out, "last500.log", tail(logFile(), 500));
            write(out, "mcp_status.json", mcpStatus().toString(2));
            write(out, "self_test.json", new JSONObject().put("results", selfTest.getResults()).toString(2));
            write(out, "gaps.json", gaps().toString(2));
            return zip;
        } catch (Throwable t) {
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

    private static String manifest(Context context) {
        StringBuilder out = new StringBuilder();
        out.append("SketchPilot diagnostics bundle\n");
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            out.append("module=").append(info.packageName).append("\n");
            out.append("moduleVersion=").append(info.versionName).append(" (").append(info.versionCode).append(")\n");
        } catch (Throwable t) {
            out.append("moduleVersion=unknown\n");
        }
        out.append("generated=").append(System.currentTimeMillis()).append("\n");
        return out.toString();
    }

    private static JSONObject appInfo(Context context) {
        JSONObject out = new JSONObject();
        try {
            out.put("sketchware", InstalledApps.getSketchwareInfo(context));
            out.put("lsposedManagerInstalled", isInstalled(context, "org.lsposed.manager"));
            out.put("xposedBridgeInThisProcess", exists("de.robv.android.xposed.XposedBridge"));
            out.put("note", "LSPosed activation state must be confirmed in the LSPosed UI (not readable here)");
        } catch (Throwable t) {
            try {
                out.put("error", String.valueOf(t.getMessage()));
            } catch (Throwable ignored) {
                // safe
            }
        }
        return out;
    }

    private static JSONObject deviceInfo() {
        JSONObject out = new JSONObject();
        try {
            out.put("androidRelease", Build.VERSION.RELEASE);
            out.put("sdk", Build.VERSION.SDK_INT);
            out.put("device", Build.DEVICE);
            out.put("model", Build.MODEL);
            out.put("brand", Build.BRAND);
            out.put("abi", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown");
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }

    private static JSONObject mcpStatus() {
        JSONObject out = new JSONObject();
        try {
            out.put("running", McpServer.RUNNING);
            out.put("port", McpConfig.DEFAULT_PORT);
            out.put("endpoint", "http://127.0.0.1:" + McpConfig.DEFAULT_PORT + "/mcp");
            out.put("tokenPresent", McpConfig.TOKEN_FILE.isFile());
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }

    private static JSONArray gaps() {
        JSONArray out = new JSONArray();
        String[][] rows = {
                {"GAP-1", "live manager instances", "constructor capture + reflection locator", "offline read"},
                {"GAP-2", "editor refresh method", "reflective candidate probe", "require reopen project"},
                {"GAP-3", "custom block palette", "read runtime files", "builtin registry only"},
                {"GAP-4", "installed versionCode", "PackageManager with <queries>", "runtime probe"},
                {"GAP-5", "java/local_library grammar", "generic sectioned parser", "treat as opaque bytes"},
                {"GAP-6", "system JSON shapes", "tolerant JSON parsing", "skip unknown sections"},
                {"GAP-7", "Gradle export", "backup-archive export", "clear 'not available yet' error"}
        };
        for (int i = 0; i < rows.length; i++) {
            JSONObject row = new JSONObject();
            try {
                row.put("id", rows[i][0]);
                row.put("unproven", rows[i][1]);
                row.put("implementation", rows[i][2]);
                row.put("fallback", rows[i][3]);
            } catch (Throwable ignored) {
                // safe
            }
            out.put(row);
        }
        return out;
    }

    private static boolean isInstalled(Context context, String pkg) {
        try {
            context.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean exists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static File logFile() {
        return new File(Environment.getExternalStorageDirectory(),
                ".sketchware/sketchpilot/sketchpilot.log");
    }

    private static String breadcrumbs(File log) {
        String text = readTextOrEmpty(log);
        String[] lines = text.split("\n");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("SP-HOOK")) {
                out.append(lines[i]).append('\n');
            }
        }
        return out.toString();
    }

    private static String tail(File file, int count) {
        String text = readTextOrEmpty(file);
        String[] lines = text.split("\n");
        StringBuilder out = new StringBuilder();
        int from = Math.max(0, lines.length - count);
        for (int i = from; i < lines.length; i++) {
            out.append(lines[i]).append('\n');
        }
        return out.toString();
    }

    private static String readTextOrEmpty(File file) {
        if (!file.isFile()) {
            return "";
        }
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            return "";
        }
    }

    private static void write(ZipOutputStream out, String name, String content) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        out.write(bytes);
        out.closeEntry();
    }
}
