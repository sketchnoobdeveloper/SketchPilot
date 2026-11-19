package snd.sketchpilot.mcp.protocol;

/** MCP protocol constants. Latest stable revision verified 2026-11-18. */
public final class McpProtocol {

    public static final String LATEST = "2026-07-28";
    public static final String[] SUPPORTED = {
            LATEST, "2025-11-25", "2025-06-18", "2025-03-26"
    };

    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_INITIALIZED = "notifications/initialized";
    public static final String METHOD_DISCOVER = "server/discover";
    public static final String METHOD_TOOLS_LIST = "tools/list";
    public static final String METHOD_TOOLS_CALL = "tools/call";
    public static final String METHOD_RESOURCES_LIST = "resources/list";
    public static final String METHOD_RESOURCES_READ = "resources/read";
    public static final String METHOD_PROMPTS_LIST = "prompts/list";
    public static final String METHOD_PROMPTS_GET = "prompts/get";

    private McpProtocol() {
    }

    public static boolean isSupported(String version) {
        if (version == null) {
            return false;
        }
        for (int i = 0; i < SUPPORTED.length; i++) {
            if (SUPPORTED[i].equals(version)) {
                return true;
            }
        }
        return false;
    }

    public static String negotiate(String requested) {
        return isSupported(requested) ? requested : LATEST;
    }
}
