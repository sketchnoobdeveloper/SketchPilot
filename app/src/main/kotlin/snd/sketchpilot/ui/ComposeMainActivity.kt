package snd.sketchpilot.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.io.File

private object Routes {
    const val PROJECTS = "projects"
    const val AGENT = "agent"
    const val CAPABILITIES = "capabilities"
    const val MCP = "mcp"
    const val SETTINGS = "settings"
}

private object T {
    fun en(key: String) = when (key) {
        "app" -> "SketchPilot"; "projects" -> "Projects"; "agent" -> "Agent"
        "caps" -> "Capabilities"; "mcp" -> "MCP"; "settings" -> "Settings"
        "refresh" -> "Refresh"; "prompt" -> "Ask the agent..."; "send" -> "Send"
        "diagnostics" -> "Run diagnostics"; "provider" -> "Provider"
        else -> key
    }
    fun bn(key: String) = when (key) {
        "app" -> "স্কেচপাইলট"; "projects" -> "প্রকল্প"; "agent" -> "এজেন্ট"
        "caps" -> "ক্ষমতা"; "mcp" -> "MCP"; "settings" -> "সেটিংস"
        "refresh" -> "রিফ্রেশ"; "prompt" -> "এজেন্টকে জিজ্ঞাসা করুন..."; "send" -> "পাঠান"
        "diagnostics" -> "ডায়াগনস্টিক্স"; "provider" -> "প্রদানকারী"
        else -> key
    }
}

class ComposeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val context = LocalContext.current
            val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context)
            else darkColorScheme()
            MaterialTheme(colorScheme = scheme) {
                Surface(Modifier.fillMaxSize()) { SketchPilotApp() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SketchPilotApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    var language by remember { mutableStateOf("en") }
    val tr = { key: String -> if (language == "bn") T.bn(key) else T.en(key) }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Routes.PROJECTS

    Scaffold(
        topBar = { TopAppBar(title = { Text(tr("app") + " — " + tr(currentTitleKey(current))) }) },
        bottomBar = {
            BottomAppBar {
                val items = listOf(
                    Routes.PROJECTS to "projects", Routes.AGENT to "agent",
                    Routes.CAPABILITIES to "caps", Routes.MCP to "mcp", Routes.SETTINGS to "settings"
                )
                items.forEach { (route, key) ->
                    NavigationBarItem(
                        selected = current == route,
                        onClick = { nav.navigate(route) },
                        icon = {},
                        label = { Text(tr(key)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Routes.PROJECTS, modifier = Modifier.padding(padding)) {
            composable(Routes.PROJECTS) { ProjectsScreen(vm, tr) }
            composable(Routes.AGENT) { AgentScreen(vm, tr) }
            composable(Routes.CAPABILITIES) { CapabilitiesScreen(tr) }
            composable(Routes.MCP) { McpScreen(tr) }
            composable(Routes.SETTINGS) { SettingsScreen(tr, language, { language = it }) }
        }
    }
}

private fun currentTitleKey(route: String) = when (route) {
    Routes.PROJECTS -> "projects"; Routes.AGENT -> "agent"; Routes.CAPABILITIES -> "caps"
    Routes.MCP -> "mcp"; else -> "settings"
}

@Composable
private fun ProjectsScreen(vm: MainViewModel, tr: (String) -> String) {
    val projects by vm.projects.collectAsState()
    val status by vm.status.collectAsState()
    LaunchedEffect(Unit) { vm.refreshProjects() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        FilledTonalButton(onClick = { vm.refreshProjects() }) { Text(tr("refresh")) }
        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(projects) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(p.workspaceName.ifEmpty { p.id }, style = MaterialTheme.typography.titleMedium)
                        Text("${p.id} • ${p.packageName}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentScreen(vm: MainViewModel, tr: (String) -> String) {
    val messages by vm.messages.collectAsState()
    val busy by vm.busy.collectAsState()
    var input by remember { mutableStateOf("") }
    var base by remember { mutableStateOf(vm.baseUrl) }
    var model by remember { mutableStateOf(vm.model) }
    var key by remember { mutableStateOf(vm.apiKey) }
    var showProvider by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        FilledTonalButton(onClick = { showProvider = !showProvider }) { Text(tr("provider")) }
        if (showProvider) {
            OutlinedTextField(base, { base = it; vm.baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(model, { model = it; vm.model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(key, { key = it; vm.apiKey = it }, label = { Text("API key") }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                Card(Modifier.fillMaxWidth()) {
                    Text("${msg.role}: ${msg.text}", Modifier.padding(12.dp))
                }
            }
        }
        OutlinedTextField(input, { input = it }, label = { Text(tr("prompt")) }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { vm.sendPrompt(input); input = "" },
            enabled = !busy && input.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { if (busy) CircularProgressIndicator() else Text(tr("send")) }
    }
}

@Composable
private fun CapabilitiesScreen(tr: (String) -> String) {
    var rows by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) {
        rows = runCatching {
            val file = File("/storage/emulated/0/.sketchware/sketchpilot/capabilities.json")
            if (!file.isFile) listOf("No snapshot yet — open Sketchware Pro with the module enabled.")
            else {
                val array = org.json.JSONArray(file.readText())
                (0 until array.length()).map { i ->
                    val row = array.getJSONObject(i)
                    "${row.optString("label")} [${row.optString("mode")}] ${row.optString("detail")}"
                }
            }
        }.getOrDefault(listOf("Snapshot unreadable"))
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows) { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) } }
    }
}

@Composable
private fun McpScreen(tr: (String) -> String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Streamable HTTP", style = MaterialTheme.typography.titleMedium)
        Text("http://127.0.0.1:17890/mcp")
        Text("Bearer token: stored in app files, never displayed")
        Spacer(Modifier.height(16.dp))
        Text("Register in OpenCode with mcp.servers + protocol auto.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsScreen(tr: (String) -> String, language: String, onLanguage: (String) -> Unit) {
    var diagnostics by remember { mutableStateOf("") }
    var kill by remember { mutableStateOf(File("/storage/emulated/0/.sketchware/sketchpilot/killswitch").exists()) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(tr("settings"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("English / বাংলা")
            Button(onClick = { onLanguage("en") }) { Text("EN") }
            Button(onClick = { onLanguage("bn") }) { Text("BN") }
        }
        Spacer(Modifier.height(12.dp))
        val activity = LocalContext.current as ComponentActivity
        Button(onClick = {
            diagnostics = "building..."
            Thread({
                val path = runCatching {
                    snd.sketchpilot.diagnostics.DiagnosticsKit.buildZip(activity.application)?.absolutePath
                }.getOrNull()
                activity.runOnUiThread { diagnostics = path ?: "failed" }
            }, "Diagnostics").start()
        }) { Text(tr("diagnostics")) }
        Text(diagnostics)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Module kill switch")
            Switch(checked = kill, onCheckedChange = {
                val file = File("/storage/emulated/0/.sketchware/sketchpilot/killswitch")
                if (it) { file.parentFile?.mkdirs(); file.createNewFile() } else file.delete()
                kill = it
            })
        }
    }
}
