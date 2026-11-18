package snd.sketchpilot.core.hook;

/** Holds live manager instances captured by constructor hooks in P3. */
public final class ManagerCapture {

    private static volatile Object dataManager;
    private static volatile Object fileManager;
    private static volatile Object libraryManager;
    private static volatile String scId = "";

    private ManagerCapture() {
    }

    public static void capture(String logicalName, Object instance) {
        if (instance == null) {
            return;
        }
        if ("project.data".equals(logicalName)) {
            dataManager = instance;
        } else if ("project.files".equals(logicalName)) {
            fileManager = instance;
        } else if ("project.libraries".equals(logicalName)) {
            libraryManager = instance;
        }
    }

    public static Object getDataManager() {
        return dataManager;
    }

    public static Object getFileManager() {
        return fileManager;
    }

    public static Object getLibraryManager() {
        return libraryManager;
    }

    public static String getScId() {
        return scId;
    }

    public static void setScId(String value) {
        scId = value == null ? "" : value;
    }

    public static void clear() {
        dataManager = null;
        fileManager = null;
        libraryManager = null;
        scId = "";
    }
}
