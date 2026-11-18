package snd.sketchpilot.agent;

/** Approval policy for tool execution. */
public final class ApprovalMode {

    public static final String ASK_EVERY_TIME = "ask_every_time";
    public static final String AUTO_SAFE = "auto_safe";
    public static final String AUTO_ALL = "auto_all_with_checkpoints";

    private ApprovalMode() {
    }

    public static boolean requiresApproval(String mode, String toolRisk) {
        if (mode == null) {
            return true;
        }
        if (ASK_EVERY_TIME.equals(mode)) {
            return true;
        }
        if (AUTO_ALL.equals(mode)) {
            return Tool.RISK_DESTRUCTIVE.equals(toolRisk) || Tool.RISK_SYSTEM.equals(toolRisk);
        }
        // AUTO_SAFE: reads and reversible writes run without prompting.
        return Tool.RISK_DESTRUCTIVE.equals(toolRisk) || Tool.RISK_SYSTEM.equals(toolRisk);
    }
}
