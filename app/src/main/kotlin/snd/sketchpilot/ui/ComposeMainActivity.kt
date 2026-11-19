package snd.sketchpilot.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import snd.sketchpilot.mcp.McpConfig
import snd.sketchpilot.mcp.McpService
import snd.sketchpilot.mcp.server.McpServer
import java.io.File

private enum class Screen { PROJECTS, AGENT, CAPABILITIES, MCP, TOOLS }
private enum class Lang { EN, BN }

private object Strings {
    fun get(lang: Lang, key: String): String {
        if (lang == Lang.BN) {
            return when (key) {
                "app" -> "স্কেচপাইলট"
                "projects" -> "প্রকল্প"
                "agent" -> "এজেন্ট"
                "caps" -> "ক্ষমতা"
                "mcp" -> "MCP"
                "tools" -> "টুল"
                "diagnostics" -> "ডায়াগনস্টিক্স"
                "run_diagnostics" -> "ডায়াগনস্টিক্স চালান"
                "settings" -> "সেটিংস"
                "language" -> "ভাষা"
                "killswitch" -> "মডিউল বন্ধ করুন"
                else -> key
            }
        }
        return when (key) {
            "app" -> "SketchPilot"
            "projects" -> "Projects"
            "agent" -> "Agent"
            "caps" -> "Capabilities"
            "mcp" -> "MCP"
            "tools" -> "Tools"
            "diagnostics" -> "Diagnostics"
            "run_diagnostics" -> "Run diagnostics"
            "settings" -> "Settings"
            "language" -> "Language"
            "killswitch" -> "Module kill switch"
            else -> key
        }
    }
}

/** Thread + Handler helper: keeps work off the UI thread without adding another concurrency framework. */
private fun <T> runBackground(work: () -> T, done: (T?) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    val runnable = Runnable {
        try {
            val result = work()
            handler.post { done(result) }
        } catch (t: Throwable) {
            handler.post { done(null) }
        }
    }
    Thread(runnable, "SketchPilotUi").start()
}

class ComposeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SketchPilotApp(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SketchPilotApp(activity: ComponentActivity) {
    var screen by remember { mutableStateOf(Screen.PROJECTS) }
    var lang by remember { mutableStateOf(Lang.EN) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(Strings.get(lang, "app") + " — " + Strings.get(lang, screen.name.lowercase())) })
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Screen.PROJECTS to "projects",
                    Screen.AGENT to "agent",
                    Screen.CAPABILITIES to "caps",
                    Screen.MCP to "mcp",
                    Screen.TOOLS to "tools"
                )
                items.forEach { (target, labelKey) ->
                    NavigationBarItem(
                        selected = screen == target,
                        onClick = { screen = target },
                        icon = {},
                        label = { Text(Strings.get(lang, labelKey)) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (screen) {
                Screen.PROJECTS -> ProjectsScreen(lang)
                Screen.AGENT -> AgentScreen(lang)
                Screen.CAPABILITIES -> CapabilitiesScreen(lang)
                Screen.MCP -> McpScreen(lang, activity)
                Screen.TOOLS -> ToolsScreen(lang, activity, onLanguage = { lang = it })
            }
        }
    }
}

@Composable
private fun ProjectsScreen(lang: Lang) {
    var loading by remember { mutableStateOf(true) }
    var rows by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun load() {
        loading = true
        runBackground({
            val repo = snd.sketchpilot.project.OfflineRepository(
                snd.sketchpilot.project.SketchwarePaths(File("/storage/emulated/0")),
                snd.sketchpilot.project.crypto.SketchwareCrypto()
            )
            repo.listProjects().map { it.id + "  •  " + it.workspaceName + "  •  " + it.packageName }
        }, { result ->
            loading = false
            if (result == null) error = "offline repository unavailable"
            else rows = result
        })
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { load() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(onClick = { load() }) { Text("Refresh") }
        Spacer(Modifier.height(8.dp))
        if (loading) CircularProgressIndicator()
    }
    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows) { row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(row, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun AgentScreen(lang: Lang) {
    var baseUrl by remember { mutableStateOf("https://api.openai.com/v1") }
    var model by remember { mutableStateOf("gpt-4o-mini") }
    var apiKey by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    Text("Provider setup", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(model, { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API key") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    Button(onClick = {
        status = "testing..."
        runBackground({
            snd.sketchpilot.provider.OpenAiCompatibleProvider().testConnection(apiKey, model, baseUrl)
        }, { result ->
            status = if (result == null) "test failed" else "ok=" + result.optBoolean("ok") +
                " " + result.optString("preview", "")
        })
    }) { Text("Test connection") }
    Spacer(Modifier.height(8.dp))
    Text(status)
    Spacer(Modifier.height(16.dp))
    Text(
        "The agent loop, approvals, context engine and redaction are implemented in the engine layer. " +
            "Native tool calling and streaming are exercised through the configured provider.",
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun CapabilitiesScreen(lang: Lang) {
    var rows by remember { mutableStateOf(listOf<String>()) }
    fun load() {
        runBackground({
            val file = File("/storage/emulated/0/.sketchware/sketchpilot/capabilities.json")
            if (!file.isFile) return@runBackground listOf("No snapshot yet — open Sketchware Pro with the module enabled.")
            val json = String(file.readBytes(), Charsets.UTF_8)
            val array = org.json.JSONArray(json)
            (0 until array.length()).map { i ->
                val row = array.getJSONObject(i)
                row.optString("label") + " [" + row.optString("mode") + "] " + row.optString("detail")
            }
        }, { result -> if (result != null) rows = result })
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { load() }
    FilledTonalButton(onClick = { load() }) { Text("Refresh") }
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows) { row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(row, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun McpScreen(lang: Lang, activity: ComponentActivity) {
    var running by remember { mutableStateOf(McpServer.RUNNING) }
    var tokenPresent by remember { mutableStateOf(McpConfig.TOKEN_FILE.isFile) }
    Column {
        Text("Streamable HTTP: http://127.0.0.1:17890/mcp")
        Text("Server running: $running")
        Text("Pairing token present: $tokenPresent")
        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = {
                activity.startForegroundService(Intent(activity, McpService::class.java))
                running = true
            }) { Text("Start") }
            Spacer(Modifier.padding(8.dp))
            FilledTonalButton(onClick = {
                activity.stopService(Intent(activity, McpService::class.java))
                running = false
            }) { Text("Stop") }
        }
        Spacer(Modifier.height(12.dp))
        Text("The bearer token is never displayed. Copy it from the app's external files directory.")
    }
}

@Composable
private fun ToolsScreen(lang: Lang, activity: ComponentActivity, onLanguage: (Lang) -> Unit) {
    var diagnosticsPath by remember { mutableStateOf("") }
    var killSwitch by remember { mutableStateOf<Boolean>(snd.sketchpilot.core.hook.KillSwitch.isActive()) }

    Button(onClick = {
        diagnosticsPath = "building..."
        runBackground({
            snd.sketchpilot.diagnostics.DiagnosticsKit.buildZip(activity)?.absolutePath
        }, { path -> diagnosticsPath = path ?: "diagnostics failed" })
    }) { Text(Strings.get(lang, "run_diagnostics")) }
    Spacer(Modifier.height(8.dp))
    Text(diagnosticsPath)

    Spacer(Modifier.height(24.dp))
    Text(Strings.get(lang, "settings"), style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(Strings.get(lang, "language") + ": ")
        Button(onClick = { onLanguage(Lang.EN) }) { Text("EN") }
        Spacer(Modifier.padding(4.dp))
        Button(onClick = { onLanguage(Lang.BN) }) { Text("BN") }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(Strings.get(lang, "killswitch"))
        Spacer(Modifier.padding(8.dp))
        Switch(checked = killSwitch, onCheckedChange = {
            snd.sketchpilot.core.hook.KillSwitch.set(it)
            killSwitch = it
        })
    }
}
