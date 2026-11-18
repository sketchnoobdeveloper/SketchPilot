package snd.sketchpilot.mcp.protocol;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.agent.ToolRegistry;
import snd.sketchpilot.project.OfflineRepository;
import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.project.model.ProjectInfo;

/**
 * Transport-independent JSON-RPC dispatcher. Used by both Streamable HTTP and
 * the stdio bridge, so behaviour is identical across clients.
 */
public final class McpDispatcher {

    private final ToolRegistry tools;
    private final OfflineRepository repository;
    private final List<String> allowedTools = new ArrayList<String>();
    private final File auditFile;

    public McpDispatcher(ToolRegistry tools, OfflineRepository repository, File auditFile) {
        this.tools = tools;
        this.repository = repository;
        this.auditFile = auditFile;
        collectNames(tools.schemas(), allowedTools);
    }

    private static void collectNames(JSONArray schemas, List<String> target) {
        for (int i = 0; i < schemas.length(); i++) {
            JSONObject entry = schemas.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            JSONObject function = entry.optJSONObject("function");
            if (function != null) {
                target.add(function.optString("name", ""));
            }
        }
    }

    private static JSONObject wrap(String key, Object value) {
        JSONObject out = new JSONObject();
        try {
            out.put(key, value);
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }

    public void setAllowedTools(List<String> names) {
        allowedTools.clear();
        if (names == null) {
            collectNames(tools.schemas(), allowedTools);
            return;
        }
        allowedTools.addAll(names);
    }

    /** Returns a JSON-RPC response string, or null for notifications. */
    public String dispatch(String requestJson) {
        JSONObject response = null;
        String method = "";
        Object id = JSONObject.NULL;
        try {
            JSONObject request = new JSONObject(requestJson);
            method = request.optString("method", "");
            id = request.has("id") ? request.get("id") : JSONObject.NULL;
            JSONObject params = request.optJSONObject("params");
            if (params == null) {
                params = new JSONObject();
            }
            response = handle(method, params, id);
        } catch (Throwable t) {
            response = error(id, -32700, "parse error: " + t.getClass().getSimpleName());
        }
        audit(method, response);
        return response == null ? null : response.toString();
    }

    private JSONObject handle(String method, JSONObject params, Object id) {
        if (McpProtocol.METHOD_INITIALIZED.equals(method)) {
            return null;
        }
        if (McpProtocol.METHOD_INITIALIZE.equals(method)) {
            String requested = params.optString("protocolVersion", McpProtocol.LATEST);
            String negotiated = McpProtocol.negotiate(requested);
            JSONObject result = new JSONObject();
            try {
                result.put("protocolVersion", negotiated);
                JSONObject capabilities = new JSONObject();
                capabilities.put("tools", new JSONObject());
                capabilities.put("resources", new JSONObject());
                capabilities.put("prompts", new JSONObject());
                result.put("capabilities", capabilities);
                JSONObject info = new JSONObject();
                info.put("name", "sketchpilot");
                info.put("version", "0.1.0");
                result.put("serverInfo", info);
                result.put("instructions",
                        "Use project.*, view.*, logic.* and build.* tools. All writes are backed up.");
            } catch (Throwable ignored) {
                // safe
            }
            return success(id, result);
        }
        if (McpProtocol.METHOD_DISCOVER.equals(method)) {
            JSONObject result = new JSONObject();
            try {
                result.put("protocolVersion", McpProtocol.LATEST);
                result.put("serverInfo", new JSONObject().put("name", "sketchpilot").put("version", "0.1.0"));
            } catch (Throwable ignored) {
                // safe
            }
            return success(id, result);
        }
        if (McpProtocol.METHOD_TOOLS_LIST.equals(method)) {
            JSONArray list = new JSONArray();
            JSONArray schemas = tools.schemas();
            for (int i = 0; i < schemas.length(); i++) {
                JSONObject entry = schemas.optJSONObject(i);
                if (entry == null) {
                    continue;
                }
                JSONObject function = entry.optJSONObject("function");
                if (function == null) {
                    continue;
                }
                String name = function.optString("name", "");
                if (!allowedTools.contains(name)) {
                    continue;
                }
                JSONObject tool = new JSONObject();
                try {
                    tool.put("name", name);
                    tool.put("description", function.optString("description", ""));
                    tool.put("inputSchema", function.optJSONObject("parameters"));
                } catch (Throwable ignored) {
                    // safe
                }
                list.put(tool);
            }
            return success(id, wrap("tools", list));
        }
        if (McpProtocol.METHOD_TOOLS_CALL.equals(method)) {
            String name = params.optString("name", "");
            if (!allowedTools.contains(name)) {
                return error(id, -32602, "tool not allowed: " + name);
            }
            Tool tool = tools.get(name);
            if (tool == null) {
                return error(id, -32602, "unknown tool: " + name);
            }
            JSONObject args = params.optJSONObject("arguments");
            if (args == null) {
                args = new JSONObject();
            }
            try {
                JSONObject result = tool.execute(args);
                JSONObject content = new JSONObject();
                content.put("type", "text");
                content.put("text", result.toString());
                JSONArray array = new JSONArray();
                array.put(content);
                JSONObject out = new JSONObject();
                out.put("content", array);
                out.put("isError", !result.optBoolean("ok", true));
                return success(id, out);
            } catch (Throwable t) {
                return error(id, -32603, "tool failed: " + t.getMessage());
            }
        }
        if (McpProtocol.METHOD_RESOURCES_LIST.equals(method)) {
            return success(id, wrap("resources", listResources()));
        }
        if (McpProtocol.METHOD_RESOURCES_READ.equals(method)) {
            return readResource(id, params.optString("uri", ""));
        }
        if (McpProtocol.METHOD_PROMPTS_LIST.equals(method)) {
            JSONArray prompts = new JSONArray();
            prompts.put(prompt("fix_build_error", "Fix a build error",
                    "Read build diagnostics and propose an IR patch"));
            prompts.put(prompt("create_screen", "Create a screen",
                    "Design a new screen as View IR"));
            prompts.put(prompt("refactor_logic", "Refactor logic",
                    "Explain and refactor a block chain"));
            return success(id, wrap("prompts", prompts));
        }
        if (McpProtocol.METHOD_PROMPTS_GET.equals(method)) {
            String name = params.optString("name", "");
            JSONObject message = new JSONObject();
            try {
                message.put("role", "user");
                message.put("content", wrap("type", "text"));
                message.optJSONObject("content").put("text", "Apply prompt " + name + " to the current project.");
            } catch (Throwable ignored) {
                // safe
            }
            JSONArray messages = new JSONArray();
            messages.put(message);
            return success(id, wrap("messages", messages));
        }
        return error(id, -32601, "method not found: " + method);
    }

    private static JSONObject prompt(String name, String title, String description) {
        JSONObject out = new JSONObject();
        try {
            out.put("name", name);
            out.put("title", title);
            out.put("description", description);
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }

    private JSONArray listResources() {
        JSONArray array = new JSONArray();
        try {
            List<ProjectInfo> projects = repository.listProjects();
            for (int i = 0; i < projects.size(); i++) {
                String id = projects.get(i).getId();
                String[] parts = {"metadata", "view", "logic", "file", "library", "config", "buildlog"};
                for (int j = 0; j < parts.length; j++) {
                    JSONObject resource = new JSONObject();
                    resource.put("uri", "sketchware://project/" + id + "/" + parts[j]);
                    resource.put("name", id + " " + parts[j]);
                    resource.put("mimeType", "application/json");
                    array.put(resource);
                }
            }
        } catch (Throwable ignored) {
            // empty list
        }
        return array;
    }

    private JSONObject readResource(Object id, String uri) {
        try {
            if (!uri.startsWith("sketchware://project/")) {
                return error(id, -32602, "unsupported URI scheme");
            }
            String[] parts = uri.substring("sketchware://project/".length()).split("/");
            if (parts.length != 2) {
                return error(id, -32602, "malformed resource URI");
            }
            String scId = parts[0];
            String text;
            if ("view".equals(parts[1])) {
                SectionedFile file = repository.readSectioned(scId, "view");
                text = file.serialize();
            } else if ("logic".equals(parts[1])) {
                text = repository.readSectioned(scId, "logic").serialize();
            } else if ("file".equals(parts[1])) {
                text = repository.readSectioned(scId, "file").serialize();
            } else if ("library".equals(parts[1])) {
                text = repository.readSectioned(scId, "library").serialize();
            } else if ("config".equals(parts[1])) {
                text = repository.getProjectConfigJson(scId);
            } else if ("buildlog".equals(parts[1])) {
                java.io.File log = new java.io.File(repository.getPaths().root(), "debug.txt");
                text = log.isFile()
                        ? new String(java.nio.file.Files.readAllBytes(log.toPath()), "UTF-8") : "";
            } else {
                ProjectInfo info = repository.getInfo(scId);
                text = info == null ? "" : "sc_id=" + info.getId() + "\nname=" + info.getWorkspaceName();
            }
            JSONObject content = new JSONObject();
            content.put("uri", uri);
            content.put("mimeType", "text/plain");
            content.put("text", text);
            JSONArray contents = new JSONArray();
            contents.put(content);
            return success(id, new JSONObject().put("contents", contents));
        } catch (Throwable t) {
            return error(id, -32603, "resource read failed: " + t.getMessage());
        }
    }

    private JSONObject success(Object id, JSONObject result) {
        JSONObject out = new JSONObject();
        try {
            out.put("jsonrpc", "2.0");
            out.put("id", id);
            out.put("result", result);
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }

    private JSONObject error(Object id, int code, String message) {
        JSONObject out = new JSONObject();
        try {
            out.put("jsonrpc", "2.0");
            out.put("id", id);
            JSONObject error = new JSONObject();
            error.put("code", code);
            error.put("message", message);
            out.put("error", error);
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }

    private void audit(String method, JSONObject response) {
        if (auditFile == null) {
            return;
        }
        java.io.FileWriter writer = null;
        try {
            java.io.File parent = auditFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            writer = new java.io.FileWriter(auditFile, true);
            writer.write(System.currentTimeMillis() + " " + method + " "
                    + (response == null ? "notification" : (response.has("error") ? "error" : "ok")) + "\n");
            writer.flush();
        } catch (Throwable ignored) {
            // audit is best-effort
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }
}
