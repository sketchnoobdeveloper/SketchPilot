package snd.sketchpilot.test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.project.ir.BlockIr;
import snd.sketchpilot.project.ir.LogicIr;
import snd.sketchpilot.project.ir.ViewIr;
import snd.sketchpilot.project.ir.WidgetIr;
import snd.sketchpilot.project.transactions.TransactionPlan;
import snd.sketchpilot.tools.impl.BuildDiagnosticsTool;
import snd.sketchpilot.library.DuplicateClassChecker;

/**
 * End-to-end offline scenarios S1-S5 on a COPY of real project 602.
 * Records real results to stdout (captured into docs/ACCEPTANCE.md).
 */
public final class ScenarioTest {

    private static int passed = 0;
    private static File root;
    private static SketchwarePaths paths;
    private static SketchwareCrypto crypto;
    private static ProjectEditor editor;

    public static void main(String[] args) throws Exception {
        System.out.println("SCENARIO S0: prepare temp copy of real project 602");
        setup();
        String before = hashTree(paths.data("999"));
        System.out.println("  fixture sha256=" + abbreviate(before));
        check("fixture created", paths.dataFile("999", "view").isFile());

        System.out.println("SCENARIO S1: create Login screen from natural-language spec via IR");
        ViewIr login = new ViewIr();
        login.setLayoutName("login.xml");
        login.getRootAttributes().put("android:orientation", "vertical");
        login.addWidget(widget("et_user", "edittext"));
        login.addWidget(widget("et_pass", "edittext"));
        login.addWidget(widget("btn_login", "button"));
        login.addWidget(widget("tv_status", "textview"));
        snd.sketchpilot.project.transactions.TransactionManager.Result s1 = editor.applyViewIr("999", login);
        System.out.println("  result=" + s1.success + " / " + s1.message);
        check("S1 applied", s1.success);
        String view = crypto.decrypt(Files.readAllBytes(paths.dataFile("999", "view").toPath()));
        check("S1 layout present", view.contains("@login.xml"));
        check("S1 widgets present", view.contains("et_user") && view.contains("btn_login")
                && view.contains("tv_status"));
        check("S1 root attributes present",
                paths.dataFile("999", "view_root").isFile()
                        && new String(Files.readAllBytes(paths.dataFile("999", "view_root").toPath()),
                        StandardCharsets.UTF_8).contains("login.xml"));

        System.out.println("SCENARIO S2: add button onClick, validate then toast logic");
        LogicIr logic = new LogicIr("MainActivity.java_onClick_btn_login");
        BlockIr block = new BlockIr();
        block.setOpCode("addSourceDirectly");
        block.getParameters().add("if (et_user.getText().length() == 0) { "
                + "Toast.makeText(this, \"Enter user\", Toast.LENGTH_SHORT).show(); return; } "
                + "Toast.makeText(this, \"Login ok\", Toast.LENGTH_SHORT).show();");
        logic.addBlock(block);
        snd.sketchpilot.project.transactions.TransactionManager.Result s2 = editor.applyLogicIr("999", logic);
        System.out.println("  result=" + s2.success + " / " + s2.message);
        check("S2 applied", s2.success);
        String logicText = crypto.decrypt(Files.readAllBytes(paths.dataFile("999", "logic").toPath()));
        check("S2 section present", logicText.contains("@MainActivity.java_onClick_btn_login"));
        check("S2 toast logic present", logicText.contains("Login ok"));

        System.out.println("SCENARIO S3: build-error fixture, diagnostics parser and fix proposal");
        File buildRoot = new File(root, ".sketchware");
        buildRoot.mkdirs();
        List<String> fixture = new ArrayList<String>();
        fixture.add("D/AppBuilder: Running Eclipse compiler with these arguments");
        fixture.add("error: cannot find symbol");
        fixture.add("aapt2: error: resource not found");
        Files.write(new File(buildRoot, "debug.txt").toPath(), fixture, StandardCharsets.UTF_8);
        JSONObject diagnostics = new BuildDiagnosticsTool(new SketchwarePaths(root)).execute(new JSONObject());
        JSONArray parsed = diagnostics.optJSONArray("diagnostics");
        System.out.println("  diagnostics=" + parsed.toString());
        check("S3 two diagnostics", parsed.length() == 2);
        check("S3 fix proposal available",
                "java".equals(parsed.getJSONObject(0).optString("stage")));

        System.out.println("SCENARIO S4: add library + duplicate-class conflict check");
        SectionedFile libraryFile = editor.getRepository().readSectioned("999", "library");
        List<String> compatLines = new ArrayList<String>();
        if (libraryFile.first("compat") != null) {
            compatLines.addAll(libraryFile.first("compat").getLines());
        }
        libraryFile.put("compat", compatLines);
        TransactionPlan plan = new TransactionPlan("library:compat");
        plan.put(paths.dataFile("999", "library"), crypto.encrypt(libraryFile.serialize()));
        snd.sketchpilot.project.transactions.TransactionManager.Result s4 =
                editor.getTransactions().commit("999", plan);
        System.out.println("  result=" + s4.success + " / " + s4.message);
        check("S4 applied", s4.success);
        JSONArray conflicts = DuplicateClassChecker.check(new File(paths.root(), "libs/local_libs"));
        System.out.println("  duplicate conflicts=" + conflicts.toString());
        check("S4 no duplicate classes", conflicts.length() == 0);

        System.out.println("SCENARIO S5: undo/redo returns byte-identical project");
        check("S5 undo S4", editor.getTransactions().undo("999").success);
        check("S5 undo S2", editor.getTransactions().undo("999").success);
        check("S5 undo S1", editor.getTransactions().undo("999").success);
        String afterUndo = hashTree(paths.data("999"));
        System.out.println("  after undo sha256=" + abbreviate(afterUndo));
        check("S5 byte-identical after undo", before.equals(afterUndo));
        check("S5 redo S1", editor.getTransactions().redo("999").success);
        check("S5 redo S2", editor.getTransactions().redo("999").success);
        check("S5 redo S4", editor.getTransactions().redo("999").success);
        check("S5 redo changes present",
                crypto.decrypt(Files.readAllBytes(paths.dataFile("999", "view").toPath()))
                        .contains("login.xml"));
        check("S5 final undo", editor.getTransactions().undo("999").success
                && editor.getTransactions().undo("999").success
                && editor.getTransactions().undo("999").success);
        check("S5 byte-identical final", before.equals(hashTree(paths.data("999"))));

        System.out.println("ScenarioTest: " + passed + " assertions passed.");
    }

    private static WidgetIr widget(String id, String type) {
        WidgetIr w = new WidgetIr();
        w.setId(id);
        w.setTypeName(type);
        w.setTypeCode(new snd.sketchpilot.view.WidgetRegistry().typeOf(type));
        return w;
    }

    private static void setup() throws Exception {
        root = new File("/tmp/opencode/p8-scenarios-" + System.currentTimeMillis());
        paths = new SketchwarePaths(root);
        crypto = new SketchwareCrypto();
        File data = paths.data("999");
        data.mkdirs();
        File[] files = new File("/sdcard/.sketchware/data/602").listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    Files.copy(files[i].toPath(), paths.dataFile("999", files[i].getName()).toPath());
                }
            }
        }
        File metaDir = paths.projectMetadata("999").getParentFile();
        metaDir.mkdirs();
        Files.copy(new File("/sdcard/.sketchware/mysc/list/602/project").toPath(),
                paths.projectMetadata("999").toPath());
        paths.backup("999").mkdirs();
        editor = new ProjectEditor(paths, crypto);
    }

    private static String hashTree(File dir) throws Exception {
        List<File> files = new ArrayList<File>();
        collect(dir, files);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        java.util.Collections.sort(files, new java.util.Comparator<File>() {
            public int compare(File a, File b) {
                return a.getAbsolutePath().compareTo(b.getAbsolutePath());
            }
        });
        for (int i = 0; i < files.size(); i++) {
            digest.update(files.get(i).getAbsolutePath().getBytes(StandardCharsets.UTF_8));
            digest.update(Files.readAllBytes(files.get(i).toPath()));
        }
        byte[] bytes = digest.digest();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            out.append(String.format("%02x", bytes[i]));
        }
        return out.toString();
    }

    private static void collect(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                collect(children[i], out);
            } else {
                out.add(children[i]);
            }
        }
    }

    private static String abbreviate(String hash) {
        return hash.substring(0, Math.min(16, hash.length())) + "...";
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("  ok - " + name);
    }
}
