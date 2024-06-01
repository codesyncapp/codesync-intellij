package org.intellij.sdk.codesync;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import icons.CodeSyncIcons;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.intellij.sdk.codesync.Constants.Notification.*;

public class NotificationManager {
    NotificationGroup notificationGroup;
    List<AnAction> actions = new ArrayList<>();

    public static NotificationManager getInstance(String notificationGroupId) {
        return new NotificationManager(notificationGroupId);
    }

    public static NotificationManager getInstance() {
        return getInstance(CODESYNC_NOTIFICATION_GROUP);
    }

    private NotificationManager (String notificationGroupId) {
        this.notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(notificationGroupId);
    }

    public NotificationManager addAction(@NotNull AnAction action) {
        this.actions.add(action);
        return this;
    }

    public void notify(String content, NotificationType notificationType) {
        Project project = CommonUtils.getCurrentProject();
        notify(content, notificationType, project);
    }

    private String getTitle(NotificationType notificationType) {
        switch (notificationType) {
            case ERROR:
                return CODESYNC_ERROR_TITLE;
            case INFORMATION:
                return CODESYNC_INFORMATION_TITLE;
            case WARNING:
                return CODESYNC_WARNING_TITLE;
            default:
                return DEFAULT_TITLE;
        }
    }

    public void notify(String content, NotificationType notificationType, Project project) {
        Notification notification = this.notificationGroup
            .createNotification(content, notificationType)
            .setTitle(getTitle(notificationType))
            .setIcon(CodeSyncIcons.codeSyncIcon);

        for (AnAction action : this.actions) {
            notification.addAction(action);
        }
        notification.notify(project);
    }

    public void notifyError(String content) {
        notify(content, NotificationType.ERROR);
    }

    public void notifyError(String content, Project project) {
        notify(content, NotificationType.ERROR, project);
    }

    public void notifyWarning(String content) {
        notify(content, NotificationType.WARNING);
    }

    public void notifyWarning(String content, Project project) {
        notify(content, NotificationType.WARNING, project);
    }

    public void notifyInformation(String content) {
        notify(content, NotificationType.INFORMATION);
    }

    public void notifyInformation(String content, Project project) {
        notify(content, NotificationType.INFORMATION, project);
    }
}
