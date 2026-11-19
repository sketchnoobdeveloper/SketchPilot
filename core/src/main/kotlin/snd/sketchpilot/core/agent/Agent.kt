package snd.sketchpilot.core.agent

import org.json.JSONArray
import org.json.JSONObject
import snd.sketchpilot.core.provider.LlmProvider
import snd.sketchpilot.core.provider.ProviderCallback
import snd.sketchpilot.core.provider.ProviderRegistry
import snd.sketchpilot.core.provider.ProviderRequest

interface Tool {
    val name: String
    val description: String
    val schema: JSONObject
    val risk: String
    val available: Boolean get() = true
    fun execute(args: JSONObject): JSONObject
}

class ToolRegistry {
    private val tools = linkedMapOf<String, Tool>()

    fun register(tool: Tool) { tools[tool.name] = tool }

    fun schemas(): JSONArray {
        val array = JSONArray()
        tools.values.filter { it.available }.forEach { tool ->
            array.put(
                JSONObject()
                    .put("type", "function")
                    .put("function", JSONObject()
                        .put("name", tool.name)
                        .put("description", tool.description)
                        .put("parameters", tool.schema)
                        .put("risk", tool.risk))
            )
        }
        return array
    }

    fun execute(name: String, args: JSONObject): JSONObject {
        val tool = tools[name] ?: return JSONObject().put("ok", false).put("error", "unknown tool: $name")
        if (!tool.available) return JSONObject().put("ok", false).put("status", "unavailable")
        return try { tool.execute(args) } catch (t: Throwable) { JSONObject().put("ok", false).put("error", t.message) }
    }
}

data class AgentEvent(val type: String, val payload: String)

interface AgentUi {
    fun onEvent(event: AgentEvent) {}
    fun approve(tool: String, risk: String, preview: String): Boolean = true
}

class ContextEngine(
    private val layers: LinkedHashMap<String, String> = linkedMapOf(),
) {
    fun setLayer(name: String, content: String) { layers[name] = content }

    fun systemPrompt(opCodes: Set<String>, widgets: Set<String>): String = buildString {
        appendLine("You are SketchPilot, an AI agent controlling Sketchware Pro projects.")
        appendLine("Use only the provided tools. Never invent block opCodes or widget types.")
        appendLine("Valid opCodes: ${opCodes.joinToString(", ")}")
        appendLine("Valid widgets: ${widgets.joinToString(", ")}")
        layers.forEach { (name, content) -> appendLine("## $name"); appendLine(content.take(4000)) }
    }
}

class AgentLoop(
    private val providerRegistry: ProviderRegistry = ProviderRegistry(),
    private val tools: ToolRegistry,
    private val context: ContextEngine,
    private val ui: AgentUi,
) {
    var providerName = "openai"
    var model = "gpt-4o-mini"
    var apiKey = ""
    var baseUrl = ""
    var maxSteps = 6
    var approvalMode = "auto_safe"

    fun run(userPrompt: String): JSONObject {
        val provider: LlmProvider = providerRegistry.get(providerName)
        val transcript = StringBuilder(userPrompt)
        var fixCycles = 0

        for (step in 0 until maxSteps) {
            val request = ProviderRequest(
                baseUrl = baseUrl.ifEmpty { ProviderRegistry.defaultBase(providerName) },
                apiKey = apiKey,
                model = model,
                systemPrompt = context.systemPrompt(opCodes, widgets),
                userPrompt = transcript.toString(),
                tools = tools.schemas(),
            )
            val tokens = StringBuilder()
            val calls = JSONArray()
            var failure: Throwable? = null
            provider.stream(request, object : ProviderCallback {
                override fun onToken(token: String) { tokens.append(token); ui.onEvent(AgentEvent("token", token)) }
                override fun onToolCall(call: JSONObject) { calls.put(call) }
                override fun onError(error: Throwable) { failure = error }
            })
            if (failure != null) return JSONObject().put("ok", false).put("error", failure.toString())

            if (tokens.isNotEmpty()) transcript.appendLine("ASSISTANT: $tokens")
            if (calls.length() == 0) return JSONObject().put("ok", true).put("answer", tokens.toString())

            for (i in 0 until calls.length()) {
                val call = calls.optJSONObject(i) ?: continue
                val fn = call.optJSONObject("function")
                val name = fn?.optString("name", "") ?: call.optString("name", "")
                val arguments = runCatching { JSONObject(fn?.optString("arguments", "") ?: "{}") }
                    .getOrDefault(fn?.optJSONObject("args") ?: JSONObject())
                ui.onEvent(AgentEvent("tool_call", name))
                val toolRisk = tools.schemas().toString().contains("\"risk\":\"write\"")
                if (approvalMode == "ask_every_time" && toolRisk) {
                    if (!ui.approve(name, "write", arguments.toString())) {
                        transcript.appendLine("TOOL_RESULT $name: rejected")
                        continue
                    }
                }
                val result = tools.execute(name, arguments)
                transcript.appendLine("TOOL_RESULT $name: $result")
                ui.onEvent(AgentEvent("tool_result", result.toString()))
                if (result.optBoolean("ok") == false) {
                    fixCycles++
                    if (fixCycles >= 3) return JSONObject().put("ok", false).put("error", "auto-fix budget exhausted")
                }
            }
        }
        return JSONObject().put("ok", false).put("error", "step budget exhausted")
    }

    companion object {
        private val opCodes = setOf("addSourceDirectly")
        private val widgets = setOf("linear", "button", "textview", "edittext", "imageview", "listview", "cardview", "recyclerview", "fab")
    }
}
