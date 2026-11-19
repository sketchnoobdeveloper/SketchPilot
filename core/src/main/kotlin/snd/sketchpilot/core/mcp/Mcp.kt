package snd.sketchpilot.core.mcp

import org.json.JSONArray
import org.json.JSONObject
import snd.sketchpilot.core.agent.ToolRegistry
import snd.sketchpilot.core.data.OfflineRepository
import java.security.MessageDigest

object McpProtocol {
    const val LATEST = "2026-07-28"
    val SUPPORTED = setOf(LATEST, "2025-11-25", "2025-06-18", "2025-03-26")
    fun negotiate(requested: String?) = if (requested in SUPPORTED) requested!! else LATEST
}

/** Transport-independent JSON-RPC 2.0 dispatcher. */
class McpDispatcher(
    private val tools: ToolRegistry,
    private val repository: OfflineRepository,
) {
    fun dispatch(request: String): String? {
        val response = try {
            val json = JSONObject(request)
            val method = json.optString("method")
            val id = if (json.has("id")) json.get("id") else JSONObject.NULL
            val params = json.optJSONObject("params") ?: JSONObject()
            handle(method, params, id)
        } catch (t: Throwable) {
            error(JSONObject.NULL, -32700, "parse error")
        }
        return response?.toString()
    }

    private fun handle(method: String, params: JSONObject, id: Any): JSONObject? = when (method) {
        "notifications/initialized" -> null
        "initialize" -> success(id, JSONObject()
            .put("protocolVersion", McpProtocol.negotiate(params.optString("protocolVersion")))
            .put("capabilities", JSONObject().put("tools", JSONObject()).put("resources", JSONObject()).put("prompts", JSONObject()))
            .put("serverInfo", JSONObject().put("name", "sketchpilot").put("version", "0.3.0-kotlin")))
        "server/discover" -> success(id, JSONObject()
            .put("protocolVersion", McpProtocol.LATEST)
            .put("serverInfo", JSONObject().put("name", "sketchpilot")))
        "tools/list" -> {
            val array = JSONArray()
            val schemas = tools.schemas()
            for (i in 0 until schemas.length()) {
                val function = schemas.getJSONObject(i).optJSONObject("function") ?: continue
                array.put(JSONObject()
                    .put("name", function.optString("name"))
                    .put("description", function.optString("description"))
                    .put("inputSchema", function.optJSONObject("parameters")))
            }
            success(id, JSONObject().put("tools", array))
        }
        "tools/call" -> {
            val result = tools.execute(params.optString("name"), params.optJSONObject("arguments") ?: JSONObject())
            val content = JSONArray().put(JSONObject().put("type", "text").put("text", result.toString()))
            success(id, JSONObject().put("content", content).put("isError", result.optBoolean("ok") == false))
        }
        "resources/list" -> {
            val array = JSONArray()
            repository.listProjects().forEach { p ->
                array.put(JSONObject()
                    .put("uri", "sketchware://project/${p.id}/view")
                    .put("name", "${p.id} view"))
            }
            success(id, JSONObject().put("resources", array))
        }
        "prompts/list" -> success(id, JSONObject().put("prompts", JSONArray()
            .put(JSONObject().put("name", "fix_build_error"))
            .put(JSONObject().put("name", "create_screen"))))
        else -> error(id, -32601, "method not found: $method")
    }

    private fun success(id: Any, result: JSONObject) =
        JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result)

    private fun error(id: Any, code: Int, message: String) = JSONObject()
        .put("jsonrpc", "2.0").put("id", id)
        .put("error", JSONObject().put("code", code).put("message", message))

    companion object {
        fun constantTimeEquals(expected: String, actual: String?): Boolean {
            if (actual == null) return false
            return MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())
        }
        fun isLocalOrigin(origin: String?): Boolean {
            if (origin == null) return true
            val value = origin.lowercase()
            return value == "null" || value.startsWith("http://127.0.0.1") || value.startsWith("http://localhost")
        }
    }
}
