package org.intellij.sdk.codesync.clients;

import kotlin.Pair;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.files.DiffFile;
import org.intellij.sdk.codesync.exceptions.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.client.utils.URIBuilder;
import org.intellij.sdk.codesync.alerts.PricingAlerts;
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

import static org.intellij.sdk.codesync.Constants.IDE_NAME;


public class CodeSyncWebSocketClient {
    URI uri;
    WebSocketClientEndpoint webSocketClientEndpoint;
    boolean isConnected = false;
    String token;

    boolean isConnected(){
        return isConnected && this.webSocketClientEndpoint.userSession != null;
    }

    public CodeSyncWebSocketClient(String token, String uri) {
        this.token = token;
        try {
            this.uri = new URIBuilder(uri).addParameter("token", token).addParameter("source", IDE_NAME).build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void connect (ConnectionHandler connectionHandler) {
        // If plan limit is reached then do not connect to the server.
        if (PricingAlerts.getPlanLimitReached()) {
            this.isConnected = false;
            connectionHandler.handleConnected(false);
            return;
        }

        if (!this.isConnected()) {
            this.webSocketClientEndpoint = new WebSocketClientEndpoint(this.uri);
            this.authenticate(isAuthenticated -> {
                this.isConnected = isAuthenticated;
                connectionHandler.handleConnected(isAuthenticated);
            });
        } else {
            try {
                connectionHandler.handleConnected(true);
            } catch (NullPointerException e)  {
                this.isConnected = false;
                throw e;
            }
        }
    }

    public void disconnect () {
        this.isConnected = false;
        try {
            this.webSocketClientEndpoint.userSession.close();
        } catch (IOException e) {
            CodeSyncLogger.error(
                String.format("Error while closing the websocket connection. %s", CommonUtils.getStackTrace(e))
            );
        }
    }

    public void authenticate(AuthenticationHandler authenticationHandler) {
        this.webSocketClientEndpoint.setMessageHandler(message -> {
            if (message.isEmpty()) {
                CodeSyncLogger.critical("Got empty response while authenticating.");
                authenticationHandler.handleAuthenticated(false);
            }

            JSONObject response;
            try {
                response = (JSONObject) JSONValue.parseWithException(message);
                Long statusCode = (Long) response.get("status");
                if (statusCode != 200) {
                    CodeSyncLogger.critical(String.format("Diff auth Failed with error: %s.", response.get("error")));
                }
                authenticationHandler.handleAuthenticated(statusCode == 200);
            } catch (org.json.simple.parser.ParseException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "Invalid JSON from server while authenticating. %s.",
                        CommonUtils.getStackTrace(error)
                    )
                );
                authenticationHandler.handleAuthenticated(false);
            } catch (ClassCastException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "Invalid JSON data from server while authenticating caused cast exception: %s.",
                        CommonUtils.getStackTrace(error)
                    )
                );
                authenticationHandler.handleAuthenticated(false);
            }
        });

        this.webSocketClientEndpoint.connectToServer();
    }

    public void sendDiff(DiffFile diffFile, Integer fileId, DataTransmissionHandler dataTransmissionHandler) throws WebSocketConnectionError {
        if (!this.isConnected()) {
            throw new WebSocketConnectionError(
                String.format("Failed to connect to the websocket endpoint at '%s'.}", this.uri.toString())
            );
        }
        System.out.printf("Sending Diff: %s.\n", diffFile.originalDiffFile.getPath());

        JSONObject diff = new JSONObject();
        diff.put("file_id", fileId);
        diff.put("diff", diffFile.diff);
        diff.put("is_deleted", diffFile.isDeleted);
        diff.put("is_rename", diffFile.isRename);
        diff.put("is_binary", diffFile.isBinary);
        diff.put("created_at", CodeSyncDateUtils.formatDate(diffFile.createdAt));
        diff.put("path", diffFile.fileRelativePath);
        diff.put("diff_file_path", diffFile.originalDiffFile.getPath());

        JSONArray diffs = new JSONArray();
        diffs.add(diff);

        JSONObject payload = new JSONObject();
        payload.put("diffs", diffs);

        this.webSocketClientEndpoint.setMessageHandler(message -> {
            if (message.isEmpty()) {
                CodeSyncLogger.error("Got empty response while sending diffs");
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            }

            JSONObject response;
            try {
                response = (JSONObject) JSONValue.parseWithException(message);
                Long statusCode = (Long) response.get("status");
                String diffFilePath = (String) response.get("diff_file_path");
                if (statusCode != 200) {
                    CodeSyncLogger.critical(String.format("Diff upload failed with error: %s.", response.get("error")));
                }
                dataTransmissionHandler.dataTransferStatusCallback(statusCode == 200, diffFilePath);
            } catch (org.json.simple.parser.ParseException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "Invalid JSON from server while sending diff file.: %s",
                        CommonUtils.getStackTrace(error)
                    )
                );
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            } catch (ClassCastException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "Invalid JSON data  from server caused cast exception: %s",
                        CommonUtils.getStackTrace(error)
                    )
                );
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            }
        });

        this.webSocketClientEndpoint.sendMessage(payload.toJSONString());
    }

    public void sendDiffs(ArrayList<Pair<Integer, DiffFile>> diffsToSend, DataTransmissionHandler dataTransmissionHandler) throws WebSocketConnectionError {
        if (!this.isConnected()) {
            throw new WebSocketConnectionError(
                String.format("Failed to connect to the websocket endpoint at '%s'.", this.uri.toString())
            );
        }
        JSONArray diffs = new JSONArray();

        for (Pair<Integer, DiffFile> diffFileEntry : diffsToSend) {
            Integer fileId = diffFileEntry.getFirst();
            DiffFile diffFile = diffFileEntry.getSecond();

            JSONObject diff = new JSONObject();
            diff.put("file_id", fileId);
            diff.put("diff", diffFile.diff);
            diff.put("is_deleted", diffFile.isDeleted);
            diff.put("is_rename", diffFile.isRename);
            diff.put("is_binary", diffFile.isBinary);
            diff.put("created_at", CodeSyncDateUtils.formatDate(diffFile.createdAt));
            diff.put("path", diffFile.fileRelativePath);
            diff.put("commit_hash", diffFile.commitHash);
            diff.put("diff_file_path", diffFile.originalDiffFile.getPath());

            diffs.add(diff);
        }

        JSONObject payload = new JSONObject();
        payload.put("diffs", diffs);

        this.webSocketClientEndpoint.setMessageHandler(message -> {
            if (message.isEmpty()) {
                CodeSyncLogger.error("Got empty response while sending diffs");
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            }

            JSONObject response;
            try {
                response = (JSONObject) JSONValue.parseWithException(message);
                Long statusCode = (Long) response.get("status");
                String diffFilePath = (String) response.get("diff_file_path");
                if (statusCode != 200) {
                    if (statusCode == Constants.ErrorCodes.PAYMENT_REQUIRED) {
                        CodeSyncLogger.error("Failed sending diff, Repo-Size Limit has been reached.");
                        PricingAlerts.setPlanLimitReached();
                    } else {
                        CodeSyncLogger.critical(String.format("Diff upload failed with error: %s.", response.get("error")));
                    }
                }
                dataTransmissionHandler.dataTransferStatusCallback(statusCode == 200, diffFilePath);
            } catch (org.json.simple.parser.ParseException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "Invalid JSON from server while sending diff file. Error: %s",
                        CommonUtils.getStackTrace(error)
                    )
                );
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            } catch (ClassCastException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "Invalid JSON data  from server caused cast exception: %s",
                        CommonUtils.getStackTrace(error)
                    )
                );
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            }
        });

        this.webSocketClientEndpoint.sendMessage(payload.toJSONString());
    }

    public static interface AuthenticationHandler {
        public void handleAuthenticated(boolean isAuthenticated);
    }

    public static interface ConnectionHandler {
        public void handleConnected(boolean isConnected);
    }

    public static interface DataTransmissionHandler {
        public void dataTransferStatusCallback(boolean successfullyTransferred, String diffFilePath);
    }
}
