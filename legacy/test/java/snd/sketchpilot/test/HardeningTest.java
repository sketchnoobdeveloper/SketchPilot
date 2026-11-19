package snd.sketchpilot.test;

import java.io.File;

import snd.sketchpilot.compat.CompatMatrix;
import snd.sketchpilot.compat.CryptoRegistry;
import snd.sketchpilot.core.bridge.BridgeException;
import snd.sketchpilot.core.bridge.BridgeMessage;
import snd.sketchpilot.core.bridge.BridgeTransport;
import snd.sketchpilot.project.ConflictGuard;
import snd.sketchpilot.project.LiveSessionRegistry;
import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.util.AuditLog;

/** Tests for compatibility gating, conflict safety and audit logging. */
public final class HardeningTest {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        CompatMatrix matrix = new CompatMatrix();
        check("v7.1.0 profile verified", matrix.isVerified(150));
        check("unknown version not verified", !matrix.isVerified(9999));
        CryptoRegistry cryptoRegistry = new CryptoRegistry(matrix);
        check("write denied on unknown version", !cryptoRegistry.allowWrite(9999, false));
        check("write allowed with override", cryptoRegistry.allowWrite(9999, true));

        byte[] metadata = SketchwareCrypto.readAll(new java.io.FileInputStream(
                "/sdcard/.sketchware/mysc/list/602/project"));
        String json = cryptoRegistry.forVersion(150).decrypt(metadata);
        check("versioned crypto decrypts real file", json.contains("\"sc_id\":\"602\""));

        File lockDir = new File("/tmp/opencode/p7-locks");
        delete(lockDir);
        ConflictGuard guard = new ConflictGuard(lockDir, new FakeTransport(false));
        check("can write when idle", guard.canWrite("777"));
        check("acquire lock", guard.acquire("777"));
        check("lock blocks second writer", !guard.canWrite("777"));
        guard.release("777");
        check("release restores write", guard.canWrite("777"));

        ConflictGuard liveGuard = new ConflictGuard(lockDir, new FakeTransport(true));
        check("live bridge blocks offline write", !liveGuard.canWrite("777"));

        LiveSessionRegistry.open("777");
        check("live session blocks offline write", !guard.canWrite("777"));
        LiveSessionRegistry.close("777");

        File audit = new File("/tmp/opencode/p7-audit.log");
        audit.delete();
        new AuditLog(audit).record("test.write", "project 777");
        check("audit log written", audit.isFile() && audit.length() > 0);

        System.out.println("HardeningTest: " + passed + " assertions passed.");
    }

    private static final class FakeTransport implements BridgeTransport {
        private final boolean available;

        FakeTransport(boolean available) {
            this.available = available;
        }

        public boolean isAvailable() {
            return available;
        }

        public BridgeMessage call(BridgeMessage message) throws BridgeException {
            return null;
        }
    }

    private static void delete(File file) {
        if (!file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                delete(children[i]);
            }
        }
        file.delete();
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("  ok - " + name);
    }
}
