package snd.sketchpilot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import snd.sketchpilot.core.crypto.SketchwareCrypto
import snd.sketchpilot.core.data.OfflineRepository
import snd.sketchpilot.core.data.Paths
import snd.sketchpilot.core.io.SectionedFile
import snd.sketchpilot.core.project.ProjectEditor
import snd.sketchpilot.core.project.ViewIr
import java.io.File

class CoreTest {
    private val crypto = SketchwareCrypto()

    private fun fixture(): File {
        val root = File("/tmp/sketchpilot-core-test-${System.currentTimeMillis()}")
        val paths = Paths(root)
        paths.data("602").mkdirs()
        paths.metadata("602").parentFile.mkdirs()
        paths.metadata("602").writeBytes(crypto.encrypt("{\"sc_id\":\"602\",\"my_ws_name\":\"QuickNotes\"}"))
        paths.dataFile("602", "view").writeBytes(
            crypto.encrypt("@main.xml\n{\"id\":\"tv\",\"type\":4}\n@note_item.xml\n{\"id\":\"linear\",\"type\":0}\n")
        )
        paths.dataFile("602", "logic").writeBytes(
            crypto.encrypt("@MainActivity.java_onCreate_initializeLogic\n{\"opCode\":\"addSourceDirectly\"}\n")
        )
        paths.dataFile("602", "view_root").writeText("{\"main.xml\":{\"class_name\":\"LinearLayout\"}}")
        return root
    }

    @Test fun cryptoRoundTrip() {
        val plain = "{\"sc_id\":\"999\"}"
        assertEquals(plain, crypto.decrypt(crypto.encrypt(plain)))
    }

    @Test fun metadataDecrypts() {
        val root = fixture()
        val json = crypto.decrypt(SketchwareCrypto.readAll(Paths(root).metadata("602")))
        assertTrue(json.contains("\"sc_id\":\"602\""))
        root.deleteRecursively()
    }

    @Test fun sectionRoundTrip() {
        val root = fixture()
        val text = SectionedFile.parse(
            crypto.decrypt(SketchwareCrypto.readAll(Paths(root).dataFile("602", "view")))
        ).serialize()
        assertTrue(text.contains("@main.xml"))
        assertTrue(text.contains("note_item.xml"))
        root.deleteRecursively()
    }

    @Test fun repositoryReadsFixture() {
        val root = fixture()
        val repo = OfflineRepository(Paths(root), crypto)
        assertTrue(repo.listProjects().size == 1)
        assertTrue(repo.views("602", "note_item.xml").size == 1)
        assertTrue(repo.logic("602").isNotEmpty())
        root.deleteRecursively()
    }

    @Test fun irApplyUndoRedo() {
        val root = fixture()
        val paths = Paths(root)
        val repo = OfflineRepository(paths, crypto)
        val before = paths.dataFile("999", "view")
        val original = "@main.xml\n{\"id\":\"old\",\"type\":4}\n"
        before.writeBytes(crypto.encrypt(original))
        val editor = ProjectEditor(paths, repo)
        val ir = ViewIr("login.xml")
        ir.widget("et_user", "edittext")
        ir.widget("btn_login", "button")
        assertTrue(editor.applyView("999", ir).ok)
        assertTrue(repo.views("999", "login.xml").size == 2)
        val changed = before.readBytes()
        assertTrue(editor.undo("999").ok)
        assertTrue(before.readBytes().contentEquals(crypto.encrypt(original)))
        assertTrue(editor.redo("999").ok)
        assertTrue(before.readBytes().contentEquals(changed))
        root.deleteRecursively()
    }
}
