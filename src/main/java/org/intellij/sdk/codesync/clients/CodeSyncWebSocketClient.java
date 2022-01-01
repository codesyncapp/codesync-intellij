package org.intellij.sdk.codesync.clients;

import kotlin.Pair;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.files.DiffFile;
import org.intellij.sdk.codesync.exceptions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.client.utils.URIBuilder;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;


public class CodeSyncWebSocketClient {
    URI uri;
    WebSocketClientEndpoint webSocketClientEndpoint;
    boolean isConnected = false;
    String token = null;

    public CodeSyncWebSocketClient(String token, String uri) {
        try {
            this.token = token;
            this.uri = new URIBuilder(uri).addParameter("token", token).build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void connect (ConnectionHandler connectionHandler) {
        if (!this.isConnected) {
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
    }

    public void authenticate(AuthenticationHandler authenticationHandler) {
        this.webSocketClientEndpoint.setMessageHandler(message -> {
            if (message.isEmpty()) {
                CodeSyncLogger.logEvent("Got empty response while authenticating.");
                authenticationHandler.handleAuthenticated(false);
            }

            JSONObject response;
            try {
                response = (JSONObject) JSONValue.parseWithException(message);
                Long statusCode = (Long) response.get("status");
                if (statusCode != 200) {
                    CodeSyncLogger.logEvent(String.format("Diff auth Failed with error: %s.", response.get("error")));
                }
                authenticationHandler.handleAuthenticated(statusCode == 200);
            } catch (org.json.simple.parser.ParseException error) {
                CodeSyncLogger.logEvent(String.format("Invalid JSON from server while authenticating. %s.", error.getMessage()));
                authenticationHandler.handleAuthenticated(false);
            } catch (ClassCastException error) {
                CodeSyncLogger.logEvent(String.format("Invalid JSON data from server while authenticating caused cast exception: %s.", error.getMessage()));
                authenticationHandler.handleAuthenticated(false);
            }
        });

        this.webSocketClientEndpoint.connectToServer();
    }

    public void sendDiff(DiffFile diffFile, Integer fileId, DataTransmissionHandler dataTransmissionHandler) throws WebSocketConnectionError {
        if (!this.isConnected) {
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
        diff.put("created_at", CommonUtils.formatDate(diffFile.createdAt));
        diff.put("path", diffFile.fileRelativePath);
        diff.put("diff_file_path", diffFile.originalDiffFile.getPath());

        JSONArray diffs = new JSONArray();
        diffs.add(diff);

        JSONObject payload = new JSONObject();
        payload.put("diffs", diffs);

        this.webSocketClientEndpoint.setMessageHandler(message -> {
            if (message.isEmpty()) {
                CodeSyncLogger.logEvent("Got empty response while sending diffs");
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            }

            JSONObject response;
            try {
                response = (JSONObject) JSONValue.parseWithException(message);
                Long statusCode = (Long) response.get("status");
                String diffFilePath = (String) response.get("diff_file_path");
                if (statusCode != 200) {
                    CodeSyncLogger.logEvent(String.format("Diff upload failed with error: %s.", response.get("error")));
                }
                dataTransmissionHandler.dataTransferStatusCallback(statusCode == 200, diffFilePath);
            } catch (org.json.simple.parser.ParseException error) {
                CodeSyncLogger.logEvent(String.format("Invalid JSON from server while sending diff file.: %s.", error.getMessage()));
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            } catch (ClassCastException error) {
                CodeSyncLogger.logEvent(String.format("Invalid JSON data  from server caused cast exception: %s.", error.getMessage()));
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            }
        });

        this.webSocketClientEndpoint.sendMessage(payload.toJSONString());
    }

    public void sendDiffs(ArrayList<Pair<Integer, DiffFile>> diffsToSend, DataTransmissionHandler dataTransmissionHandler) throws WebSocketConnectionError {
        if (!this.isConnected) {
            throw new WebSocketConnectionError(
                String.format("Failed to connect to the websocket endpoint at '%s'.}", this.uri.toString())
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
            diff.put("created_at", CommonUtils.formatDate(diffFile.createdAt));
            diff.put("path", diffFile.fileRelativePath);
            diff.put("diff_file_path", diffFile.originalDiffFile.getPath());

            diffs.add(diff);
        }

        JSONObject payload = new JSONObject();
        payload.put("diffs", diffs);

        this.webSocketClientEndpoint.setMessageHandler(message -> {
            if (message.isEmpty()) {
                CodeSyncLogger.logEvent("Got empty response while sending diffs");
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            }

            JSONObject response;
            try {
                response = (JSONObject) JSONValue.parseWithException(message);
                Long statusCode = (Long) response.get("status");
                String diffFilePath = (String) response.get("diff_file_path");
                if (statusCode != 200) {
                    CodeSyncLogger.logEvent(String.format("Diff upload failed with error: %s.", response.get("error")));
                }
                dataTransmissionHandler.dataTransferStatusCallback(statusCode == 200, diffFilePath);
            } catch (org.json.simple.parser.ParseException error) {
                CodeSyncLogger.logEvent(String.format("Invalid JSON from server while sending diff file.: %s.", error.getMessage()));
                dataTransmissionHandler.dataTransferStatusCallback(false, null);
            } catch (ClassCastException error) {
                CodeSyncLogger.logEvent(String.format("Invalid JSON data  from server caused cast exception: %s.", error.getMessage()));
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
