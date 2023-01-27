package org.intellij.sdk.codesync.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.time.Instant;

import static org.intellij.sdk.codesync.Constants.DATE_TIME_FORMAT_WITHOUT_TIMEZONE;

/*
Utility class for handling operations on the data, usually the response from CodeSync REST API.
 */
public class DataUtils {
    /*
    Check if there is any team activity during the specified time period.
     */
    public static boolean hasActivity(JSONObject activityResponse, Instant from, Instant to) {
        if (!activityResponse.containsKey("activities")) {
            // No activity
            return false;
        }
        JSONArray activities = (JSONArray) activityResponse.get("activities");
        return activities
            .stream()
            .anyMatch(
                activity -> {
                    Instant lastSyncedAt = CodeSyncDateUtils.parseDateToInstant(
                        (String) ((JSONObject) activity).get("last_synced_at"),
                        DATE_TIME_FORMAT_WITHOUT_TIMEZONE
                    );
                    return lastSyncedAt != null && (lastSyncedAt.isAfter(from) && lastSyncedAt.isBefore(to));
                }
            );

    }
}
