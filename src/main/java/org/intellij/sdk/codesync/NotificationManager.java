package org.intellij.sdk.codesync;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class NotificationManager {
    public static void notify(String content, NotificationType notificationType) {
        Project project = Utils.getCurrentProject();
        NotificationGroupManager.getInstance().getNotificationGroup("CodeSync Notifications")
                .createNotification(content, notificationType)
                .notify(project);
    }

    public static void notifyError(String content) {
        notify(content, NotificationType.ERROR);
    }

    public static void notifyWarning(String content) {
        notify(content, NotificationType.WARNING);
    }

    public static void notifyInformation(String content) {
        notify(content, NotificationType.INFORMATION);
    }
}
