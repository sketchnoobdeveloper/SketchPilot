package snd.sketchpilot.core.hook;

/** Immutable context handed to every HookBridge implementation. */
public final class HookContext {

    private final ClassLoader classLoader;
    private final String packageName;
    private final int versionCode;
    private final String versionKey;
    private final String apkPath;
    private final ClassResolver resolver;

    public HookContext(ClassLoader classLoader, String packageName, int versionCode,
                       String versionKey, String apkPath, ClassResolver resolver) {
        this.classLoader = classLoader;
        this.packageName = packageName;
        this.versionCode = versionCode;
        this.versionKey = versionKey;
        this.apkPath = apkPath;
        this.resolver = resolver;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionKey() {
        return versionKey;
    }

    public String getApkPath() {
        return apkPath;
    }

    public ClassResolver getResolver() {
        return resolver;
    }
}
