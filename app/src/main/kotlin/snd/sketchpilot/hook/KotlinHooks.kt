package snd.sketchpilot.hook

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import snd.sketchpilot.core.agent.ToolRegistry
import snd.sketchpilot.core.crypto.SketchwareCrypto
import snd.sketchpilot.core.data.OfflineRepository
import snd.sketchpilot.core.data.Paths
import snd.sketchpilot.core.mcp.McpDispatcher
import snd.sketchpilot.core.mcp.McpServer
import snd.sketchpilot.core.project.ProjectEditor
import snd.sketchpilot.core.tools.registerCoreTools
import snd.sketchpilot.log.KotlinLogger
import java.io.File
import java.lang.reflect.Field
import java.security.MessageDigest

object KotlinManagerCapture {
    @Volatile var data: Any? = null
    @Volatile var files: Any? = null
    @Volatile var libraries: Any? = null

    fun capture(logical: String, instance: Any?) {
        if (instance == null) return
        when (logical) {
            "project.data" -> data = instance
            "project.files" -> files = instance
            "project.libraries" -> libraries = instance
        }
    }
}

class KotlinShapeResolver(private val loader: ClassLoader) {
    private val extra = mutableListOf<String>()
    private val hints = mapOf(
        "project.data" to listOf("a.a.a.eC"),
        "project.files" to listOf("a.a.a.hC"),
        "project.libraries" to listOf("a.a.a.iC"),
        "project.crypto" to listOf("a.a.a.oB"),
        "project.paths" to listOf("a.a.a.yq"),
        "build.pipeline" to listOf("a.a.a.ProjectBuilder"),
        "ui.main" to listOf("pro.sketchware.activities.main.activities.MainActivity"),
        "ui.design" to listOf("com.besome.sketch.design.DesignActivity"),
        "ui.logic" to listOf("com.besome.sketch.editor.LogicEditorActivity"),
        "ui.settings" to listOf("com.besome.sketch.projects.MyProjectSettingActivity"),
        "ui.buildlog" to listOf("com.besome.sketch.tools.CompileLogActivity")
    )

    fun resolve(logical: String): Class<*>? {
        val candidates = hints[logical] ?: return null
        for (name in candidates + extra) {
            val cls = try { Class.forName(name, false, loader) } catch (t: Throwable) { null } ?: continue
            if (shapeMatches(logical, cls)) return cls
        }
        return null
    }

    private fun shapeMatches(logical: String, cls: Class<*>): Boolean {
        val required = when (logical) {
            "project.data" -> "java.util.HashMap" to "java.lang.StringBuffer"
            "project.files" -> "java.util.ArrayList" to "java.lang.StringBuffer"
            "project.libraries" -> "com.besome.sketch.beans.ProjectLibraryBean" to "java.lang.StringBuffer"
            "project.crypto" -> "" to "java.lang.String"
            "build.pipeline" -> "" to "compileResources"
            else -> "" to ""
        }
        if (required.first.isNotEmpty() && cls.declaredFields.none { it.type.name.contains(required.first.substringAfterLast('.')) }) return false
        if (required.second.isNotEmpty() && cls.declaredMethods.none { it.toString().contains(required.second) }) return false
        return true
    }
}

class KotlinCapabilityMatrix {
    private val rows = mutableListOf<Triple<String, String, String>>()
    val capabilities get() = rows.toList()

    init {
        listOf(
            "project.data" to "Project data manager", "project.files" to "Project file manager",
            "project.libraries" to "Project library manager", "project.crypto" to "Encrypted file I/O",
            "project.paths" to "Build path generator", "build.pipeline" to "Compile pipeline",
            "ui.main" to "Project list", "ui.design" to "Designer", "ui.logic" to "Logic editor",
            "ui.settings" to "Project settings", "ui.buildlog" to "Build log"
        ).forEach { rows.add(Triple(it.first, it.second, "OFFLINE")) }
    }

    fun evaluate(resolver: KotlinShapeResolver) {
        for (i in rows.indices) {
            val id = rows[i].first
            val cls = resolver.resolve(id)
            val row = rows[i]
            rows[i] = Triple(row.first, row.second, if (cls != null) "LIVE: ${cls.name}" else "OFFLINE: unresolved")
        }
    }

    fun snapshot(): String {
        val array = org.json.JSONArray()
        rows.forEach { array.put(org.json.JSONObject().put("id", it.first).put("label", it.second).put("mode", it.third)) }
        return array.toString()
    }
}

object KotlinLiveBridge {
    private var server: McpServer? = null

    fun start() {
        try {
            val paths = Paths(File("/storage/emulated/0"))
            val repo = OfflineRepository(paths)
            val editor = ProjectEditor(paths, repo)
            val tools = ToolRegistry().also { registerCoreTools(it, repo, editor) }
            val token = "live-bridge"
            server = McpServer(17891, token, McpDispatcher(tools, repo)).also { it.start() }
            KotlinLogger.d("Bridge", "live bridge started on 17891")
            KotlinLogger.breadcrumb("Bridge.start", true)
        } catch (t: Throwable) {
            KotlinLogger.hookFailure("KotlinLiveBridge.start", t)
            KotlinLogger.breadcrumb("Bridge.start", false)
        }
    }
}

object KotlinOverlay {
    fun attach(activity: Activity) {
        Handler(Looper.getMainLooper()).post {
            try {
                val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@post
                val fab = Button(activity).apply { text = "SP"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#3F51B5")) }
                val panel = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor("#F5F5F5"))
                    visibility = View.GONE
                }
                val status = TextView(activity).apply { text = "SketchPilot agent panel" }
                panel.addView(status)
                val button = Button(activity).apply { text = "Projects" }
                button.setOnClickListener {
                    Thread({
                        val repo = OfflineRepository()
                        val text = "Projects: ${repo.listProjects().size}"
                        Handler(Looper.getMainLooper()).post { status.text = text }
                    }, "Panel").start()
                }
                panel.addView(button)
                val fp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, 32, 160) }
                val pp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, 32, 230) }
                content.addView(panel, pp)
                content.addView(fab, fp)
                val last = intArrayOf(0, 0)
                val dragged = booleanArrayOf(false)
                fab.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        when (event?.action) {
                            MotionEvent.ACTION_DOWN -> { last[0] = event.rawX.toInt(); last[1] = event.rawY.toInt(); dragged[0] = false }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = event.rawX.toInt() - last[0]
                                val dy = event.rawY.toInt() - last[1]
                                if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
                                    dragged[0] = true
                                    fp.leftMargin += dx; fp.topMargin -= dy
                                    fab.layoutParams = fp
                                }
                                last[0] = event.rawX.toInt(); last[1] = event.rawY.toInt()
                            }
                            MotionEvent.ACTION_UP -> if (!dragged[0]) panel.visibility = if (panel.visibility == View.GONE) View.VISIBLE else View.GONE
                        }
                        return true
                    }
                })
                KotlinLogger.breadcrumb("Overlay.attach", true)
            } catch (t: Throwable) {
                KotlinLogger.hookFailure("KotlinOverlay.attach", t)
                KotlinLogger.breadcrumb("Overlay.attach", false)
            }
        }
    }
}

class KotlinSketchPilotEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val pkg = lpparam.packageName ?: return
            if (pkg != "pro.sketchware") return
            KotlinLogger.d("Entry", "loading in $pkg")
            val kill = File("/storage/emulated/0/.sketchware/sketchpilot/killswitch")
            if (kill.exists()) { KotlinLogger.w("Entry", "kill switch active"); return }

            val resolver = KotlinShapeResolver(lpparam.classLoader)
            val matrix = KotlinCapabilityMatrix().also { it.evaluate(resolver) }
            File("/storage/emulated/0/.sketchware/sketchpilot").mkdirs()
            File("/storage/emulated/0/.sketchware/sketchpilot/capabilities.json").writeText(matrix.snapshot())

            val captureTargets = listOf("project.data" to "project.data", "project.files" to "project.files", "project.libraries" to "project.libraries")
            captureTargets.forEach { (logical, key) ->
                val cls = resolver.resolve(logical) ?: return@forEach
                try {
                    XposedHelpers.findAndHookConstructor(cls, String::class.java, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
                            try {
                                KotlinManagerCapture.capture(key, param.thisObject)
                                KotlinLogger.breadcrumb("Manager.$key", true)
                            } catch (t: Throwable) { KotlinLogger.hookFailure("Manager.$key", t) }
                        }
                    })
                } catch (t: Throwable) { KotlinLogger.hookFailure("Hook.$key", t) }
            }

            listOf("ui.main", "ui.design", "ui.logic", "ui.settings", "ui.buildlog").forEach { id ->
                val cls = resolver.resolve(id) ?: return@forEach
                try {
                    XposedHelpers.findAndHookMethod(cls, "onCreate", Bundle::class.java, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
                            try {
                                if (param.thisObject is Activity) KotlinOverlay.attach(param.thisObject as Activity)
                            } catch (t: Throwable) { KotlinLogger.hookFailure("Overlay.$id", t) }
                        }
                    })
                } catch (t: Throwable) { KotlinLogger.hookFailure("Activity.$id", t) }
            }

            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam) {
                    KotlinLogger.breadcrumb("Application.onCreate", true)
                }
            })

            KotlinLiveBridge.start()
        } catch (t: Throwable) {
            KotlinLogger.hookFailure("KotlinSketchPilotEntry", t)
        }
    }
}
