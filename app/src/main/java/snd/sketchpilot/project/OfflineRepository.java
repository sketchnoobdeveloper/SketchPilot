package snd.sketchpilot.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import snd.sketchpilot.project.crypto.SketchwareCrypto;
import snd.sketchpilot.project.io.SectionedFile;
import snd.sketchpilot.project.model.BlockNode;
import snd.sketchpilot.project.model.ProjectInfo;
import snd.sketchpilot.project.model.ViewNode;
import snd.sketchpilot.util.JsonUtil;

/**
 * Offline, read-only implementation backed directly by project files.
 * Knows nothing about Android UI or Xposed; safe to unit-test on the JVM.
 *
 * P1 scope: reads only. All writes arrive in P2 through transactions that
 * snapshot before mutating (hard constraint: backup before every write).
 */
public final class OfflineRepository implements ProjectRepository {

    private final SketchwarePaths paths;
    private final SketchwareCrypto crypto;

    public OfflineRepository(SketchwarePaths paths, SketchwareCrypto crypto) {
        this.paths = paths;
        this.crypto = crypto;
    }

    public SketchwarePaths getPaths() {
        return paths;
    }

    public SketchwareCrypto getCrypto() {
        return crypto;
    }

    public List<ProjectInfo> listProjects() throws RepositoryException {
        List<ProjectInfo> result = new ArrayList<ProjectInfo>();
        File listDir = paths.list();
        if (!listDir.isDirectory()) {
            return result;
        }
        File[] children = listDir.listFiles();
        if (children == null) {
            return result;
        }
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (!child.isDirectory()) {
                continue;
            }
            try {
                ProjectInfo info = readMetadata(child.getName());
                if (info != null && child.getName().equals(info.getId())) {
                    result.add(info);
                }
            } catch (Throwable t) {
                // One corrupt project must not break enumeration of the others.
            }
        }
        Collections.sort(result, new ProjectInfoComparator());
        return result;
    }

    private static final class ProjectInfoComparator implements java.util.Comparator<ProjectInfo> {
        public int compare(ProjectInfo a, ProjectInfo b) {
            return a.getId().compareTo(b.getId());
        }
    }

    public ProjectInfo getInfo(String scId) throws RepositoryException {
        if (scId == null || scId.length() == 0) {
            throw new RepositoryException("sc_id is required");
        }
        return readMetadata(scId);
    }

    private ProjectInfo readMetadata(String scId) throws RepositoryException {
        File file = paths.projectMetadata(scId);
        if (!file.isFile()) {
            return null;
        }
        String json = readMaybeEncrypted(file);
        return ProjectInfo.fromJson(json);
    }

    public List<String> getLayoutNames(String scId) throws RepositoryException {
        List<String> result = new ArrayList<String>();
        SectionedFile file = readSectioned(scId, "view");
        List<SectionedFile.Section> sections = file.getSections();
        for (int i = 0; i < sections.size(); i++) {
            String name = sections.get(i).getName();
            if (name.length() > 0 && name.endsWith(".xml")) {
                result.add(name);
            }
        }
        return result;
    }

    public List<ViewNode> getViews(String scId, String layoutName) throws RepositoryException {
        List<ViewNode> result = new ArrayList<ViewNode>();
        SectionedFile file = readSectioned(scId, "view");
        List<SectionedFile.Section> sections = file.getSections();
        for (int i = 0; i < sections.size(); i++) {
            SectionedFile.Section section = sections.get(i);
            if (layoutName != null && !section.getName().equals(layoutName)) {
                continue;
            }
            if (!section.getName().endsWith(".xml")) {
                continue;
            }
            List<String> lines = section.getLines();
            for (int j = 0; j < lines.size(); j++) {
                JSONObject object = JsonUtil.tryParseObject(lines.get(j));
                if (object != null) {
                    result.add(new ViewNode(object));
                }
            }
        }
        return result;
    }

    /** Maps section name to block list. Non-JSON lines (variables, lists) are skipped. */
    public Map<String, List<BlockNode>> getLogic(String scId) throws RepositoryException {
        Map<String, List<BlockNode>> result = new LinkedHashMap<String, List<BlockNode>>();
        SectionedFile file = readSectioned(scId, "logic");
        List<SectionedFile.Section> sections = file.getSections();
        for (int i = 0; i < sections.size(); i++) {
            SectionedFile.Section section = sections.get(i);
            List<BlockNode> blocks = new ArrayList<BlockNode>();
            List<String> lines = section.getLines();
            for (int j = 0; j < lines.size(); j++) {
                String line = lines.get(j).trim();
                if (!line.startsWith("{")) {
                    continue;
                }
                JSONObject object = JsonUtil.tryParseObject(line);
                if (object != null && object.has("opCode")) {
                    blocks.add(new BlockNode(object));
                }
            }
            result.put(section.getName(), blocks);
        }
        return result;
    }

    public List<String> getFileSections(String scId) throws RepositoryException {
        return sectionNames(readSectioned(scId, "file"));
    }

    public List<String> getLibrarySections(String scId) throws RepositoryException {
        return sectionNames(readSectioned(scId, "library"));
    }

    private static List<String> sectionNames(SectionedFile file) {
        List<String> names = new ArrayList<String>();
        List<SectionedFile.Section> sections = file.getSections();
        for (int i = 0; i < sections.size(); i++) {
            names.add(sections.get(i).getName());
        }
        return names;
    }

    public String getRootLayoutsJson(String scId) throws RepositoryException {
        return readMaybeEncrypted(paths.dataFile(scId, "view_root"));
    }

    public String getProjectConfigJson(String scId) throws RepositoryException {
        return readMaybeEncrypted(paths.dataFile(scId, "project_config"));
    }

    /** Reused by P2 transactions and P5 tools to parse native sectioned files. */
    public SectionedFile readSectioned(String scId, String fileName) throws RepositoryException {
        File file = paths.dataFile(scId, fileName);
        if (!file.isFile()) {
            return new SectionedFile();
        }
        return SectionedFile.parse(readMaybeEncrypted(file));
    }

    private String readMaybeEncrypted(File file) throws RepositoryException {
        byte[] bytes;
        try {
            bytes = SketchwareCrypto.readAll(new FileInputStream(file));
        } catch (IOException e) {
            throw new RepositoryException("Cannot read " + file.getAbsolutePath(), e);
        }
        if (bytes.length == 0) {
            return "";
        }
        if (!crypto.looksEncrypted(bytes)) {
            try {
                return new String(bytes, "UTF-8");
            } catch (IOException e) {
                throw new RepositoryException("Invalid text in " + file.getAbsolutePath(), e);
            }
        }
        try {
            return crypto.decrypt(bytes);
        } catch (Throwable t) {
            throw new RepositoryException("Cannot decrypt " + file.getAbsolutePath(), t);
        }
    }
}
