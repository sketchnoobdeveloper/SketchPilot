package snd.sketchpilot.test;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.agent.AgentLoop;
import snd.sketchpilot.agent.AgentUi;
import snd.sketchpilot.agent.ApprovalMode;
import snd.sketchpilot.agent.ContextEngine;
import snd.sketchpilot.agent.DiffPreview;
import snd.sketchpilot.agent.Tool;
import snd.sketchpilot.agent.ToolRegistry;
import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.provider.LlmProvider;
import snd.sketchpilot.provider.ProviderCallback;
import snd.sketchpilot.provider.ProviderRegistry;
import snd.sketchpilot.provider.ProviderRequest;
import snd.sketchpilot.provider.SseEvent;
import snd.sketchpilot.provider.SseParser;
import snd.sketchpilot.util.Redactor;
import snd.sketchpilot.view.WidgetRegistry;

/** Pure-Java tests for SSE parsing, redaction, diff, context and the agent loop. */
public final class AgentTest {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        // SSE framing
        SseParser parser = new SseParser();
        check(parser.feed("event: message") == null, "event field buffered");
        check(parser.feed("data: {\"a\":1}") == null, "data buffered");
        SseEvent event = parser.feed("");
        check(event != null && "message".equals(event.getEvent()), "event type parsed");
        check("{\"a\":1}".equals(event.getData()), "data parsed");
        SseParser doneParser = new SseParser();
        doneParser.feed("data: [DONE]");
        SseEvent done = doneParser.feed("");
        check(done != null && done.isDone(), "DONE sentinel detected");

        // Redaction
        JSONObject secret = new JSONObject();
        secret.put("apiKey", "sk-abcdefghijklmnop");
        secret.put("name", "safe");
        JSONObject redacted = Redactor.redactJson(secret);
        check(Redactor.MASK.equals(redacted.optString("apiKey")), "JSON key redacted");
        check("safe".equals(redacted.optString("name")), "safe field preserved");
        check(Redactor.redactText("Bearer sk-12345678901234").contains(Redactor.MASK), "bearer token redacted");

        // Diff
        List<String> diff = DiffPreview.generate("a\nb\nc", "a\nx\nc");
        check(diff.contains("- b") && diff.contains("+ x"), "diff shows removal and addition");
        check("a\nx\nc".equals(DiffPreview.apply("a\nb\nc", diff)), "diff applies cleanly");

        // Context engine
        ContextEngine context = new ContextEngine();
        context.setLayer(ContextEngine.PROJECT, "project-info");
        context.setLayer(ContextEngine.VIEW_TREE, "view-tree");
        String built = context.buildContext(12);
        check(built.contains("project-info"), "budget keeps first layer");
        check(!built.contains("view-tree"), "budget drops later layer");
        BlockSpecRegistry specs = new BlockSpecRegistry();
        String system = context.buildSystemPrompt(specs, new WidgetRegistry());
        check(system.contains("addSourceDirectly"), "system prompt embeds opCodes");
        check(system.contains("recyclerview"), "system prompt embeds widgets");

        // Agent loop with a mock provider
        final ToolRegistry tools = new ToolRegistry();
        tools.register(new Tool() {
            public String getName() {
                return "echo";
            }

            public String getDescription() {
                return "echo a value";
            }

            public JSONObject getSchema() {
                JSONObject schema = new JSONObject();
                try {
                    schema.put("type", "object");
                } catch (Throwable ignored) {
                    // safe
                }
                return schema;
            }

            public String getRisk() {
                return RISK_READ;
            }

            public boolean supportsDryRun() {
                return true;
            }

            public JSONObject execute(JSONObject arguments) throws Exception {
                JSONObject out = new JSONObject();
                out.put("echo", arguments.optString("value", ""));
                return out;
            }
        });

        ProviderRegistry providers = new ProviderRegistry();
        providers.register(new MockProvider());
        final List<String> events = new ArrayList<String>();
        AgentUi ui = new AgentUi() {
            public void post(Runnable work) {
                work.run();
            }

            public void onEvent(JSONObject event) {
                events.add(event.optString("type"));
            }

            public boolean approve(String toolName, String risk, String diffPreview) {
                return true;
            }
        };
        AgentLoop loop = new AgentLoop(providers, tools, context, specs, new WidgetRegistry(), ui);
        loop.configure("mock", "mock-model", "secret", "http://localhost");
        loop.setApprovalMode(ApprovalMode.AUTO_SAFE);
        loop.setBudgets(2, 2);
        JSONObject result = loop.run("say hello");
        check(result.has("ok"), "agent loop returned a result");
        check(events.contains("tool_call"), "tool call event emitted");
        check(events.contains("tool_result"), "tool result event emitted");

        System.out.println("AgentTest: " + passed + " assertions passed.");
    }

    private static final class MockProvider implements LlmProvider {
        public String getName() {
            return "mock";
        }

        public void stream(ProviderRequest request, ProviderCallback callback) throws Exception {
            callback.onToken("hello");
            JSONObject args = new JSONObject();
            args.put("value", "x");
            JSONObject function = new JSONObject();
            function.put("name", "echo");
            function.put("arguments", args.toString());
            JSONObject call = new JSONObject();
            call.put("function", function);
            callback.onToolCall(call);
            callback.onDone();
        }

        public JSONObject testConnection(String apiKey, String model, String baseUrl) {
            JSONObject out = new JSONObject();
            try {
                out.put("ok", true);
            } catch (Throwable ignored) {
                // safe
            }
            return out;
        }
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("  ok - " + name);
    }
}
