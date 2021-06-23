package org.intellij.sdk.codesync.clients;

import org.intellij.sdk.codesync.exceptions.WebSocketConnectionError;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.DiffFile;
import static org.intellij.sdk.codesync.Constants.*;


public class CodeSyncClient {
    public CodeSyncClient() {

    }

    public Boolean isServerUp() {
        return true;
    }

    public CodeSyncWebSocketClient connectWebSocket() throws WebSocketConnectionError {
        return new CodeSyncWebSocketClient(WEBSOCKET_ENDPOINT);
    }

    public Integer uploadFile(ConfigRepo configRepo, DiffFile diffFile)  {
        return 1;
    }
}
