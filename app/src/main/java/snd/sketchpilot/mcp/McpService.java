package snd.sketchpilot.mcp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import snd.sketchpilot.project.OfflineRepository;
import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.mcp.protocol.McpDispatcher;
import snd.sketchpilot.mcp.server.McpServer;
import snd.sketchpilot.tools.ToolCatalog;
import snd.sketchpilot.util.Logger;

/**
 * Foreground service that will host the MCP server in P6. P1 only establishes
 * the lifecycle and the non-crashing wrapper.
 */
public final class McpService extends Service {

    private static final String CHANNEL = "sketchpilot.mcp";
    private McpServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL,
                        "SketchPilot MCP", NotificationManager.IMPORTANCE_LOW);
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification = new Notification.Builder(this, CHANNEL)
                        .setContentTitle("SketchPilot MCP")
                        .setContentText("SketchPilot MCP server")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .build();
            } else {
                notification = new Notification.Builder(this)
                        .setContentTitle("SketchPilot MCP")
                        .setContentText("SketchPilot MCP server")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .build();
            }
            startForeground(1001, notification);
            startMcpServer();
        } catch (Throwable t) {
            Logger.hookFailure("McpService.onCreate", t);
        }
    }

    private void startMcpServer() {
        try {
            SketchwarePaths paths = new SketchwarePaths(
                    android.os.Environment.getExternalStorageDirectory());
            SketchwareCrypto crypto = new SketchwareCrypto();
            ProjectEditor editor = new ProjectEditor(paths, crypto);
            McpDispatcher dispatcher = new McpDispatcher(
                    ToolCatalog.build(editor, paths, this), editor.getRepository(), McpConfig.AUDIT_FILE);
            server = new McpServer(McpConfig.DEFAULT_PORT, McpConfig.getOrCreateToken(), dispatcher);
            server.start();
            snd.sketchpilot.core.hook.HookBreadcrumbs.ok("MCP.start");
        } catch (Throwable t) {
            snd.sketchpilot.core.hook.HookBreadcrumbs.fail("MCP.start", t);
            Logger.hookFailure("McpService.startMcpServer", t);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Throwable t) {
            Logger.hookFailure("McpService.onDestroy", t);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
