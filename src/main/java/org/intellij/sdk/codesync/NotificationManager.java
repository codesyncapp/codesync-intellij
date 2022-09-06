package org.intellij.sdk.codesync;

import com.intellij.diagnostic.ReportMessages;
//import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.utils.CommonUtils;

public class NotificationManager {
    public static void notify(String content, NotificationType notificationType) {
        Project project = CommonUtils.getCurrentProject();
        notify(content, notificationType, project);
    }

    public static void notify(String content, NotificationType notificationType, Project project) {
        // TODO: jetbrains does not allow even conditional use of new API and raises compatibilty errors.
        // TODO: uncomment this when we aree ready to drop support for IDEs older than March 2020
//        if (CommonUtils.isIDEOlderOrEqual(2020, 3)) {
//            // 2020.3 or older, ref: https://plugins.jetbrains.com/docs/intellij/notifications.html#35da3371
//            NotificationGroupManager.getInstance()
//                    .getNotificationGroup("CodeSync Notifications")
//                    .createNotification(content, NotificationType.ERROR)
//                    .notify(project);
//        } else {
            // Pre 2020.3, ref: https://plugins.jetbrains.com/docs/intellij/notifications.html#35da3371
        ReportMessages.GROUP.createNotification(
                "CodeSync notification",
                content,
                notificationType,
                NotificationListener.URL_OPENING_LISTENER
        ).setImportant(false).notify(project);
//        }
    }

    public static void notifyError(String content) {
        notify(content, NotificationType.ERROR);
    }

    public static void notifyError(String content, Project project) {
        notify(content, NotificationType.ERROR, project);
    }

    public static void notifyWarning(String content) {
        notify(content, NotificationType.WARNING);
    }


    public static void notifyWarning(String content, Project project) {
        notify(content, NotificationType.WARNING, project);
    }

    public static void notifyInformation(String content) {
        notify(content, NotificationType.INFORMATION);
    }

    public static void notifyInformation(String content, Project project) {
        notify(content, NotificationType.INFORMATION, project);
    }
}
