package snd.sketchpilot.core.bridge;

/** Transport-agnostic IPC contract. P3 supplies the real binder/socket implementation. */
public interface BridgeTransport {

    boolean isAvailable();

    BridgeMessage call(BridgeMessage message) throws BridgeException;
}
