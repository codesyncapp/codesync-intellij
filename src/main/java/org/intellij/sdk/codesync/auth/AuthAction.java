package org.intellij.sdk.codesync.auth;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.commands.ClearReposToIgnoreCache;
import org.intellij.sdk.codesync.commands.ReloadStateCommand;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.models.UserAccount;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.jetbrains.annotations.NotNull;

public class AuthAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project ==  null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        PluginState pluginState = StateUtils.getGlobalState();
        Presentation presentation = e.getPresentation();
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

        PluginState pluginState = StateUtils.getGlobalState();

        CodeSyncAuthServer server;
        try {
            server =  CodeSyncAuthServer.getInstance();
            String targetURL = server.getAuthorizationUrl();
            if (pluginState.isAuthenticated) {
                UserAccount userAccount;
                try{
                    userAccount = new UserAccount();
                }catch (SQLiteDBConnectionError error){
                    NotificationManager.getInstance().notifyError(
                            "An error occurred trying to logout the user, please tyr again later.", project
                    );
                    CodeSyncLogger.error(
                            String.format("[INTELLIJ_AUTH_ERROR]:  SQLite Database Connection Error, %s", error.getMessage())
                    );
                    return;
                }

                // Clear any cache that depends on user authentication status.
                new ClearReposToIgnoreCache().execute();

                try{
                    userAccount.makeAllUsersInActive();
                } catch (SQLiteDBConnectionError error){
                    NotificationManager.getInstance().notifyError(
                            "An error occurred trying to logout the user, please tyr again later.", project
                    );
                    CodeSyncLogger.error(
                            String.format("[INTELLIJ_AUTH_ERROR]: Could not write to database due to database connection error. Error: %s", error.getMessage())
                    );
                    return;
                } catch (SQLiteDataError error){
                    NotificationManager.getInstance().notifyError(
                            "An error occurred trying to logout the user, please tyr again later.", project
                    );
                    CodeSyncLogger.error(
                            String.format("[INTELLIJ_AUTH_ERROR]: Could not write to database due to SQL error. Error: %s", error.getMessage())
                    );
                    return;
                }

                // Reload the state now.
                StateUtils.reloadState(project);

                NotificationManager.getInstance().notifyInformation(
                        "You have been logged out successfully.", project
                );
            } else {
                CodeSyncLogger.debug(
                        "[INTELLIJ_AUTH]: User initiated login flow."
                );
                BrowserUtil.browse(targetURL);
                CodeSyncAuthServer.registerPostAuthCommand(new ReloadStateCommand(project));
            }

        } catch (Exception exc) {
            exc.printStackTrace();
            CodeSyncLogger.critical(
                String.format("[INTELLIJ_AUTH_ERROR]: IntelliJ Login Error, an error occurred during user authentication. Error: %s", exc.getMessage()),
                pluginState.userEmail
            );
        }
    }
}
