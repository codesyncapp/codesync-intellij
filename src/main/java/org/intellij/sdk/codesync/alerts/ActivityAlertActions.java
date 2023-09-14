package org.intellij.sdk.codesync.alerts;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static org.intellij.sdk.codesync.Constants.*;

public class ActivityAlertActions extends NotificationAction {

    String actionName;
    Project project;
    String email;
    String teamActivityURL;

    // Intellij does not have event for closing notification using X button.
    // We only have callback for when notification gets expired, it can be either through
    // programmatically or x button. To differentiate that we are using this toggle.
    static boolean closedUsingX = true;

    public ActivityAlertActions(String actionName, Project project, String email, String teamActivityURL) {
        super(actionName);
        this.actionName = actionName;
        this.project = project;
        this.email = email;
        this.teamActivityURL = teamActivityURL;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
        switch(actionName){
            case VIEW_ACTIVITY:
                BrowserUtil.browse(teamActivityURL);
                // User has already been redirected to the activity page, we can now skip subsequent alerts.
                ActivityAlerts.skipToday();
                break;
            case REMIND_LATER:
                ActivityAlerts.remindLater();
                break;
            case SKIP_TODAY:
                ActivityAlerts.skipToday();
                break;
            default:
                return;
        }

        ActivityAlerts.updateActivityAlert(actionName, email);
        closedUsingX = false;
        notification.expire();

    }
}
