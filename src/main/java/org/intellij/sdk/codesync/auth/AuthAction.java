package org.intellij.sdk.codesync.auth;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.Presentation;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.commands.ReloadStateCommand;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.jetbrains.annotations.NotNull;

import static org.intellij.sdk.codesync.Constants.CODESYNC_LOGOUT_URL;


public class AuthAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        PluginState pluginState = StateUtils.getGlobalState();
        if (pluginState.isAuthenticated) {
            Presentation presentation = e.getPresentation();
            presentation.setText("Logout");
            presentation.setDescription("Use a different account.");
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PluginState pluginState = StateUtils.getGlobalState();

        CodeSyncAuthServer server;
        try {
            server =  CodeSyncAuthServer.getInstance();
            String targetURL = server.getAuthorizationUrl();
            if (pluginState.isAuthenticated) {
                targetURL = CODESYNC_LOGOUT_URL;
            }
            BrowserUtil.browse(targetURL);
            CodeSyncAuthServer.registerPostAuthCommand(new ReloadStateCommand(e.getProject()));
        } catch (Exception exc) {
            exc.printStackTrace();
            CodeSyncLogger.logEvent(
                "[INTELLIJ_AUTH_ERROR]: IntelliJ Login Error, an error occurred during user authentication."
            );
        }
    }
}
