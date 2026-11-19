package snd.sketchpilot.core.tools

import org.json.JSONArray
import org.json.JSONObject
import snd.sketchpilot.core.agent.Tool
import snd.sketchpilot.core.agent.ToolRegistry
import snd.sketchpilot.core.data.OfflineRepository
import snd.sketchpilot.core.project.BlockIr
import snd.sketchpilot.core.project.LogicIr
import snd.sketchpilot.core.project.ProjectEditor
import snd.sketchpilot.core.project.ViewIr
import java.io.File

private fun objectSchema(vararg names: String): JSONObject {
    val props = JSONObject()
    names.forEach { props.put(it, JSONObject().put("type", "string")) }
    return JSONObject().put("type", "object").put("properties", props)
}

private fun ok(key: String, value: Any): JSONObject = JSONObject().put("ok", true).put(key, value)
private fun fail(message: String): JSONObject = JSONObject().put("ok", false).put("error", message)

fun registerCoreTools(
    registry: ToolRegistry,
    repository: OfflineRepository,
    editor: ProjectEditor,
) {
    registry.register(object : Tool {
        override val name = "project.list"
        override val description = "List Sketchware Pro projects"
        override val schema = objectSchema()
        override val risk = "read"
        override fun execute(args: JSONObject): JSONObject = runCatching {
            val array = JSONArray()
            repository.listProjects().forEach { array.put(JSONObject()
                .put("id", it.id).put("name", it.workspaceName).put("package", it.packageName)) }
            ok("projects", array)
        }.getOrDefault(fail("failed"))
    })

    registry.register(object : Tool {
        override val name = "view.get_tree"
        override val description = "Get the view tree of a layout"
        override val schema = objectSchema("scId", "layout")
        override val risk = "read"
        override fun execute(args: JSONObject): JSONObject = runCatching {
            val views = repository.views(args.optString("scId"), args.optString("layout").ifEmpty { null })
            val array = JSONArray()
            views.forEach { array.put(it.raw) }
            ok("views", array)
        }.getOrDefault(fail("failed"))
    })

    registry.register(object : Tool {
        override val name = "view.apply_ir"
        override val description = "Apply a typed View IR (backs up first)"
        override val schema = objectSchema("scId", "ir")
        override val risk = "write"
        override fun execute(args: JSONObject): JSONObject = runCatching {
            val json = JSONObject(args.optString("ir", "{}"))
            val ir = ViewIr(json.optString("layout", ""))
            json.optJSONArray("widgets")?.let { widgets ->
                for (i in 0 until widgets.length()) {
                    val w = widgets.optJSONObject(i) ?: continue
                    ir.widget(w.optString("id"), w.optString("type"))
                }
            }
            val result = editor.applyView(args.optString("scId"), ir)
            JSONObject().put("ok", result.ok).put("message", result.message)
        }.getOrDefault(fail("compile failed"))
    })

    registry.register(object : Tool {
        override val name = "logic.get"
        override val description = "Read logic blocks by section"
        override val schema = objectSchema("scId")
        override val risk = "read"
        override fun execute(args: JSONObject): JSONObject = runCatching {
            val sections = JSONObject()
            repository.logic(args.optString("scId")).forEach { (name, blocks) ->
                val array = JSONArray()
                blocks.forEach { array.put(it.raw) }
                sections.put(name, array)
            }
            ok("sections", sections)
        }.getOrDefault(fail("failed"))
    })

    registry.register(object : Tool {
        override val name = "logic.apply_ir"
        override val description = "Apply a typed Logic IR (backs up first)"
        override val schema = objectSchema("scId", "ir")
        override val risk = "write"
        override fun execute(args: JSONObject): JSONObject = runCatching {
            val json = JSONObject(args.optString("ir", "{}"))
            val ir = LogicIr(json.optString("section", ""))
            json.optJSONArray("blocks")?.let { blocks ->
                for (i in 0 until blocks.length()) {
                    val b = blocks.optJSONObject(i) ?: continue
                    val params = b.optJSONArray("parameters")
                    val list = mutableListOf<String>()
                    if (params != null) for (j in 0 until params.length()) list.add(params.optString(j))
                    ir.blocks.add(BlockIr(b.optString("opCode"), list))
                }
            }
            val result = editor.applyLogic(args.optString("scId"), ir)
            JSONObject().put("ok", result.ok).put("message", result.message)
        }.getOrDefault(fail("compile failed"))
    })

    registry.register(object : Tool {
        override val name = "project.backup"
        override val description = "Back up a project"
        override val schema = objectSchema("scId")
        override val risk = "read"
        override fun execute(args: JSONObject): JSONObject = runCatching {
            val scId = args.optString("scId")
            val dest = File(repository.paths.backup(scId), "manual-${System.currentTimeMillis()}")
            dest.mkdirs()
            repository.paths.data(scId).copyRecursively(File(dest, "data"), overwrite = true)
            ok("path", dest.absolutePath)
        }.getOrDefault(fail("backup failed"))
    })

    registry.register(object : Tool {
        override val name = "kb.search"
        override val description = "Search the offline knowledge base"
        override val schema = objectSchema("query")
        override val risk = "read"
        override fun execute(args: JSONObject): JSONObject {
            val query = args.optString("query").lowercase()
            val results = JSONArray()
            if ("opcode" in query || "block" in query) results.put("opCode: addSourceDirectly")
            if ("widget" in query || "view" in query) results.put("widgets: button, textview, edittext, listview")
            if ("error" in query) results.put("fix: check generated Java symbols and enabled libraries")
            return ok("results", results)
        }
    })
}
