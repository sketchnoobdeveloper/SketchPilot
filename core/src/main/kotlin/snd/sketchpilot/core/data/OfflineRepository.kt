package snd.sketchpilot.core.data

import org.json.JSONArray
import org.json.JSONObject
import snd.sketchpilot.core.crypto.SketchwareCrypto
import snd.sketchpilot.core.io.SectionedFile
import snd.sketchpilot.core.model.BlockNode
import snd.sketchpilot.core.model.ProjectInfo
import snd.sketchpilot.core.model.ViewNode
import java.io.File

class Paths(private val externalRoot: File) {
    fun root(): File = File(externalRoot, ".sketchware")
    fun data(scId: String): File = File(root(), "data/$scId")
    fun metadata(scId: String): File = File(root(), "mysc/list/$scId/project")
    fun dataFile(scId: String, name: String): File = File(data(scId), name)
    fun list(): File = File(root(), "mysc/list")
    fun backup(scId: String): File = File(root(), "bak/$scId")
}

/** Pure-Kotlin offline repository. Read-only in this first rewrite stage. */
class OfflineRepository(
    val paths: Paths = Paths(File("/storage/emulated/0")),
    private val crypto: SketchwareCrypto = SketchwareCrypto(),
) {
    fun listProjects(): List<ProjectInfo> = (paths.list().listFiles() ?: emptyArray())
        .filter { it.isDirectory }
        .mapNotNull { dir ->
            runCatching {
                val info = ProjectInfo.fromJson(readText(paths.metadata(dir.name)))
                if (info.id == dir.name) info else null
            }.getOrNull()
        }
        .sortedBy { it.id }

    fun views(scId: String, layout: String? = null): List<ViewNode> {
        val result = mutableListOf<ViewNode>()
        sections(scId, "view").all().forEach { section ->
            if (!section.name.endsWith(".xml")) return@forEach
            if (layout != null && section.name != layout) return@forEach
            section.lines.forEach { line ->
                runCatching { result.add(ViewNode(JSONObject(line))) }
            }
        }
        return result
    }

    fun logic(scId: String): Map<String, List<BlockNode>> {
        val result = linkedMapOf<String, List<BlockNode>>()
        sections(scId, "logic").all().forEach { section ->
            val blocks = section.lines.mapNotNull { line ->
                runCatching {
                    val o = JSONObject(line)
                    if (o.has("opCode")) BlockNode(o) else null
                }.getOrNull()
            }
            result[section.name] = blocks
        }
        return result
    }

    fun sections(scId: String, name: String): SectionedFile =
        SectionedFile.parse(readText(paths.dataFile(scId, name)))

    fun rootLayouts(scId: String): JSONObject =
        runCatching { JSONObject(readText(paths.dataFile(scId, "view_root"))) }.getOrDefault(JSONObject())

    fun projectConfig(scId: String): JSONObject =
        runCatching { JSONObject(readText(paths.dataFile(scId, "project_config"))) }.getOrDefault(JSONObject())

    fun libraryNames(scId: String): List<String> = sections(scId, "library").all().map { it.name }

    fun resourceSections(scId: String): JSONArray {
        val out = JSONArray()
        sections(scId, "resource").all().forEach { section ->
            out.put(JSONObject().put("name", section.name).put("items", JSONArray(section.lines)))
        }
        return out
    }

    private fun readText(file: File): String {
        if (!file.isFile) return ""
        val bytes = SketchwareCrypto.readAll(file)
        if (bytes.isEmpty()) return ""
        return if (crypto.looksEncrypted(bytes)) crypto.decrypt(bytes) else String(bytes, Charsets.UTF_8)
    }
}
