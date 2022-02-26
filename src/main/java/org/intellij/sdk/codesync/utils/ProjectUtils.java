package org.intellij.sdk.codesync.utils;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;

public class ProjectUtils {
    public static String getRepoPath(VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        VirtualFile moduleContentRoot = projectFileIndex.getContentRootForFile(virtualFile);

        if (moduleContentRoot == null) {
            throw new FileNotInModuleError(String.format(
                    "File '%s' does not belong to the project '%s' index.", virtualFile.getPath(), project.getName()
            ));
        }

        return FileUtils.normalizeFilePath(moduleContentRoot);
    }

    public static String getRepoName(VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        VirtualFile moduleContentRoot = projectFileIndex.getContentRootForFile(virtualFile);

        if (moduleContentRoot == null) {
            throw new FileNotInModuleError(String.format(
                    "File '%s' does not belong to the project '%s' index.", virtualFile.getPath(), project.getName()
            ));
        }

        return moduleContentRoot.getName();
    }

    public static VirtualFile[] getAllContentRoots(Project project) {
        return ProjectRootManager.getInstance(project).getContentRootsFromAllModules();
    }

    public static PluginState getModuleState(VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        String repoPath = getRepoPath(virtualFile, project);
        return StateUtils.getState(repoPath);
    }

    public static String getPluginVersion() {
        String version = "unknown";
        IdeaPluginDescriptor ideaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("com.codesync"));
        if (ideaPluginDescriptor != null) {
            version = ideaPluginDescriptor.getVersion();
        }
        return version;
    }
}
