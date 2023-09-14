package org.intellij.sdk.codesync.alerts;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.Constants;

import java.util.ArrayList;

import static org.intellij.sdk.codesync.Constants.*;

public class ActivityAlertNotification {

    private static Notification notification;

    public static void showAlert(String teamActivityURL, Project project, String email) {

        if(notification != null && !notification.isExpired()){
            return;
        }

        ArrayList<ActivityAlertActions> actions = new ArrayList<>();
        actions.add(new ActivityAlertActions(VIEW_ACTIVITY, project, email, teamActivityURL));
        actions.add(new ActivityAlertActions(REMIND_LATER, project, email, teamActivityURL));
        actions.add(new ActivityAlertActions(SKIP_TODAY, project, email, teamActivityURL));
        ActivityAlertActions.closedUsingX = true;

        notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeSync Daily Digest")
                .createNotification(Constants.Notification.ACTIVITY_ALERT_MESSAGE, NotificationType.INFORMATION)
                .setTitle("CodeSync Daily Digest")
                .whenExpired(() -> {
                    if(ActivityAlertActions.closedUsingX){
                        ActivityAlerts.skipToday();
                        ActivityAlerts.updateActivityAlert(SKIP_TODAY, email);
                    }
                });

        notification.addActions(actions);
        notification.notify(project);

    }

}
