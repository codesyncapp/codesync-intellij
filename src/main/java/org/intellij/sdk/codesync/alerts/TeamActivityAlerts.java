package org.intellij.sdk.codesync.alerts;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.files.UserFile;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.ui.dialogs.TeamActivityAlertDialog;
import org.intellij.sdk.codesync.utils.DataUtils;
import org.json.simple.JSONObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.intellij.sdk.codesync.Constants.TEAM_ACTIVITY_ALERT_LOCK_KEY;
import static org.intellij.sdk.codesync.Constants.WEBAPP_DASHBOARD_URL;

public class TeamActivityAlerts {
    private static void acquireTeamActivityLock(Instant expiry) {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(Constants.LockFileType.PROJECT_LOCK, TEAM_ACTIVITY_ALERT_LOCK_KEY);
        pricingAlertLock.acquireLock(TEAM_ACTIVITY_ALERT_LOCK_KEY, expiry);
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
    Return `true` if user should be shown a team activity alert, `false` otherwise.
    */
    public static boolean canShowTeamActivityAlert() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(Constants.LockFileType.PROJECT_LOCK, TEAM_ACTIVITY_ALERT_LOCK_KEY);

        // If lock is acquired, then we should not show team activity alert.
        // We would only show alerts once the lock expires.
        return !pricingAlertLock.isLockAcquired(TEAM_ACTIVITY_ALERT_LOCK_KEY);
    }

    /*
        Update the lock to remind the user 15 minutes later.
    */
    public static void remindLater() {
        Instant fifteenMinutesInFuture = Instant.now().plus(15, ChronoUnit.MINUTES);
        acquireTeamActivityLock(fifteenMinutesInFuture);
    }

    /*
    Update the lock to skip the reminder for today and remind the user tomorrow.
     */
    public static void skipToday() {
        Instant reminderInstant = getTomorrowAlertInstant();
        acquireTeamActivityLock(reminderInstant);
    }

    public static void showTeamActivityAlert(Project project) {
        if (!canShowTeamActivityAlert()) {
            return;
        }

        String accessToken = UserFile.getAccessToken();
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject jsonResponse = codeSyncClient.getTeamActivity(accessToken);
        if (hasActivityInTheLastDay(jsonResponse)) {
            TeamActivityAlertDialog teamActivityAlertDialog = new TeamActivityAlertDialog(WEBAPP_DASHBOARD_URL, project);
            teamActivityAlertDialog.show();
        }
    }
}
