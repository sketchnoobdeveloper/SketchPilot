package snd.sketchpilot.core.hook;

import java.util.ArrayList;
import java.util.List;

/**
 * Version-independent class fingerprint. Obfuscated class names are only
 * candidate hints; a candidate is accepted only when its shape matches.
 */
public final class Fingerprint {

    private final String logicalName;
    private final List<String> candidateClassNames = new ArrayList<String>();
    private final List<String> requiredFieldTypes = new ArrayList<String>();
    private final List<String> requiredMethodSignatures = new ArrayList<String>();
    private final List<String> hintStrings = new ArrayList<String>();

    public Fingerprint(String logicalName) {
        this.logicalName = logicalName;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public Fingerprint hintClass(String name) {
        candidateClassNames.add(name);
        return this;
    }

    public Fingerprint fieldType(String typeName) {
        requiredFieldTypes.add(typeName);
        return this;
    }

    public Fingerprint methodSignature(String signature) {
        requiredMethodSignatures.add(signature);
        return this;
    }

    public Fingerprint hintString(String value) {
        hintStrings.add(value);
        return this;
    }

    public List<String> getCandidateClassNames() {
        return candidateClassNames;
    }

    public List<String> getRequiredFieldTypes() {
        return requiredFieldTypes;
    }

    public List<String> getRequiredMethodSignatures() {
        return requiredMethodSignatures;
    }

    public List<String> getHintStrings() {
        return hintStrings;
    }

    /** True when at least one identifying signal exists. */
    public boolean isUsable() {
        return !candidateClassNames.isEmpty() || !requiredFieldTypes.isEmpty()
                || !requiredMethodSignatures.isEmpty() || !hintStrings.isEmpty();
    }
}
