package snd.sketchpilot.core.mcp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

/** Streamable HTTP MCP server bound to loopback only. Thread-only, no heavy dependencies. */
class McpServer(
    private val port: Int,
    private val token: String,
    private val dispatcher: McpDispatcher,
) {
    private var server: ServerSocket? = null
    @Volatile var running = false
        private set

    fun start() {
        if (running) return
        try {
            server = ServerSocket(port, 10, InetAddress.getByName("127.0.0.1"))
            running = true
            Thread({ acceptLoop() }, "SketchPilotMcpKt").apply { isDaemon = true }.start()
        } catch (t: Throwable) {
            running = false
        }
    }

    fun stop() {
        running = false
        try { server?.close() } catch (t: Throwable) { }
    }

    private fun acceptLoop() {
        while (running) {
            try {
                val client = server?.accept() ?: break
                Thread({ serve(client) }, "SketchPilotMcpWorker").apply { isDaemon = true }.start()
            } catch (t: Throwable) {
                if (!running) break
            }
        }
    }

    private fun serve(client: Socket) {
        val reader: BufferedReader
        try {
            client.soTimeout = 10000
            reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val headers = HashMap<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                val colon = line!!.indexOf(':')
                if (colon > 0) headers[line!!.substring(0, colon).lowercase()] = line!!.substring(colon + 1).trim()
            }
            if (!McpDispatcher.isLocalOrigin(headers["origin"])) {
                write(client, 403, "{\"error\":\"non-local origin\"}"); return
            }
            val length = headers["content-length"]?.toIntOrNull() ?: 0
            if (length > 1_000_000) {
                write(client, 413, "{\"error\":\"body too large\"}"); return
            }
            val body = CharArray(length.coerceAtLeast(0))
            var offset = 0
            while (offset < body.size) {
                val read = reader.read(body, offset, body.size - offset)
                if (read < 0) break
                offset += read
            }
            if (!McpDispatcher.constantTimeEquals("Bearer $token", headers["authorization"])) {
                write(client, 401, "{\"error\":\"unauthorized\"}"); return
            }
            val parts = requestLine.split(" ")
            if (parts.size > 1 && parts[0] == "POST" && parts[1] == "/mcp") {
                val response = dispatcher.dispatch(String(body, 0, offset))
                if (response == null) write(client, 202, "") else write(client, 200, response)
            } else if (parts.size > 1 && parts[0] == "GET" && parts[1] == "/health") {
                write(client, 200, "{\"status\":\"ok\"}")
            } else {
                write(client, 404, "{\"error\":\"not found\"}")
            }
        } catch (t: Throwable) {
            try { write(client, 500, "{\"error\":\"internal\"}") } catch (ignored: Throwable) { }
        } finally {
            try { client.close() } catch (ignored: Throwable) { }
        }
    }

    private fun write(client: Socket, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val reason = when (status) { 200 -> "OK"; 202 -> "Accepted"; 401 -> "Unauthorized"; 403 -> "Forbidden"; 413 -> "Payload Too Large"; 404 -> "Not Found"; else -> "Error" }
        val header = "HTTP/1.1 $status $reason\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        client.getOutputStream().write(header.toByteArray(StandardCharsets.UTF_8))
        client.getOutputStream().write(bytes)
        client.getOutputStream().flush()
    }
}
