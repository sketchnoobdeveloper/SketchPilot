package snd.sketchpilot.project;

import java.io.File;
import java.io.FileWriter;

import snd.sketchpilot.core.bridge.BridgeConfig;
import snd.sketchpilot.core.bridge.BridgeTransport;
import snd.sketchpilot.core.bridge.SocketBridgeTransport;

/**
 * Prevents offline writes to a project currently open in a live Sketchware
 * session: checks the in-process registry, the live bridge and lock files.
 */
public final class ConflictGuard {

    private final File lockDir;
    private final BridgeTransport transport;

    public ConflictGuard(File lockDir) {
        this(lockDir, new SocketBridgeTransport(BridgeConfig.HOST, BridgeConfig.PORT));
    }

    public ConflictGuard(File lockDir, BridgeTransport transport) {
        this.lockDir = lockDir;
        this.transport = transport;
    }

    public boolean canWrite(String scId) {
        if (LiveSessionRegistry.isOpen(scId)) {
            return false;
        }
        if (transport != null && transport.isAvailable()) {
            return false;
        }
        return !lockFile(scId).exists();
    }

    public boolean acquire(String scId) {
        if (!canWrite(scId)) {
            return false;
        }
        File file = lockFile(scId);
        FileWriter writer = null;
        try {
            if (!lockDir.exists()) {
                lockDir.mkdirs();
            }
            writer = new FileWriter(file, false);
            writer.write(String.valueOf(System.currentTimeMillis()));
            writer.flush();
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }

    public void release(String scId) {
        lockFile(scId).delete();
    }

    private File lockFile(String scId) {
        return new File(lockDir, scId + ".lock");
    }
}
