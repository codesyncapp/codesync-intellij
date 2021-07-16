package org.intellij.sdk.codesync;

import org.json.simple.JSONObject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;

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
            String logGroupName, String streamName, String accessKey, String secretKey, String message
    ) {
        try
        {
            CloudWatchLogsClient logsClient = CloudWatchLogsClient.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey))
                    .build();
            DescribeLogStreamsRequest logStreamRequest = DescribeLogStreamsRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamNamePrefix(streamName)
                    .build();
            DescribeLogStreamsResponse describeLogStreamsResponse = logsClient.describeLogStreams(logStreamRequest);

            // Assume that a single stream is returned since a specific stream name was specified in the previous request.
            String sequenceToken = describeLogStreamsResponse.logStreams().get(0).uploadSequenceToken();

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
            PutLogEventsRequest putLogEventsRequest = PutLogEventsRequest.builder()
                    .logEvents(Collections.singletonList(inputLogEvent))
                    .logGroupName(logGroupName)
                    .logStreamName(streamName)
                    .sequenceToken(sequenceToken)
                    .build();

            logsClient.putLogEvents(putLogEventsRequest);

            System.out.println("Successfully put CloudWatch log event");
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
            user = userFile.getUser();
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
