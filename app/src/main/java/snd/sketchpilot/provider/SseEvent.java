package snd.sketchpilot.provider;

/** One Server-Sent Event. */
public final class SseEvent {

    private final String event;
    private final String data;
    private final String id;

    public SseEvent(String event, String data, String id) {
        this.event = event == null ? "" : event;
        this.data = data == null ? "" : data;
        this.id = id == null ? "" : id;
    }

    public String getEvent() {
        return event;
    }

    public String getData() {
        return data;
    }

    public String getId() {
        return id;
    }

    public boolean isDone() {
        return "[DONE]".equals(data.trim());
    }
}
