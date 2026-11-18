package snd.sketchpilot.provider;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pluggable provider factory and per-project model profiles. */
public final class ProviderRegistry {

    private final Map<String, LlmProvider> providers = new LinkedHashMap<String, LlmProvider>();

    public ProviderRegistry() {
        register(new OpenAiCompatibleProvider("openai"));
        register(new OpenAiCompatibleProvider("openrouter"));
        register(new AnthropicProvider());
        register(new GeminiProvider());
        register(new OllamaProvider());
    }

    public void register(LlmProvider provider) {
        providers.put(provider.getName(), provider);
    }

    public LlmProvider get(String name) {
        LlmProvider provider = providers.get(name);
        return provider == null ? providers.get("openai") : provider;
    }

    public Map<String, LlmProvider> all() {
        return providers;
    }

    /** Default base URLs used when a profile does not override them. */
    public static String defaultBaseUrl(String providerName) {
        if ("openrouter".equals(providerName)) {
            return "https://openrouter.ai/api/v1";
        }
        if ("openai".equals(providerName)) {
            return "https://api.openai.com/v1";
        }
        if ("anthropic".equals(providerName)) {
            return "https://api.anthropic.com/";
        }
        if ("gemini".equals(providerName)) {
            return "https://generativelanguage.googleapis.com/";
        }
        if ("ollama".equals(providerName)) {
            return OllamaProvider.DEFAULT_BASE;
        }
        return "";
    }
}
