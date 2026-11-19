package snd.sketchpilot.core.provider

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ProviderRequest(
    var baseUrl: String = "",
    var apiKey: String = "",
    var model: String = "",
    var systemPrompt: String = "",
    var userPrompt: String = "",
    var tools: JSONArray = JSONArray(),
    var maxTokens: Int = 2048,
    var stream: Boolean = true,
)

interface ProviderCallback {
    fun onToken(token: String) {}
    fun onToolCall(call: JSONObject) {}
    fun onError(error: Throwable) {}
    fun onDone() {}
}

interface LlmProvider {
    val name: String
    fun stream(request: ProviderRequest, callback: ProviderCallback)
    fun testConnection(apiKey: String, model: String, baseUrl: String): JSONObject
}

data class SseEvent(val event: String, val data: String, val id: String) {
    val isDone: Boolean get() = data.trim() == "[DONE]"
}

/** Dependency-free SSE framer. */
class SseParser {
    private var event = ""
    private var id = ""
    private val data = mutableListOf<String>()

    fun feed(line: String?): SseEvent? {
        val clean = line?.trimEnd('\r') ?: ""
        if (clean.isEmpty()) {
            if (data.isEmpty() && event.isEmpty() && id.isEmpty()) return null
            val result = SseEvent(event, data.joinToString("\n"), id)
            reset()
            return result
        }
        if (clean.startsWith(":")) return null
        val colon = clean.indexOf(':')
        val field = if (colon < 0) clean else clean.substring(0, colon)
        var value = if (colon < 0) "" else clean.substring(colon + 1)
        if (value.startsWith(" ")) value = value.substring(1)
        when (field) {
            "event" -> event = value
            "id" -> id = value
            "data" -> data.add(value)
        }
        return null
    }

    private fun reset() { event = ""; id = ""; data.clear() }
}

/** OpenAI-compatible provider; covers OpenAI, OpenRouter and Ollama /v1 via baseUrl. */
open class OpenAiCompatibleProvider(override val name: String = "openai") : LlmProvider {
    protected open fun endpoint(request: ProviderRequest): String {
        val base = request.baseUrl.ifEmpty { "https://api.openai.com/v1" }.trimEnd('/')
        return "$base/chat/completions"
    }

    protected open fun headers(connection: HttpURLConnection, request: ProviderRequest) {
        if (request.apiKey.isNotEmpty()) connection.setRequestProperty("Authorization", "Bearer ${request.apiKey}")
    }

    protected open fun body(request: ProviderRequest): JSONObject {
        val messages = org.json.JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", request.systemPrompt))
        messages.put(JSONObject().put("role", "user").put("content", request.userPrompt))
        val json = JSONObject()
            .put("model", request.model)
            .put("messages", messages)
            .put("stream", request.stream)
            .put("max_tokens", request.maxTokens)
        if (request.tools.length() > 0) json.put("tools", request.tools).put("tool_choice", "auto")
        return json
    }

    protected open fun parseChunk(root: JSONObject, callback: ProviderCallback, finished: BooleanArray) {
        val choices = root.optJSONArray("choices") ?: return
        if (choices.length() == 0) return
        val choice = choices.optJSONObject(0) ?: return
        val delta = choice.optJSONObject("delta") ?: return
        val token = delta.optString("content", "")
        if (token.isNotEmpty() && token != "null") callback.onToken(token)
        delta.optJSONArray("tool_calls")?.let { calls ->
            for (i in 0 until calls.length()) calls.optJSONObject(i)?.let { callback.onToolCall(it) }
        }
        val reason = choice.optString("finish_reason", "")
        if (reason == "stop" || reason == "tool_calls") finished[0] = true
    }

    override fun stream(request: ProviderRequest, callback: ProviderCallback) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(endpoint(request)).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 90000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "text/event-stream")
            headers(connection, request)
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body(request).toString())
                writer.flush()
            }
            if (connection.responseCode >= 400) {
                callback.onError(IllegalStateException("HTTP ${connection.responseCode}"))
                return
            }
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                val parser = SseParser()
                val finished = booleanArrayOf(false)
                while (!finished[0]) {
                    val line = reader.readLine() ?: break
                    parser.feed(line)?.let { event ->
                        if (event.isDone) finished[0] = true
                        else if (event.data.isNotEmpty()) runCatching { parseChunk(JSONObject(event.data), callback, finished) }
                            .onFailure { callback.onError(it) }
                    }
                }
            }
            callback.onDone()
        } catch (t: Throwable) {
            callback.onError(t)
        } finally {
            connection?.disconnect()
        }
    }

    override fun testConnection(apiKey: String, model: String, baseUrl: String): JSONObject {
        val request = ProviderRequest(apiKey = apiKey, model = model, baseUrl = baseUrl, systemPrompt = "Reply OK.", userPrompt = "ping", stream = false)
        val answer = StringBuilder()
        var failure: Throwable? = null
        stream(request, object : ProviderCallback {
            override fun onToken(token: String) { answer.append(token) }
            override fun onError(error: Throwable) { failure = error }
        })
        return if (failure != null) JSONObject().put("ok", false).put("error", failure.toString())
        else JSONObject().put("ok", true).put("preview", answer.toString().take(200))
    }
}

class AnthropicProvider : OpenAiCompatibleProvider("anthropic") {
    override fun endpoint(request: ProviderRequest) =
        request.baseUrl.ifEmpty { "https://api.anthropic.com" }.trimEnd('/') + "/v1/messages"

    override fun headers(connection: HttpURLConnection, request: ProviderRequest) {
        if (request.apiKey.isNotEmpty()) connection.setRequestProperty("x-api-key", request.apiKey)
        connection.setRequestProperty("anthropic-version", "2023-06-01")
    }

    override fun body(request: ProviderRequest): JSONObject {
        val messages = org.json.JSONArray().put(JSONObject().put("role", "user").put("content", request.userPrompt))
        return JSONObject()
            .put("model", request.model)
            .put("max_tokens", request.maxTokens)
            .put("system", request.systemPrompt)
            .put("messages", messages)
            .put("stream", request.stream)
    }

    override fun parseChunk(root: JSONObject, callback: ProviderCallback, finished: BooleanArray) {
        when (root.optString("type")) {
            "message_stop" -> finished[0] = true
            "content_block_delta" -> {
                val text = root.optJSONObject("delta")?.optString("text", "") ?: ""
                if (text.isNotEmpty()) callback.onToken(text)
            }
        }
    }
}

class OllamaProvider : OpenAiCompatibleProvider("ollama") {
    override fun endpoint(request: ProviderRequest) =
        request.baseUrl.ifEmpty { "http://127.0.0.1:11434/v1" }.trimEnd('/') + "/chat/completions"
}

class ProviderRegistry {
    private val providers = mutableMapOf<String, LlmProvider>(
        "openai" to OpenAiCompatibleProvider(),
        "openrouter" to OpenAiCompatibleProvider("openrouter"),
        "anthropic" to AnthropicProvider(),
        "ollama" to OllamaProvider(),
    )

    fun register(provider: LlmProvider) { providers[provider.name] = provider }

    fun get(name: String): LlmProvider = providers[name] ?: providers.getValue("openai")
    fun names(): Set<String> = providers.keys

    companion object {
        fun defaultBase(name: String) = when (name) {
            "openrouter" -> "https://openrouter.ai/api/v1"
            "anthropic" -> "https://api.anthropic.com"
            "ollama" -> "http://127.0.0.1:11434/v1"
            else -> "https://api.openai.com/v1"
        }
    }
}
