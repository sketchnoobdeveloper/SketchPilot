package snd.sketchpilot.agent;

import org.json.JSONObject;

/** A tool exposed to the agent and to MCP clients. */
public interface Tool {

    String RISK_READ = "read";
    String RISK_WRITE = "write";
    String RISK_DESTRUCTIVE = "destructive";
    String RISK_SYSTEM = "system";

    String getName();

    String getDescription();

    JSONObject getSchema();

    String getRisk();

    boolean supportsDryRun();

    /** False for tools that are catalogued but not implemented (never advertised as working). */
    default boolean isAvailable() {
        return true;
    }

    JSONObject execute(JSONObject arguments) throws Exception;
}
