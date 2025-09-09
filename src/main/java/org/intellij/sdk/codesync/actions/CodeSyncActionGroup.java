package org.intellij.sdk.codesync.actions;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.intellij.sdk.codesync.state.PluginState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.state.PluginState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetupAction;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import com.intellij.openapi.ui.Messages;
import org.intellij.sdk.codesync.Constants.*;

public class CodeSyncActionGroup extends DefaultActionGroup {

    PluginState pluginState = StateUtils.getGlobalState();

    private VirtualFile getRepoRoot(AnActionEvent anActionEvent, Project project) {
        VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(project);

        if (contentRoots.length > 1) {
            // If more than one module are present in the project then a file must be open to show repo setup action
            // this is needed because without the file we can not determine the correct repo sync.
            VirtualFile virtualFile = anActionEvent.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
            return ProjectUtils.getRepoRoot(virtualFile, project);
        } else if (contentRoots.length == 1) {
            // If there is only one module then we can simply show the repo playback for the only repo present.
            // Disable the button if repo is not in sync.
            return contentRoots[0];
        }

        return null;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        boolean visible = true;

        // Hide group if account is deactivated
        if (StateUtils.getGlobalState().isAccountDeactivated) {
            visible = false;
            // Display alert message
            Messages.showInfoMessage(
                    AlertMessages.ACCOUNT_DEACTIVATED,
                    AlertTitles.CODESYNC
            );
        }

        // Hide group if invalid project path
        if (project == null || (project.getBasePath() != null && project.getBasePath().startsWith("/tmp/"))) {
            visible = false;
        }

        VirtualFile repoRoot = this.getRepoRoot(e, project);
        if (repoRoot == null) {
            visible = false;
        }

        // A single file is opened, no need to sync it.
        if (!repoRoot.isDirectory()) {
            visible = false;
            // Display alert message
            Messages.showInfoMessage(
                    AlertMessages.OPEN_FOLDER,
                    AlertTitles.CODESYNC
            );
        }

        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible);
    }
}
