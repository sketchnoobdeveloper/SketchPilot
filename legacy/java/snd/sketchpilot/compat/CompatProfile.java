package snd.sketchpilot.compat;

/** Per-version facts used by the resolver, crypto and path layers. */
public final class CompatProfile {

    private final String versionName;
    private final int versionCode;
    private final String cryptoKeyLiteral;
    private final boolean cryptoVerified;
    private final String managerDataClassHint;
    private final String notes;

    public CompatProfile(String versionName, int versionCode, String cryptoKeyLiteral,
                         boolean cryptoVerified, String managerDataClassHint, String notes) {
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.cryptoKeyLiteral = cryptoKeyLiteral;
        this.cryptoVerified = cryptoVerified;
        this.managerDataClassHint = managerDataClassHint;
        this.notes = notes;
    }

    public String getVersionName() {
        return versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getCryptoKeyLiteral() {
        return cryptoKeyLiteral;
    }

    public boolean isCryptoVerified() {
        return cryptoVerified;
    }

    public String getManagerDataClassHint() {
        return managerDataClassHint;
    }

    public String getNotes() {
        return notes;
    }
}
