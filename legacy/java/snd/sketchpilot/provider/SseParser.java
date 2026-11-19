package snd.sketchpilot.provider;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free SSE framer. Handles comments, multi-line data,
 * event/id fields, CRLF, and the [DONE] sentinel.
 */
public final class SseParser {

    private String event = "";
    private String id = "";
    private final List<String> dataLines = new ArrayList<String>();

    /** Feeds one raw line (without line terminator); returns a completed event or null. */
    public SseEvent feed(String line) {
        String clean = line == null ? "" : stripCr(line);
        if (clean.length() == 0) {
            if (dataLines.isEmpty() && event.length() == 0 && id.length() == 0) {
                return null;
            }
            StringBuilder data = new StringBuilder();
            for (int i = 0; i < dataLines.size(); i++) {
                if (i > 0) {
                    data.append('\n');
                }
                data.append(dataLines.get(i));
            }
            SseEvent result = new SseEvent(event, data.toString(), id);
            reset();
            return result;
        }
        if (clean.startsWith(":")) {
            return null;
        }
        int colon = clean.indexOf(':');
        String field = colon < 0 ? clean : clean.substring(0, colon);
        String value = colon < 0 ? "" : clean.substring(colon + 1);
        if (value.startsWith(" ")) {
            value = value.substring(1);
        }
        if ("event".equals(field)) {
            event = value;
        } else if ("id".equals(field)) {
            id = value;
        } else if ("data".equals(field)) {
            dataLines.add(value);
        }
        return null;
    }

    public SseEvent finish() {
        if (dataLines.isEmpty() && event.length() == 0 && id.length() == 0) {
            return null;
        }
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < dataLines.size(); i++) {
            if (i > 0) {
                data.append('\n');
            }
            data.append(dataLines.get(i));
        }
        SseEvent result = new SseEvent(event, data.toString(), id);
        reset();
        return result;
    }

    private void reset() {
        event = "";
        id = "";
        dataLines.clear();
    }

    private static String stripCr(String value) {
        return value.endsWith("\r") ? value.substring(0, value.length() - 1) : value;
    }
}
