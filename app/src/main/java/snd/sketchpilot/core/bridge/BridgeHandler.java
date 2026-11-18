package snd.sketchpilot.core.bridge;

/** One request handler registered in the live bridge server. */
public interface BridgeHandler {

    String getType();

    BridgeMessage handle(BridgeMessage message) throws BridgeException;
}
