package snd.sketchpilot.compat;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility matrix. Only v7.1.0/code150 is empirically verified; other
 * rows are explicitly marked unverified and must be re-probed at runtime.
 */
public final class CompatMatrix {

    private final List<CompatProfile> profiles = new ArrayList<CompatProfile>();

    public CompatMatrix() {
        profiles.add(new CompatProfile("v7.1.0", 150, "sketchwaresecure", true,
                "a.a.a.eC", "verified: source + on-device projects 601/602 + bytecode"));
        profiles.add(new CompatProfile("v7.0.0-beta", 0, "sketchwaresecure", false,
                "a.a.a.eC", "hypothesis: key and hints inherited; re-verify before write"));
        profiles.add(new CompatProfile("legacy/unknown", -1, "sketchwaresecure", false,
                "", "unknown fork: resolver must use shape fingerprints only"));
    }

    public CompatProfile forVersion(int versionCode) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getVersionCode() == versionCode) {
                return profiles.get(i);
            }
        }
        return profiles.get(profiles.size() - 1);
    }

    public List<CompatProfile> all() {
        return profiles;
    }

    public boolean isVerified(int versionCode) {
        return forVersion(versionCode).isCryptoVerified();
    }
}
