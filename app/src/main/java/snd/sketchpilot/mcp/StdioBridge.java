package snd.sketchpilot.mcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import snd.sketchpilot.mcp.protocol.McpDispatcher;

/** stdio MCP bridge for desktop clients that cannot reach loopback HTTP. */
public final class StdioBridge {

    private StdioBridge() {
    }

    public static void run(McpDispatcher dispatcher) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(System.out);
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }
            String response = dispatcher.dispatch(line);
            if (response != null) {
                writer.println(response);
                writer.flush();
            }
        }
    }
}
