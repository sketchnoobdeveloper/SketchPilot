package snd.sketchpilot.provider;

import java.net.HttpURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

/** Anthropic Messages API with native tool schema conversion. */
public final class AnthropicProvider extends OpenAiCompatibleProvider {

    public AnthropicProvider() {
        super("anthropic");
    }

    @Override
    protected String completionsPath() {
        return "v1/messages";
    }

    @Override
    protected void addHeaders(HttpURLConnection connection, ProviderRequest request) {
        if (request.getApiKey() != null && request.getApiKey().length() > 0) {
            connection.setRequestProperty("x-api-key", request.getApiKey());
        }
        connection.setRequestProperty("anthropic-version", "2023-06-01");
    }

    @Override
    protected JSONObject buildBody(ProviderRequest request) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", request.getModel());
        body.put("max_tokens", request.getMaxTokens());
        body.put("system", request.getSystemPrompt());
        JSONArray messages = new JSONArray();
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", request.getUserPrompt());
        messages.put(user);
        body.put("messages", messages);
        body.put("stream", request.isStream());

        JSONArray openAiTools = request.getTools();
        if (openAiTools.length() > 0) {
            JSONArray tools = new JSONArray();
            for (int i = 0; i < openAiTools.length(); i++) {
                JSONObject wrapper = openAiTools.getJSONObject(i);
                JSONObject function = wrapper.optJSONObject("function");
                if (function == null) {
                    continue;
                }
                JSONObject tool = new JSONObject();
                tool.put("name", function.optString("name", ""));
                tool.put("description", function.optString("description", ""));
                tool.put("input_schema", function.optJSONObject("parameters"));
                tools.put(tool);
            }
            body.put("tools", tools);
        }
        return body;
    }

    @Override
    protected void parseChunk(JSONObject root, ProviderCallback callback, boolean[] finished) {
        String type = root.optString("type", "");
        if ("message_stop".equals(type)) {
            finished[0] = true;
            return;
        }
        if ("content_block_delta".equals(type)) {
            JSONObject delta = root.optJSONObject("delta");
            if (delta != null) {
                String text = delta.optString("text", "");
                if (text.length() > 0) {
                    callback.onToken(text);
                }
                JSONObject toolUse = delta.optJSONObject("tool_use");
                if (toolUse != null) {
                    JSONObject call = new JSONObject();
                    try {
                        call.put("id", toolUse.optString("id", ""));
                        call.put("function", toolUse);
                    } catch (Throwable ignored) {
                        // safe
                    }
                    callback.onToolCall(call);
                }
            }
        }
    }
}
