package org.intellij.sdk.codesync.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;


public abstract class BaseModuleAction extends AnAction {
    public boolean isRepoInSync (VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        PluginState pluginState = ProjectUtils.getModuleState(virtualFile, project);
        return pluginState != null && pluginState.isRepoInSync;
    }

    public boolean isRepoInSync (VirtualFile moduleRoot) {
        PluginState pluginState = StateUtils.getState(moduleRoot.getPath());
        return pluginState != null && pluginState.isRepoInSync;
    }
}