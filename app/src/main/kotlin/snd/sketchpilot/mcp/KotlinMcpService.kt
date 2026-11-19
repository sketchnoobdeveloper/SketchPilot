package snd.sketchpilot.mcp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import snd.sketchpilot.core.agent.ToolRegistry
import snd.sketchpilot.core.crypto.SketchwareCrypto
import snd.sketchpilot.core.data.OfflineRepository
import snd.sketchpilot.core.data.Paths
import snd.sketchpilot.core.mcp.McpDispatcher
import snd.sketchpilot.core.mcp.McpServer
import snd.sketchpilot.core.project.ProjectEditor
import snd.sketchpilot.core.tools.registerCoreTools
import java.io.File

/** Pure-Kotlin foreground service hosting the MCP server. */
class KotlinMcpService : Service() {
    private var server: McpServer? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        try {
            val paths = Paths(File("/storage/emulated/0"))
            val crypto = SketchwareCrypto()
            val repository = OfflineRepository(paths, crypto)
            val editor = ProjectEditor(paths, repository, crypto)
            val tools = ToolRegistry().also { registerCoreTools(it, repository, editor) }
            val tokenFile = File(filesDir, "mcp_token")
            val token = if (tokenFile.isFile) tokenFile.readText().trim() else {
                val generated = "sp_" + java.util.UUID.randomUUID().toString().replace("-", "")
                tokenFile.writeText(generated)
                generated
            }
            server = McpServer(17890, token, McpDispatcher(tools, repository)).also { it.start() }
        } catch (t: Throwable) {
            // never crash the service
        }
    }

    private fun startForegroundNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                    NotificationChannel("mcp", "SketchPilot MCP", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.Notification.Builder(this, "mcp")
            } else {
                android.app.Notification.Builder(this)
            }.setContentTitle("SketchPilot MCP")
                .setContentText("Kotlin MCP server running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
            startForeground(1001, notification)
        } catch (t: Throwable) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
