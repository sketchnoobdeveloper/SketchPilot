package snd.sketchpilot.ui
import androidx.compose.foundation.background

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
            SketchPilotTheme {
                Surface(Modifier.fillMaxSize()) { SketchPilotApp() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun SketchPilotApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    var language by rememberSaveable { mutableStateOf("en") }
    val tr = { key: String -> if (language == "bn") T.bn(key) else T.en(key) }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Routes.PROJECTS

    val activity = LocalContext.current as android.app.Activity
    val compact = calculateWindowSizeClass(activity).widthSizeClass == WindowWidthSizeClass.Compact
    val navigate = { route: String ->
        nav.navigate(route) { launchSingleTop = true }
    }
    val topBar: @Composable () -> Unit = {
        TopAppBar(title = { Text(tr("app") + " — " + tr(currentTitleKey(current))) })
    }

    if (compact) {
        Scaffold(topBar = topBar, bottomBar = {
            NavigationBar { NavBarItems(current, navigate, tr) }
        }) { padding -> AppNav(nav, vm, tr, language, padding, { language = it }) }
    } else {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(header = {
                Text(tr("app"), Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }) { NavRailItems(current, navigate, tr) }
            Scaffold(topBar = topBar) { padding -> AppNav(nav, vm, tr, language, padding, { language = it }) }
        }
    }
}

private fun navItemList() = listOf(
    Triple(Routes.PROJECTS, "projects", Icons.Filled.Home),
    Triple(Routes.AGENT, "agent", Icons.Filled.Build),
    Triple(Routes.CAPABILITIES, "caps", Icons.Filled.List),
    Triple(Routes.MCP, "mcp", Icons.Filled.Info),
    Triple(Routes.SETTINGS, "settings", Icons.Filled.Settings)
)

@Composable
private fun RowScope.NavBarItems(current: String, navigate: (String) -> Unit, tr: (String) -> String) {
    navItemList().forEach { (route, key, icon) ->
        NavigationBarItem(
            selected = current == route,
            onClick = { navigate(route) },
            icon = { Icon(icon, tr(key)) },
            label = { Text(tr(key)) },
        )
    }
}

@Composable
private fun ColumnScope.NavRailItems(current: String, navigate: (String) -> Unit, tr: (String) -> String) {
    navItemList().forEach { (route, key, icon) ->
        NavigationRailItem(
            selected = current == route,
            onClick = { navigate(route) },
            icon = { Icon(icon, tr(key)) },
            label = { Text(tr(key)) },
        )
    }
}

@Composable
private fun AppNav(
    nav: NavHostController,
    vm: MainViewModel,
    tr: (String) -> String,
    language: String,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onLanguage: (String) -> Unit,
) {
    NavHost(
        nav,
        startDestination = Routes.PROJECTS,
        modifier = Modifier.padding(padding),
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(160)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(160)) },
    ) {
        composable(Routes.PROJECTS) { ProjectsScreen(vm, tr) }
        composable(Routes.AGENT) { AgentScreen(vm, tr) }
        composable(Routes.CAPABILITIES) { CapabilitiesScreen(tr) }
        composable(Routes.MCP) { McpScreen(tr) }
        composable(Routes.SETTINGS) { SettingsScreen(tr, language, onLanguage) }
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
        val isError = status.contains("unavailable", true) || status.contains("failed", true)
        Text(status, style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (projects.isEmpty() && !isError) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("No projects found", style = MaterialTheme.typography.titleMedium)
                    Text("Give Sketchware storage access, then refresh.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { vm.refreshProjects() }) { Text(tr("refresh")) }
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(projects, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                (p.workspaceName.ifEmpty { p.id }).take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.workspaceName.ifEmpty { p.id }, style = MaterialTheme.typography.titleMedium)
                            Text(p.packageName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("ID ${p.id}", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
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
    var base by rememberSaveable { mutableStateOf(vm.baseUrl) }
    var model by rememberSaveable { mutableStateOf(vm.model) }
    var key by remember { mutableStateOf(vm.apiKey) }
    var showProvider by rememberSaveable { mutableStateOf(false) }
    var showKey by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        FilledTonalButton(onClick = { showProvider = !showProvider }) { Text(tr("provider")) }
        if (showProvider) {
            OutlinedTextField(base, { base = it; vm.baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(model, { model = it; vm.model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                key, { key = it; vm.apiKey = it },
                label = { Text("API key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") }
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        if (messages.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ask your first question", style = MaterialTheme.typography.titleMedium)
                    Text("SketchPilot can inspect projects, edit IR and explain logic.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    fun reload() {
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
    FilledTonalButton(onClick = { reload() }) { Text(tr("refresh")) }
    Spacer(Modifier.height(12.dp))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows) { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) } }
    }
}

@Composable
private fun McpScreen(tr: (String) -> String) {
    val context = LocalContext.current
    var running by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Streamable HTTP", style = MaterialTheme.typography.titleMedium)
        Text("http://127.0.0.1:17890/mcp")
        Text("Bearer token: stored in app files, never displayed")
        Text("Status: ${if (running) "running" else "stopped"}")
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            context.startForegroundService(Intent(context, snd.sketchpilot.mcp.KotlinMcpService::class.java))
            running = true
        }) { Text("Start MCP") }
        Button(onClick = {
            context.stopService(Intent(context, snd.sketchpilot.mcp.KotlinMcpService::class.java))
            running = false
        }) { Text("Stop MCP") }
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
            var confirm by remember { mutableStateOf(false) }
            Switch(checked = kill, onCheckedChange = { confirm = it })
            if (confirm) {
                AlertDialog(
                    onDismissRequest = { confirm = false },
                    title = { Text("Disable module?") },
                    text = { Text("This stops all Sketchware Pro hooks until re-enabled. Reopen Sketchware Pro afterwards.") },
                    confirmButton = {
                        TextButton(onClick = {
                            val file = File("/storage/emulated/0/.sketchware/sketchpilot/killswitch")
                            file.parentFile?.mkdirs(); file.createNewFile(); kill = true; confirm = false
                        }) { Text("Disable") }
                    },
                    dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
                )
            }
        }
    }
}
