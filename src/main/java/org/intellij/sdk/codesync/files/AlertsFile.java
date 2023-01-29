package org.intellij.sdk.codesync.files;

import org.apache.commons.lang.time.DateUtils;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.FileLockedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.*;

/*
This file is mainly for compatibility with VSCode plugin.
at the moment we are not reading from this file in the intelliJ plugin. This may change in the future and we will
update this message then.

We can have the following data in alerts.yml
    team_activity:
        userEmail:
            checked_for: "2022-07-05"
            shown_at_intellij: 2023-01-28 14:08:32.191 UTC
 */
public class AlertsFile extends CodeSyncYmlFile{
    File alertsFile;
    public Map<String, Object> contentsMap;
    @Override
    public File getYmlFile() {
        return this.alertsFile;
    }

    @Override
    public Map<String, Object> getYMLAsHashMap() {
        return contentsMap;
    }

    public AlertsFile (String filePath) throws FileNotFoundException, InvalidYmlFileError {
        File alertsFile = new File(filePath);

        if (!alertsFile.isFile()) {
            throw new FileNotFoundException(String.format("Alerts file \"%s\" does not exist.", filePath));
        }
        this.alertsFile = alertsFile;
        this.contentsMap = this.readYml();
    }

    private static AlertsFile getInstance() {
        try {
            return new AlertsFile(ALERTS_FILE_PATH);
        } catch (InvalidYmlFileError | FileNotFoundException error) {
            CodeSyncLogger.error(
                String.format(
                    "Error while instantiating AlertsFile instance. Error: %s",
                    error.getMessage()
                )
            );
        }
        return null;
    }

    public static void updateUserActivity(Instant activityInstant) {
        AlertsFile alertsFile = getInstance();
        if (alertsFile == null) {
            return;
        }
        alertsFile.contentsMap.put(
            "user_activity", CodeSyncDateUtils.formatDate(Date.from(activityInstant), DATE_TIME_FORMAT_WITHOUT_TIMEZONE)
        );

        try {
            alertsFile.writeYml();
        } catch (FileNotFoundException | FileLockedError error) {
            CodeSyncLogger.error(String.format("Error while writing to alerts.yml file. Error: %s", error.getMessage()));
        } catch (InvalidYmlFileError error) {
            CodeSyncLogger.error(
                String.format(
                    "Error while writing to alerts.yml file, removing file contents to avoid further errors. Error: %s",
                    error.getMessage()
                )
            );
            removeFileContents(alertsFile.getYmlFile());
        }
    }

    public static void updateUpgradePlanActivity(Instant activityInstant) {
        AlertsFile alertsFile = getInstance();
        if (alertsFile == null) {
            return;
        }
        alertsFile.contentsMap.put(
            "upgrade_plan", CodeSyncDateUtils.formatDate(Date.from(activityInstant), DATE_TIME_FORMAT)
        );

        try {
            alertsFile.writeYml();
        } catch (FileNotFoundException | FileLockedError error) {
            CodeSyncLogger.error(String.format("Error while writing to alerts.yml file. Error: %s", error.getMessage()));
        } catch (InvalidYmlFileError error) {
            CodeSyncLogger.error(
                String.format(
                    "Error while writing to alerts.yml file, removing file contents to avoid further errors. Error: %s",
                    error.getMessage()
                )
            );
            removeFileContents(alertsFile.getYmlFile());
        }
    }

    public static boolean isTeamActivityAlreadyShown(String userEmail) {
        AlertsFile alertsFile = getInstance();
        if (alertsFile == null) {
            return false;
        }
        Map<String, Object> teamActivity = null;
        if (alertsFile.contentsMap.containsKey("team_activity")) {
            teamActivity = (Map<String, Object>)  alertsFile.contentsMap.get("team_activity");
        }

        if (teamActivity == null) {
            return false;
        }

        Map<String, String> userActivityDetails = (Map<String, String>) teamActivity.get(userEmail);

        if (userActivityDetails == null) {
            return false;
        }

        Instant checkedForInstant = CodeSyncDateUtils.parseDateToInstant(
            userActivityDetails.get("checked_for"),
            DATE_FORMAT
        );

        if (checkedForInstant != null) {
            Instant todayInstant = CodeSyncDateUtils.getTodayInstant();
            // Truncate time information.
            todayInstant = DateUtils.truncate(Date.from(todayInstant), Calendar.DATE).toInstant();

            // Return true if alert was checked either yesterday or after that
            return checkedForInstant.equals(todayInstant);
        }

        return false;
    }

    public static void updateTeamActivity(String userEmail, Instant checkedAt, Instant checkedFor, Instant shownAt ) {
        AlertsFile alertsFile = getInstance();
        if (alertsFile == null) {
            return;
        }
        Map<String, String> alertDetails = new HashMap<>();
        String newCheckedAt = null;
        Map<String, Object> teamActivity = null;
        if (alertsFile.contentsMap.containsKey("team_activity")) {
            teamActivity = (Map<String, Object>)  alertsFile.contentsMap.get("team_activity");
        }
        if (teamActivity == null) {
            teamActivity = new HashMap<>();
        }

        if (checkedAt == null) {

            if (alertsFile.contentsMap.containsKey(userEmail)) {
                newCheckedAt = (String) alertsFile.contentsMap.get("checked_at");
            }
            alertDetails.put("checked_at", newCheckedAt);
        } else {
            alertDetails.put("checked_at", CodeSyncDateUtils.formatDate(Date.from(checkedAt), DATE_FORMAT));
        }
        alertDetails.put("checked_for", CodeSyncDateUtils.formatDate(Date.from(checkedFor), DATE_FORMAT));
        alertDetails.put("shown_at_intellij", CodeSyncDateUtils.formatDate(Date.from(shownAt), DATE_TIME_FORMAT));

        teamActivity.put(userEmail, alertDetails);
        alertsFile.contentsMap.put("team_activity", teamActivity);

        try {
            alertsFile.writeYml();
        } catch (FileNotFoundException | FileLockedError error) {
            CodeSyncLogger.error(String.format("Error while writing to alerts.yml file. Error: %s", error.getMessage()));
        } catch (InvalidYmlFileError error) {
            CodeSyncLogger.error(
                String.format(
                    "Error while writing to alerts.yml file, removing file contents to avoid further errors. Error: %s",
                    error.getMessage()
                )
            );
            removeFileContents(alertsFile.getYmlFile());
        }
    }
}
