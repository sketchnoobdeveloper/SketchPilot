package snd.sketchpilot.core.hook;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import snd.sketchpilot.util.Logger;

/**
 * Declares every live hook the module intends to own and records whether its
 * target resolved in the current host version. Unresolved live hooks degrade
 * to OFFLINE mode; they never crash the host.
 */
public final class CapabilityMatrix {

    private final List<Capability> capabilities = new ArrayList<Capability>();

    public CapabilityMatrix() {
        // Logical names only. Obfuscated names are candidate hints and are always
        // shape-verified by ShapeMatchingResolver.
        fingerprint("project.metadata", "Project metadata registry", "a.a.a.lC")
                .fieldType("a.a.a.DB").hintString("project");
        fingerprint("project.data", "Project data manager (views/logic)", "a.a.a.eC")
                .methodSignature("a(java.lang.StringBuffer)")
                .fieldType("com.besome.sketch.beans.ViewBean");
        fingerprint("project.files", "Project file manager (screens)", "a.a.a.hC")
                .methodSignature("a(java.lang.StringBuffer)")
                .fieldType("com.besome.sketch.beans.ProjectFileBean");
        fingerprint("project.libraries", "Project library manager", "a.a.a.iC")
                .methodSignature("a(java.lang.StringBuffer)")
                .fieldType("com.besome.sketch.beans.ProjectLibraryBean");
        fingerprint("project.crypto", "Encrypted file I/O", "a.a.a.oB")
                .methodSignature("d(java.lang.String)")
                .methodSignature("h(java.lang.String)");
        fingerprint("project.paths", "Build path generator", "a.a.a.yq")
                .methodSignature("getXMLString()");
        fingerprint("build.pipeline", "Compile pipeline", "a.a.a.ProjectBuilder")
                .methodSignature("compileResources()")
                .methodSignature("buildApk()");
        fingerprint("ui.main", "Project list screen", "pro.sketchware.activities.main.activities.MainActivity");
        fingerprint("ui.design", "Designer screen", "com.besome.sketch.design.DesignActivity");
        fingerprint("ui.logic", "Logic editor screen", "com.besome.sketch.editor.LogicEditorActivity");
        fingerprint("ui.settings", "Project settings screen", "com.besome.sketch.projects.MyProjectSettingActivity");
        fingerprint("ui.buildlog", "Build log screen", "com.besome.sketch.tools.CompileLogActivity");
    }

    private Fingerprint fingerprint(String id, String label, String candidate) {
        Fingerprint fp = new Fingerprint(id).hintClass(candidate);
        capabilities.add(new Capability(id, label, candidate));
        RESOLVERS.put(id, fp);
        return fp;
    }

    private static final java.util.Map<String, Fingerprint> RESOLVERS =
            new java.util.HashMap<String, Fingerprint>();

    public static Fingerprint getFingerprint(String id) {
        return RESOLVERS.get(id);
    }

    public List<Capability> getCapabilities() {
        return capabilities;
    }

    public void evaluate(ClassResolver resolver) {
        for (int i = 0; i < capabilities.size(); i++) {
            Capability capability = capabilities.get(i);
            try {
                Class<?> target = resolver.resolve(RESOLVERS.get(capability.getId()));
                if (target != null) {
                    capability.setMode(Capability.MODE_LIVE);
                    capability.setDetail(target.getName());
                } else {
                    capability.setMode(Capability.MODE_OFFLINE);
                    capability.setDetail("target unresolved in this version; offline fallback");
                }
            } catch (Throwable t) {
                capability.setMode(Capability.MODE_OFFLINE);
                capability.setDetail("resolution error: " + t.getClass().getSimpleName());
                Logger.hookFailure("CapabilityMatrix." + capability.getId(), t);
            }
        }
    }

    public String toJson() {
        JSONArray array = new JSONArray();
        for (int i = 0; i < capabilities.size(); i++) {
            Capability c = capabilities.get(i);
            JSONObject row = new JSONObject();
            try {
                row.put("id", c.getId());
                row.put("label", c.getLabel());
                row.put("mode", c.getMode());
                row.put("detail", c.getDetail());
            } catch (Throwable ignored) {
                // JSONObject.put with Strings cannot fail
            }
            array.put(row);
        }
        return array.toString();
    }

    /** Writes a snapshot readable by the companion app's CapabilityActivity. */
    public void saveSnapshot(File snapshotFile) {
        FileWriter writer = null;
        try {
            File parent = snapshotFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            writer = new FileWriter(snapshotFile, false);
            writer.write(toJson());
            writer.flush();
        } catch (Throwable t) {
            Logger.hookFailure("CapabilityMatrix.saveSnapshot", t);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }
}
