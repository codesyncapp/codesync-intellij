package org.intellij.sdk.codesync.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;

public class ProjectUtils {
    public static String getRepoPath(VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        VirtualFile moduleContentRoot = projectFileIndex.getContentRootForFile(virtualFile);

        if (moduleContentRoot == null) {
            throw new FileNotInModuleError(String.format(
                    "File '%s' does not belong to the project '%s' index.", virtualFile.getPath(), project.getName()
            ));
        }

        return CommonUtils.formatPath(moduleContentRoot);
    }
}