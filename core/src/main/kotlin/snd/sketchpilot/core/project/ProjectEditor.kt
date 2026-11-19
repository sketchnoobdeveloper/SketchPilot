package snd.sketchpilot.core.project

import org.json.JSONObject
import snd.sketchpilot.core.crypto.SketchwareCrypto
import snd.sketchpilot.core.data.OfflineRepository
import snd.sketchpilot.core.data.Paths
import snd.sketchpilot.core.io.SectionedFile
import java.io.File

data class WidgetIr(var id: String = "", var typeName: String = "", var typeCode: Int = -1)
data class ViewIr(val layout: String, val widgets: MutableList<WidgetIr> = mutableListOf()) {
    fun widget(id: String, type: String) { widgets.add(WidgetIr(id, type)) }
}

data class BlockIr(var opCode: String = "", var parameters: MutableList<String> = mutableListOf())
data class LogicIr(val section: String, val blocks: MutableList<BlockIr> = mutableListOf()) {
    fun block(opCode: String, vararg params: String) { blocks.add(BlockIr(opCode, params.toMutableList())) }
}

data class TxResult(val ok: Boolean, val message: String)

private data class Snapshot(val before: Map<String, ByteArray?>, val after: Map<String, ByteArray?>)

/** Validates typed IR and commits it as a backed-up transaction with undo/redo. */
class ProjectEditor(
    val paths: Paths,
    val repository: OfflineRepository,
    private val crypto: SketchwareCrypto = SketchwareCrypto(),
) {
    private val undo = mutableMapOf<String, MutableList<Snapshot>>()
    private val redo = mutableMapOf<String, MutableList<Snapshot>>()

    private val widgetTypes = setOf(
        "linear", "relativelayout", "button", "textview", "edittext", "imageview",
        "listview", "checkbox", "switch", "cardview", "recyclerview", "fab"
    )
    private val opCodes = setOf("addSourceDirectly")

    fun applyView(scId: String, ir: ViewIr): TxResult {
        if (!ir.layout.endsWith(".xml")) return TxResult(false, "layout must end with .xml")
        val ids = ir.widgets.map { it.id }
        if (ids.any { it.isEmpty() }) return TxResult(false, "empty widget id")
        if (ids.toSet().size != ids.size) return TxResult(false, "duplicate widget id")
        ir.widgets.forEach { if (it.typeCode < 0 && it.typeName !in widgetTypes) return TxResult(false, "unknown widget ${it.typeName}") }

        return try {
            val current = repository.sections(scId, "view")
            val replacement = SectionedFile.Section(ir.layout)
            ir.widgets.forEach { w ->
                val line = JSONObject()
                    .put("id", w.id)
                    .put("type", if (w.typeCode >= 0) w.typeCode else WIDGET_INDEX[w.typeName] ?: -1)
                    .put("index", replacement.lines.size)
                replacement.lines.add(line.toString())
            }
            var replaced = false
            val newSections = SectionedFile()
            current.all().forEach { section ->
                if (section.name == ir.layout) {
                    newSections.all().add(replacement)
                    replaced = true
                } else {
                    newSections.all().add(section)
                }
            }
            if (!replaced) newSections.all().add(replacement)
            val file = paths.dataFile(scId, "view")
            commit(scId, "view:${ir.layout}", mapOf(file to crypto.encrypt(newSections.serialize())))
        } catch (t: Throwable) {
            TxResult(false, "view compile failed: ${t.message}")
        }
    }

    fun applyLogic(scId: String, ir: LogicIr): TxResult {
        if (!ir.section.contains(".java_")) return TxResult(false, "bad logic section name")
        ir.blocks.forEach { if (it.opCode !in opCodes) return TxResult(false, "unknown opCode ${it.opCode}") }

        return try {
            val current = repository.sections(scId, "logic")
            val section = SectionedFile.Section(ir.section)
            ir.blocks.forEachIndexed { index, block ->
                section.lines.add(
                    JSONObject()
                        .put("id", (index + 1).toString())
                        .put("nextBlock", if (index + 1 < ir.blocks.size) index + 2 else -1)
                        .put("opCode", block.opCode)
                        .put("parameters", org.json.JSONArray(block.parameters))
                        .put("spec", "add source directly %s.inputOnly")
                        .put("subStack1", -1)
                        .put("subStack2", -1)
                        .toString()
                )
            }
            var replaced = false
            val newSections = SectionedFile()
            current.all().forEach { s ->
                if (s.name == ir.section) { newSections.all().add(section); replaced = true }
                else newSections.all().add(s)
            }
            if (!replaced) newSections.all().add(section)
            val file = paths.dataFile(scId, "logic")
            commit(scId, "logic:${ir.section}", mapOf(file to crypto.encrypt(newSections.serialize())))
        } catch (t: Throwable) {
            TxResult(false, "logic compile failed: ${t.message}")
        }
    }

    private fun commit(scId: String, label: String, changes: Map<File, ByteArray>): TxResult {
        val before = linkedMapOf<String, ByteArray?>()
        changes.keys.forEach { before[it.absolutePath] = if (it.isFile) it.readBytes() else null }
        val backupDir = File(paths.backup(scId), "${System.currentTimeMillis()}_$label")
        runCatching {
            changes.keys.forEach { src ->
                val dest = File(backupDir, src.name)
                dest.parentFile?.mkdirs()
                if (src.isFile) src.copyTo(dest, overwrite = true)
            }
        }.onFailure { return TxResult(false, "backup failed: ${it.message}") }

        val after = linkedMapOf<String, ByteArray?>()
        changes.forEach { (file, bytes) ->
            if (runCatching { file.parentFile?.mkdirs(); file.writeBytes(bytes) }.isFailure) {
                before.forEach { (path, data) ->
                    val f = File(path)
                    if (data == null) f.delete() else f.writeBytes(data)
                }
                return TxResult(false, "write failed")
            }
            after[file.absolutePath] = bytes
        }
        undo.getOrPut(scId) { mutableListOf() }.add(Snapshot(before, after))
        redo[scId]?.clear()
        return TxResult(true, "committed: $label")
    }

    fun undo(scId: String): TxResult {
        val stack = undo[scId] ?: return TxResult(false, "nothing to undo")
        if (stack.isEmpty()) return TxResult(false, "nothing to undo")
        val snap = stack.removeAt(stack.size - 1)
        snap.before.forEach { (path, data) -> restore(File(path), data) }
        redo.getOrPut(scId) { mutableListOf() }.add(snap)
        return TxResult(true, "undone")
    }

    fun redo(scId: String): TxResult {
        val stack = redo[scId] ?: return TxResult(false, "nothing to redo")
        if (stack.isEmpty()) return TxResult(false, "nothing to redo")
        val snap = stack.removeAt(stack.size - 1)
        snap.after.forEach { (path, data) -> restore(File(path), data) }
        undo.getOrPut(scId) { mutableListOf() }.add(snap)
        return TxResult(true, "redone")
    }

    private fun restore(file: File, data: ByteArray?) {
        if (data == null) file.delete() else { file.parentFile?.mkdirs(); file.writeBytes(data) }
    }

    companion object {
        private val WIDGET_INDEX = mapOf(
            "linear" to 0, "relativelayout" to 1, "button" to 3, "textview" to 4,
            "edittext" to 5, "imageview" to 6, "listview" to 9, "checkbox" to 11,
            "switch" to 13, "fab" to 16, "cardview" to 36, "recyclerview" to 48
        )
    }
}
