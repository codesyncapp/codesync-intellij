package org.intellij.sdk.codesync.clients;

import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.files.DiffFile;
import org.intellij.sdk.codesync.exceptions.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;


public class CodeSyncWebSocketClient {
    URI uri;
    WebSocketClientEndpoint webSocketClientEndpoint;
    boolean isConnected = false;

    public CodeSyncWebSocketClient(String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public CodeSyncWebSocketClient(URI uri) {
        this.uri = uri;
    }

    public void connect (String token, ConnectionHandler connectionHandler) {
        if (!this.isConnected) {
            this.webSocketClientEndpoint = new WebSocketClientEndpoint(this.uri);
            this.authenticate(token, isAuthenticated -> {
                this.isConnected = isAuthenticated;
                connectionHandler.handleConnected(isAuthenticated);
            });
        }
    }
    public void disconnect () {
        this.isConnected = false;
    }

    public void authenticate(String token, AuthenticationHandler authenticationHandler) {
        this.webSocketClientEndpoint.setMessageHandler(message -> {
            if (message.isEmpty()) {
                System.out.println("Got empty response while authenticating.");
                authenticationHandler.handleAuthenticated(false);
            }

            JSONObject response;
            try {
                response = (JSONObject) JSONValue.parseWithException(message);
                Long statusCode = (Long) response.get("status");
                if (statusCode != 200) {
                    System.out.printf("Diff auth Failed with error: %s.", response.get("error"));
                }
                authenticationHandler.handleAuthenticated(statusCode == 200);
            } catch (org.json.simple.parser.ParseException error) {
                System.out.println("Socket connection lost with server.");
                authenticationHandler.handleAuthenticated(false);
            } catch (ClassCastException error) {
                System.out.println("Invalid status code.");
                authenticationHandler.handleAuthenticated(false);
            }
        });

        this.webSocketClientEndpoint.sendMessage(token);
    }

    public void sendDiff(DiffFile diffFile, Integer fileId, DataTransmissionHandler dataTransmissionHandler) throws WebSocketConnectionError {
        if (!this.isConnected) {
            throw new WebSocketConnectionError(
                String.format("Failed to connect to the websocket endpoint at '%s'.}", this.uri.toString())
            );
        }
        JSONObject diff = new JSONObject();
        diff.put("file_id", fileId);
        diff.put("diff", diffFile.diff);
        diff.put("is_deleted", diffFile.isDeleted);
        diff.put("is_rename", diffFile.isRename);
        diff.put("is_binary", diffFile.isBinary);
        diff.put("created_at", Utils.formatDate(diffFile.createdAt));
        diff.put("path", diffFile.fileRelativePath);

        JSONArray diffs = new JSONArray();
        diffs.add(diff);

        JSONObject payload = new JSONObject();
        payload.put("diffs", diffs);

        this.webSocketClientEndpoint.setMessageHandler(message -> {
            if (message.isEmpty()) {
                System.out.println("Got empty response while authenticating diff.");
                dataTransmissionHandler.dataTransferStatusCallback(false);
            }

            JSONObject response;
            try {
                response = (JSONObject) JSONValue.parseWithException(message);
                Long statusCode = (Long) response.get("status");
                if (statusCode != 200) {
                    System.out.printf("Diff auth Failed with error: %s.", response.get("error"));
                }
                dataTransmissionHandler.dataTransferStatusCallback(statusCode == 200);
            } catch (org.json.simple.parser.ParseException error) {
                System.out.println("Invalid response from the server.");
                dataTransmissionHandler.dataTransferStatusCallback(false);
            } catch (ClassCastException error) {
                System.out.println("Invalid status code.");
                dataTransmissionHandler.dataTransferStatusCallback(false);
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
        public void dataTransferStatusCallback(boolean successfullyTransferred);
    }
}
