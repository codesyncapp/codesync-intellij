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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.intellij.sdk.codesync.Constants.*;

public class ActivityAlerts {
    private static void acquireActivityLock(Instant expiry) {
        CodeSyncLock activityAlertLock = new CodeSyncLock(Constants.LockFileType.PROJECT_LOCK, ACTIVITY_ALERT_LOCK_KEY);
        activityAlertLock.acquireLock(ACTIVITY_ALERT_LOCK_KEY, expiry);
    }

    private static boolean hasActivityInTheLastDay(JSONObject jsonResponse) {
        ZoneId timeZone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(timeZone);
        Instant yesterday = CommonUtils.getYesterdayInstant();

        return DataUtils.hasActivity(jsonResponse, yesterday, now.toInstant());
    }

    /*
    Return `true` if user should be shown an activity alert, `false` otherwise.
    */
    public static boolean canShowActivityAlert() {
        CodeSyncLock activityAlertLock = new CodeSyncLock(Constants.LockFileType.PROJECT_LOCK, ACTIVITY_ALERT_LOCK_KEY);

        // If lock is acquired, then we should not show activity alert.
        // We would only show alerts once the lock expires.
        return !activityAlertLock.isLockAcquired(ACTIVITY_ALERT_LOCK_KEY);
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
        Instant reminderInstant = CommonUtils.getTomorrowAlertInstant();
        acquireActivityLock(reminderInstant);
    }

    public static void showActivityAlert(Project project) {
        if (!canShowActivityAlert()) {
            return;
        }

        String accessToken = UserFile.getAccessToken();
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject jsonResponse = codeSyncClient.getTeamActivity(accessToken);
        if (hasActivityInTheLastDay(jsonResponse)) {
            boolean isTeamActivity = jsonResponse.containsKey("is_team_activity") &&
                (boolean) jsonResponse.get("is_team_activity");

            ActivityAlertDialog activityAlertDialog = new ActivityAlertDialog(
                WEBAPP_DASHBOARD_URL, isTeamActivity, project
            );
            boolean hasUserChecked = activityAlertDialog.showAndGet();
            if (isTeamActivity) {
                String email = UserFile.getEmail();
                if (email != null) {
                    AlertsFile.updateTeamActivity(
                        email,
                        hasUserChecked ? CommonUtils.getTodayInstant(): null,
                        CommonUtils.getYesterdayInstant(),
                        CommonUtils.getTodayInstant()
                    );
                }
            } else {
                AlertsFile.updateUserActivity(CommonUtils.getTodayInstant());
            }
        }
    }

    public static void startActivityAlertDaemon(Project project) {
        ProjectUtils.startDaemonProcess(() -> showActivityAlert(project));
    }
}
