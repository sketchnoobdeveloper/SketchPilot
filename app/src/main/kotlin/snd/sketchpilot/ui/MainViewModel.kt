package snd.sketchpilot.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import snd.sketchpilot.core.agent.AgentEvent
import snd.sketchpilot.core.agent.AgentLoop
import snd.sketchpilot.core.agent.AgentUi
import snd.sketchpilot.core.agent.ContextEngine
import snd.sketchpilot.core.agent.ToolRegistry
import snd.sketchpilot.core.crypto.SketchwareCrypto
import snd.sketchpilot.core.data.OfflineRepository
import snd.sketchpilot.core.data.Paths
import snd.sketchpilot.core.model.ProjectInfo
import snd.sketchpilot.core.project.ProjectEditor
import snd.sketchpilot.core.tools.registerCoreTools

data class ChatMessage(val role: String, val text: String)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val handler = Handler(Looper.getMainLooper())
    private val repository = OfflineRepository(Paths(java.io.File("/storage/emulated/0")))
    private val editor = ProjectEditor(repository.paths, repository, SketchwareCrypto())

    private val _projects = MutableStateFlow<List<ProjectInfo>>(emptyList())
    val projects: StateFlow<List<ProjectInfo>> = _projects

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    var providerName = "openai"
    var model = "gpt-4o-mini"
    var apiKey = ""
    var baseUrl = ""

    fun refreshProjects() {
        _status.value = "Loading projects..."
        background({
            OfflineRepository().listProjects()
        }, { result ->
            _projects.value = result ?: emptyList()
            _status.value = if (result == null) "Offline repository unavailable" else "${result.size} projects"
        })
    }

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank()) return
        _messages.value = _messages.value + ChatMessage("user", prompt)
        _busy.value = true
        _status.value = "Agent running..."
        background({
            val tools = ToolRegistry().also { registerCoreTools(it, repository, editor) }
            val context = ContextEngine().also { ctx ->
                ctx.setLayer("project", _projects.value.take(5).joinToString { it.id + ":" + it.workspaceName })
            }
            val loop = AgentLoop(tools = tools, context = context, ui = object : AgentUi {
                override fun onEvent(event: AgentEvent) {
                    handler.post {
                        if (event.type == "token") _status.value = "Streaming..."
                        if (event.type == "tool_call") _messages.value = _messages.value + ChatMessage("tool", event.payload)
                    }
                }
            })
            loop.providerName = providerName
            loop.model = model
            loop.apiKey = apiKey
            loop.baseUrl = baseUrl
            loop.run(prompt)
        }, { result ->
            _busy.value = false
            val text = result?.optString("answer", "") ?: result?.optString("error", "") ?: "No response"
            _messages.value = _messages.value + ChatMessage("assistant", text.ifEmpty { "No response" })
            _status.value = "Agent finished"
        })
    }

    private fun <T> background(work: () -> T, done: (T?) -> Unit) {
        Thread({
            try { val value = work(); handler.post { done(value) } }
            catch (t: Throwable) { handler.post { done(null) } }
        }, "SketchPilotVm").start()
    }
}
