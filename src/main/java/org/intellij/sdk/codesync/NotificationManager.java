package org.intellij.sdk.codesync;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.BuildNumber;
import org.intellij.sdk.codesync.utils.CommonUtils;

public class NotificationManager {
    private static final NotificationGroup NOTIFICATION_GROUP =
            new NotificationGroup("CodeSync Notifications", NotificationDisplayType.BALLOON, true);

    public static void notify(String content, NotificationType notificationType) {
        BuildNumber buildNumber = ApplicationInfo.getInstance().getBuild();
        Project project = CommonUtils.getCurrentProject();
        if (buildNumber.getBaselineVersion() >= 203) {
            NotificationGroupManager.getInstance().getNotificationGroup("CodeSync Notifications")
                    .createNotification(content, notificationType)
                    .notify(project);

        } else {
            NOTIFICATION_GROUP.createNotification(content, NotificationType.ERROR)
                    .notify(project);
        }
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
