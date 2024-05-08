package org.intellij.sdk.codesync.auth;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.actions.BaseModuleAction;
import org.intellij.sdk.codesync.commands.ClearReposToIgnoreCache;
import org.intellij.sdk.codesync.commands.ReloadStateCommand;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.server.CodeSyncReactivateAccountServer;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;


import java.sql.SQLException;

import static org.intellij.sdk.codesync.Constants.Notification.ACCOUNT_REACTIVATE_BUTTON;

public class AuthAction extends BaseModuleAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project ==  null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        Presentation presentation = e.getPresentation();
        if (this.isAccountDeactivated()) {
            presentation.setText(ACCOUNT_REACTIVATE_BUTTON);
            presentation.setDescription("Reactivate account to resume syncing.");
            return;
        }

        PluginState pluginState = StateUtils.getGlobalState();
        if (pluginState.isAuthenticated) {
            presentation.setText(
                pluginState.userEmail == null ? "Logout": String.format("Logout %s", pluginState.userEmail)
            );
            presentation.setDescription("Use a different account.");
        } else {
            presentation.setText("Login");
            presentation.setDescription("Login to start syncing.");
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            NotificationManager.getInstance().notifyError("An error occurred trying to perform authentication action.");
            CodeSyncLogger.warning("An error occurred trying to perform authentication action. e.getProject() is null.");

            return;
        }

        if (this.isAccountDeactivated()) {
            try {
                CodeSyncReactivateAccountServer codeSyncReactivateAccountServer = CodeSyncReactivateAccountServer.getInstance();
                BrowserUtil.browse(codeSyncReactivateAccountServer.getReactivateAccountUrl());
            } catch (Exception error) {
                CodeSyncLogger.critical(String.format(
                    "[REACTIVATE_ACCOUNT]: Error while activating the account. %nError: %s",
                    CommonUtils.getStackTrace(error)
                ));
            }
            return;
        }

        PluginState pluginState = StateUtils.getGlobalState();

        try {
            if (pluginState.isAuthenticated) {
                // Clear any cache that depends on user authentication status.
                new ClearReposToIgnoreCache().execute();
                BrowserUtil.browse(CodeSyncAuthServer.getInstance().getLogoutURL());
            } else {
                CodeSyncLogger.debug("[INTELLIJ_AUTH]: User initiated login flow.");
                BrowserUtil.browse(CodeSyncAuthServer.getInstance().getLoginURL());
            }
            CodeSyncAuthServer.registerPostAuthCommand(new ReloadStateCommand(project));
        } catch (Exception exc) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLIJ_AUTH]: An error occurred during user authentication. Error: %s",
                    CommonUtils.getStackTrace(exc)
                ),
                pluginState.userEmail
            );
        }
    }
}
