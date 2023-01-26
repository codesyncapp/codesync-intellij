package org.intellij.sdk.codesync.alerts;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.files.AlertsFile;
import org.intellij.sdk.codesync.files.UserFile;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.ui.dialogs.ActivityAlertDialog;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.DataUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.json.simple.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.intellij.sdk.codesync.Constants.*;

public class ActivityAlerts {
    private static void acquireActivityLock(Instant expiry) {
        CodeSyncLock activityAlertLock = new CodeSyncLock(Constants.LockFileType.PROJECT_LOCK, ACTIVITY_ALERT_LOCK_KEY);
        activityAlertLock.acquireLock(expiry);
    }

    /*
    Checks if user has had any activity from 4:30 PM yesterday to 4:30 PM today.
    */
    private static boolean hasActivityInTheLastDay(JSONObject jsonResponse) {
        Instant today = CommonUtils.getTodayInstant(16, 30, 0);
        Instant yesterday = CommonUtils.getYesterdayInstant();

        return DataUtils.hasActivity(jsonResponse, yesterday, today);
    }

    /*
    Return `true` if user should be shown an activity alert, `false` otherwise.
    */
    public static boolean canShowActivityAlert() {
        CodeSyncLock activityAlertLock = new CodeSyncLock(Constants.LockFileType.PROJECT_LOCK, ACTIVITY_ALERT_LOCK_KEY);

        // If lock is acquired, then we should not show activity alert.
        // We would only show alerts once the lock expires.
        return !activityAlertLock.isLockAcquired();
    }

    /*
        Update the lock to remind the user 15 minutes later.
    */
    public static void remindLater() {
        Instant fifteenMinutesInFuture = Instant.now().plus(15, ChronoUnit.MINUTES);
        acquireActivityLock(fifteenMinutesInFuture);
    }

    /*
    Update the lock to skip the reminder for today and remind the user tomorrow.
     */
    public static void skipToday() {
        Instant now = CommonUtils.getTodayInstant();
        Instant reminderInstant = CommonUtils.getTomorrowAlertInstant();

        // if activity is shown before 4 PM then it was for yesterday's activity,
        // and we need to show another notification after 4:30 PM today only if there has been an activity in past 24h.
        if (now.atZone(ZoneId.systemDefault()).getHour() < 16) {
            reminderInstant = CommonUtils.getTodayInstant(16, 30, 0);
        }
        acquireActivityLock(reminderInstant);
    }

    public static void showActivityAlert(Project project) {
        if (!canShowActivityAlert()) {
            return;
        }

        String accessToken = UserFile.getAccessToken();
        String email = UserFile.getEmail();

        if (accessToken == null) {
            return;
        }

        // If team activity is already shown in other IDE then no need to proceed.
        if (AlertsFile.isTeamActivityAlreadyShown(email)) {
            skipToday();
            return;
        }

        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject jsonResponse = codeSyncClient.getTeamActivity(accessToken);

        if (jsonResponse == null) {
            return;
        }

        if (!hasActivityInTheLastDay(jsonResponse)) {
            boolean isTeamActivity = jsonResponse.containsKey("is_team_activity") &&
                (boolean) jsonResponse.get("is_team_activity");

            ActivityAlertDialog activityAlertDialog = new ActivityAlertDialog(
                WEBAPP_DASHBOARD_URL, isTeamActivity, project
            );
            boolean hasUserChecked = activityAlertDialog.showAndGet();
            Instant checkedFor;
            Instant now = CommonUtils.getTodayInstant();

            // if activity is shown before 4 PM then it was for yesterday's activity,
            // and we need to show another notification after 4:30 PM today.
            if (now.atZone(ZoneId.systemDefault()).getHour() < 16) {
                checkedFor = CommonUtils.getYesterdayInstant();
            } else {
                checkedFor = CommonUtils.getTodayInstant();
            }
            if (email != null && hasUserChecked) {
                AlertsFile.updateTeamActivity(
                    email,
                    CommonUtils.getTodayInstant(),
                    checkedFor,
                    CommonUtils.getTodayInstant()
                );
            }
        }
    }

    /*
    Schedule activity alert daemon, daemon will make sure to run only once every 5 seconds.
    */
    public static void startActivityAlertDaemon(Project project) {
        ProjectUtils.startDaemonProcess(() -> {
            boolean canRunDaemon = ProjectUtils.canRunDaemon(
                LockFileType.PROJECT_LOCK,
                ACTIVITY_ALERT_DAEMON_LOCK_KEY,
                project.getName()
            );

            if (canRunDaemon) {
                // Start the daemon.
                ProjectUtils.startDaemonProcess(() -> showActivityAlert(project));
            }
        });
    }
}
