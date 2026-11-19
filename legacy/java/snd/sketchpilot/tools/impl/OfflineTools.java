package snd.sketchpilot.tools.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.agent.ToolRegistry;
import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.project.ir.LogicIr;
import snd.sketchpilot.project.ir.ViewIr;
import snd.sketchpilot.project.model.BlockNode;
import snd.sketchpilot.project.model.ProjectInfo;
import snd.sketchpilot.project.model.ViewNode;
import snd.sketchpilot.tools.Schemas;

/** Read/write tools backed by the offline repository and transactional editor. */
public final class OfflineTools {

    private OfflineTools() {
    }

    public static void register(ToolRegistry registry, final ProjectEditor editor,
                                final SketchwarePaths paths) {
        registry.register(new ProjectListTool(editor));
        registry.register(new ProjectInfoTool(editor));
        registry.register(new ProjectBackupTool(editor, paths));
        registry.register(new ScreenListTool(editor));
        registry.register(new ViewGetTreeTool(editor));
        registry.register(new ViewApplyIrTool(editor));
        registry.register(new LogicGetTool(editor));
        registry.register(new LogicApplyIrTool(editor));
        registry.register(new CodeReadTool(paths));
        registry.register(new ResourceListTool(editor));
        registry.register(new LibraryListTool(editor));
        registry.register(new BuildLogTool(paths));
    }

    private abstract static class BaseTool implements Tool {
        private final String name;
        private final String description;
        private final JSONObject schema;
        private final String risk;
        private final boolean dryRun;

        BaseTool(String name, String description, JSONObject schema, String risk, boolean dryRun) {
            this.name = name;
            this.description = description;
            this.schema = schema;
            this.risk = risk;
            this.dryRun = dryRun;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public JSONObject getSchema() {
            return schema;
        }

        public String getRisk() {
            return risk;
        }

        public boolean supportsDryRun() {
            return dryRun;
        }

        protected JSONObject ok(String key, Object value) {
            JSONObject out = new JSONObject();
            try {
                out.put("ok", true);
                out.put(key, value);
            } catch (Throwable ignored) {
                // safe
            }
            return out;
        }

        protected JSONObject error(String message) {
            JSONObject out = new JSONObject();
            try {
                out.put("ok", false);
                out.put("error", message);
            } catch (Throwable ignored) {
                // safe
            }
            return out;
        }
    }

    private static final class ProjectListTool extends BaseTool {
        private final ProjectEditor editor;

        ProjectListTool(ProjectEditor editor) {
            super("project.list", "List all Sketchware projects", Schemas.object(), RISK_READ, false);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) {
            try {
                List<ProjectInfo> projects = editor.getRepository().listProjects();
                JSONArray array = new JSONArray();
                for (int i = 0; i < projects.size(); i++) {
                    ProjectInfo info = projects.get(i);
                    JSONObject row = new JSONObject();
                    row.put("id", info.getId());
                    row.put("name", info.getWorkspaceName());
                    row.put("appName", info.getAppName());
                    row.put("package", info.getPackageName());
                    array.put(row);
                }
                return ok("projects", array);
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }

    private static final class ProjectInfoTool extends BaseTool {
        private final ProjectEditor editor;

        ProjectInfoTool(ProjectEditor editor) {
            super("project.info", "Get project metadata", Schemas.object("scId"), RISK_READ, false);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) {
            try {
                ProjectInfo info = editor.getRepository().getInfo(args.optString("scId", ""));
                if (info == null) {
                    return error("project not found");
                }
                JSONObject out = ok("project", new JSONObject());
                JSONObject row = out.optJSONObject("project");
                row.put("id", info.getId());
                row.put("name", info.getWorkspaceName());
                row.put("appName", info.getAppName());
                row.put("package", info.getPackageName());
                row.put("versionCode", info.getVersionCode());
                row.put("versionName", info.getVersionName());
                row.put("buildSystem", info.getBuildSystem());
                return out;
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }

    private static final class ProjectBackupTool extends BaseTool {
        private final ProjectEditor editor;
        private final SketchwarePaths paths;

        ProjectBackupTool(ProjectEditor editor, SketchwarePaths paths) {
            super("project.backup", "Create a timestamped backup", Schemas.object("scId"), RISK_READ, false);
            this.editor = editor;
            this.paths = paths;
        }

        public JSONObject execute(JSONObject args) {
            String scId = args.optString("scId", "");
            File backup = new snd.sketchpilot.project.backup.BackupManager(paths).backup(scId, "manual");
            return backup == null ? error("backup failed") : ok("path", backup.getAbsolutePath());
        }
    }

    private static final class ScreenListTool extends BaseTool {
        private final ProjectEditor editor;

        ScreenListTool(ProjectEditor editor) {
            super("screen.list", "List screens/activities", Schemas.object("scId"), RISK_READ, false);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) {
            try {
                List<String> sections = editor.getRepository().getFileSections(args.optString("scId", ""));
                return ok("sections", new JSONArray(sections));
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }

    private static final class ViewGetTreeTool extends BaseTool {
        private final ProjectEditor editor;

        ViewGetTreeTool(ProjectEditor editor) {
            super("view.get_tree", "Get the view tree of a layout",
                    Schemas.object("scId", "layout"), RISK_READ, false);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) {
            try {
                String scId = args.optString("scId", "");
                String layout = args.optString("layout", "");
                List<ViewNode> views = editor.getRepository().getViews(scId, layout.length() > 0 ? layout : null);
                JSONArray array = new JSONArray();
                for (int i = 0; i < views.size(); i++) {
                    array.put(views.get(i).getRaw());
                }
                return ok("views", array);
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }

    private static final class ViewApplyIrTool extends BaseTool {
        private final ProjectEditor editor;

        ViewApplyIrTool(ProjectEditor editor) {
            super("view.apply_ir", "Apply a typed view IR (backs up first)",
                    Schemas.object("scId", "ir"), RISK_WRITE, true);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) throws Exception {
            String scId = args.optString("scId", "");
            String irJson = args.optString("ir", "{}");
            JSONObject root = new JSONObject(irJson);
            ViewIr ir = new ViewIr();
            ir.setLayoutName(root.optString("layout", ""));
            ir.setRootAttributes(root.optJSONObject("rootAttributes"));
            JSONArray widgets = root.optJSONArray("widgets");
            if (widgets != null) {
                for (int i = 0; i < widgets.length(); i++) {
                    JSONObject w = widgets.getJSONObject(i);
                    snd.sketchpilot.project.ir.WidgetIr widget = new snd.sketchpilot.project.ir.WidgetIr();
                    widget.setId(w.optString("id", ""));
                    widget.setTypeName(w.optString("type", ""));
                    widget.setTypeCode(w.optInt("typeCode", -1));
                    widget.setParent(w.optString("parent", ""));
                    widget.setIndex(w.optInt("index", 0));
                    JSONObject props = w.optJSONObject("properties");
                    if (props != null) {
                        java.util.Iterator<String> keys = props.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            widget.getProperties().put(key, props.get(key));
                        }
                    }
                    ir.addWidget(widget);
                }
            }
            snd.sketchpilot.project.transactions.TransactionManager.Result result = editor.applyViewIr(scId, ir);
            JSONObject out = new JSONObject();
            out.put("ok", result.success);
            out.put("message", result.message);
            return out;
        }
    }

    private static final class LogicGetTool extends BaseTool {
        private final ProjectEditor editor;

        LogicGetTool(ProjectEditor editor) {
            super("logic.get", "Read logic blocks by section", Schemas.object("scId"), RISK_READ, false);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) {
            try {
                Map<String, List<BlockNode>> logic = editor.getRepository().getLogic(args.optString("scId", ""));
                JSONObject sections = new JSONObject();
                java.util.Iterator<String> keys = logic.keySet().iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONArray blocks = new JSONArray();
                    List<BlockNode> list = logic.get(key);
                    for (int i = 0; i < list.size(); i++) {
                        blocks.put(list.get(i).getRaw());
                    }
                    sections.put(key, blocks);
                }
                return ok("sections", sections);
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }

    private static final class LogicApplyIrTool extends BaseTool {
        private final ProjectEditor editor;

        LogicApplyIrTool(ProjectEditor editor) {
            super("logic.apply_ir", "Apply a typed logic IR (backs up first)",
                    Schemas.object("scId", "ir"), RISK_WRITE, true);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) throws Exception {
            String scId = args.optString("scId", "");
            JSONObject root = new JSONObject(args.optString("ir", "{}"));
            LogicIr ir = new LogicIr(root.optString("section", ""));
            JSONArray blocks = root.optJSONArray("blocks");
            if (blocks != null) {
                for (int i = 0; i < blocks.length(); i++) {
                    JSONObject b = blocks.getJSONObject(i);
                    snd.sketchpilot.project.ir.BlockIr block = new snd.sketchpilot.project.ir.BlockIr();
                    block.setId(b.optString("id", ""));
                    block.setOpCode(b.optString("opCode", ""));
                    JSONArray params = b.optJSONArray("parameters");
                    if (params != null) {
                        for (int j = 0; j < params.length(); j++) {
                            block.getParameters().add(String.valueOf(params.get(j)));
                        }
                    }
                    ir.addBlock(block);
                }
            }
            snd.sketchpilot.project.transactions.TransactionManager.Result result = editor.applyLogicIr(scId, ir);
            JSONObject out = new JSONObject();
            out.put("ok", result.success);
            out.put("message", result.message);
            return out;
        }
    }

    private static final class CodeReadTool extends BaseTool {
        private final SketchwarePaths paths;

        CodeReadTool(SketchwarePaths paths) {
            super("code.read", "Read a custom source/resource file", Schemas.object("scId", "path"), RISK_READ, false);
            this.paths = paths;
        }

        public JSONObject execute(JSONObject args) {
            try {
                String relative = args.optString("path", "").replace("\\", "/");
                if (relative.contains("..")) {
                    return error("path traversal denied");
                }
                File file = new File(paths.data(args.optString("scId", "")), relative);
                if (!file.isFile()) {
                    return error("file not found");
                }
                return ok("content", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }

    private static final class ResourceListTool extends BaseTool {
        private final ProjectEditor editor;

        ResourceListTool(ProjectEditor editor) {
            super("resource.list", "List project resources", Schemas.object("scId"), RISK_READ, false);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) {
            try {
                String scId = args.optString("scId", "");
                SectionedFile file = editor.getRepository().readSectioned(scId, "resource");
                JSONArray sections = new JSONArray();
                List<SectionedFile.Section> list = file.getSections();
                for (int i = 0; i < list.size(); i++) {
                    JSONObject row = new JSONObject();
                    row.put("name", list.get(i).getName());
                    row.put("items", new JSONArray(list.get(i).getLines()));
                    sections.put(row);
                }
                return ok("resources", sections);
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }

    private static final class LibraryListTool extends BaseTool {
        private final ProjectEditor editor;

        LibraryListTool(ProjectEditor editor) {
            super("library.list", "List library configurations", Schemas.object("scId"), RISK_READ, false);
            this.editor = editor;
        }

        public JSONObject execute(JSONObject args) {
            try {
                return ok("libraries", new JSONArray(editor.getRepository().getLibrarySections(args.optString("scId", ""))));
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }

    private static final class BuildLogTool extends BaseTool {
        private final SketchwarePaths paths;

        BuildLogTool(SketchwarePaths paths) {
            super("build.log", "Read the tail of the build log", Schemas.object("lines"), RISK_READ, false);
            this.paths = paths;
        }

        public JSONObject execute(JSONObject args) {
            try {
                File log = new File(paths.root(), "debug.txt");
                if (!log.isFile()) {
                    return error("build log not found");
                }
                List<String> lines = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
                int count = args.optInt("lines", 100);
                JSONArray tail = new JSONArray();
                int from = Math.max(0, lines.size() - count);
                for (int i = from; i < lines.size(); i++) {
                    tail.put(lines.get(i));
                }
                return ok("lines", tail);
            } catch (Throwable t) {
                return error(String.valueOf(t.getMessage()));
            }
        }
    }
}
