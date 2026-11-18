package snd.sketchpilot.tools.impl;

import java.io.File;

import org.json.JSONObject;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.gaps.Gap5OpaquePayloads;
import snd.sketchpilot.gaps.Gap6SystemFiles;
import snd.sketchpilot.gaps.Gap7GradleExport;
import snd.sketchpilot.library.DuplicateClassChecker;
import snd.sketchpilot.project.ProjectEditor;
import snd.sketchpilot.project.SketchwarePaths;
import snd.sketchpilot.tools.Schemas;

/** Real tools that close parts of GAP-5, GAP-6, GAP-7 and the library conflict check. */
public final class GapTools {

    private GapTools() {
    }

    public static void register(snd.sketchpilot.agent.ToolRegistry registry,
                                final SketchwarePaths paths, final ProjectEditor editor) {
        registry.register(new LibraryCheckTool(paths));
        registry.register(new ProjectExportTool(paths));
        registry.register(new OpaqueReadTool(paths, editor));
        registry.register(new SystemReadTool(paths));
    }

    private abstract static class Base implements Tool {
        private final String name;
        private final String description;

        Base(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public JSONObject getSchema() {
            return Schemas.object("scId");
        }

        public boolean supportsDryRun() {
            return false;
        }
    }

    private static final class LibraryCheckTool extends Base {
        private final SketchwarePaths paths;

        LibraryCheckTool(SketchwarePaths paths) {
            super("library.check", "Check local libraries for duplicate classes");
            this.paths = paths;
        }

        public String getRisk() {
            return RISK_READ;
        }

        public JSONObject execute(JSONObject arguments) {
            File libs = new File(paths.root(), "libs/local_libs");
            JSONObject out = new JSONObject();
            try {
                out.put("ok", true);
                out.put("conflicts", DuplicateClassChecker.check(libs));
            } catch (Throwable t) {
                try {
                    out.put("ok", false);
                    out.put("error", String.valueOf(t.getMessage()));
                } catch (Throwable ignored) {
                    // safe
                }
            }
            return out;
        }
    }

    private static final class ProjectExportTool extends Base {
        private final SketchwarePaths paths;

        ProjectExportTool(SketchwarePaths paths) {
            super("project.export", "Export a portable backup archive of a project");
            this.paths = paths;
        }

        public String getRisk() {
            return RISK_READ;
        }

        public JSONObject execute(JSONObject arguments) {
            String scId = arguments.optString("scId", "");
            String format = arguments.optString("format", "backup");
            JSONObject out = new JSONObject();
            if (!"backup".equals(format)) {
                try {
                    out.put("ok", false);
                    out.put("status", "unavailable");
                    out.put("error", "Gradle/Android Studio export is not available yet; use format=backup");
                } catch (Throwable ignored) {
                    // safe
                }
                return out;
            }
            File dir = new File(paths.root(), "exports");
            File zip = Gap7GradleExport.backupArchive(paths, scId, dir);
            try {
                out.put("ok", zip != null);
                if (zip != null) {
                    out.put("path", zip.getAbsolutePath());
                    out.put("bytes", zip.length());
                }
            } catch (Throwable ignored) {
                // safe
            }
            return out;
        }
    }

    private static final class OpaqueReadTool extends Base {
        private final SketchwarePaths paths;
        private final ProjectEditor editor;

        OpaqueReadTool(SketchwarePaths paths, ProjectEditor editor) {
            super("code.read_opaque", "Read java/local_library payload generically");
            this.paths = paths;
            this.editor = editor;
        }

        public String getRisk() {
            return RISK_READ;
        }

        public JSONObject execute(JSONObject arguments) {
            return Gap5OpaquePayloads.read(paths, new snd.sketchpilot.project.crypto.SketchwareCrypto(),
                    arguments.optString("scId", ""), arguments.optString("file", "java"));
        }
    }

    private static final class SystemReadTool extends Base {
        private final SketchwarePaths paths;

        SystemReadTool(SketchwarePaths paths) {
            super("kb.system", "Read system events/listeners JSON tolerantly");
            this.paths = paths;
        }

        public String getRisk() {
            return RISK_READ;
        }

        public JSONObject execute(JSONObject arguments) {
            JSONObject out = new JSONObject();
            try {
                out.put("ok", true);
                out.put("events", Gap6SystemFiles.read(paths, "events.json"));
                out.put("listeners", Gap6SystemFiles.read(paths, "listeners.json"));
                out.put("components", Gap6SystemFiles.read(paths, "component.json"));
            } catch (Throwable t) {
                try {
                    out.put("ok", false);
                    out.put("error", String.valueOf(t.getMessage()));
                } catch (Throwable ignored) {
                    // safe
                }
            }
            return out;
        }
    }
}
