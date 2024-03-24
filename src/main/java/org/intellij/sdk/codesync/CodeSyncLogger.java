package org.intellij.sdk.codesync;

import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.json.simple.JSONObject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import static org.intellij.sdk.codesync.Constants.*;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;

public class CodeSyncLogger {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static void logConsoleMessage(String message) {
        logConsoleMessage(message, LogMessageType.INFO);
    }

    /*
    This will log given message to the console. It will not send the message to cloud watch.
    This is useful only with local debugging or special cases when we can get log file from the client.
     */
    public static void logConsoleMessage(String message, String level) {
        String color = null;
        switch (level) {
            case LogMessageType.CRITICAL:
            case LogMessageType.ERROR:
                color = ANSI_RED;
                break;
            case LogMessageType.WARNING:
                color = ANSI_YELLOW;
                break;
            case LogMessageType.INFO:
            case LogMessageType.DEBUG:
                color = ANSI_CYAN;
                break;
        }

        // Using null check here to use default colors according to user's theme in case color is not set.
        if (color != null) {
            System.out.printf("%s[CODESYNC] [%s] [%s]: %s%n%s", color, new Date(), level, message, ANSI_RESET);
        } else {
            System.out.printf("[CODESYNC] [%s] [%s]: %s%n", new Date(), level, message);
        }
    }

    private static Integer retryCount = 0;

    private static void logMessageToCloudWatch(
            String logGroupName, String streamName, String accessKey, String secretKey, String message, String type
    ) {
        try
        {
            CloudWatchLogsClient logsClient = CloudWatchLogsClient.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey))
                    .build();

            String version = ProjectUtils.getPluginVersion();

            // Build an input log message to put to CloudWatch.
            JSONObject msg = new JSONObject();
            msg.put("msg", message);
            msg.put("source", DIFF_SOURCE);
            msg.put("ide_name", IDE_NAME);
            msg.put("version", version);
            msg.put("type", type);
            msg.put("platform", CommonUtils.getOS());
            msg.put("mac_address", CommonUtils.getMacAddress());
            InputLogEvent inputLogEvent = InputLogEvent.builder()
                    .message(msg.toJSONString())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Specify the request parameters.
            PutLogEventsRequest.Builder putLogEventsRequestBuilder = PutLogEventsRequest.builder()
                    .logEvents(Collections.singletonList(inputLogEvent))
                    .logGroupName(logGroupName)
                    .logStreamName(streamName);
            PutLogEventsRequest putLogEventsRequest = putLogEventsRequestBuilder.build();

            try {
                logsClient.putLogEvents(putLogEventsRequest);
            } catch (SdkClientException error){
                logConsoleMessage("Network Error: " + error.getMessage(), LogMessageType.ERROR);
            }

        } catch (CloudWatchException error) {
            logConsoleMessage(
                String.format("Cloudwatch exception while logging message: '%s'", error.getMessage()),
                LogMessageType.ERROR
            );
            throw error;
        }
    }

    private static void logEvent(String message, String userEmail, String type) {
        logConsoleMessage(message, type);
        User user = null;

        try {
            if (userEmail != null) {
                user = User.getTable().find(userEmail);
            } else {
                user = User.getTable().getActive();
            }
        } catch (SQLException error) {
            logConsoleMessage(
                String.format("Error getting active user from database. Error: %s", error.getMessage()),
                LogMessageType.CRITICAL
            );
        }

        String streamName, accessKey, secretKey;

        if (user == null || user.getAccessKey() == null || user.getSecretKey() == null || user.getEmail() == null) {
            // Log with the plugin user.
            streamName = PLUGIN_USER_LOG_STREAM;
            if (PLUGIN_USER_ACCESS_KEY == null || PLUGIN_USER_SECRET_KEY == null) {
                // get directly from configuration, this will trigger config reload from the server.s
                accessKey = configuration.getPluginUserAccessKey();
                secretKey = configuration.getPluginUserSecretKey();
            } else {
                accessKey = PLUGIN_USER_ACCESS_KEY;
                secretKey = PLUGIN_USER_SECRET_KEY;
            }

            if (accessKey == null || secretKey == null) {
                logConsoleMessage(
                    "Plugin is not able communicate with the server. Please check your internet connection.",
                    LogMessageType.CRITICAL
                );
                return;
            }
        } else {
            streamName = user.getEmail();
            accessKey = user.getAccessKey();
            secretKey = user.getSecretKey();
        }

        try {
            logMessageToCloudWatch(
                CLIENT_LOGS_GROUP_NAME, streamName, accessKey, secretKey, message, type
            );
        } catch (UnrecognizedClientException error) {
            String errorMessage = String.format(
                "Error publishing message to cloudwatch. Error: %s%nOriginal Message: %s", error.getMessage(), message
            );
            if (retryCount > 10) {
                // Do not try more than 10 times.
                retryCount = 0;
                logConsoleMessage(
                    String.format("Could not log the message to cloud watch. Error: %s%n", error.getMessage()),
                    LogMessageType.CRITICAL
                );
            } else {
                retryCount += 1;

                logMessageToCloudWatch(
                    CLIENT_LOGS_GROUP_NAME,
                    PLUGIN_USER_LOG_STREAM,
                    PLUGIN_USER_ACCESS_KEY == null ? configuration.getPluginUserAccessKey(): PLUGIN_USER_ACCESS_KEY,
                    PLUGIN_USER_SECRET_KEY == null ? configuration.getPluginUserSecretKey(): PLUGIN_USER_SECRET_KEY,
                    errorMessage,
                    type
                );
            }
        } catch (CloudWatchException | CloudWatchLogsException error) {
            if (retryCount > 10) {
                // Do not try more than 10 times.
                retryCount = 0;
                logConsoleMessage(
                    String.format("Could not log the message to cloud watch. Error: %s%n", error.getMessage()),
                    LogMessageType.CRITICAL
                );
            } else {
                // try again.
                retryCount += 1;
                logEvent(message, userEmail, type);
            }
        }
    }

    public static void debug (String message, String userEmail) {
        logEvent(message, userEmail, LogMessageType.DEBUG);
    }

    public static void debug (String message) {
        debug(message, null);
    }

    public static void info (String message, String userEmail) {
        logEvent(message, userEmail, LogMessageType.INFO);
    }

    public static void info (String message) {
        info(message, null);
    }

    public static void warning (String message, String userEmail) {
        logEvent(message, userEmail, LogMessageType.WARNING);
    }

    public static void warning (String message) {
        warning(message, null);
    }

    public static void error (String message, String userEmail) {
        logEvent(message, userEmail, LogMessageType.ERROR);
    }

    public static void error (String message) {
        error(message, null);
    }

    public static void critical (String message, String userEmail) {
        logEvent(message, userEmail, LogMessageType.CRITICAL);
    }

    public static void critical (String message) {
        critical(message, null);
    }
}
