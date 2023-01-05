package org.intellij.sdk.codesync.alerts;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.files.UserFile;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.ui.dialogs.ActivityAlertDialog;
import org.intellij.sdk.codesync.utils.DataUtils;
import org.json.simple.JSONObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.intellij.sdk.codesync.Constants.ACTIVITY_ALERT_LOCK_KEY;
import static org.intellij.sdk.codesync.Constants.WEBAPP_DASHBOARD_URL;

public class ActivityAlerts {
    private static void acquireActivityLock(Instant expiry) {
        CodeSyncLock activityAlertLock = new CodeSyncLock(Constants.LockFileType.PROJECT_LOCK, ACTIVITY_ALERT_LOCK_KEY);
        activityAlertLock.acquireLock(ACTIVITY_ALERT_LOCK_KEY, expiry);
    }

    /*
        Return an Instant for 4:30 PM for tomorrow (tomorrow here means 1 day in the future of the time when this function is called.)
        Instant would be in the local time zone.
    */
    private static Instant getTomorrowAlertInstant() {
        ZoneId timeZone = ZoneId.systemDefault();
        LocalDateTime reminderDateTime = LocalDateTime.now(timeZone)
            .withHour(16)
            .withMinute(30)
            .withSecond(0)
            .plusDays(1);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(reminderDateTime, timeZone);
        return zonedDateTime.toInstant();
    }
    private static boolean hasActivityInTheLastDay(JSONObject jsonResponse) {
        ZoneId timeZone = ZoneId.systemDefault();
        LocalDateTime yesterdayDateTime = LocalDateTime.now(timeZone)
            .withHour(16)
            .withMinute(30)
            .withSecond(0)
            .minusDays(1);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(yesterdayDateTime, timeZone);
        ZonedDateTime now = ZonedDateTime.now(timeZone);

        Instant yesterday = zonedDateTime.toInstant();

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
        Instant reminderInstant = getTomorrowAlertInstant();
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
            activityAlertDialog.show();
        }
    }
}
