package org.intellij.sdk.codesync.ui.notifications;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.Constants.Notification;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.server.CodeSyncReactivateAccountServer;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

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
                        CodeSyncReactivateAccountServer codeSyncReactivateAccountServer = CodeSyncReactivateAccountServer.getInstance();
                        BrowserUtil.browse(codeSyncReactivateAccountServer.getReactivateAccountUrl());
                    } catch (Exception error) {
                        CodeSyncLogger.critical(String.format(
                            "[REACTIVATE_ACCOUNT]: Error while activating the account. \nError: %s",
                            CommonUtils.getStackTrace(error)
                        ));
                    }
                }
            }));
    }

    public void showAlert() {
        this.notificationManager.notifyError(message, project);
    }
}
