package snd.sketchpilot.provider;

import org.json.JSONArray;

/** Provider-agnostic request description. */
public final class ProviderRequest {

    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private String systemPrompt = "";
    private String userPrompt = "";
    private JSONArray tools = new JSONArray();
    private int maxTokens = 4096;
    private double temperature = 0.2;
    private boolean stream = true;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public JSONArray getTools() {
        return tools;
    }

    public void setTools(JSONArray tools) {
        this.tools = tools == null ? new JSONArray() : tools;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
}
