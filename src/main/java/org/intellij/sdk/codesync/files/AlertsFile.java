package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.FileLockedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
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
            checked_at: "2022-07-05"
            checked_for: "2022-07-05"
            shown_at: 2022-07-05 16:30:27.210
    user_activity: '2022-07-05 16:30:27.210'
    upgrade_plan: '2022-07-05 21:51:27.210'
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
            "user_activity", CommonUtils.formatDate(Date.from(activityInstant), DATE_TIME_FORMAT_WITHOUT_TIMEZONE)
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
            alertsFile.removeFileContents();
        }
    }

    public static void updateUpgradePlanActivity(Instant activityInstant) {
        AlertsFile alertsFile = getInstance();
        if (alertsFile == null) {
            return;
        }
        alertsFile.contentsMap.put(
            "upgrade_plan", CommonUtils.formatDate(Date.from(activityInstant), DATE_TIME_FORMAT_WITHOUT_TIMEZONE)
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
            alertsFile.removeFileContents();
        }
    }
    public static void updateTeamActivity(String userEmail, Instant checkedAt, Instant checkedFor, Instant shownAt) {
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
            alertDetails.put("checked_at", CommonUtils.formatDate(Date.from(checkedAt), DATE_FORMAT));
        }
        alertDetails.put("checked_for", CommonUtils.formatDate(Date.from(checkedFor), DATE_FORMAT));
        alertDetails.put("shown_at", CommonUtils.formatDate(Date.from(shownAt), DATE_TIME_FORMAT_WITHOUT_TIMEZONE));

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
            alertsFile.removeFileContents();
        }
    }
}
