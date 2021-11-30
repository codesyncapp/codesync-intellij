package org.intellij.sdk.codesync.codeSyncSetup;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.exceptions.base.BaseException;
import org.intellij.sdk.codesync.exceptions.base.BaseNetworkException;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.jetbrains.annotations.NotNull;

import static org.intellij.sdk.codesync.Constants.*;

public class CodeSyncSetupAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        // Using the event, evaluate the context, and enable or disable the action.
        System.out.println("CodeSyncSetup Action:update called.");
        PluginState pluginState = StateUtils.getState(e.getProject());
        if (pluginState != null && pluginState.isRepoInSync) {
            Presentation presentation = e.getPresentation();
            presentation.setText("Disconnect Repo");
            presentation.setDescription("Disconnect repo.");
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Using the event, implement an action. For example, create and show a dialog.
        Project project = e.getProject();
        PluginState pluginState = StateUtils.getState(project);

        if (project != null) {
            if (pluginState != null && pluginState.isRepoInSync) {
                try {
                    CodeSyncSetup.disconnectRepo(project);
                } catch (BaseException | BaseNetworkException error) {
                    NotificationManager.notifyError(Notification.REPO_UNSYNC_FAILED, project);
                    NotificationManager.notifyError(error.getMessage(), project);
                }
            } else {
                CodeSyncSetup.setupCodeSyncRepoAsync(project, true);
            }
        } else {
            CodeSyncLogger.logEvent(
                    "[CODESYNC_SETUP_ACTION] Could not trigger CodeSyncSetupAction because of null value of project."
            );
        }
     }
}
