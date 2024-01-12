package org.intellij.sdk.codesync.ui.notifications;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.apache.http.client.utils.URIBuilder;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.Constants.Notification;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.server.CodeSyncServer;
import org.intellij.sdk.codesync.server.servlets.ReactivateAccountHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import static org.intellij.sdk.codesync.Constants.SETTINGS_PAGE_URL;

public class DeactivatedAccountNotification {
    String message = Notification.ACCOUNT_DEACTIVATED;
    String buttonText = Notification.ACCOUNT_REACTIVATE_BUTTON;

    private final Project project;

    private final NotificationManager notificationManager;

    public DeactivatedAccountNotification (Project project) {
        this.project = project;
        this.notificationManager = NotificationManager
            .getInstance()
            .addActions(Collections.singletonList(new AnAction(buttonText) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    try {
                        // Start the server.
                        CodeSyncServer reactivateAccountServer = CodeSyncServer
                            .getInstance()
                            .addPathMapping("/reactivate-callback", ReactivateAccountHandler.class);
                        reactivateAccountServer.start();

                        String callbackURL = String.format("%sreactivate-callback", reactivateAccountServer.getServerURL());

                        URIBuilder uriBuilder = new URIBuilder(SETTINGS_PAGE_URL);
                        uriBuilder.addParameter("callback", callbackURL);

                        // Redirect the user to settings page.
                        BrowserUtil.browse(uriBuilder.build().toURL());
                    } catch (Exception error) {
                        CodeSyncLogger.critical(String.format(
                            "[REACTIVATE_ACCOUNT]: Error while activating the account. \nError: %s", error.getMessage()
                        ));
                    }
                }
            }));
    }

    public void showAlert() {
        this.notificationManager.notifyError(message, project);
    }
}
