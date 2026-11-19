package snd.sketchpilot.provider;

import org.json.JSONArray;
import org.json.JSONObject;

/** Google Gemini streamGenerateContent provider (SSE). */
public final class GeminiProvider extends OpenAiCompatibleProvider {

    public GeminiProvider() {
        super("gemini");
    }

    @Override
    protected String endpoint(ProviderRequest request) {
        String base = request.getBaseUrl();
        if (base == null || base.length() == 0) {
            base = "https://generativelanguage.googleapis.com/";
        }
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String key = request.getApiKey() == null ? "" : request.getApiKey();
        return base + "v1beta/models/" + request.getModel()
                + ":streamGenerateContent?alt=sse&key=" + key;
    }

    @Override
    protected void addHeaders(java.net.HttpURLConnection connection, ProviderRequest request) {
        // intentionally empty: Gemini uses the key query parameter
    }

    @Override
    protected JSONObject buildBody(ProviderRequest request) throws Exception {
        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject user = new JSONObject();
        user.put("role", "user");
        JSONArray parts = new JSONArray();
        JSONObject text = new JSONObject();
        text.put("text", request.getUserPrompt());
        parts.put(text);
        user.put("parts", parts);
        contents.put(user);
        body.put("contents", contents);

        JSONObject system = new JSONObject();
        JSONArray systemParts = new JSONArray();
        JSONObject systemText = new JSONObject();
        systemText.put("text", request.getSystemPrompt());
        systemParts.put(systemText);
        system.put("parts", systemParts);
        body.put("systemInstruction", system);

        JSONObject config = new JSONObject();
        config.put("maxOutputTokens", request.getMaxTokens());
        config.put("temperature", request.getTemperature());
        body.put("generationConfig", config);

        JSONArray tools = new JSONArray();
        JSONArray openAiTools = request.getTools();
        for (int i = 0; i < openAiTools.length(); i++) {
            JSONObject function = openAiTools.getJSONObject(i).optJSONObject("function");
            if (function == null) {
                continue;
            }
            JSONObject declaration = new JSONObject();
            declaration.put("name", function.optString("name", ""));
            declaration.put("description", function.optString("description", ""));
            declaration.put("parameters", function.optJSONObject("parameters"));
            JSONArray declarations = new JSONArray();
            declarations.put(declaration);
            JSONObject tool = new JSONObject();
            tool.put("functionDeclarations", declarations);
            tools.put(tool);
        }
        if (tools.length() > 0) {
            body.put("tools", tools);
        }
        return body;
    }

    @Override
    protected void parseChunk(JSONObject root, ProviderCallback callback, boolean[] finished) {
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return;
        }
        JSONObject content = candidates.optJSONObject(0).optJSONObject("content");
        if (content == null) {
            return;
        }
        JSONArray parts = content.optJSONArray("parts");
        if (parts == null) {
            return;
        }
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part == null) { continue; }
            String text = part.optString("text", "");
            if (text.length() > 0) {
                callback.onToken(text);
            }
            JSONObject functionCall = part.optJSONObject("functionCall");
            if (functionCall != null) {
                JSONObject call = new JSONObject();
                try {
                    call.put("function", functionCall);
                } catch (Throwable ignored) {
                    // safe
                }
                callback.onToolCall(call);
            }
        }
    }

}
