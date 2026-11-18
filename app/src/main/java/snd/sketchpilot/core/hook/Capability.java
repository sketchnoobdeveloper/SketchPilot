package snd.sketchpilot.core.hook;

/** One row of the Capability Matrix screen. */
public final class Capability {

    public static final String MODE_LIVE = "LIVE";
    public static final String MODE_OFFLINE = "OFFLINE";
    public static final String MODE_UNAVAILABLE = "UNAVAILABLE";

    private final String id;
    private final String label;
    private final String logicalTarget;
    private String mode = MODE_UNAVAILABLE;
    private String detail = "";

    public Capability(String id, String label, String logicalTarget) {
        this.id = id;
        this.label = label;
        this.logicalTarget = logicalTarget;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLogicalTarget() {
        return logicalTarget;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isLive() {
        return MODE_LIVE.equals(mode);
    }
}
