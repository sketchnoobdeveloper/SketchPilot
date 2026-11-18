package snd.sketchpilot.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.build.BuildDiagnosticsParser;
import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.tools.ToolCatalog;
import snd.sketchpilot.agent.ToolRegistry;

/** Tests for the assembled tool catalog and build diagnostics. */
public final class ToolsTest {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        SketchwarePaths paths = new SketchwarePaths(new File("/sdcard"));
        ProjectEditor editor = new ProjectEditor(paths, new SketchwareCrypto());
        ToolRegistry registry = ToolCatalog.build(editor, paths);
        check("available tool count >= 18", registry.schemas().length() >= 18);
        JSONObject unavailable = registry.execute("project.create", new JSONObject());
        check("unavailable tool returns clear error",
                "unavailable".equals(unavailable.optString("status")));

        JSONObject projects = registry.execute("project.list", new JSONObject());
        check("project.list ok", projects.optBoolean("ok"));
        check("project.list has entries", projects.optJSONArray("projects").length() >= 2);

        JSONObject viewArgs = new JSONObject();
        viewArgs.put("scId", "602");
        viewArgs.put("layout", "note_item.xml");
        JSONObject views = registry.execute("view.get_tree", viewArgs);
        check("view.get_tree ok", views.optBoolean("ok"));
        check("view.get_tree has widgets", views.optJSONArray("views").length() > 0);

        JSONObject diag = registry.execute("build.diagnostics", new JSONObject());
        check("build.diagnostics ok", diag.optBoolean("ok"));

        JSONObject kbArgs = new JSONObject();
        kbArgs.put("query", "recycler");
        JSONObject kb = registry.execute("kb.search", kbArgs);
        check("kb.search ok", kb.optBoolean("ok"));
        check("kb.search has results", kb.optJSONArray("results").length() > 0);

        JSONObject shell = registry.execute("shell.exec", new JSONObject());
        check("shell disabled by default", !shell.optBoolean("ok"));

        List<String> sample = new ArrayList<String>();
        sample.add("D/AppBuilder: Running Eclipse compiler");
        sample.add("error: cannot find symbol");
        sample.add("aapt2: error: resource not found");
        JSONArray parsed = BuildDiagnosticsParser.parse(sample);
        check("diagnostics parsed", parsed.length() == 2);
        check("java stage detected", "java".equals(parsed.getJSONObject(0).optString("stage")));
        check("resource stage detected", "resources".equals(parsed.getJSONObject(1).optString("stage")));

        System.out.println("ToolsTest: " + passed + " assertions passed.");
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("  ok - " + name);
    }
}
