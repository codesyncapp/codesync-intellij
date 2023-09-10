package org.intellij.sdk.codesync.alerts;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.files.AlertsFile;
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.time.Instant;
import java.time.ZoneId;

import static org.intellij.sdk.codesync.Constants.*;

public class ActivityAlertActions extends NotificationAction {

    String actionName;
    Project project;
    String email;
    String teamActivityURL;

    public ActivityAlertActions(String actionName, Project project, String email, String teamActivityURL) {
        super(actionName);
        this.actionName = actionName;
        this.project = project;
        this.email = email;
        this.teamActivityURL = teamActivityURL;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {

        notification.expire();

        switch(actionName){
            case VIEW_ACTIVITY:
                BrowserUtil.browse(this.teamActivityURL);
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

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                boolean hasUserChecked = actionName.equals(VIEW_ACTIVITY) || actionName.equals(REMIND_LATER);
                CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] Activity alert dialog shown to the user.");
                CodeSyncLogger.debug(String.format("[CODESYNC_DAEMON] [ACTIVITY_ALERT]. User responded with '%s' status", hasUserChecked));
                Instant checkedFor;
                Instant now = CodeSyncDateUtils.getTodayInstant();

                // if activity is shown before 4 PM then it was for yesterday's activity,
                // and we need to show another notification after 4:30 PM today.
                if (now.atZone(ZoneId.systemDefault()).getHour() < 16) {
                    checkedFor = CodeSyncDateUtils.getYesterdayInstant();
                } else {
                    checkedFor = CodeSyncDateUtils.getTodayInstant();
                }
                if (email != null && hasUserChecked) {
                    CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] Updating team activity yml file.");
                    AlertsFile.updateTeamActivity(
                            email,
                            CodeSyncDateUtils.getTodayInstant(),
                            checkedFor,
                            CodeSyncDateUtils.getTodayInstant()
                    );
                }
                return null;
            }
        };
        worker.execute();
    }
}
