package snd.sketchpilot.compat;

import snd.sketchpilot.project.crypto.SketchwareCrypto;

/** Returns a crypto instance for a host version, refusing unverified writes. */
public final class CryptoRegistry {

    private final CompatMatrix matrix;

    public CryptoRegistry(CompatMatrix matrix) {
        this.matrix = matrix;
    }

    public SketchwareCrypto forVersion(int versionCode) {
        CompatProfile profile = matrix.forVersion(versionCode);
        return new SketchwareCrypto(profile.getCryptoKeyLiteral());
    }

    /** Offline writes should require a verified profile unless explicitly overridden. */
    public boolean allowWrite(int versionCode, boolean userOverride) {
        CompatProfile profile = matrix.forVersion(versionCode);
        return profile.isCryptoVerified() || userOverride;
    }
}
