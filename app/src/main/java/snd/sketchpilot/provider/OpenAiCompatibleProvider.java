package snd.sketchpilot.provider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * OpenAI-compatible SSE provider. Covers OpenAI, OpenRouter and Ollama's
 * /v1 endpoint by changing baseUrl. HttpURLConnection only, no dependencies.
 */
public class OpenAiCompatibleProvider implements LlmProvider {

    private final String name;

    public OpenAiCompatibleProvider() {
        this("openai-compatible");
    }

    public OpenAiCompatibleProvider(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected String completionsPath() {
        return "chat/completions";
    }

    protected String endpoint(ProviderRequest request) {
        String base = request.getBaseUrl();
        if (base == null || base.length() == 0) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + completionsPath();
    }

    protected void addHeaders(HttpURLConnection connection, ProviderRequest request) {
        if (request.getApiKey() != null && request.getApiKey().length() > 0) {
            connection.setRequestProperty("Authorization", "Bearer " + request.getApiKey());
        }
    }

    protected JSONObject buildBody(ProviderRequest request) throws Exception {
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", request.getSystemPrompt());
        messages.put(system);
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", request.getUserPrompt());
        messages.put(user);
        body.put("model", request.getModel());
        body.put("messages", messages);
        body.put("stream", request.isStream());
        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());
        if (request.getTools().length() > 0) {
            body.put("tools", request.getTools());
            body.put("tool_choice", "auto");
        }
        return body;
    }

    public void stream(ProviderRequest request, ProviderCallback callback) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint(request)).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(90000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "text/event-stream");
            addHeaders(connection, request);

            Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            try {
                writer.write(buildBody(request).toString());
                writer.flush();
            } finally {
                writer.close();
            }

            int status = connection.getResponseCode();
            if (status >= 400) {
                String error = readError(connection);
                callback.onError(new IllegalStateException("HTTP " + status + ": " + error));
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            SseParser parser = new SseParser();
            final boolean[] finished = {false};
            try {
                String line;
                while (!finished[0] && (line = reader.readLine()) != null) {
                    SseEvent event = parser.feed(line);
                    if (event != null) {
                        handle(event, callback, finished);
                    }
                }
                SseEvent tail = parser.finish();
                if (tail != null) {
                    handle(tail, callback, finished);
                }
            } finally {
                reader.close();
            }
            callback.onDone();
        } catch (Throwable t) {
            callback.onError(t);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected void handle(SseEvent event, ProviderCallback callback, boolean[] finished) {
        if (event.isDone()) {
            finished[0] = true;
            return;
        }
        String data = event.getData();
        if (data.length() == 0) {
            return;
        }
        try {
            parseChunk(new JSONObject(data), callback, finished);
        } catch (Throwable t) {
            callback.onError(t);
        }
    }

    protected void parseChunk(JSONObject root, ProviderCallback callback, boolean[] finished) {
        try {
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return;
            }
            JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
            if (delta == null) {
                return;
            }
            String token = delta.optString("content", "");
            if (token.length() > 0 && !"null".equals(token)) {
                callback.onToken(token);
            }
            JSONArray calls = delta.optJSONArray("tool_calls");
            if (calls != null) {
                for (int i = 0; i < calls.length(); i++) {
                    callback.onToolCall(calls.getJSONObject(i));
                }
            }
            String reason = choices.getJSONObject(0).optString("finish_reason", "");
            if ("stop".equals(reason) || "tool_calls".equals(reason)) {
                finished[0] = true;
            }
        } catch (Throwable t) {
            callback.onError(t);
        }
    }

    private static String readError(HttpURLConnection connection) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            return out.toString();
        } catch (Throwable t) {
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }

    public JSONObject testConnection(String apiKey, String model, String baseUrl) throws Exception {
        ProviderRequest request = new ProviderRequest();
        request.setApiKey(apiKey);
        request.setModel(model);
        request.setBaseUrl(baseUrl);
        request.setSystemPrompt("Reply with OK.");
        request.setUserPrompt("ping");
        request.setStream(false);
        final StringBuilder response = new StringBuilder();
        final Throwable[] failure = {null};
        stream(request, new ProviderCallback() {
            public void onToken(String token) {
                response.append(token);
            }

            public void onToolCall(JSONObject toolCall) {
                // no tools in a connection test
            }

            public void onError(Throwable error) {
                failure[0] = error;
            }

            public void onDone() {
                // nothing
            }
        });
        JSONObject result = new JSONObject();
        if (failure[0] != null) {
            result.put("ok", false);
            result.put("error", String.valueOf(failure[0].getMessage()));
        } else {
            result.put("ok", true);
            result.put("preview", response.substring(0, Math.min(200, response.length())));
        }
        return result;
    }
}
