package snd.sketchpilot.agent;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.blocks.BlockSpecRegistry;
import snd.sketchpilot.provider.LlmProvider;
import snd.sketchpilot.provider.ProviderCallback;
import snd.sketchpilot.provider.ProviderRegistry;
import snd.sketchpilot.provider.ProviderRequest;
import snd.sketchpilot.util.Redactor;
import snd.sketchpilot.view.WidgetRegistry;

/**
 * plan, act, observe, verify, reflect loop with step/token budgets,
 * approval gates and bounded auto-fix cycles.
 */
public final class AgentLoop {

    private final ProviderRegistry providers;
    private final ToolRegistry tools;
    private final ContextEngine context;
    private final BlockSpecRegistry specs;
    private final WidgetRegistry widgets;
    private final AgentUi ui;

    private String providerName = "openai";
    private String model = "gpt-4o-mini";
    private String apiKey = "";
    private String baseUrl = "";
    private String approvalMode = ApprovalMode.AUTO_SAFE;
    private int maxSteps = 8;
    private int maxFixCycles = 3;

    public AgentLoop(ProviderRegistry providers, ToolRegistry tools, ContextEngine context,
                     BlockSpecRegistry specs, WidgetRegistry widgets, AgentUi ui) {
        this.providers = providers;
        this.tools = tools;
        this.context = context;
        this.specs = specs;
        this.widgets = widgets;
        this.ui = ui;
    }

    public void configure(String providerName, String model, String apiKey, String baseUrl) {
        this.providerName = providerName;
        this.model = model;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public void setApprovalMode(String approvalMode) {
        this.approvalMode = approvalMode;
    }

    public void setBudgets(int maxSteps, int maxFixCycles) {
        this.maxSteps = maxSteps;
        this.maxFixCycles = maxFixCycles;
    }

    public JSONObject run(String userPrompt) {
        LlmProvider provider = providers.get(providerName);
        StringBuilder transcript = new StringBuilder(Redactor.redactText(userPrompt));
        StringBuilder answer = new StringBuilder();
        int fixCycles = 0;

        for (int step = 0; step < maxSteps; step++) {
            ProviderRequest request = new ProviderRequest();
            request.setBaseUrl(baseUrl.length() > 0 ? baseUrl : ProviderRegistry.defaultBaseUrl(providerName));
            request.setApiKey(apiKey);
            request.setModel(model);
            request.setSystemPrompt(context.buildSystemPrompt(specs, widgets));
            request.setUserPrompt(transcript.toString() + "\n\n" + context.buildContext(6000));
            request.setTools(tools.schemas());
            request.setMaxTokens(2048);

            final StringBuilder tokens = new StringBuilder();
            final JSONArray calls = new JSONArray();
            final Throwable[] failure = {null};
            try {
                provider.stream(request, new ProviderCallback() {
                    public void onToken(String token) {
                        tokens.append(token);
                        emit("token", token);
                    }

                    public void onToolCall(JSONObject toolCall) {
                        calls.put(toolCall);
                    }

                    public void onError(Throwable error) {
                        failure[0] = error;
                    }

                    public void onDone() {
                        // nothing
                    }
                });
            } catch (Throwable t) {
                failure[0] = t;
            }

            if (failure[0] != null) {
                emit("error", String.valueOf(failure[0].getMessage()));
                return result(false, "provider error: " + failure[0].getMessage(), answer.toString());
            }

            if (tokens.length() > 0) {
                answer.setLength(0);
                answer.append(tokens);
                context.addSummary(tokens.toString());
                transcript.append("\nASSISTANT: ").append(tokens);
            }

            if (calls.length() == 0) {
                emit("done", "finished");
                return result(true, "done", answer.toString());
            }

            for (int i = 0; i < calls.length(); i++) {
                JSONObject parsed = parseCall(calls.optJSONObject(i));
                String name = parsed.optString("name", "");
                JSONObject arguments = parsed.optJSONObject("arguments");
                if (arguments == null) {
                    JSONObject empty = new JSONObject();
                    JSONObject error = new JSONObject();
                    try {
                        error.put("error", "invalid tool arguments");
                        empty.put("raw", error);
                    } catch (Throwable ignored) {
                        // safe
                    }
                    arguments = empty;
                }
                emit("tool_call", name + " " + Redactor.redactJson(arguments).toString());

                Tool tool = tools.get(name);
                if (tool == null) {
                    transcript.append("\nTOOL_RESULT ").append(name).append(": unknown tool");
                    continue;
                }

                if (ApprovalMode.requiresApproval(approvalMode, tool.getRisk())) {
                    String diff = Redactor.redactJson(arguments).toString();
                    boolean approved = ui.approve(name, tool.getRisk(), diff);
                    if (!approved) {
                        transcript.append("\nTOOL_RESULT ").append(name).append(": rejected by user");
                        continue;
                    }
                }

                JSONObject toolResult;
                try {
                    toolResult = tool.execute(arguments);
                } catch (Throwable t) {
                    toolResult = new JSONObject();
                    try {
                        toolResult.put("error", String.valueOf(t.getMessage()));
                    } catch (Throwable ignored) {
                        // safe
                    }
                }
                JSONObject safeResult = Redactor.redactJson(toolResult);
                transcript.append("\nTOOL_RESULT ").append(name).append(": ").append(safeResult);
                emit("tool_result", name + " " + safeResult.toString());

                if (safeResult.has("error")) {
                    fixCycles++;
                    if (fixCycles >= maxFixCycles) {
                        emit("done", "auto-fix budget exhausted");
                        return result(false, "auto-fix budget exhausted", answer.toString());
                    }
                }
            }
        }
        emit("done", "step budget exhausted");
        return result(false, "step budget exhausted", answer.toString());
    }

    private static JSONObject parseCall(JSONObject call) {
        JSONObject out = new JSONObject();
        if (call == null) {
            return out;
        }
        JSONObject function = call.optJSONObject("function");
        try {
            if (function != null) {
                out.put("name", function.optString("name", ""));
                String raw = function.optString("arguments", "");
                JSONObject args = raw.length() > 0 ? new JSONObject(raw) : function.optJSONObject("args");
                out.put("arguments", args == null ? new JSONObject() : args);
            } else {
                out.put("name", call.optString("name", ""));
                out.put("arguments", call.optJSONObject("args"));
            }
        } catch (Throwable t) {
            // malformed JSON arguments: return empty object for self-repair
        }
        return out;
    }

    private void emit(String type, String payload) {
        if (ui == null) {
            return;
        }
        final JSONObject event = new JSONObject();
        try {
            event.put("type", type);
            event.put("payload", payload);
        } catch (Throwable ignored) {
            // safe
        }
        ui.onEvent(event);
    }

    private static JSONObject result(boolean success, String message, String answer) {
        JSONObject out = new JSONObject();
        try {
            out.put("ok", success);
            out.put("message", message);
            out.put("answer", answer);
        } catch (Throwable ignored) {
            // safe
        }
        return out;
    }
}
