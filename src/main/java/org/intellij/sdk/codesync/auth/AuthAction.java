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
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.UserFile;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;

import static org.intellij.sdk.codesync.Constants.*;


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
            NotificationManager.notifyError("An error occurred trying to perform authentication action.");
            CodeSyncLogger.warning("An error occurred trying to perform authentication action. e.getProject() is null.");

            return;
        }

        PluginState pluginState = StateUtils.getGlobalState();

        CodeSyncAuthServer server;
        try {
            server =  CodeSyncAuthServer.getInstance();
            String targetURL = server.getAuthorizationUrl();
            if (pluginState.isAuthenticated) {
                UserFile userFile;
                try {
                    userFile = new UserFile(USER_FILE_PATH);
                } catch (FileNotFoundException error) {
                    NotificationManager.notifyError(
                            "An error occurred trying to logout the user, please tyr again later.", project
                    );
                    CodeSyncLogger.error(
                            String.format("[INTELLIJ_AUTH_ERROR]: auth file not found. Error: %s", error.getMessage())
                    );
                    return;
                } catch (InvalidYmlFileError error) {
                    error.printStackTrace();
                    NotificationManager.notifyError(
                            "An error occurred trying to logout the user, please tyr again later.", project
                    );
                    CodeSyncLogger.critical(
                            String.format("[INTELLIJ_AUTH_ERROR]: Invalid auth file. Error: %s", error.getMessage()),
                            pluginState.userEmail
                    );
                    // Could not read user file.
                    return;
                }
                userFile.makeAllUsersInActive();

                // Clear any cache that depends on user authentication status.
                new ClearReposToIgnoreCache().execute();
                try {
                    userFile.writeYml();
                } catch (FileNotFoundException | InvalidYmlFileError error) {
                    NotificationManager.notifyError(
                            "An error occurred trying to logout the user, please tyr again later.", project
                    );
                    CodeSyncLogger.error(
                            String.format("[INTELLIJ_AUTH_ERROR]: Could write to auth file. Error: %s", error.getMessage())
                    );
                    return;
                }
                // Reload the state now.
                StateUtils.reloadState(project);

                NotificationManager.notifyInformation(
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
