package org.intellij.sdk.codesync.clients;

import org.intellij.sdk.codesync.exceptions.WebSocketConnectionError;


public class CodeSyncClient {
    public CodeSyncClient() {

    }

    public Boolean isServerUp() {
        return true;
    }

    public WebSocketClient connectWebSocket() throws WebSocketConnectionError {
        return new WebSocketClient();
    }
}
