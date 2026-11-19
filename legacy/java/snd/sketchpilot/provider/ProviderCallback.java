package snd.sketchpilot.provider;

import org.json.JSONObject;

/** Streamed provider events, delivered on the caller's thread. */
public interface ProviderCallback {

    void onToken(String token);

    void onToolCall(JSONObject toolCall);

    void onError(Throwable error);

    void onDone();
}
