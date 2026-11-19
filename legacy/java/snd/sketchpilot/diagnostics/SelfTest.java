package snd.sketchpilot.diagnostics;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.backup.BackupManager;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.project.ir.LogicIr;
import snd.sketchpilot.project.ir.ViewIr;
import snd.sketchpilot.project.ir.ViewIrCompiler;
import snd.sketchpilot.project.ir.LogicIrCompiler;
import snd.sketchpilot.project.validators.IrValidator;
import snd.sketchpilot.view.WidgetRegistry;

/** Pure-Java self-tests executed on a temporary COPY of a real project. */
public final class SelfTest {

    private final JSONArray results = new JSONArray();

    public SelfTest() {
    }

    public void runAll(SketchwarePaths realPaths) {
        add("crypto_round_trip", cryptoRoundTrip());
        add("offline_decrypt_copy", offlineDecryptCopy(realPaths));
        add("backup_restore_copy", backupRestoreCopy(realPaths));
        add("ir_validate_compile", irValidateCompile());
    }

    public JSONArray getResults() {
        return results;
    }

    private static TestResult cryptoRoundTrip() {
        try {
            SketchwareCrypto crypto = new SketchwareCrypto();
            String sample = "{\"sc_id\":\"selftest\",\"ok\":true}";
            byte[] encrypted = crypto.encrypt(sample);
            String decrypted = crypto.decrypt(encrypted);
            return new TestResult(sample.equals(decrypted), "AES/CBC/PKCS5 key=IV, round-trip ok");
        } catch (Throwable t) {
            return new TestResult(false, "crypto failed: " + t);
        }
    }

    private static TestResult offlineDecryptCopy(SketchwarePaths realPaths) {
        File work = new File("/tmp/sketchpilot-selftest-" + System.currentTimeMillis());
        try {
            File source = realPaths.projectMetadata("602");
            if (!source.isFile()) {
                return new TestResult(false, "no real project 602 metadata available on this device");
            }
            File dir = new File(work, ".sketchware/mysc/list/602");
            dir.mkdirs();
            Files.copy(source.toPath(), new File(dir, "project").toPath());
            SketchwarePaths paths = new SketchwarePaths(work);
            byte[] bytes = Files.readAllBytes(paths.projectMetadata("602").toPath());
            String json = new SketchwareCrypto().decrypt(bytes);
            boolean ok = json.contains("\"sc_id\":\"602\"");
            delete(work);
            return new TestResult(ok, ok ? "real metadata decrypted on temp copy" : "unexpected JSON");
        } catch (Throwable t) {
            delete(work);
            return new TestResult(false, "offline decrypt failed: " + t);
        }
    }

    private static TestResult backupRestoreCopy(SketchwarePaths realPaths) {
        File work = new File("/tmp/sketchpilot-selftest-bak-" + System.currentTimeMillis());
        try {
            File source = realPaths.dataFile("602", "view");
            if (!source.isFile()) {
                return new TestResult(false, "no real project 602 view file available");
            }
            SketchwarePaths paths = new SketchwarePaths(work);
            File data = paths.data("602");
            data.mkdirs();
            Files.copy(source.toPath(), paths.dataFile("602", "view").toPath());
            long before = Files.readAllBytes(paths.dataFile("602", "view").toPath()).length;
            BackupManager manager = new BackupManager(paths);
            File backup = manager.backup("602", "selftest");
            boolean restored = manager.restore("602", backup);
            long after = Files.readAllBytes(paths.dataFile("602", "view").toPath()).length;
            delete(work);
            boolean ok = backup != null && restored && before == after;
            return new TestResult(ok, "backup=" + (backup == null ? "null" : backup.getName())
                    + " restore=" + restored + " bytes=" + before + "/" + after);
        } catch (Throwable t) {
            delete(work);
            return new TestResult(false, "backup/restore failed: " + t);
        }
    }

    private static TestResult irValidateCompile() {
        try {
            WidgetRegistry widgets = new WidgetRegistry();
            BlockSpecRegistry specs = new BlockSpecRegistry();
            IrValidator validator = new IrValidator(widgets, specs);
            ViewIr view = new ViewIr();
            view.setLayoutName("login.xml");
            snd.sketchpilot.project.ir.WidgetIr w1 = new snd.sketchpilot.project.ir.WidgetIr();
            w1.setId("et_user");
            w1.setTypeName("edittext");
            snd.sketchpilot.project.ir.WidgetIr w2 = new snd.sketchpilot.project.ir.WidgetIr();
            w2.setId("btn_login");
            w2.setTypeName("button");
            view.addWidget(w1);
            view.addWidget(w2);
            List<String> errors = validator.validate(view);
            if (!errors.isEmpty()) {
                return new TestResult(false, "validation errors: " + errors);
            }
            SectionedFile compiled = new ViewIrCompiler(widgets).compile(new SectionedFile(), view);
            String serialized = compiled.serialize();
            LogicIr logic = new LogicIr("MainActivity.java_onClick_btn_login");
            snd.sketchpilot.project.ir.BlockIr block = new snd.sketchpilot.project.ir.BlockIr();
            block.setOpCode("addSourceDirectly");
            block.getParameters().add("Toast.makeText(this, \"ok\", Toast.LENGTH_SHORT).show();");
            logic.addBlock(block);
            SectionedFile compiledLogic = new LogicIrCompiler(specs).compile(new SectionedFile(), logic);
            boolean ok = serialized.contains("et_user") && serialized.contains("btn_login")
                    && compiledLogic.serialize().contains("addSourceDirectly");
            return new TestResult(ok, ok ? "view+logic IR validated and compiled" : "compile output missing ids");
        } catch (Throwable t) {
            return new TestResult(false, "IR test failed: " + t);
        }
    }

    private void add(String name, TestResult result) {
        JSONObject row = new JSONObject();
        try {
            row.put("name", name);
            row.put("ok", result.ok);
            row.put("detail", result.detail);
        } catch (Throwable ignored) {
            // safe
        }
        results.put(row);
    }

    private static final class TestResult {
        final boolean ok;
        final String detail;

        TestResult(boolean ok, String detail) {
            this.ok = ok;
            this.detail = detail;
        }
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
}
