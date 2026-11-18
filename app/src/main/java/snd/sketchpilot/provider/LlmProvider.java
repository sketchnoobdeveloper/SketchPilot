package snd.sketchpilot.provider;

import org.json.JSONObject;

/** Pluggable LLM provider. Implementations use HttpURLConnection only. */
public interface LlmProvider {

    String getName();

    void stream(ProviderRequest request, ProviderCallback callback) throws Exception;

    JSONObject testConnection(String apiKey, String model, String baseUrl) throws Exception;
}
