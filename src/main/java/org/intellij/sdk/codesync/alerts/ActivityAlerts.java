package org.intellij.sdk.codesync.alerts;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.files.AlertsFile;
import org.intellij.sdk.codesync.database.models.UserAccount;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.DataUtils;
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

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
        Instant today = CodeSyncDateUtils.getTodayInstant(16, 30, 0);
        Instant yesterday = CodeSyncDateUtils.getYesterdayInstant();

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
        remindLater(15);
    }

    /*
        Update the lock to remind the user 15 minutes later.
    */
    public static void remindLater(long waitTimeInMinutes) {
        Instant fifteenMinutesInFuture = Instant.now().plus(waitTimeInMinutes, ChronoUnit.MINUTES);
        acquireActivityLock(fifteenMinutesInFuture);
    }

    /*
    Update the lock to skip the reminder for today and remind the user tomorrow.
     */
    public static void skipToday() {
        Instant now = CodeSyncDateUtils.getTodayInstant();
        Instant reminderInstant = CodeSyncDateUtils.getTomorrowAlertInstant();

        // if activity is shown before 4 PM then it was for yesterday's activity,
        // and we need to show another notification after 4:30 PM today only if there has been an activity in past 24h.
        if (now.atZone(ZoneId.systemDefault()).getHour() < 16) {
            reminderInstant = CodeSyncDateUtils.getTodayInstant(16, 30, 0);
        }
        acquireActivityLock(reminderInstant);
    }

    public static void showActivityAlert(Project project) {
        if (!canShowActivityAlert()) {
            return;
        }
        CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] Activity alert lock acquired.");

        String accessToken = UserAccount.getAccessTokenByEmail();
        String email = UserAccount.getEmail();

        if (accessToken == null) {
            ActivityAlerts.remindLater(5);
            CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] Access token is null, waiting for 5 minutes.");
            return;
        }

        // If team activity is already shown in other IDE then no need to proceed.
        if (AlertsFile.isTeamActivityAlreadyShown(email)) {
            CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] Alert is already shown to the user by some other IDE.");
            skipToday();
            return;
        }

        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject jsonResponse = codeSyncClient.getTeamActivity(accessToken);

        if (jsonResponse == null) {
            ActivityAlerts.remindLater(5);
            CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] null response returned by the server, waiting 5 minutes.");
            return;
        }

        if (hasActivityInTheLastDay(jsonResponse)) {
            CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] Showing activity alert dialog to the user.");
            ActivityAlertNotification.showAlert(WEBAPP_DASHBOARD_URL, project, email);
        } else {
            CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] User does not have any activity so skipping today.");

            // No activity today, so skip today.
            ActivityAlerts.skipToday();

            Instant today = CodeSyncDateUtils.getTodayInstant();
            AlertsFile.updateTeamActivity(email, today, today, today);
        }
        CodeSyncLogger.debug("[CODESYNC_DAEMON] [ACTIVITY_ALERT] exiting activity alert logic.");
    }
    public static void startActivityAlertDaemon(Project project) {
        Timer timer = new Timer(true);
        CodeSyncLogger.debug("[CODESYNC_DAEMON]: Starting activity alert daemon.");
        activityDaemon(timer, project);
    }

    public static void updateActivityAlert(String actionName, String email){
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                boolean hasUserChecked = actionName.equals(VIEW_ACTIVITY) || actionName.equals(SKIP_TODAY);
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

    /*
    Schedule activity alert daemon, daemon will make sure to run only once every 5 seconds.
    */
    private static void activityDaemon(final Timer timer, Project project) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    boolean canRunDaemon = ProjectUtils.canRunDaemon(
                        LockFileType.PROJECT_LOCK,
                        ACTIVITY_ALERT_DAEMON_LOCK_KEY,
                        project.getName()
                    );

                    if (canRunDaemon && !StateUtils.getGlobalState().isAccountDeactivated) {
                        CodeSyncLogger.debug("[CODESYNC_DAEMON]: showActivityAlert called.");
                        showActivityAlert(project);
                    }
                } catch (Exception e) {
                    System.out.printf("Error Running the activity alert daemon. Error: %s%n", e.getMessage());
                }

                activityDaemon(timer, project);
            }
        }, DELAY_BETWEEN_ACTIVITY_ALERT_TASKS);
    }

}
