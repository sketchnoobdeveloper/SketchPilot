package snd.sketchpilot.core.hook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import snd.sketchpilot.util.Logger;

/**
 * DexKit-based resolver with a mandatory shape-verified fallback.
 *
 * DexKit is optional at runtime and is accessed reflectively so that no heavy
 * compile-time dependency is introduced (hard constraint: no heavy deps,
 * Xposed API compileOnly). When DexKit is unavailable, or when its string-based
 * search is defeated by StringFog, the shape-matching resolver decides.
 */
public final class DexKitResolver implements ClassResolver {

    private static final String[] BRIDGE_CLASSES = {
            "org.lsposed.dexkit.DexKitBridge",
            "de.robv.android.xposed.dexkit.DexKitBridge"
    };

    private final ClassResolver fallback;
    private final Object bridge;
    private Method findClassMethod;

    public DexKitResolver(ClassLoader loader, String apkPath, ClassResolver fallback) {
        this.fallback = fallback;
        Object created = null;
        for (int i = 0; i < BRIDGE_CLASSES.length && created == null; i++) {
            created = createBridge(BRIDGE_CLASSES[i], apkPath, loader);
        }
        this.bridge = created;
        if (created != null) {
            findClassMethod = findMethod(created.getClass(), "findClass");
        }
    }

    private static Object createBridge(String className, String apkPath, ClassLoader loader) {
        try {
            Class<?> cls = Class.forName(className, false, loader);
            Method create = null;
            try {
                create = cls.getMethod("create", String.class);
            } catch (NoSuchMethodException ignored) {
                try {
                    create = cls.getMethod("create");
                } catch (NoSuchMethodException ignored2) {
                    return null;
                }
            }
            Object result;
            if (create.getParameterTypes().length == 1) {
                result = create.invoke(null, apkPath);
            } else {
                result = create.invoke(null);
            }
            Logger.d("Resolver", "DexKit bridge created: " + className);
            return result;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method findMethod(Class<?> cls, String name) {
        Method[] methods = cls.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name) && methods[i].getParameterTypes().length > 0) {
                return methods[i];
            }
        }
        return null;
    }

    public Class<?> resolve(Fingerprint fingerprint) {
        if (bridge != null && findClassMethod != null) {
            List<String> hints = fingerprint.getHintStrings();
            for (int i = 0; i < hints.size(); i++) {
                try {
                    Object found = findClassMethod.invoke(bridge, hints.get(i));
                    Class<?> cls = toClass(found);
                    if (cls != null && ShapeMatchingResolver.matches(cls, fingerprint)) {
                        return cls;
                    }
                } catch (Throwable t) {
                    Logger.hookFailure("DexKitResolver.findClass", t);
                }
            }
        }
        return fallback == null ? null : fallback.resolve(fingerprint);
    }

    private static Class<?> toClass(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Class<?>) {
            return (Class<?>) value;
        }
        // DexKit result wrappers usually expose getClassInstance()/getClass().
        Method[] methods = value.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            String name = methods[i].getName();
            if (("getClassInstance".equals(name) || "asClass".equals(name))
                    && methods[i].getParameterTypes().length == 0) {
                try {
                    Object inner = methods[i].invoke(value);
                    if (inner instanceof Class<?>) {
                        return (Class<?>) inner;
                    }
                } catch (Throwable ignored) {
                    // keep looking
                }
            }
        }
        return null;
    }

    public boolean isDexKitActive() {
        return bridge != null;
    }

    public String describe() {
        return "DexKit=" + (bridge != null ? "active" : "absent") + ", fallback=" + fallback.describe();
    }

    /** Helper used by capability evaluation to flatten a result list. */
    public static List<Class<?>> toClassList(List<Object> values) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        for (int i = 0; i < values.size(); i++) {
            result.add(toClass(values.get(i)));
        }
        return result;
    }
}
