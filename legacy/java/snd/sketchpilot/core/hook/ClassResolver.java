package snd.sketchpilot.core.hook;

/** Resolves a logical target to a concrete host class, or null when unresolved. */
public interface ClassResolver {

    Class<?> resolve(Fingerprint fingerprint);

    String describe();
}
