package org.intellij.sdk.codesync.auth;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.apache.http.client.utils.URIBuilder;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.jetbrains.annotations.NotNull;

import static org.intellij.sdk.codesync.Constants.CODESYNC_LOGOUT_URL;

// TODO: remove this if we decide to never use this action.
public class LogoutAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        // Using the event, evaluate the context, and enable or disable the action.
        System.out.println("Logout Action:update called.");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Using the event, implement an action. For example, create and show a dialog.
        CodeSyncAuthServer server = null;
        try {
            server =  CodeSyncAuthServer.getInstance();
            URIBuilder uriBuilder = new URIBuilder(CODESYNC_LOGOUT_URL);
            uriBuilder.addParameter("redirect_uri", server.getServerURL());
            BrowserUtil.browse(uriBuilder.toString());
        } catch (Exception exc) {
            exc.printStackTrace();
            CodeSyncLogger.logEvent(
                    "[INTELLIJ_AUTH_ERROR]: IntelliJ Logout Error, an error occurred during user logout flow."
            );
        }
    }

}
