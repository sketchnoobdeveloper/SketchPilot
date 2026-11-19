package snd.sketchpilot.tools;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.agent.ToolRegistry;
import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.tools.impl.BuildDiagnosticsTool;
import snd.sketchpilot.tools.impl.KbSearchTool;
import snd.sketchpilot.tools.impl.OfflineTools;
import snd.sketchpilot.tools.impl.ShellTool;
import snd.sketchpilot.view.WidgetRegistry;
import snd.sketchpilot.tools.impl.UnavailableTool;

/** Completes the tool catalog from the master prompt section 6b. */
public final class ToolCatalog {

    private ToolCatalog() {
    }

    public static ToolRegistry build(ProjectEditor editor, SketchwarePaths paths) {
        return build(editor, paths, null);
    }

    public static ToolRegistry build(ProjectEditor editor, SketchwarePaths paths,
                                     android.content.Context context) {
        ToolRegistry registry = new ToolRegistry();
        OfflineTools.register(registry, editor, paths);
        registry.register(new ShellTool());
        if (context != null) {
            registry.register(new snd.sketchpilot.tools.impl.DiagnosticsTool(context));
        }
        registry.register(new BuildDiagnosticsTool(paths));
        registry.register(new KbSearchTool(new BlockSpecRegistry(), new WidgetRegistry()));
        snd.sketchpilot.tools.impl.GapTools.register(registry, paths, editor);

        // project.*
        placeholder(registry, "project.create", "Create a project", Tool.RISK_WRITE);
        placeholder(registry, "project.clone", "Clone a project", Tool.RISK_WRITE);
        placeholder(registry, "project.rename", "Rename a project", Tool.RISK_WRITE);
        placeholder(registry, "project.restore", "Restore a backup", Tool.RISK_DESTRUCTIVE);
        placeholder(registry, "project.settings", "Edit project settings", Tool.RISK_WRITE);

        // screen.*
        placeholder(registry, "screen.add", "Add a screen", Tool.RISK_WRITE);
        placeholder(registry, "screen.remove", "Remove a screen", Tool.RISK_DESTRUCTIVE);
        placeholder(registry, "screen.rename", "Rename a screen", Tool.RISK_WRITE);
        placeholder(registry, "screen.settings", "Edit screen settings", Tool.RISK_WRITE);

        // view.*
        placeholder(registry, "view.add", "Add a widget", Tool.RISK_WRITE);
        placeholder(registry, "view.edit", "Edit a widget", Tool.RISK_WRITE);
        placeholder(registry, "view.move", "Move a widget", Tool.RISK_WRITE);
        placeholder(registry, "view.delete", "Delete a widget", Tool.RISK_DESTRUCTIVE);
        placeholder(registry, "view.set_properties", "Set widget properties", Tool.RISK_WRITE);
        placeholder(registry, "view.set_xml", "Set raw layout XML", Tool.RISK_WRITE);

        // logic.*
        placeholder(registry, "logic.events", "Edit events", Tool.RISK_WRITE);
        placeholder(registry, "logic.variables", "Edit variables", Tool.RISK_WRITE);
        placeholder(registry, "logic.lists", "Edit lists", Tool.RISK_WRITE);
        placeholder(registry, "logic.more_blocks", "Edit more-blocks", Tool.RISK_WRITE);
        placeholder(registry, "logic.components", "Edit components", Tool.RISK_WRITE);
        placeholder(registry, "logic.explain", "Explain logic", Tool.RISK_READ);
        placeholder(registry, "logic.generate", "Generate logic from IR", Tool.RISK_WRITE);

        // code.*
        placeholder(registry, "code.write", "Write custom source", Tool.RISK_WRITE);
        placeholder(registry, "code.generated", "View generated source", Tool.RISK_READ);
        placeholder(registry, "code.add_source_blocks", "Add source-directly blocks", Tool.RISK_WRITE);

        // resource.* / library.*
        placeholder(registry, "resource.images", "Manage images", Tool.RISK_WRITE);
        placeholder(registry, "resource.fonts", "Manage fonts", Tool.RISK_WRITE);
        placeholder(registry, "resource.sounds", "Manage sounds", Tool.RISK_WRITE);
        placeholder(registry, "resource.strings", "Manage strings", Tool.RISK_WRITE);
        placeholder(registry, "resource.colors", "Manage colors", Tool.RISK_WRITE);
        placeholder(registry, "resource.generate", "Generate drawables", Tool.RISK_WRITE);
        placeholder(registry, "library.local", "Manage local libraries", Tool.RISK_WRITE);
        placeholder(registry, "library.maven", "Import Maven dependency", Tool.RISK_WRITE);
        placeholder(registry, "library.toggle", "Toggle built-in library", Tool.RISK_WRITE);

        // build.* / debug.*
        placeholder(registry, "build.compile", "Compile a project", Tool.RISK_SYSTEM);
        placeholder(registry, "build.stream_log", "Stream build log", Tool.RISK_READ);
        placeholder(registry, "build.compile_and_fix", "Compile and auto-fix", Tool.RISK_SYSTEM);
        placeholder(registry, "build.install", "Install built APK", Tool.RISK_SYSTEM);
        placeholder(registry, "debug.logcat", "Read logcat", Tool.RISK_READ);
        placeholder(registry, "debug.crash", "Analyze crash trace", Tool.RISK_READ);
        // debug.diagnostics is registered above when a Context is available.

        // fs.* / web.* / kb.search
        placeholder(registry, "fs.list", "List sandboxed files", Tool.RISK_READ);
        placeholder(registry, "web.fetch", "Fetch docs (opt-in)", Tool.RISK_READ);
        return registry;
    }

    private static void placeholder(ToolRegistry registry, String name, String description, String risk) {
        registry.register(new UnavailableTool(name, description, risk,
                "live backend pending; see docs/COVERAGE.md"));
    }
}
