package org.intellij.sdk.codesync.auth;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ide.BrowserUtil;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.jetbrains.annotations.NotNull;


public class AuthAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        // Using the event, evaluate the context, and enable or disable the action.
        System.out.println("Auth Action:update called.");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Using the event, implement an action. For example, create and show a dialog.
        CodeSyncAuthServer server;
        try {
            server =  CodeSyncAuthServer.getInstance();
            BrowserUtil.browse(server.getAuthorizationUrl());
        } catch (Exception exc) {
            exc.printStackTrace();
            CodeSyncLogger.logEvent(
                    "[INTELLIJ_AUTH_ERROR]: IntelliJ Login Error, an error occurred during user authentication."
            );
        }
    }
}
