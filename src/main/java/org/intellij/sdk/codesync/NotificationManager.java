package org.intellij.sdk.codesync;

import com.intellij.diagnostic.ReportMessages;
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
        ReportMessages.GROUP.createNotification(
                "CodeSync notification",
                content,
                notificationType,
                NotificationListener.URL_OPENING_LISTENER
        ).setImportant(false).notify(project);
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
