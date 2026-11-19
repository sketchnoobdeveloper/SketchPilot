package snd.sketchpilot.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import snd.sketchpilot.project.OfflineRepository;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.project.model.BlockNode;
import snd.sketchpilot.project.model.ProjectInfo;
import snd.sketchpilot.project.model.ViewNode;

/** Dependency-free pure-Java test harness. Run with java -ea. */
public final class CoreTest {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        File external = new File("/sdcard");
        SketchwareCrypto crypto = new SketchwareCrypto();
        OfflineRepository repo = new OfflineRepository(new SketchwarePaths(external), crypto);

        // 1. Key/IV round trip.
        String sample = "{\"sc_id\":\"999\",\"name\":\"test\"}";
        check("crypto round trip", sample.equals(crypto.decrypt(crypto.encrypt(sample))));

        // 2. Real metadata decrypts to expected sc_id.
        byte[] meta = SketchwareCrypto.readAll(new FileInputStream("/sdcard/.sketchware/mysc/list/602/project"));
        check("real metadata is AES", crypto.looksEncrypted(meta));
        String metaJson = crypto.decrypt(meta);
        check("real metadata sc_id", metaJson.contains("\"sc_id\":\"602\""));

        // 3. Sectioned file parse then serialize is lossless for the real view file.
        String viewPlain = crypto.decrypt(SketchwareCrypto.readAll(
                new FileInputStream("/sdcard/.sketchware/data/602/view")));
        SectionedFile parsed = SectionedFile.parse(viewPlain);
        check("section round trip", viewPlain.equals(parsed.serialize()));

        // 4. Repository enumerates real projects.
        List<ProjectInfo> projects = repo.listProjects();
        check("list projects >= 2", projects.size() >= 2);
        boolean has601 = false;
        boolean has602 = false;
        for (int i = 0; i < projects.size(); i++) {
            if ("601".equals(projects.get(i).getId())) {
                has601 = true;
            }
            if ("602".equals(projects.get(i).getId())) {
                has602 = true;
            }
        }
        check("has 601", has601);
        check("has 602", has602);

        // 5. Views parse with typed accessors.
        List<ViewNode> views = repo.getViews("602", "main.xml");
        check("main.xml has views", views.size() > 0);
        check("view has id", views.get(0).getId().length() > 0);

        // 6. Logic blocks parse with opCode.
        Map<String, List<BlockNode>> logic = repo.getLogic("602");
        boolean foundOp = false;
        java.util.Iterator<String> keys = logic.keySet().iterator();
        while (keys.hasNext()) {
            List<BlockNode> blocks = logic.get(keys.next());
            for (int i = 0; i < blocks.size(); i++) {
                if ("addSourceDirectly".equals(blocks.get(i).getOpCode())) {
                    foundOp = true;
                }
            }
        }
        check("logic opCode present", foundOp);

        // 7. Plaintext config is detected as such.
        byte[] config = SketchwareCrypto.readAll(
                new FileInputStream("/sdcard/.sketchware/data/601/project_config"));
        check("config not AES", !crypto.looksEncrypted(config));

        System.out.println("CoreTest: " + passed + " assertions passed.");
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("  ok - " + name);
    }
}
