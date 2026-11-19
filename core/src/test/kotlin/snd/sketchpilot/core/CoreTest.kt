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
    private val root = File("/sdcard")
    private val paths = Paths(root)
    private val crypto = SketchwareCrypto()
    private val repo = OfflineRepository(paths, crypto)

    @Test fun cryptoRoundTrip() {
        val plain = "{\"sc_id\":\"999\"}"
        assertEquals(plain, crypto.decrypt(crypto.encrypt(plain)))
    }

    @Test fun realMetadataDecrypts() {
        val json = crypto.decrypt(SketchwareCrypto.readAll(paths.metadata("602")))
        assertTrue(json.contains("\"sc_id\":\"602\""))
    }

    @Test fun sectionRoundTrip() {
        val text = SectionedFile.parse(crypto.decrypt(SketchwareCrypto.readAll(paths.dataFile("602", "view")))).serialize()
        assertTrue(text.contains("@"))
    }

    @Test fun repositoryReadsRealProject() {
        assertTrue(repo.listProjects().size >= 2)
        assertTrue(repo.views("602", "note_item.xml").isNotEmpty())
        assertTrue(repo.logic("602").isNotEmpty())
    }

    @Test fun irApplyUndoRedo() {
        val work = File("/tmp/sketchpilot-kotlin-core-Test-${System.currentTimeMillis()}")
        val workPaths = Paths(work)
        val workRepo = OfflineRepository(workPaths, crypto)
        workPaths.data("999").mkdirs()
        File("/sdcard/.sketchware/data/602/view").copyTo(workPaths.dataFile("999", "view"), true)
        File("/sdcard/.sketchware/data/602/view_root").copyTo(workPaths.dataFile("999", "view_root"), true)
        val before = workPaths.dataFile("999", "view").readBytes()

        val editor = ProjectEditor(workPaths, workRepo)
        val ir = ViewIr("login.xml")
        ir.widget("et_user", "edittext")
        ir.widget("btn_login", "button")
        assertTrue(editor.applyView("999", ir).ok)
        assertTrue(workRepo.views("999", "login.xml").size == 2)
        assertTrue(editor.undo("999").ok)
        assertTrue(before.contentEquals(workPaths.dataFile("999", "view").readBytes()))
        assertTrue(editor.redo("999").ok)
        work.deleteRecursively()
    }
}
