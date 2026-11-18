package snd.sketchpilot.project;

import java.util.List;
import java.util.Map;

import snd.sketchpilot.project.model.BlockNode;
import snd.sketchpilot.project.model.ProjectInfo;
import snd.sketchpilot.project.model.ViewNode;

/**
 * One interface, two implementations: LiveRepository (hooks) and
 * OfflineRepository (direct file I/O). Only read operations in P1; P2 extends
 * this interface with transactional mutations.
 */
public interface ProjectRepository {

    List<ProjectInfo> listProjects() throws RepositoryException;

    ProjectInfo getInfo(String scId) throws RepositoryException;

    List<String> getLayoutNames(String scId) throws RepositoryException;

    List<ViewNode> getViews(String scId, String layoutName) throws RepositoryException;

    Map<String, List<BlockNode>> getLogic(String scId) throws RepositoryException;

    List<String> getFileSections(String scId) throws RepositoryException;

    List<String> getLibrarySections(String scId) throws RepositoryException;

    String getRootLayoutsJson(String scId) throws RepositoryException;

    String getProjectConfigJson(String scId) throws RepositoryException;
}
