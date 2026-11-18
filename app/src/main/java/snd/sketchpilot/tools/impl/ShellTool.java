package snd.sketchpilot.tools.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.tools.Schemas;

/**
 * ProcessBuilder-only shell tool. OFF by default; enable by writing an
 * allowlist file. Never uses Runtime.exec.
 */
public final class ShellTool implements Tool {

    private static final java.io.File ALLOWLIST =
            new java.io.File("/storage/emulated/0/.sketchware/sketchpilot/shell_allowlist.txt");

    public String getName() {
        return "shell.exec";
    }

    public String getDescription() {
        return "Run an allowlisted command via ProcessBuilder (off by default)";
    }

    public JSONObject getSchema() {
        return Schemas.object("command", "arg");
    }

    public String getRisk() {
        return RISK_SYSTEM;
    }

    public boolean supportsDryRun() {
        return true;
    }

    public JSONObject execute(JSONObject arguments) throws Exception {
        JSONObject out = new JSONObject();
        String command = arguments.optString("command", "");
        if (!isEnabled() || !isAllowed(command)) {
            out.put("ok", false);
            out.put("error", "shell tool disabled or command not allowlisted");
            return out;
        }
        List<String> commandLine = new ArrayList<String>();
        commandLine.add(command);
        String arg = arguments.optString("arg", "");
        if (arg.length() > 0) {
            commandLine.add(arg);
        }
        ProcessBuilder builder = new ProcessBuilder(commandLine);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        int exit = process.waitFor();
        out.put("ok", exit == 0);
        out.put("exit", exit);
        out.put("output", text.substring(0, Math.min(4000, text.length())));
        return out;
    }

    private static boolean isEnabled() {
        return ALLOWLIST.isFile();
    }

    private static boolean isAllowed(String command) {
        if (!ALLOWLIST.isFile()) {
            return false;
        }
        try {
            List<String> lines = java.nio.file.Files.readAllLines(ALLOWLIST.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String allowed = lines.get(i).trim();
                if (allowed.length() > 0 && !allowed.startsWith("#") && allowed.equals(command)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // deny
        }
        return false;
    }
}
