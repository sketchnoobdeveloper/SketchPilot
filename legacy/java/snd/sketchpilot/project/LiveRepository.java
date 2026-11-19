package snd.sketchpilot.project;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import snd.sketchpilot.core.bridge.ReflectBeans;
import snd.sketchpilot.core.hook.ManagerCapture;
import snd.sketchpilot.project.model.BlockNode;
import snd.sketchpilot.project.model.ProjectInfo;
import snd.sketchpilot.project.model.ViewNode;
import snd.sketchpilot.util.JsonUtil;

/**
 * Live implementation backed by Sketchware Pro's in-memory managers.
 * Whenever a manager is not available (unresolved hook, wrong version, host
 * closed) the call degrades to the offline repository; it never throws into
 * the host process.
 */
public final class LiveRepository implements ProjectRepository {

    private final OfflineRepository fallback;

    public LiveRepository(OfflineRepository fallback) {
        this.fallback = fallback;
    }

    public List<ProjectInfo> listProjects() throws RepositoryException {
        return fallback.listProjects();
    }

    public ProjectInfo getInfo(String scId) throws RepositoryException {
        return fallback.getInfo(scId);
    }

    public List<String> getLayoutNames(String scId) throws RepositoryException {
        Map<?, ?> views = readFieldMap(ManagerCapture.getDataManager(), "c");
        if (views != null && !views.isEmpty()) {
            List<String> result = new ArrayList<String>();
            Iterator<?> keys = views.keySet().iterator();
            while (keys.hasNext()) {
                result.add(String.valueOf(keys.next()));
            }
            return result;
        }
        return fallback.getLayoutNames(scId);
    }

    public List<ViewNode> getViews(String scId, String layoutName) throws RepositoryException {
        Map<?, ?> views = readFieldMap(ManagerCapture.getDataManager(), "c");
        if (views != null) {
            List<ViewNode> result = new ArrayList<ViewNode>();
            Iterator<?> keys = views.keySet().iterator();
            while (keys.hasNext()) {
                Object key = keys.next();
                if (layoutName != null && !String.valueOf(key).equals(layoutName)) {
                    continue;
                }
                Object value = views.get(key);
                if (value instanceof java.util.Collection<?>) {
                    java.util.Iterator<?> beans = ((java.util.Collection<?>) value).iterator();
                    while (beans.hasNext()) {
                        JSONObject json = ReflectBeans.toJson(beans.next());
                        result.add(new ViewNode(json));
                    }
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return fallback.getViews(scId, layoutName);
    }

    public Map<String, List<BlockNode>> getLogic(String scId) throws RepositoryException {
        Map<?, ?> outer = readFieldMap(ManagerCapture.getDataManager(), "d");
        if (outer != null && !outer.isEmpty()) {
            Map<String, List<BlockNode>> result = new LinkedHashMap<String, List<BlockNode>>();
            Iterator<?> fileKeys = outer.keySet().iterator();
            while (fileKeys.hasNext()) {
                Object innerValue = outer.get(fileKeys.next());
                if (!(innerValue instanceof Map<?, ?>)) {
                    continue;
                }
                Map<?, ?> sections = (Map<?, ?>) innerValue;
                Iterator<?> sectionKeys = sections.keySet().iterator();
                while (sectionKeys.hasNext()) {
                    Object sectionKey = sectionKeys.next();
                    Object blocks = sections.get(sectionKey);
                    List<BlockNode> converted = new ArrayList<BlockNode>();
                    if (blocks instanceof java.util.Collection<?>) {
                        java.util.Iterator<?> it = ((java.util.Collection<?>) blocks).iterator();
                        while (it.hasNext()) {
                            converted.add(new BlockNode(ReflectBeans.toJson(it.next())));
                        }
                    }
                    result.put(String.valueOf(sectionKey), converted);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return fallback.getLogic(scId);
    }

    public List<String> getFileSections(String scId) throws RepositoryException {
        Object files = ManagerCapture.getFileManager();
        java.util.Collection<?> list = (java.util.Collection<?>) readField(files, "c");
        if (list != null && !list.isEmpty()) {
            List<String> result = new ArrayList<String>();
            result.add("activity");
            return result;
        }
        return fallback.getFileSections(scId);
    }

    public List<String> getLibrarySections(String scId) throws RepositoryException {
        Object libs = ManagerCapture.getLibraryManager();
        if (libs != null) {
            List<String> result = new ArrayList<String>();
            String[] names = {"c", "d", "e", "f"};
            for (int i = 0; i < names.length; i++) {
                Object bean = readField(libs, names[i]);
                if (bean != null) {
                    result.add(JsonUtil.optString(ReflectBeans.toJson(bean), "name", names[i]));
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return fallback.getLibrarySections(scId);
    }

    public String getRootLayoutsJson(String scId) throws RepositoryException {
        return fallback.getRootLayoutsJson(scId);
    }

    public String getProjectConfigJson(String scId) throws RepositoryException {
        return fallback.getProjectConfigJson(scId);
    }

    public boolean hasLiveData() {
        return ManagerCapture.getDataManager() != null;
    }

    private static Map<?, ?> readFieldMap(Object target, String name) {
        Object value = readField(target, name);
        return value instanceof Map<?, ?> ? (Map<?, ?>) value : null;
    }

    private static Object readField(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field field = findField(target.getClass(), name);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field findField(Class<?> cls, String name) {
        Class<?> current = cls;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
