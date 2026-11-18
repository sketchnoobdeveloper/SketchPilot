package snd.sketchpilot.agent;

import org.json.JSONObject;

/** UI hook so the engine stays Android-free; the app supplies a Handler bridge. */
public interface AgentUi {

    void post(Runnable work);

    void onEvent(JSONObject event);

    boolean approve(String toolName, String risk, String diffPreview);
}
