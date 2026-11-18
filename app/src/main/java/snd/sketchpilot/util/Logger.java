package snd.sketchpilot.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * File-backed logger used by both the hooked host process and the companion app.
 * Never throws: a logging failure must never crash the host.
 */
public final class Logger {

    private static final String TAG = "SketchPilot";
    private static final Object LOCK = new Object();
    private static String logPath = null;

    private Logger() {
    }

    public static void init() {
        synchronized (LOCK) {
            if (logPath != null) {
                return;
            }
            try {
                File dir = new File(Environment.getExternalStorageDirectory(), ".sketchware/sketchpilot");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                logPath = new File(dir, "sketchpilot.log").getAbsolutePath();
            } catch (Throwable t) {
                logPath = null;
            }
        }
    }

    public static void d(String tag, String message) {
        write("D", tag, message, null);
    }

    public static void w(String tag, String message) {
        write("W", tag, message, null);
    }

    public static void e(String tag, String message, Throwable throwable) {
        write("E", tag, message, throwable);
    }

    /** Mandatory wrapper body for every hook: logs the throwable and never rethrows. */
    public static void hookFailure(String hookName, Throwable throwable) {
        e("Hook", "hook failed: " + hookName, throwable);
    }

    private static void write(String level, String tag, String message, Throwable throwable) {
        String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date())
                + " " + level + "/" + tag + ": " + message;
        try {
            Log.println(level.equals("E") ? Log.ERROR : (level.equals("W") ? Log.WARN : Log.DEBUG), TAG, line);
        } catch (Throwable ignored) {
            // host may not expose Log
        }
        synchronized (LOCK) {
            if (logPath == null) {
                init();
            }
            if (logPath == null) {
                return;
            }
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new FileWriter(logPath, true));
                writer.println(line);
                if (throwable != null) {
                    throwable.printStackTrace(writer);
                }
                writer.flush();
            } catch (Throwable ignored) {
                // never crash because of logging
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable ignored) {
                        // ignore
                    }
                }
            }
        }
    }
}
