package snd.sketchpilot.core.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import snd.sketchpilot.util.Logger;

/**
 * Resolves classes by verifying their shape (field types + method signatures),
 * using obfuscated names only as candidate hints. A name is never trusted on
 * its own: this satisfies the "never hardcode obfuscated names without a
 * resolver fallback" constraint.
 */
public final class ShapeMatchingResolver implements ClassResolver {

    private final ClassLoader loader;
    private final List<String> extraCandidates = new ArrayList<String>();

    public ShapeMatchingResolver(ClassLoader loader) {
        this.loader = loader;
    }

    public void addCandidate(String className) {
        if (className != null && className.length() > 0) {
            extraCandidates.add(className);
        }
    }

    public Class<?> resolve(Fingerprint fingerprint) {
        if (fingerprint == null || !fingerprint.isUsable()) {
            return null;
        }
        List<String> candidates = new ArrayList<String>();
        candidates.addAll(fingerprint.getCandidateClassNames());
        candidates.addAll(extraCandidates);
        for (int i = 0; i < candidates.size(); i++) {
            String name = candidates.get(i);
            Class<?> cls = load(name);
            if (cls != null && matches(cls, fingerprint)) {
                Logger.d("Resolver", "matched " + fingerprint.getLogicalName() + " to " + cls.getName());
                return cls;
            }
        }
        return null;
    }

    private Class<?> load(String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean matches(Class<?> cls, Fingerprint fingerprint) {
        if (cls == null) {
            return false;
        }
        List<String> requiredTypes = fingerprint.getRequiredFieldTypes();
        Field[] fields = cls.getDeclaredFields();
        for (int t = 0; t < requiredTypes.size(); t++) {
            boolean found = false;
            for (int i = 0; i < fields.length; i++) {
                if (typeMatches(fields[i].getType(), requiredTypes.get(t))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        List<String> signatures = fingerprint.getRequiredMethodSignatures();
        Method[] methods = cls.getDeclaredMethods();
        for (int t = 0; t < signatures.size(); t++) {
            boolean found = false;
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].toString().contains(signatures.get(t))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean typeMatches(Class<?> actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.getName().equals(expected) || actual.getSimpleName().equals(expected);
    }

    public String describe() {
        return "shape-matching (" + extraCandidates.size() + " extra candidates)";
    }
}
