package org.intellij.sdk.codesync.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.RepoStatus;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;


public abstract class BaseModuleAction extends AnAction {
    public RepoStatus getRepoStatus(VirtualFile moduleRoot) {
        String repoPath = FileUtils.normalizeFilePath(moduleRoot.getPath());
        return StateUtils.getRepoStatus(repoPath);
    }

    public boolean isRepoInSync (VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        PluginState pluginState = ProjectUtils.getModuleState(virtualFile, project);
        return pluginState != null && pluginState.isRepoInSync;
    }

    public boolean isRepoInSync (VirtualFile moduleRoot) {
        String repoPath = FileUtils.normalizeFilePath(moduleRoot.getPath());
        PluginState pluginState = StateUtils.getState(repoPath);
        return pluginState != null && pluginState.isRepoInSync;
    }

    public boolean isAccountDeactivated() {
        PluginState pluginState = StateUtils.getGlobalState();
        return pluginState.isAccountDeactivated;
    }
}
