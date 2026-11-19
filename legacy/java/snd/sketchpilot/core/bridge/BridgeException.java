package snd.sketchpilot.core.bridge;

public final class BridgeException extends Exception {

    public BridgeException(String message) {
        super(message);
    }

    public BridgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
