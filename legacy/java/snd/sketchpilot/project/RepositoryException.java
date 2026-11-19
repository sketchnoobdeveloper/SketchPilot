package snd.sketchpilot.project;

/** Checked failure raised by any ProjectRepository implementation. */
public final class RepositoryException extends Exception {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
