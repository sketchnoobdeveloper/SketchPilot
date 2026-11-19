package snd.sketchpilot.provider;

/** Local Ollama through its OpenAI-compatible /v1 endpoint. */
public final class OllamaProvider extends OpenAiCompatibleProvider {

    public static final String DEFAULT_BASE = "http://127.0.0.1:11434/v1";

    public OllamaProvider() {
        super("ollama");
    }

    @Override
    protected String endpoint(ProviderRequest request) {
        String base = request.getBaseUrl();
        if (base == null || base.length() == 0) {
            base = DEFAULT_BASE;
        }
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + completionsPath();
    }
}
