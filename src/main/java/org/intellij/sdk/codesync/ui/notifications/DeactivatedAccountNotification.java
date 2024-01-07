package org.intellij.sdk.codesync.ui.notifications;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.Constants.Notification;
import org.intellij.sdk.codesync.NotificationManager;
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
            .setTitle(Notification.DEFAULT_TITLE, Notification.ACCOUNT_DEACTIVATED_SHORT_TITLE)
            .addActions(Collections.singletonList(new AnAction(buttonText) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    System.out.println("Activated Account here.");
                }
            }));
    }

    public void showAlert() {
        this.notificationManager.notifyError(message, project);
    }
}
