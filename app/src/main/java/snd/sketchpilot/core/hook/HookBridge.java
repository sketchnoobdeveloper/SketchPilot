package snd.sketchpilot.core.hook;

import java.util.List;

/**
 * Abstraction over the hooking framework so the module can later support the
 * libxposed API as well as classic Xposed. Implementations must wrap every
 * hook body in try/catch(Throwable) and log through Logger.
 */
public interface HookBridge {

    String getName();

    List<Capability> getCapabilities();

    void activate(HookContext context) throws Throwable;

    void setEnabled(boolean enabled);

    boolean isEnabled();
}
