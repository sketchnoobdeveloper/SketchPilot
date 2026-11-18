package snd.sketchpilot.test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import snd.sketchpilot.project.LiveSessionRegistry;
import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.project.ir.LogicIr;
import snd.sketchpilot.project.ir.ViewIr;
import snd.sketchpilot.project.ir.WidgetIr;
import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.project.transactions.TransactionManager;
import snd.sketchpilot.project.transactions.TransactionPlan;

/** Pure-Java tests for P2: IR, validation, transactions, backups, conflict safety. */
public final class TransactionTest {

    private static int passed = 0;
    private static File root;
    private static SketchwarePaths paths;
    private static SketchwareCrypto crypto;
    private static ProjectEditor editor;

    public static void main(String[] args) throws Exception {
        setupFixture();

        List<ViewIr> views = editor.getViewIr("999");
        check("extract view IR", views.size() > 0);
        ViewIr ir = views.get(0);
        check("IR has widgets", ir.getWidgets().size() > 0);

        String originalId = ir.getWidgets().get(0).getId();
        String newId = "ai_test_widget";
        ir.getWidgets().get(0).setId(newId);
        TransactionManager.Result applied = editor.applyViewIr("999", ir);
        check("apply view IR", applied.success);

        String persisted = readDecrypted("view");
        check("new id persisted", persisted.contains("\"id\":\"" + newId + "\""));
        check("backup created", paths.backup("999").listFiles().length > 0);

        check("undo", editor.getTransactions().undo("999").success);
        check("old id restored", readDecrypted("view").contains("\"id\":\"" + originalId + "\""));
        check("redo", editor.getTransactions().redo("999").success);
        check("new id after redo", readDecrypted("view").contains("\"id\":\"" + newId + "\""));

        ViewIr invalid = editor.getViewIr("999").get(0);
        invalid.getWidgets().get(1).setId(invalid.getWidgets().get(0).getId());
        check("duplicate id rejected", !editor.applyViewIr("999", invalid).success);

        LiveSessionRegistry.open("999");
        ViewIr blocked = editor.getViewIr("999").get(0);
        blocked.getWidgets().get(0).setId("should_not_write");
        check("live session blocks offline write", !editor.applyViewIr("999", blocked).success);
        LiveSessionRegistry.close("999");

        LogicIr logic = new LogicIr("MainActivity.java_onCreate_initializeLogic");
        snd.sketchpilot.project.ir.BlockIr block = new snd.sketchpilot.project.ir.BlockIr();
        block.setOpCode("addSourceDirectly");
        block.getParameters().add("Log.d(\"test\",\"ok\");");
        logic.addBlock(block);
        check("apply logic IR", editor.applyLogicIr("999", logic).success);
        check("logic op persisted", readDecrypted("logic").contains("addSourceDirectly"));

        // Sandbox rejects writes outside the project.
        TransactionPlan evil = new TransactionPlan("evil");
        evil.put(new File("/tmp/outside-sketchpilot.txt"), new byte[]{1});
        check("sandbox rejects outside write",
                !new TransactionManager(paths,
                        new snd.sketchpilot.project.io.ProjectFileIO(crypto),
                        new snd.sketchpilot.project.backup.BackupManager(paths))
                        .commit("999", evil).success);

        System.out.println("TransactionTest: " + passed + " assertions passed.");
    }

    private static String readDecrypted(String file) throws Exception {
        byte[] bytes = Files.readAllBytes(paths.dataFile("999", file).toPath());
        return crypto.decrypt(bytes);
    }

    private static void setupFixture() throws Exception {
        root = new File("/tmp/opencode/p2test");
        delete(root);
        paths = new SketchwarePaths(root);
        crypto = new SketchwareCrypto();
        Files.createDirectories(paths.data("999").toPath());
        Files.createDirectories(paths.projectMetadata("999").getParentFile().toPath());
        Files.createDirectories(paths.backup("999").toPath());
        copy("/sdcard/.sketchware/data/602/view", paths.dataFile("999", "view"));
        copy("/sdcard/.sketchware/data/602/logic", paths.dataFile("999", "logic"));
        copy("/sdcard/.sketchware/data/602/file", paths.dataFile("999", "file"));
        copy("/sdcard/.sketchware/data/602/view_root", paths.dataFile("999", "view_root"));
        copy("/sdcard/.sketchware/mysc/list/602/project", paths.projectMetadata("999"));
        editor = new ProjectEditor(paths, crypto);
    }

    private static void copy(String from, File to) throws Exception {
        Files.copy(new File(from).toPath(), to.toPath());
    }

    private static void delete(File file) {
        if (!file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                delete(children[i]);
            }
        }
        file.delete();
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("  ok - " + name);
    }
}
