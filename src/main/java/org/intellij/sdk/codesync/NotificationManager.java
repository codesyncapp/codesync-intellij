package org.intellij.sdk.codesync;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;

public class NotificationManager {
    public static void notify(String content, NotificationType notificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("CodeSync Notifications")
                .createNotification(content, notificationType)
                .notify(null);
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
