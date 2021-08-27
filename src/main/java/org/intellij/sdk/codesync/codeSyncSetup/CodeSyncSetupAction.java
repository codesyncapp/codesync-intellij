package org.intellij.sdk.codesync.codeSyncSetup;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.jetbrains.annotations.NotNull;

public class CodeSyncSetupAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        // Using the event, evaluate the context, and enable or disable the action.
        System.out.println("CodeSyncSetup Action:update called.");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Using the event, implement an action. For example, create and show a dialog.
        Project project = e.getProject();
        if (project != null) {
            CodeSyncSetup.setupCodeSyncRepo(project.getBasePath(), project.getName());
        } else {
            CodeSyncLogger.logEvent(
                    "[CODESYNC_SETUP_ACTION] Could not trigger CodeSyncSetupAction because of null value of project."
            );
        }
     }
}
