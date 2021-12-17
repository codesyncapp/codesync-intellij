package org.intellij.sdk.codesync;

import org.intellij.sdk.codesync.files.SequenceTokenFile;
import org.json.simple.JSONObject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.UserFile;
import static org.intellij.sdk.codesync.Constants.*;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CodeSyncLogger {
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static Integer retryCount = 0;

    private static void logMessageToCloudWatch(
            String logGroupName, String userEmail, String accessKey, String secretKey, String message
    ) {
        try
        {
            CloudWatchLogsClient logsClient = CloudWatchLogsClient.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey))
                    .build();

            String sequenceToken = null;

            try {
                SequenceTokenFile sequenceTokenFile = new SequenceTokenFile(SEQUENCE_TOKEN_FILE_PATH);
                SequenceTokenFile.SequenceToken sequenceTokenInstance = sequenceTokenFile.getSequenceToken(userEmail);
                if (sequenceTokenInstance != null) {
                    sequenceToken = sequenceTokenInstance.getTokenString();
                }
            } catch (FileNotFoundException | InvalidYmlFileError e) {
                // Ignore
            }

            // Build an input log message to put to CloudWatch.
            JSONObject msg = new JSONObject();
            msg.put("msg", message);
            msg.put("source", DIFF_SOURCE);
            InputLogEvent inputLogEvent = InputLogEvent.builder()
                    .message(msg.toJSONString())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Specify the request parameters.
            // Sequence token is required so that the log can be written to the
            // latest location in the stream.
            PutLogEventsRequest.Builder putLogEventsRequestBuilder = PutLogEventsRequest.builder()
                    .logEvents(Collections.singletonList(inputLogEvent))
                    .logGroupName(logGroupName)
                    .logStreamName(userEmail);
            if (sequenceToken != null) {
                putLogEventsRequestBuilder = putLogEventsRequestBuilder.sequenceToken(sequenceToken);
            }
            PutLogEventsRequest putLogEventsRequest = putLogEventsRequestBuilder.build();

            String nextSequenceToken;
            try {
                PutLogEventsResponse putLogEventsResponse = logsClient.putLogEvents(putLogEventsRequest);
                nextSequenceToken = putLogEventsResponse.nextSequenceToken();
            } catch (DataAlreadyAcceptedException error) {
                nextSequenceToken = error.expectedSequenceToken();
            }

            System.out.println("Successfully put CloudWatch log event");
            // Update sequence token file for other plugins and daemon
            try {
                SequenceTokenFile sequenceTokenFile = new SequenceTokenFile(SEQUENCE_TOKEN_FILE_PATH);

                // user email is the streamName.
                sequenceTokenFile.publishNewSequenceToken(userEmail, nextSequenceToken);
            } catch (FileNotFoundException | InvalidYmlFileError e) {
                // skip update to sequence file if not found;
                return;
            }
        } catch (CloudWatchException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void logEvent(String message) {
        logEvent(message, null);
    }

    public static void logEvent(String message, String userEmail) {
        LOGGER.log(Level.SEVERE, message);
        UserFile.User user = null;
        UserFile userFile = null;

        try {
            userFile = new UserFile(USER_FILE_PATH);
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            e.printStackTrace();
        }

        if (userEmail != null && userFile != null) {
            user = userFile.getUser(userEmail);
        } else if (userFile != null) {
            user = userFile.getActiveUser();
        }

        if (user == null) {
            // Can't log any thing.
            return;
        } else if (user.getAccessKey() == null || user.getSecretKey() == null || user.getUserEmail() == null) {
            // can't log anything
            return;
        }

        try {
            logMessageToCloudWatch(
                    CLIENT_LOGS_GROUP_NAME, user.getUserEmail(), user.getAccessKey(), user.getSecretKey(), message
            );
        } catch (CloudWatchException e) {
            if (retryCount > 10) {
                // Do not try more than 10 times.
                retryCount = 0;
            } else {
                // try again.
                logEvent(message, userEmail);
            }
        }
    }
}
